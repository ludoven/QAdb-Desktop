package com.ludoven.adbtool.agent

import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CancellationException

class AgentOrchestrator(
    private val modelClient: AgentModelClient,
    private val deviceGateway: AgentDeviceGateway,
    private val maxActions: Int = DEFAULT_MAX_ACTIONS,
    private val taskBudget: AgentBudget = AgentBudget(),
    private val riskEvaluator: AgentRiskEvaluator = AgentRiskEvaluator(),
    private val memoryPreferences: AgentMemoryPreferences = AgentMemoryRuntime.preferences,
    private val memoryStoreProvider: () -> AgentMemoryStore = { AgentMemoryRuntime.store },
    private val taskLogStoreProvider: () -> AgentTaskLogStore = { AgentTaskLogRuntime.store },
    private val workflowStoreProvider: () -> AgentWorkflowStore = { AgentWorkflowRuntime.store }
) {
    suspend fun run(
        task: String,
        deviceId: String,
        config: AiModelConfig,
        apiKey: String,
        taskSource: AgentTaskSource = AgentTaskSource.NATURAL_LANGUAGE,
        initialState: AgentTaskUiState = AgentTaskUiState(),
        onState: (AgentTaskUiState) -> Unit,
        confirmSensitiveAction: suspend (AgentStep) -> Boolean
    ): AgentTaskUiState {
        val taskLogStore = runCatching { taskLogStoreProvider() }.getOrElse { NoopAgentTaskLogStore }
        val taskLogRunId = newId()
        val publishState: (AgentTaskUiState) -> Unit = { snapshot ->
            runCatching { taskLogStore.record(taskLogRunId, snapshot) }
            onState(snapshot)
        }
        val memoryEnabled = memoryPreferences.enabled.value
        val contextManager = AgentContextManager(
            memoryStore = if (memoryEnabled) memoryStoreProvider() else null,
            memoryEnabled = memoryEnabled
        )
        var state = initialState.copy(
            messages = initialState.messages + AgentMessage(
                id = newId(),
                role = AgentMessageRole.USER,
                text = task
            ),
            steps = emptyList(),
            isRunning = true,
            boundDeviceId = deviceId,
            pendingConfirmation = null,
            errorMessage = null,
            phase = AgentRunPhase.OBSERVING,
            usage = AgentUsage(),
            memoryEnabled = memoryEnabled,
            memoryHitCount = 0,
            savedMemoryCount = 0,
            compactionCount = 0
        )
        var visionAvailable = config.visionMode != VisionMode.DISABLED
        var contextSnapshot: AgentContextSnapshot? = null
        var targetPackage: String? = null
        var appScopedMemoryLoaded = false
        val loopGuard = AgentLoopGuard()
        val planExecutor = AgentPlanExecutor(deviceGateway, riskEvaluator)
        val budgetTracker = AgentBudgetTracker(taskBudget)
        val taskMode = task.classifyMode()
        var missingElementRecoveryCount = 0
        val workflowEvidence = mutableListOf<WorkflowRecordedStep>()
        var appKnowledgeContext = ""
        state = state.copy(budgetStatus = budgetTracker.current())
        runCatching { taskLogStore.start(taskLogRunId, task, state) }
        publishState(state)

        try {
            if (taskMode != AgentTaskMode.INTERACTIVE) {
                val observation = when (taskMode) {
                    AgentTaskMode.CONVERSATIONAL -> conversationObservation()
                    AgentTaskMode.READ_ONLY_DEVICE -> observeLightweightSafely(
                        deviceId = deviceId,
                        includeUiHierarchy = true
                    ) ?: throw AgentException("Unable to read the device overview")
                    AgentTaskMode.INTERACTIVE -> error("Interactive tasks are handled by the execution loop")
                }
                val deviceState = if (taskMode == AgentTaskMode.READ_ONLY_DEVICE) {
                    PageSignatureEngine.state(observation)
                } else {
                    null
                }
                state = state.copy(
                    observationMode = AgentObservationMode.TEXT_ONLY,
                    phase = AgentRunPhase.THINKING,
                    deviceState = deviceState,
                    pageDiff = deviceState?.let { PageSignatureEngine.diff(state.deviceState, it) },
                    budgetStatus = budgetTracker.current(),
                    executionDetails = (
                        state.executionDetails + when (taskMode) {
                            AgentTaskMode.CONVERSATIONAL -> "Conversational request: device observation and screenshot skipped"
                            AgentTaskMode.READ_ONLY_DEVICE -> "Read-only device analysis: UI hierarchy used; screenshot and device actions skipped"
                            AgentTaskMode.INTERACTIVE -> ""
                        }
                    ).takeLast(40)
                )
                publishState(state)
                if (!budgetTracker.canRequest(includeVision = false, isReplan = false)) {
                    throw AgentException(budgetTracker.current().stopReason ?: "Model budget reached")
                }
                val decision = requestDecision(
                    config = config,
                    apiKey = apiKey,
                    context = AgentModelContext(task = task, observation = observation, completedSteps = emptyList()),
                    includeScreenshot = false,
                    finalResponseOnly = true,
                    onVisionFallback = {}
                )
                state = state.copy(
                    usage = state.usage + decision.usage,
                    budgetStatus = budgetTracker.record(decision.usage, usedVision = false),
                    phase = AgentRunPhase.THINKING
                )
                val finish = decision.action as? AgentAction.Finish
                    ?: throw ModelProtocolException("Read-only tasks may only return a finish result; no device action was executed")
                if (finish.outcome == AgentFinishOutcome.BLOCKED) {
                    val message = "Task blocked: ${finish.summary}"
                    state = state.copy(
                        messages = state.messages + AgentMessage(id = newId(), role = AgentMessageRole.SYSTEM, text = message),
                        isRunning = false,
                        phase = AgentRunPhase.FAILED,
                        errorMessage = message
                    )
                } else {
                    state = state.copy(
                        messages = state.messages + AgentMessage(id = newId(), role = AgentMessageRole.ASSISTANT, text = finish.summary),
                        isRunning = false,
                        phase = AgentRunPhase.COMPLETED
                    )
                }
                publishState(state)
                return state
            }

            val workflowStore = runCatching { workflowStoreProvider() }.getOrNull()
            val workflowStart = System.currentTimeMillis()
            val workflowObservation = observeLightweightSafely(deviceId, includeUiHierarchy = true)
            val workflowState = workflowObservation?.let(PageSignatureEngine::state)
            val workflow = workflowState?.let { current -> workflowStore?.findEnabled(task, current) }
            if (workflow != null) {
                val replay = GuardedWorkflowExecutor(deviceGateway, riskEvaluator).execute(
                    workflow = workflow,
                    deviceId = deviceId,
                    initialState = state.copy(deviceState = workflowState),
                    onState = publishState,
                    confirmSensitiveAction = confirmSensitiveAction
                )
                state = replay.state
                workflowStore?.recordResult(workflow.id, replay.completed, System.currentTimeMillis() - workflowStart)
                if (replay.completed) {
                    return completePlannedTask(state, "Workflow completed", publishState)
                }
                state = state.withPlanningNotice("Workflow stopped safely: ${replay.failure}. Switching to adaptive planning.")
                publishState(state)
            }

            AgentTaskTemplateMatcher.match(task)
                ?.takeIf { taskSource == AgentTaskSource.DIRECT_TEMPLATE }
                ?.let { template ->
                val execution = planExecutor.execute(
                    plan = template,
                    deviceId = deviceId,
                    initialState = state,
                    onState = publishState,
                    confirmSensitiveAction = confirmSensitiveAction,
                    strategy = AgentExecutionStrategy.FAST_TEMPLATE,
                    maxSteps = TEMPLATE_MAX_STEPS
                )
                state = execution.state
                if (execution.completed) return completePlannedTask(state, template.summary, publishState)
                state = state.withPlanningNotice(
                    "Fast path verification failed: ${execution.failure}. Switching to adaptive planning."
                )
                publishState(state)
            }

            val planningObservation = observeLightweightSafely(deviceId)
            if (planningObservation != null) {
                if (!budgetTracker.canRequest(includeVision = false, isReplan = false)) {
                    throw AgentException(budgetTracker.current().stopReason ?: "Model budget reached")
                }
                val planned = requestPlanSafely(
                    config = config,
                    apiKey = apiKey,
                    task = task,
                    observation = planningObservation
                )
                if (planned != null) {
                    state = state.copy(
                        usage = state.usage + planned.usage,
                        budgetStatus = budgetTracker.record(planned.usage, usedVision = false),
                        phase = AgentRunPhase.THINKING
                    )
                    publishState(state)
                    if (planned.plan.mode == AgentPlanMode.BATCH) {
                        val execution = planExecutor.execute(
                            plan = planned.plan,
                            deviceId = deviceId,
                            initialState = state,
                            onState = publishState,
                            confirmSensitiveAction = confirmSensitiveAction,
                            strategy = AgentExecutionStrategy.BATCH_PLAN,
                            maxSteps = BATCH_MAX_STEPS
                        )
                        state = execution.state
                        if (execution.completed) return completePlannedTask(state, planned.plan.summary, publishState)

                        val repairObservation = observeLightweightSafely(deviceId)
                        val repair = repairObservation?.let {
                            if (budgetTracker.canRequest(includeVision = false, isReplan = true)) requestRepairSafely(
                                config = config,
                                apiKey = apiKey,
                                task = task,
                                observation = it,
                                completedSteps = state.steps,
                                failure = execution.failure
                            ) else null
                        }
                        if (repair?.plan?.mode == AgentPlanMode.BATCH) {
                            state = state.copy(
                                usage = state.usage + repair.usage,
                                budgetStatus = budgetTracker.record(repair.usage, usedVision = false, isReplan = true),
                                phase = AgentRunPhase.THINKING
                            )
                            publishState(state)
                            val repaired = planExecutor.execute(
                                plan = repair.plan,
                                deviceId = deviceId,
                                initialState = state,
                                onState = publishState,
                                confirmSensitiveAction = confirmSensitiveAction,
                                strategy = AgentExecutionStrategy.REPAIR_PLAN,
                                maxSteps = REPAIR_MAX_STEPS
                            )
                            state = repaired.state
                            if (repaired.completed) return completePlannedTask(state, repair.plan.summary, publishState)
                            state = state.withPlanningNotice(
                                "Batch repair failed: ${repaired.failure}. Switching to adaptive execution."
                            )
                        } else {
                            state = state.withPlanningNotice(
                                "Batch plan failed: ${execution.failure}. Switching to adaptive execution."
                            )
                        }
                        publishState(state)
                    } else {
                        state = state.copy(executionStrategy = AgentExecutionStrategy.INTERACTIVE)
                        publishState(state)
                    }
                }
            }
            repeat(maxActions) {
                if (!deviceGateway.isConnected(deviceId)) {
                    throw AgentException("The selected device is no longer connected")
                }
                state = state.copy(phase = AgentRunPhase.OBSERVING)
                publishState(state)
                val observation = deviceGateway.observe(deviceId)
                val nextDeviceState = PageSignatureEngine.state(observation)
                val pageDiff = PageSignatureEngine.diff(state.deviceState, nextDeviceState)
                val canSendScreenshot = visionAvailable &&
                    config.visionMode != VisionMode.DISABLED &&
                    (config.visionMode == VisionMode.ENABLED || task.shouldUseVisionAutomatically(observation)) &&
                    observation.screenshotPng != null &&
                    budgetTracker.current().mode !in setOf(AgentBudgetMode.NO_OPTIONAL_VISION, AgentBudgetMode.FINAL_RECOVERY_ONLY, AgentBudgetMode.EXHAUSTED) &&
                    budgetTracker.canRequest(includeVision = true, isReplan = false)
                state = state.copy(
                    observationMode = if (canSendScreenshot) {
                        AgentObservationMode.VISION
                    } else {
                        AgentObservationMode.TEXT_ONLY
                    },
                    phase = AgentRunPhase.THINKING,
                    deviceState = nextDeviceState,
                    pageDiff = pageDiff,
                    latestScreenshot = observation.screenshotPng ?: state.latestScreenshot,
                    budgetStatus = budgetTracker.current(),
                    executionDetails = (state.executionDetails + "Observed ${nextDeviceState.pageSignature.value}; ${if (pageDiff.changed) "page changed" else "page stable"}").takeLast(40)
                )

                if (nextDeviceState.packageName != null) {
                    appKnowledgeContext = runCatching {
                        workflowStoreProvider().listKnowledgeCards(nextDeviceState.packageName, enabledOnly = true)
                            .joinToString("\n\n") { card -> "[${card.title}]\n${card.guide}" }
                    }.getOrDefault("")
                }

                contextSnapshot = if (contextSnapshot == null || (targetPackage != null && !appScopedMemoryLoaded)) {
                    contextManager.prepare(
                        task = task,
                        deviceId = deviceId,
                        packageName = targetPackage,
                        steps = state.steps,
                        observation = observation,
                        config = config,
                        existingCompactions = state.compactionCount
                    ).also {
                        if (targetPackage != null) appScopedMemoryLoaded = true
                    }
                } else {
                    contextManager.compact(
                        task = task,
                        memoryText = contextSnapshot!!.memoryText,
                        memoryIds = contextSnapshot!!.memoryIds,
                        steps = state.steps,
                        observation = observation,
                        config = config,
                        existingCompactions = state.compactionCount
                    )
                }
                if (contextManager.needsModelCompaction(contextSnapshot!!) && budgetTracker.canSpendOnCompaction()) {
                    val compaction = modelClient.compactContext(
                        config = config,
                        apiKey = apiKey,
                        context = AgentModelContext(
                            task = task,
                            observation = observation,
                            completedSteps = contextSnapshot!!.recentSteps,
                            memoryContext = contextSnapshot!!.memoryText,
                            compactedHistory = contextSnapshot!!.compactedHistory,
                            appKnowledgeContext = appKnowledgeContext
                        )
                    )
                    if (compaction != null) {
                        contextSnapshot = contextManager.applyModelCompaction(
                            snapshot = contextSnapshot!!,
                            summary = compaction.summary,
                            task = task,
                            observation = observation
                        )
                        state = state.copy(usage = state.usage + compaction.usage)
                        state = state.copy(budgetStatus = budgetTracker.record(compaction.usage, usedVision = false))
                    }
                } else if (contextManager.needsModelCompaction(contextSnapshot!!)) {
                    state = state.copy(
                        executionDetails = (
                            state.executionDetails + "Skipped optional model compaction to preserve action and final-result budget"
                            ).takeLast(40)
                    )
                }
                state = state.copy(
                    memoryHitCount = contextSnapshot!!.memoryIds.size,
                    compactionCount = contextSnapshot!!.compactionCount
                )
                publishState(state)

                var context = AgentModelContext(
                    task = task,
                    observation = observation,
                    completedSteps = contextSnapshot!!.recentSteps,
                    memoryContext = contextSnapshot!!.memoryText,
                    compactedHistory = contextSnapshot!!.compactedHistory,
                    appKnowledgeContext = appKnowledgeContext
                )
                val recoveringMissingElement = missingElementRecoveryCount > 0
                if (!budgetTracker.canRequest(canSendScreenshot, isReplan = recoveringMissingElement)) {
                    throw AgentException(budgetTracker.current().stopReason ?: "Model budget reached")
                }
                val finalResponseOnly = budgetTracker.shouldForceFinalResponse()
                if (finalResponseOnly) {
                    state = state.copy(
                        executionDetails = (
                            state.executionDetails + "Final reserved model call: requesting a verified task result"
                            ).takeLast(40)
                    )
                    publishState(state)
                }
                val decision = try {
                    requestDecision(config, apiKey, context, canSendScreenshot, finalResponseOnly) {
                        visionAvailable = false
                        state = state.copy(observationMode = AgentObservationMode.TEXT_ONLY)
                        publishState(state)
                    }
                } catch (overflow: ModelContextOverflowException) {
                    contextManager.recordContextOverflow(config)
                    state = state.copy(phase = AgentRunPhase.RETRYING)
                    publishState(state)
                    contextSnapshot = contextManager.compact(
                        task = task,
                        memoryText = contextSnapshot!!.memoryText,
                        memoryIds = contextSnapshot!!.memoryIds,
                        steps = state.steps,
                        observation = observation,
                        config = config,
                        existingCompactions = state.compactionCount
                    )
                    context = context.copy(
                        completedSteps = contextSnapshot!!.recentSteps,
                        compactedHistory = contextSnapshot!!.compactedHistory
                    )
                    requestDecision(config, apiKey, context, false, finalResponseOnly) {
                        visionAvailable = false
                    }
                }
                state = state.copy(
                    usage = state.usage + decision.usage,
                    budgetStatus = budgetTracker.record(
                        decision.usage,
                        decision.usedVision,
                        isReplan = recoveringMissingElement
                    ),
                    phase = AgentRunPhase.THINKING
                )

                val action = decision.action
                val validationError = validateAgentAction(action, observation).exceptionOrNull()
                if (validationError != null) {
                    val message = validationError.message ?: "Invalid action arguments"
                    if (action.hasMissingElementReference(message)) {
                        missingElementRecoveryCount += 1
                        val recoveryDetail = action.missingElementRecoveryDetail(observation, message)
                        val failedStep = AgentStep(
                            id = newId(),
                            action = action,
                            status = AgentStepStatus.FAILED,
                            result = recoveryDetail
                        )
                        state = state.copy(
                            steps = state.steps + failedStep,
                            phase = if (missingElementRecoveryCount < MAX_MISSING_ELEMENT_RECOVERIES) {
                                AgentRunPhase.RETRYING
                            } else {
                                AgentRunPhase.FAILED
                            },
                            executionDetails = (state.executionDetails + recoveryDetail).takeLast(40)
                        )
                        publishState(state)
                        if (missingElementRecoveryCount < MAX_MISSING_ELEMENT_RECOVERIES) {
                            return@repeat
                        }
                        throw ModelProtocolException(
                            "UI element resolution failed $missingElementRecoveryCount times; task stopped safely"
                        )
                    }
                    throw ModelProtocolException(message)
                }
                missingElementRecoveryCount = 0
                if (action is AgentAction.Finish) {
                    if (action.outcome == AgentFinishOutcome.BLOCKED) {
                        val message = "Task blocked: ${action.summary}"
                        state = state.copy(
                            messages = state.messages + AgentMessage(
                                id = newId(),
                                role = AgentMessageRole.SYSTEM,
                                text = message
                            ),
                            isRunning = false,
                            pendingConfirmation = null,
                            phase = AgentRunPhase.FAILED,
                            errorMessage = message
                        )
                        publishState(state)
                        return state
                    }
                    val saved = contextManager.saveSuccessfulTask(
                        task = task,
                        deviceId = deviceId,
                        packageName = targetPackage,
                        finish = action,
                        steps = state.steps
                    )
                    val workflowDraft = WorkflowDraftFactory.fromRecordedTask(
                        task,
                        workflowEvidence.firstOrNull()?.before,
                        workflowEvidence
                    )
                    if (workflowDraft != null) {
                        runCatching { AgentWorkflowRuntime.store.save(workflowDraft) }
                    }
                    state = state.copy(
                        messages = state.messages + AgentMessage(
                            id = newId(),
                            role = AgentMessageRole.ASSISTANT,
                            text = action.summary
                        ),
                        isRunning = false,
                        pendingConfirmation = null,
                        phase = AgentRunPhase.COMPLETED,
                        savedMemoryCount = saved,
                        executionDetails = (state.executionDetails + if (workflowDraft == null) {
                            "Workflow draft was not created because the task was not fully safe and verified"
                        } else {
                            "Workflow draft created; review it before enabling"
                        }).takeLast(40)
                    )
                    publishState(state)
                    return state
                }

                when (val loopDecision = loopGuard.inspect(action, observation)) {
                    is LoopDecision.RecoveryRequired -> {
                        val loopStep = AgentStep(
                            id = newId(),
                            action = action,
                            status = AgentStepStatus.FAILED,
                            result = "Repeated ${loopDecision.actionLabel} on the same device state. " +
                                "Choose a different recovery action or finish with BLOCKED."
                        )
                        state = state.copy(
                            steps = state.steps + loopStep,
                            phase = AgentRunPhase.THINKING
                        )
                        publishState(state)
                        return@repeat
                    }
                    is LoopDecision.Abort -> throw AgentLoopException(
                        "Repeated navigation loop detected for ${loopDecision.actionLabel}; task stopped to avoid further actions"
                    )
                    null -> Unit
                }

                val capabilityReason = deviceGateway.confirmationRequirement(deviceId, action, observation)
                val risk = riskEvaluator.evaluate(action, observation, capabilityReason)
                var step = AgentStep(
                    id = newId(),
                    action = action,
                    status = if (risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED) {
                        AgentStepStatus.AWAITING_CONFIRMATION
                    } else {
                        AgentStepStatus.RUNNING
                    },
                    riskLevel = risk.level,
                    confirmationReason = risk.reason
                )
                state = state.copy(
                    steps = state.steps + step,
                    pendingConfirmation = step.takeIf {
                        risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED
                    },
                    phase = if (risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED) {
                        AgentRunPhase.AWAITING_CONFIRMATION
                    } else {
                        AgentRunPhase.EXECUTING
                    }
                )
                publishState(state)

                if (risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED) {
                    val approved = confirmSensitiveAction(step)
                    if (!approved) {
                        step = step.copy(
                            status = AgentStepStatus.DENIED,
                            result = "User denied this action"
                        )
                        state = state.replaceStep(step).copy(
                            pendingConfirmation = null,
                            phase = AgentRunPhase.THINKING
                        )
                        publishState(state)
                        return@repeat
                    }
                    step = step.copy(status = AgentStepStatus.RUNNING)
                    state = state.replaceStep(step).copy(
                        pendingConfirmation = null,
                        phase = AgentRunPhase.EXECUTING
                    )
                    publishState(state)
                }

                val result = deviceGateway.execute(deviceId, action)
                state = state.copy(phase = AgentRunPhase.VERIFYING)
                publishState(state)
                val postObservation = runCatching {
                    if (action.requiresVisibleChange()) {
                        waitForStableObservation(observe = { deviceGateway.observe(deviceId) }) ?: deviceGateway.observe(deviceId)
                    } else {
                        deviceGateway.observe(deviceId)
                    }
                }.getOrNull()
                val visibleChange = postObservation?.let { hasVisibleChange(observation, it) }
                val verification = verifyAction(
                    action = action,
                    result = result,
                    postObservation = postObservation,
                    visibleChange = visibleChange
                )
                step = step.copy(
                    status = verification.status,
                    result = buildString {
                        append(result.output)
                        if (verification.message.isNotBlank()) {
                            if (isNotEmpty()) append("\n")
                            append(verification.message)
                        }
                    }.take(MAX_STEP_RESULT_LENGTH)
                )
                state = state.replaceStep(step).copy(
                    phase = AgentRunPhase.THINKING,
                    executionDetails = (state.executionDetails + "${action.toolName}: ${verification.status.name.lowercase()} ${verification.message}").takeLast(40)
                )
                if (action is AgentAction.LaunchPackage && verification.status == AgentStepStatus.COMPLETED) {
                    targetPackage = action.packageName
                }
                if (verification.status == AgentStepStatus.COMPLETED && postObservation != null) {
                    workflowEvidence += WorkflowRecordedStep(
                        action = action,
                        before = PageSignatureEngine.state(observation),
                        after = PageSignatureEngine.state(postObservation)
                    )
                }
                publishState(state)
            }
            throw AgentException("The task stopped after reaching the $maxActions-action safety limit")
        } catch (cancelled: CancellationException) {
            state = state.copy(
                isRunning = false,
                pendingConfirmation = null,
                phase = AgentRunPhase.CANCELLED,
                messages = state.messages + AgentMessage(
                    id = newId(),
                    role = AgentMessageRole.SYSTEM,
                    text = "Task cancelled"
                )
            )
            publishState(state)
            throw cancelled
        } catch (error: Exception) {
            state = state.copy(
                isRunning = false,
                pendingConfirmation = null,
                phase = AgentRunPhase.FAILED,
                errorMessage = error.message ?: "Agent task failed",
                messages = state.messages + AgentMessage(
                    id = newId(),
                    role = AgentMessageRole.SYSTEM,
                    text = error.message ?: "Agent task failed"
                )
            )
            publishState(state)
            return state
        }
    }

    private suspend fun requestDecision(
        config: AiModelConfig,
        apiKey: String,
        context: AgentModelContext,
        includeScreenshot: Boolean,
        finalResponseOnly: Boolean,
        onVisionFallback: () -> Unit
    ): AgentModelDecision = try {
        if (finalResponseOnly) {
            modelClient.finishTask(config, apiKey, context, includeScreenshot)
        } else {
            modelClient.nextAction(config, apiKey, context, includeScreenshot)
        }
    } catch (unsupported: UnsupportedVisionException) {
        if (!includeScreenshot || config.visionMode == VisionMode.ENABLED) throw unsupported
        onVisionFallback()
        if (finalResponseOnly) {
            modelClient.finishTask(config, apiKey, context, includeScreenshot = false)
        } else {
            modelClient.nextAction(config, apiKey, context, includeScreenshot = false)
        }
    }

    private suspend fun requestPlanSafely(
        config: AiModelConfig,
        apiKey: String,
        task: String,
        observation: AgentObservation
    ): AgentPlanDecision? = try {
        modelClient.planTask(config, apiKey, task, observation)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private suspend fun observeLightweightSafely(
        deviceId: String,
        includeUiHierarchy: Boolean = false
    ): AgentObservation? = try {
        deviceGateway.observeLightweight(deviceId, includeUiHierarchy)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private suspend fun requestRepairSafely(
        config: AiModelConfig,
        apiKey: String,
        task: String,
        observation: AgentObservation,
        completedSteps: List<AgentStep>,
        failure: String
    ): AgentPlanDecision? = try {
        modelClient.repairPlan(config, apiKey, task, observation, completedSteps, failure)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private fun completePlannedTask(
        state: AgentTaskUiState,
        summary: String,
        onState: (AgentTaskUiState) -> Unit
    ): AgentTaskUiState {
        val completed = state.copy(
            messages = state.messages + AgentMessage(
                id = newId(),
                role = AgentMessageRole.ASSISTANT,
                text = summary.ifBlank { "Task completed" }
            ),
            isRunning = false,
            pendingConfirmation = null,
            phase = AgentRunPhase.COMPLETED
        )
        onState(completed)
        return completed
    }

    private fun AgentTaskUiState.withPlanningNotice(message: String): AgentTaskUiState = copy(
        messages = messages + AgentMessage(id = newId(), role = AgentMessageRole.SYSTEM, text = message),
        executionStrategy = AgentExecutionStrategy.INTERACTIVE,
        phase = AgentRunPhase.THINKING
    )

    private fun AgentTaskUiState.replaceStep(updated: AgentStep): AgentTaskUiState = copy(
        steps = steps.map { if (it.id == updated.id) updated else it }
    )
}

private fun AgentAction.requiresVisibleChange(): Boolean = when (this) {
    AgentAction.Observe,
    is AgentAction.FindApp,
    is AgentAction.Wait -> false
    else -> true
}

private fun verifyAction(
    action: AgentAction,
    result: AgentToolResult,
    postObservation: AgentObservation?,
    visibleChange: Boolean?
): ActionVerification {
    if (!result.success) return ActionVerification(AgentStepStatus.FAILED)
    if (action is AgentAction.LaunchPackage) {
        val activity = postObservation?.currentActivity.orEmpty()
        return when {
            activity.isBlank() -> ActionVerification(
                AgentStepStatus.UNVERIFIED,
                "Launch command succeeded, but the foreground Activity is unavailable for verification"
            )
            activity.isExpectedLaunchTarget(action.packageName) -> ActionVerification(
                AgentStepStatus.COMPLETED,
                if (activity.belongsToPackage(action.packageName)) {
                    "Verified foreground Activity: $activity"
                } else {
                    "Verified approved launch handoff: $activity"
                }
            )
            else -> ActionVerification(
                AgentStepStatus.FAILED,
                "Launch command succeeded, but foreground Activity is $activity instead of ${action.packageName}"
            )
        }
    }
    if (!action.requiresVisibleChange()) return ActionVerification(AgentStepStatus.COMPLETED)
    return when (visibleChange) {
        true -> ActionVerification(AgentStepStatus.COMPLETED, "Verified visible device change")
        false -> ActionVerification(AgentStepStatus.UNVERIFIED, "No visible device change was observed")
        null -> ActionVerification(
            AgentStepStatus.UNVERIFIED,
            "Action succeeded, but post-action observation was unavailable for verification"
        )
    }
}

private fun String.belongsToPackage(packageName: String): Boolean =
    contains("$packageName/") || contains("$packageName ") || trim() == packageName

/**
 * Gemini's exported Bard entry point intentionally hands the foreground task to the
 * Google Search assistant deeplink host. The ActivityTaskManager log records this
 * handoff as a successful launch, so treating it as a different-app failure is wrong.
 */
private fun String.isExpectedLaunchTarget(packageName: String): Boolean {
    if (belongsToPackage(packageName)) return true
    return packageName == GEMINI_PACKAGE &&
        contains("$GOOGLE_SEARCH_PACKAGE/") &&
        contains("MainAssistantDeeplink", ignoreCase = true)
}

private fun hasVisibleChange(before: AgentObservation, after: AgentObservation): Boolean {
    if (before.currentActivity != after.currentActivity) return true
    if (before.uiHierarchy != after.uiHierarchy) return true
    return when {
        before.screenshotPng == null && after.screenshotPng == null -> false
        before.screenshotPng == null || after.screenshotPng == null -> true
        else -> !MessageDigest.isEqual(before.screenshotPng, after.screenshotPng)
    }
}

private data class ActionVerification(
    val status: AgentStepStatus,
    val message: String = ""
)

private sealed interface LoopDecision {
    val actionLabel: String

    data class RecoveryRequired(override val actionLabel: String) : LoopDecision
    data class Abort(override val actionLabel: String) : LoopDecision
}

private class AgentLoopGuard {
    private val checkpoints = ArrayDeque<DecisionCheckpoint>()
    private var recoveryUsed = false

    fun inspect(action: AgentAction, observation: AgentObservation): LoopDecision? {
        val actionLabel = action.loopActionLabel() ?: return null
        val checkpoint = DecisionCheckpoint(actionLabel, observation.stateFingerprint())
        val repeated = checkpoints.any { it == checkpoint }
        checkpoints.addLast(checkpoint)
        while (checkpoints.size > LOOP_CHECKPOINT_WINDOW) checkpoints.removeFirst()
        if (!repeated) return null
        return if (recoveryUsed) {
            LoopDecision.Abort(actionLabel)
        } else {
            recoveryUsed = true
            LoopDecision.RecoveryRequired(actionLabel)
        }
    }
}

private data class DecisionCheckpoint(
    val actionLabel: String,
    val stateFingerprint: String
)

private fun AgentAction.loopActionLabel(): String? = when (this) {
    is AgentAction.LaunchPackage -> "launch_package:$packageName"
    is AgentAction.KeyEvent -> "key_event:${key.name}"
    is AgentAction.FindApp -> "find_app:${query.trim().lowercase()}"
    AgentAction.Observe -> null // Observation is a local runtime refresh, never a navigation loop.
    is AgentAction.Wait -> "wait:$durationMs"
    else -> null
}

private fun AgentAction.hasMissingElementReference(message: String): Boolean =
    (this is AgentAction.TapElement && message == "The requested UI element does not exist") ||
        (this is AgentAction.InputText && message == "The requested input element does not exist")

private fun AgentAction.missingElementRecoveryDetail(
    observation: AgentObservation,
    validationMessage: String
): String = buildString {
    append("Selector recovery: ").append(validationMessage)
    when (this@missingElementRecoveryDetail) {
        is AgentAction.TapElement -> append("; requested_observation=").append(observationId)
            .append("; requested_element=").append(elementId)
        is AgentAction.InputText -> append("; requested_observation=").append(observationId ?: "<none>")
            .append("; requested_element=").append(elementId ?: "<none>")
        else -> Unit
    }
    append("; available_nodes=")
    append(
        observation.uiNodes.take(MAX_LOGGED_SELECTOR_NODES).joinToString(",") { node ->
            buildString {
                append(node.elementId).append("{")
                append("role=").append(node.role.ifBlank { node.className })
                if (node.resourceId.isNotBlank()) append(",id=").append(node.resourceId)
                append(",enabled=").append(node.enabled).append("}")
            }
        }.ifBlank { "<none>" }
    )
}

private fun AgentObservation.stateFingerprint(): String {
    val normalizedHierarchy = uiHierarchy.replace(Regex("\\s+"), " ").trim()
    val bytes = "$currentActivity\n$normalizedHierarchy".toByteArray()
    return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

private enum class AgentTaskMode {
    CONVERSATIONAL,
    READ_ONLY_DEVICE,
    INTERACTIVE
}

private fun String.classifyMode(): AgentTaskMode {
    val normalized = lowercase().replace(Regex("\\s+"), "")
    if (normalized in setOf("你好", "您好", "嗨", "hello", "hi", "hey")) {
        return AgentTaskMode.CONVERSATIONAL
    }
    if (INTERACTIVE_TASK_TERMS.any(normalized::contains)) return AgentTaskMode.INTERACTIVE
    return if (DEVICE_READ_ONLY_TERMS.any(normalized::contains)) {
        AgentTaskMode.READ_ONLY_DEVICE
    } else {
        AgentTaskMode.INTERACTIVE
    }
}

private fun String.shouldUseVisionAutomatically(observation: AgentObservation): Boolean {
    val normalized = lowercase()
    return observation.uiNodes.isEmpty() || AUTO_VISION_TERMS.any(normalized::contains)
}

private fun conversationObservation(): AgentObservation = AgentObservation(
    screenshotPng = null,
    uiHierarchy = "",
    currentActivity = "",
    screenWidth = 0,
    screenHeight = 0,
    warnings = listOf("No device observation was requested for this conversational message")
)

private class AgentLoopException(message: String) : AgentException(message)

private fun newId(): String = UUID.randomUUID().toString()

private const val DEFAULT_MAX_ACTIONS = 20
private const val MAX_STEP_RESULT_LENGTH = 2_000
private const val LOOP_CHECKPOINT_WINDOW = 8
private const val MAX_MISSING_ELEMENT_RECOVERIES = 2
private const val MAX_LOGGED_SELECTOR_NODES = 24
private const val TEMPLATE_MAX_STEPS = 3
private const val BATCH_MAX_STEPS = 6
private const val REPAIR_MAX_STEPS = 3
private const val GEMINI_PACKAGE = "com.google.android.apps.bard"
private const val GOOGLE_SEARCH_PACKAGE = "com.google.android.googlequicksearchbox"

private val INTERACTIVE_TASK_TERMS = listOf(
    "打开", "启动", "点击", "点一下", "输入", "发送", "安装", "卸载", "清除", "删除", "重启", "关闭",
    "返回", "滑动", "下拉", "授权", "允许", "拒绝", "切换", "设置", "调整", "修改", "搜索"
)

private val DEVICE_READ_ONLY_TERMS = listOf(
    "设备概览", "设备情况", "设备状态", "当前设备", "设备信息", "分析设备", "分析我", "诊断设备",
    "电量", "网络状态", "无线调试", "wifi", "wi-fi", "版本信息", "系统信息", "硬件信息"
)

private val AUTO_VISION_TERMS = listOf(
    "截图", "屏幕", "画面", "图片", "图像", "图标", "视觉", "坐标", "识别", "看图"
)
