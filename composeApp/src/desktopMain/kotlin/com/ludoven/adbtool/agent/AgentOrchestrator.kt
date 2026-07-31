package com.ludoven.adbtool.agent

import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CancellationException

class AgentOrchestrator(
    private val modelClient: AgentModelClient,
    private val deviceGateway: AgentDeviceGateway,
    private val maxActions: Int = DEFAULT_MAX_ACTIONS,
    private val riskEvaluator: AgentRiskEvaluator = AgentRiskEvaluator(),
    private val memoryPreferences: AgentMemoryPreferences = AgentMemoryRuntime.preferences,
    private val memoryStoreProvider: () -> AgentMemoryStore = { AgentMemoryRuntime.store }
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
        onState(state)

        try {
            AgentTaskTemplateMatcher.match(task)
                ?.takeIf { taskSource == AgentTaskSource.DIRECT_TEMPLATE }
                ?.let { template ->
                val execution = planExecutor.execute(
                    plan = template,
                    deviceId = deviceId,
                    initialState = state,
                    onState = onState,
                    confirmSensitiveAction = confirmSensitiveAction,
                    strategy = AgentExecutionStrategy.FAST_TEMPLATE,
                    maxSteps = TEMPLATE_MAX_STEPS
                )
                state = execution.state
                if (execution.completed) return completePlannedTask(state, template.summary, onState)
                state = state.withPlanningNotice(
                    "Fast path verification failed: ${execution.failure}. Switching to adaptive planning."
                )
                onState(state)
            }

            val planningObservation = observeLightweightSafely(deviceId)
            if (planningObservation != null) {
                val planned = requestPlanSafely(
                    config = config,
                    apiKey = apiKey,
                    task = task,
                    observation = planningObservation
                )
                if (planned != null) {
                    state = state.copy(usage = state.usage + planned.usage, phase = AgentRunPhase.THINKING)
                    onState(state)
                    if (planned.plan.mode == AgentPlanMode.BATCH) {
                        val execution = planExecutor.execute(
                            plan = planned.plan,
                            deviceId = deviceId,
                            initialState = state,
                            onState = onState,
                            confirmSensitiveAction = confirmSensitiveAction,
                            strategy = AgentExecutionStrategy.BATCH_PLAN,
                            maxSteps = BATCH_MAX_STEPS
                        )
                        state = execution.state
                        if (execution.completed) return completePlannedTask(state, planned.plan.summary, onState)

                        val repairObservation = observeLightweightSafely(deviceId)
                        val repair = repairObservation?.let {
                            requestRepairSafely(
                                config = config,
                                apiKey = apiKey,
                                task = task,
                                observation = it,
                                completedSteps = state.steps,
                                failure = execution.failure
                            )
                        }
                        if (repair?.plan?.mode == AgentPlanMode.BATCH) {
                            state = state.copy(usage = state.usage + repair.usage, phase = AgentRunPhase.THINKING)
                            onState(state)
                            val repaired = planExecutor.execute(
                                plan = repair.plan,
                                deviceId = deviceId,
                                initialState = state,
                                onState = onState,
                                confirmSensitiveAction = confirmSensitiveAction,
                                strategy = AgentExecutionStrategy.REPAIR_PLAN,
                                maxSteps = REPAIR_MAX_STEPS
                            )
                            state = repaired.state
                            if (repaired.completed) return completePlannedTask(state, repair.plan.summary, onState)
                            state = state.withPlanningNotice(
                                "Batch repair failed: ${repaired.failure}. Switching to adaptive execution."
                            )
                        } else {
                            state = state.withPlanningNotice(
                                "Batch plan failed: ${execution.failure}. Switching to adaptive execution."
                            )
                        }
                        onState(state)
                    } else {
                        state = state.copy(executionStrategy = AgentExecutionStrategy.INTERACTIVE)
                        onState(state)
                    }
                }
            }
            repeat(maxActions) {
                if (!deviceGateway.isConnected(deviceId)) {
                    throw AgentException("The selected device is no longer connected")
                }
                state = state.copy(phase = AgentRunPhase.OBSERVING)
                onState(state)
                val observation = deviceGateway.observe(deviceId)
                val canSendScreenshot = visionAvailable &&
                    config.visionMode != VisionMode.DISABLED &&
                    observation.screenshotPng != null
                state = state.copy(
                    observationMode = if (canSendScreenshot) {
                        AgentObservationMode.VISION
                    } else {
                        AgentObservationMode.TEXT_ONLY
                    },
                    phase = AgentRunPhase.THINKING
                )

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
                if (contextManager.needsModelCompaction(contextSnapshot!!)) {
                    val compaction = modelClient.compactContext(
                        config = config,
                        apiKey = apiKey,
                        context = AgentModelContext(
                            task = task,
                            observation = observation,
                            completedSteps = contextSnapshot!!.recentSteps,
                            memoryContext = contextSnapshot!!.memoryText,
                            compactedHistory = contextSnapshot!!.compactedHistory
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
                    }
                }
                state = state.copy(
                    memoryHitCount = contextSnapshot!!.memoryIds.size,
                    compactionCount = contextSnapshot!!.compactionCount
                )
                onState(state)

                var context = AgentModelContext(
                    task = task,
                    observation = observation,
                    completedSteps = contextSnapshot!!.recentSteps,
                    memoryContext = contextSnapshot!!.memoryText,
                    compactedHistory = contextSnapshot!!.compactedHistory
                )
                val decision = try {
                    requestDecision(config, apiKey, context, canSendScreenshot) {
                        visionAvailable = false
                        state = state.copy(observationMode = AgentObservationMode.TEXT_ONLY)
                        onState(state)
                    }
                } catch (overflow: ModelContextOverflowException) {
                    contextManager.recordContextOverflow(config)
                    state = state.copy(phase = AgentRunPhase.RETRYING)
                    onState(state)
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
                    requestDecision(config, apiKey, context, false) {
                        visionAvailable = false
                    }
                }
                state = state.copy(
                    usage = state.usage + decision.usage,
                    phase = AgentRunPhase.THINKING
                )

                val action = decision.action
                validateAgentAction(action, observation).getOrElse {
                    throw ModelProtocolException(it.message ?: "Invalid action arguments")
                }
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
                        onState(state)
                        return state
                    }
                    val saved = contextManager.saveSuccessfulTask(
                        task = task,
                        deviceId = deviceId,
                        packageName = targetPackage,
                        finish = action,
                        steps = state.steps
                    )
                    state = state.copy(
                        messages = state.messages + AgentMessage(
                            id = newId(),
                            role = AgentMessageRole.ASSISTANT,
                            text = action.summary
                        ),
                        isRunning = false,
                        pendingConfirmation = null,
                        phase = AgentRunPhase.COMPLETED,
                        savedMemoryCount = saved
                    )
                    onState(state)
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
                        onState(state)
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
                onState(state)

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
                        onState(state)
                        return@repeat
                    }
                    step = step.copy(status = AgentStepStatus.RUNNING)
                    state = state.replaceStep(step).copy(
                        pendingConfirmation = null,
                        phase = AgentRunPhase.EXECUTING
                    )
                    onState(state)
                }

                val result = deviceGateway.execute(deviceId, action)
                state = state.copy(phase = AgentRunPhase.VERIFYING)
                onState(state)
                val postObservation = runCatching { deviceGateway.observe(deviceId) }.getOrNull()
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
                state = state.replaceStep(step).copy(phase = AgentRunPhase.THINKING)
                if (action is AgentAction.LaunchPackage && verification.status == AgentStepStatus.COMPLETED) {
                    targetPackage = action.packageName
                }
                onState(state)
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
            onState(state)
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
            onState(state)
            return state
        }
    }

    private suspend fun requestDecision(
        config: AiModelConfig,
        apiKey: String,
        context: AgentModelContext,
        includeScreenshot: Boolean,
        onVisionFallback: () -> Unit
    ): AgentModelDecision = try {
        modelClient.nextAction(config, apiKey, context, includeScreenshot)
    } catch (unsupported: UnsupportedVisionException) {
        if (!includeScreenshot || config.visionMode == VisionMode.ENABLED) throw unsupported
        onVisionFallback()
        modelClient.nextAction(config, apiKey, context, includeScreenshot = false)
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

    private suspend fun observeLightweightSafely(deviceId: String): AgentObservation? = try {
        deviceGateway.observeLightweight(deviceId)
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
            activity.belongsToPackage(action.packageName) -> ActionVerification(
                AgentStepStatus.COMPLETED,
                "Verified foreground Activity: $activity"
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
    AgentAction.Observe -> "observe"
    is AgentAction.Wait -> "wait:$durationMs"
    else -> null
}

private fun AgentObservation.stateFingerprint(): String {
    val normalizedHierarchy = uiHierarchy.replace(Regex("\\s+"), " ").trim()
    val bytes = "$currentActivity\n$normalizedHierarchy".toByteArray()
    return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

private class AgentLoopException(message: String) : AgentException(message)

private fun newId(): String = UUID.randomUUID().toString()

private const val DEFAULT_MAX_ACTIONS = 20
private const val MAX_STEP_RESULT_LENGTH = 2_000
private const val LOOP_CHECKPOINT_WINDOW = 8
private const val TEMPLATE_MAX_STEPS = 3
private const val BATCH_MAX_STEPS = 6
private const val REPAIR_MAX_STEPS = 3
