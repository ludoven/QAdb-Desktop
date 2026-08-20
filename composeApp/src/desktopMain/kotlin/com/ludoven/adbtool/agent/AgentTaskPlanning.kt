package com.ludoven.adbtool.agent

import com.ludoven.adbtool.util.AdbProcessTimeoutContext
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class AgentOperationTimeoutException : AgentException(AGENT_OPERATION_TIMEOUT_MESSAGE)
internal class AgentModelCallTimeoutException : AgentException("V2 model call timed out")
internal class AgentModelWallClockBudgetException : AgentException("V2 model wall-clock budget exhausted")

/** Monotonic operation budget shared by adaptive, batch, and workflow execution. */
internal class AgentOperationDeadline(
    durationMillis: Long,
    private val nanoTime: () -> Long = System::nanoTime
) {
    private val lock = Any()
    private val budgetNanos = durationMillis
        .coerceAtLeast(1L)
        .coerceAtMost(Long.MAX_VALUE / NANOS_PER_MILLISECOND) * NANOS_PER_MILLISECOND
    private var lastObservedNanos = nanoTime()
    private val startedAtNanos = lastObservedNanos
    private var pausedAtNanos: Long? = null
    private var accumulatedPausedNanos = 0L
    private var pauseDepth = 0

    fun check() {
        remainingMillisOrThrow()
    }

    suspend fun <T> runWithin(block: suspend () -> T): T {
        val timeoutMillis = remainingMillisOrThrow()
        val result = withContext(AdbProcessTimeoutContext.asContextElement(timeoutMillis)) {
            withTimeoutOrNull(timeoutMillis) {
                DeadlineResult(block())
            }
        } ?: throw AgentOperationTimeoutException()
        check()
        return result.value
    }

    suspend fun <T> pauseForConfirmation(block: suspend () -> T): T {
        pause()
        return try {
            block()
        } finally {
            resume()
        }
    }

    private fun pause() = synchronized(lock) {
        val now = monotonicNowLocked()
        if (remainingNanosLocked(now) <= 0L) throw AgentOperationTimeoutException()
        if (pauseDepth++ == 0) pausedAtNanos = now
    }

    private fun resume() = synchronized(lock) {
        if (pauseDepth <= 0) return@synchronized
        pauseDepth -= 1
        if (pauseDepth == 0) {
            val pausedAt = pausedAtNanos ?: return@synchronized
            accumulatedPausedNanos += (monotonicNowLocked() - pausedAt).coerceAtLeast(0L)
            pausedAtNanos = null
        }
    }

    private fun remainingMillisOrThrow(): Long = synchronized(lock) {
        val remainingNanos = remainingNanosLocked(monotonicNowLocked())
        if (remainingNanos <= 0L) throw AgentOperationTimeoutException()
        (remainingNanos / NANOS_PER_MILLISECOND +
            if (remainingNanos % NANOS_PER_MILLISECOND == 0L) 0L else 1L)
            .coerceAtLeast(1L)
    }

    private fun remainingNanosLocked(now: Long): Long {
        val effectiveNow = pausedAtNanos ?: now
        val activeElapsed = (effectiveNow - startedAtNanos - accumulatedPausedNanos).coerceAtLeast(0L)
        return budgetNanos - activeElapsed
    }

    private fun monotonicNowLocked(): Long {
        val observed = nanoTime()
        if (observed > lastObservedNanos) lastObservedNanos = observed
        return lastObservedNanos
    }

    private data class DeadlineResult<T>(val value: T)

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}

internal suspend fun <T> AgentOperationDeadline?.runWithinDeadline(block: suspend () -> T): T =
    this?.runWithin(block) ?: block()

internal suspend fun <T> AgentOperationDeadline?.pauseForConfirmationDeadline(block: suspend () -> T): T =
    this?.pauseForConfirmation(block) ?: block()

internal fun AgentOperationDeadline?.checkDeadline() {
    this?.check()
}

internal const val AGENT_OPERATION_TIMEOUT_MESSAGE =
    "设备操作已达到总时限，已停止后续操作。已发送的设备命令可能仍会完成，系统不会将其视为已撤销。"

internal class AgentPlanExecutor(
    private val deviceGateway: AgentDeviceGateway,
    private val riskEvaluator: AgentRiskEvaluator,
    private val predicateEvaluator: AgentPredicateEvaluator = AgentPredicateEvaluator(deviceGateway),
    private val observationCoordinator: AgentObservationCoordinator = AgentObservationCoordinator(deviceGateway),
    private val maxDeviceActions: Int = 20
) {
    suspend fun execute(
        plan: AgentTaskPlan,
        deviceId: String,
        initialState: AgentTaskUiState,
        onState: (AgentTaskUiState) -> Unit,
        confirmSensitiveAction: suspend (AgentStep) -> Boolean,
        strategy: AgentExecutionStrategy,
        maxSteps: Int,
        deadline: AgentOperationDeadline? = null
    ): AgentPlanExecution {
        deadline.checkDeadline()
        validatePlan(plan, maxSteps).getOrElse {
            return AgentPlanExecution(initialState, false, it.message ?: "Invalid batch plan")
        }
        var state = initialState.copy(executionStrategy = strategy, phase = AgentRunPhase.EXECUTING)
        val resolvedPackages = mutableMapOf<String, String>()
        var latestObservation = observe(deviceId, includeUiHierarchy = true, deadline = deadline)
        onState(state)

        for (plannedStep in plan.steps) {
            deadline.checkDeadline()
            val before = observe(
                deviceId,
                includeUiHierarchy = plannedStep.requiresUiHierarchy(),
                deadline = deadline
            )
            latestObservation = before
            val precondition = deadline.runWithinDeadline {
                predicateEvaluator.evaluate(
                    plannedStep.precondition,
                    deviceId,
                    before,
                    resolvedPackages
                )
            }
            if (!precondition.matches) {
                return fail(state, plannedStep, "Precondition failed: ${precondition.reason}", onState)
            }

            val resolved = resolveAction(plannedStep.action, before, resolvedPackages)
                ?: return fail(state, plannedStep, "Referenced app or UI selector is unavailable", onState)
            val action = resolved.action
            if (action.isDeviceAction() && state.steps.executedDeviceActionCount() >= maxDeviceActions) {
                return AgentPlanExecution(state, false, "Device action budget reached ($maxDeviceActions)")
            }
            validateAgentAction(action, before).getOrElse {
                return fail(state, plannedStep, it.message ?: "Invalid batch action", onState, action)
            }
            val capabilityReason = deadline.runWithinDeadline {
                deviceGateway.confirmationRequirement(deviceId, action, before)
            }
            val risk = riskEvaluator.evaluate(action, before, capabilityReason)
            if (risk.level == AgentRiskLevel.BLOCKED) {
                return fail(
                    state = state,
                    plannedStep = plannedStep,
                    failure = risk.reason,
                    onState = onState,
                    action = action,
                    riskLevel = risk.level,
                    policyBlocked = true
                )
            }
            var step = AgentStep(
                id = newId(),
                action = action,
                status = if (risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED) {
                    AgentStepStatus.AWAITING_CONFIRMATION
                } else {
                    AgentStepStatus.RUNNING
                },
                riskLevel = risk.level,
                confirmationReason = risk.reason,
                containsSensitiveData = plannedStep.action is AgentPlanAction.InputSelector ||
                    plannedStep.action is AgentPlanAction.ExtractText
            )
            state = state.copy(
                steps = state.steps + step,
                pendingConfirmation = step.takeIf { risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED },
                phase = if (risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED) {
                    AgentRunPhase.AWAITING_CONFIRMATION
                } else {
                    AgentRunPhase.EXECUTING
                }
            )
            onState(state)
            if (risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED) {
                val approved = deadline.pauseForConfirmationDeadline {
                    confirmSensitiveAction(step)
                }
                if (!approved) {
                    step = step.copy(status = AgentStepStatus.DENIED, result = "User denied this action")
                    state = state.replaceStep(step).copy(pendingConfirmation = null, phase = AgentRunPhase.THINKING)
                    onState(state)
                    return AgentPlanExecution(
                        state = state,
                        completed = false,
                        failure = "User denied a batch action",
                        deniedByUser = true
                    )
                }
                step = step.copy(status = AgentStepStatus.RUNNING)
                state = state.replaceStep(step).copy(pendingConfirmation = null, phase = AgentRunPhase.EXECUTING)
                onState(state)
            }

            val execution = executeResolvedAction(
                deviceId,
                plannedStep,
                resolved,
                before,
                deadline
            ) {
                if (action.isDeviceAction() && state.steps.executedDeviceActionCount() >= maxDeviceActions) {
                    throw AgentException("Device action budget reached ($maxDeviceActions)")
                }
                step = step.copy(executedActionCount = step.executedActionCount + 1)
                state = state.replaceStep(step)
                onState(state)
            }
            if (execution.result.success && action is AgentAction.FindApp && execution.result.resolvedPackages.size == 1) {
                resolvedPackages[plannedStep.id] = execution.result.resolvedPackages.single()
            }
            latestObservation = execution.after
            val verificationFailure = verifyStep(
                plannedStep,
                execution.result,
                before,
                execution.after,
                deviceId,
                resolvedPackages,
                deadline
            )
            if (verificationFailure != null) {
                step = step.copy(
                    status = AgentStepStatus.FAILED,
                    result = "${execution.result.output.ifBlank { "ADB action failed" }}\n$FAILURE_PREFIX$verificationFailure"
                )
                state = state.replaceStep(step).copy(phase = AgentRunPhase.THINKING)
                onState(state)
                return AgentPlanExecution(state, false, verificationFailure)
            }
            step = step.copy(
                status = AgentStepStatus.COMPLETED,
                result = execution.result.output.ifBlank { "Completed" }.take(MAX_STEP_RESULT_LENGTH)
            )
            state = state.replaceStep(step).copy(
                phase = AgentRunPhase.EXECUTING,
                deviceState = PageSignatureEngine.state(latestObservation),
                deviceChannel = latestObservation.source,
                observationTimings = latestObservation.timings
            )
            onState(state)
        }

        val deterministicGoalProvided = plan.goal.isDeterministicCompletionGoal()
        var deterministicGoalMatched = false
        if (plan.goal !is AgentPredicate.Unspecified) {
            latestObservation = observe(
                deviceId,
                includeUiHierarchy = true,
                freshness = ObservationFreshness.REQUIRE_FRESH,
                deadline = deadline
            )
            val goal = deadline.runWithinDeadline {
                predicateEvaluator.evaluate(plan.goal, deviceId, latestObservation, resolvedPackages)
            }
            if (!goal.matches) {
                return AgentPlanExecution(state, false, "Task goal verification failed: ${goal.reason}")
            }
            deterministicGoalMatched = deterministicGoalProvided && goal.conclusive
        }
        deadline.checkDeadline()
        return AgentPlanExecution(
            state = state,
            completed = true,
            deterministicGoalProvided = deterministicGoalProvided,
            deterministicGoalMatched = deterministicGoalMatched,
            verifiedObservation = latestObservation
        )
    }

    private suspend fun executeResolvedAction(
        deviceId: String,
        step: AgentPlanStep,
        resolved: ResolvedPlanAction,
        before: AgentObservation,
        deadline: AgentOperationDeadline?,
        onExecutionStarted: () -> Unit
    ): ResolvedExecution {
        return when (val action = step.action) {
            is AgentPlanAction.WaitUntil -> {
                val stepDeadlineMs = System.currentTimeMillis() +
                    action.timeoutMs.coerceIn(100, MAX_STEP_TIMEOUT_MS)
                var observation = before
                var matched = deadline.runWithinDeadline {
                    predicateEvaluator.evaluate(action.predicate, deviceId, observation)
                }.matches
                while (!matched && System.currentTimeMillis() < stepDeadlineMs) {
                    deadline.runWithinDeadline { delay(PREDICATE_POLL_MS) }
                    observation = observe(
                        deviceId,
                        includeUiHierarchy = true,
                        freshness = ObservationFreshness.REQUIRE_FRESH,
                        deadline = deadline
                    )
                    matched = deadline.runWithinDeadline {
                        predicateEvaluator.evaluate(action.predicate, deviceId, observation)
                    }.matches
                }
                ResolvedExecution(
                    AgentToolResult(matched, if (matched) "Condition satisfied" else "Condition timed out"),
                    observation
                )
            }
            is AgentPlanAction.ScrollUntil -> executeScrollUntil(
                deviceId,
                action,
                before,
                deadline,
                onExecutionStarted
            )
            is AgentPlanAction.ExtractText -> {
                val node = (SelectorResolver.resolveUnique(action.selector, before.uiNodes) as SelectorResolution.Resolved).node
                val text = node.text.ifBlank { node.contentDescription }
                ResolvedExecution(AgentToolResult(text.isNotBlank(), text.ifBlank { "Selected element has no readable text" }), before)
            }
            else -> {
                val result = executeGatewayAction(deviceId, resolved.action, deadline, onExecutionStarted)
                val after = if (resolved.action.requiresFreshVerification()) {
                    observe(
                        deviceId = deviceId,
                        includeUiHierarchy = true,
                        freshness = ObservationFreshness.WAIT_FOR_CHANGE,
                        baselineRevision = before.revision,
                        timeoutMs = step.timeoutMs.coerceIn(100, MAX_STEP_TIMEOUT_MS),
                        deadline = deadline
                    )
                } else {
                    observe(
                        deviceId,
                        includeUiHierarchy = step.requiresUiHierarchy(),
                        deadline = deadline
                    )
                }
                ResolvedExecution(result, after)
            }
        }
    }

    private suspend fun executeScrollUntil(
        deviceId: String,
        action: AgentPlanAction.ScrollUntil,
        initial: AgentObservation,
        deadline: AgentOperationDeadline?,
        onExecutionStarted: () -> Unit
    ): ResolvedExecution {
        var observation = initial
        repeat(action.maxSwipes.coerceIn(1, MAX_SCROLL_SWIPES) + 1) { attempt ->
            when (SelectorResolver.resolveUnique(action.selector, observation.uiNodes)) {
                is SelectorResolution.Resolved -> return ResolvedExecution(
                    AgentToolResult(true, "Target became visible after $attempt swipe(s)"),
                    observation
                )
                is SelectorResolution.Ambiguous -> return ResolvedExecution(
                    AgentToolResult(false, "Target selector is ambiguous"),
                    observation
                )
                SelectorResolution.Missing -> Unit
            }
            if (attempt >= action.maxSwipes) return@repeat
            val swipe = swipeAction(action.direction, observation)
            val result = executeGatewayAction(deviceId, swipe, deadline, onExecutionStarted)
            if (!result.success) return ResolvedExecution(result, observation)
            observation = observe(
                deviceId = deviceId,
                includeUiHierarchy = true,
                freshness = ObservationFreshness.WAIT_FOR_CHANGE,
                baselineRevision = observation.revision,
                deadline = deadline
            )
        }
        return ResolvedExecution(AgentToolResult(false, "Target did not become visible"), observation)
    }

    private fun validatePlan(plan: AgentTaskPlan, maxSteps: Int): Result<Unit> = runCatching {
        require(plan.mode == AgentPlanMode.BATCH) { "Only batch plans can be executed directly" }
        require(plan.summary.isNotBlank()) { "Batch plan summary cannot be blank" }
        require(plan.steps.isNotEmpty() && plan.steps.size <= maxSteps) { "Batch plan step count is invalid" }
        require(plan.steps.map { it.id }.all { it.matches(STEP_ID_PATTERN) }) { "Batch plan has an invalid step ID" }
        require(plan.steps.map { it.id }.distinct().size == plan.steps.size) { "Batch plan has duplicate step IDs" }
        val seen = mutableMapOf<String, AgentPlanAction>()
        val navigationActions = mutableSetOf<String>()
        plan.steps.forEach { step ->
            require(step.timeoutMs in 100..MAX_STEP_TIMEOUT_MS) { "Step timeout is invalid" }
            when (val action = step.action) {
                is AgentPlanAction.KeyEvent -> Unit
                is AgentPlanAction.FindApp -> require(action.query.trim().length in 1..100)
                is AgentPlanAction.LaunchResolvedApp -> require(seen[action.sourceStepId] is AgentPlanAction.FindApp) {
                    "Launch must reference an earlier find_app step"
                }
                is AgentPlanAction.Wait -> require(action.durationMs in 100..3_000)
                is AgentPlanAction.TapSelector -> require(action.selector.isUsable())
                is AgentPlanAction.InputSelector -> {
                    require(action.selector.isUsable())
                    require(action.text.isNotBlank() && action.text.length <= MAX_PLAN_INPUT_LENGTH)
                }
                is AgentPlanAction.SwipeDirection -> {
                    require(action.distancePercent in 20..85)
                    require(action.durationMs in 50..3_000)
                }
                is AgentPlanAction.ScrollUntil -> {
                    require(action.selector.isUsable())
                    require(action.maxSwipes in 1..MAX_SCROLL_SWIPES)
                }
                is AgentPlanAction.WaitUntil -> require(action.predicate.isDeterministicCompletionGoal())
                is AgentPlanAction.ExtractText -> require(action.selector.isUsable())
            }
            val label = step.action.navigationLabel()
            if (label != null) require(navigationActions.add(label)) { "Batch plan repeats the same navigation action" }
            seen[step.id] = step.action
        }
        require(plan.goal.isDeterministicCompletionGoal()) {
            "Every model plan requires a meaningful deterministic task goal"
        }
        if (plan.steps.any { it.action.isSemanticUiAction() }) {
            require(plan.steps.filter { it.action.requiresPostcondition() }.all {
                it.postcondition.isDeterministicCompletionGoal()
            }) { "UI-changing plan steps require deterministic postconditions" }
        }
    }

    private fun resolveAction(
        action: AgentPlanAction,
        observation: AgentObservation,
        resolvedPackages: Map<String, String>
    ): ResolvedPlanAction? = when (action) {
        is AgentPlanAction.KeyEvent -> ResolvedPlanAction(AgentAction.KeyEvent(action.key))
        is AgentPlanAction.FindApp -> ResolvedPlanAction(AgentAction.FindApp(action.query))
        is AgentPlanAction.LaunchResolvedApp -> resolvedPackages[action.sourceStepId]
            ?.let { ResolvedPlanAction(AgentAction.LaunchPackage(it)) }
        is AgentPlanAction.Wait -> ResolvedPlanAction(AgentAction.Wait(action.durationMs))
        is AgentPlanAction.TapSelector -> resolveSelector(action.selector, observation)?.let { node ->
            ResolvedPlanAction(AgentAction.TapElement(observation.observationId, node.elementId, action.meta))
        }
        is AgentPlanAction.InputSelector -> resolveSelector(action.selector, observation)
            ?.takeIf { it.editable && !it.password }
            ?.let { node -> ResolvedPlanAction(AgentAction.InputText(action.text, observation.observationId, node.elementId, action.meta)) }
        is AgentPlanAction.SwipeDirection -> ResolvedPlanAction(
            swipeAction(action.direction, observation, action.distancePercent, action.durationMs)
        )
        is AgentPlanAction.ScrollUntil -> ResolvedPlanAction(swipeAction(action.direction, observation))
        is AgentPlanAction.WaitUntil,
        is AgentPlanAction.ExtractText -> ResolvedPlanAction(AgentAction.Observe)
    }

    private fun resolveSelector(selector: AgentSelector, observation: AgentObservation): UiNodeSnapshot? =
        (SelectorResolver.resolveUnique(selector, observation.uiNodes) as? SelectorResolution.Resolved)?.node

    private suspend fun verifyStep(
        step: AgentPlanStep,
        result: AgentToolResult,
        before: AgentObservation,
        after: AgentObservation,
        deviceId: String,
        resolvedPackages: Map<String, String>,
        deadline: AgentOperationDeadline?
    ): String? {
        if (!result.success) return result.output.ifBlank { "ADB action failed" }
        val predicate = if (step.postcondition !is AgentPredicate.Unspecified) {
            step.postcondition
        } else {
            step.verification.toPredicate(before, resolvedPackages)
        }
        if (predicate is AgentPredicate.Unspecified || predicate is AgentPredicate.Always) return null
        val verified = deadline.runWithinDeadline {
            predicateEvaluator.evaluate(predicate, deviceId, after, resolvedPackages)
        }
        return verified.reason.takeIf { !verified.matches }
    }

    private fun AgentVerification.toPredicate(
        before: AgentObservation,
        resolvedPackages: Map<String, String>
    ): AgentPredicate = when (this) {
        AgentVerification.None,
        AgentVerification.WaitCompleted -> AgentPredicate.Always
        AgentVerification.ActivityChanged -> if (before.currentActivity.isBlank()) {
            AgentPredicate.Unspecified
        } else {
            AgentPredicate.Not(AgentPredicate.ActivityMatches(Regex.escape(before.currentActivity)))
        }
        is AgentVerification.ForegroundPackage -> AgentPredicate.ForegroundPackage(
            packageName = packageName,
            sourceStepId = sourceStepId?.takeIf(resolvedPackages::containsKey)
        )
        is AgentVerification.UiElementPresent -> AgentPredicate.ElementPresent(
            AgentSelector(textAny = listOf(elementId), requireEnabled = false)
        )
    }

    private fun fail(
        state: AgentTaskUiState,
        plannedStep: AgentPlanStep,
        failure: String,
        onState: (AgentTaskUiState) -> Unit,
        action: AgentAction = AgentAction.FindApp("batch-plan"),
        riskLevel: AgentRiskLevel = AgentRiskLevel.SAFE,
        policyBlocked: Boolean = false
    ): AgentPlanExecution {
        val step = AgentStep(
            id = newId(),
            action = action,
            status = AgentStepStatus.FAILED,
            result = failure,
            riskLevel = riskLevel,
            confirmationReason = failure.takeIf { riskLevel == AgentRiskLevel.BLOCKED }.orEmpty()
        )
        val updated = state.copy(steps = state.steps + step, phase = AgentRunPhase.THINKING)
        onState(updated)
        return AgentPlanExecution(updated, false, failure, policyBlocked = policyBlocked)
    }

    private fun AgentTaskUiState.replaceStep(updated: AgentStep): AgentTaskUiState =
        copy(steps = steps.map { if (it.id == updated.id) updated else it })

    private fun AgentPlanStep.requiresUiHierarchy(): Boolean =
        action.isSemanticUiAction() || precondition.requiresUi() || postcondition.requiresUi() ||
            verification is AgentVerification.UiElementPresent

    private fun AgentPlanAction.isSemanticUiAction(): Boolean = when (this) {
        is AgentPlanAction.TapSelector,
        is AgentPlanAction.InputSelector,
        is AgentPlanAction.ScrollUntil,
        is AgentPlanAction.WaitUntil,
        is AgentPlanAction.ExtractText -> true
        else -> false
    }

    private fun AgentPlanAction.requiresPostcondition(): Boolean = when (this) {
        is AgentPlanAction.TapSelector,
        is AgentPlanAction.InputSelector,
        is AgentPlanAction.SwipeDirection -> true
        else -> false
    }

    private fun AgentPredicate.requiresUi(): Boolean = when (this) {
        is AgentPredicate.ElementPresent,
        is AgentPredicate.ElementAbsent,
        is AgentPredicate.ElementState,
        is AgentPredicate.TextPresent -> true
        is AgentPredicate.All -> predicates.any { it.requiresUi() }
        is AgentPredicate.Any -> predicates.any { it.requiresUi() }
        is AgentPredicate.Not -> predicate.requiresUi()
        else -> false
    }

    private fun AgentSelector.isUsable(): Boolean =
        !resourceId.isNullOrBlank() || textAny.isNotEmpty() || contentDescriptionAny.isNotEmpty()

    private fun AgentPlanAction.navigationLabel(): String? = when (this) {
        is AgentPlanAction.KeyEvent -> "key:${key.name}"
        is AgentPlanAction.FindApp -> "find:${query.trim().lowercase()}"
        is AgentPlanAction.LaunchResolvedApp -> "launch:$sourceStepId"
        is AgentPlanAction.TapSelector -> "tap:${selector.resourceId}:${selector.textAny}:${selector.role}"
        is AgentPlanAction.InputSelector -> "input:${selector.resourceId}:${selector.role}"
        is AgentPlanAction.SwipeDirection -> null
        is AgentPlanAction.ScrollUntil -> null
        is AgentPlanAction.WaitUntil -> null
        is AgentPlanAction.ExtractText -> null
        is AgentPlanAction.Wait -> null
    }

    private fun swipeAction(
        direction: AgentSwipeDirection,
        observation: AgentObservation,
        distancePercent: Int = 60,
        durationMs: Int = 350
    ): AgentAction.Swipe {
        val centerX = observation.screenWidth / 2
        val centerY = observation.screenHeight / 2
        val horizontal = observation.screenWidth * distancePercent / 200
        val vertical = observation.screenHeight * distancePercent / 200
        val points = when (direction) {
            AgentSwipeDirection.UP -> listOf(centerX, centerY + vertical, centerX, centerY - vertical)
            AgentSwipeDirection.DOWN -> listOf(centerX, centerY - vertical, centerX, centerY + vertical)
            AgentSwipeDirection.LEFT -> listOf(centerX + horizontal, centerY, centerX - horizontal, centerY)
            AgentSwipeDirection.RIGHT -> listOf(centerX - horizontal, centerY, centerX + horizontal, centerY)
        }
        return AgentAction.Swipe(points[0], points[1], points[2], points[3], durationMs)
    }

    private fun AgentAction.requiresFreshVerification(): Boolean = when (this) {
        AgentAction.Observe,
        is AgentAction.FindApp,
        is AgentAction.Wait -> false
        else -> true
    }

    private suspend fun observe(
        deviceId: String,
        includeUiHierarchy: Boolean,
        freshness: ObservationFreshness = ObservationFreshness.ALLOW_CACHE,
        baselineRevision: Long? = null,
        timeoutMs: Long = 8_000L,
        deadline: AgentOperationDeadline? = null
    ): AgentObservation = deadline.runWithinDeadline {
        observationCoordinator.observe(
            deviceId,
            AgentObservationRequest(
                includeUiHierarchy = includeUiHierarchy,
                freshness = freshness,
                baselineRevision = baselineRevision,
                timeoutMs = timeoutMs
            )
        ).observation
    }

    private suspend fun executeGatewayAction(
        deviceId: String,
        action: AgentAction,
        deadline: AgentOperationDeadline?,
        onExecutionStarted: () -> Unit
    ): AgentToolResult {
        var commandStarted = false
        return try {
            deadline.runWithinDeadline {
                onExecutionStarted()
                commandStarted = true
                deviceGateway.execute(deviceId, action)
            }
        } finally {
            if (commandStarted && action.isDeviceAction()) observationCoordinator.markMutation(deviceId)
        }
    }

    private fun newId(): String = UUID.randomUUID().toString()

    private companion object {
        val STEP_ID_PATTERN = Regex("^[a-z][a-z0-9_-]{0,31}$")
        const val FAILURE_PREFIX = "Verification failed: "
        const val MAX_STEP_RESULT_LENGTH = 2_000
        const val MAX_STEP_TIMEOUT_MS = 30_000L
        const val MAX_SCROLL_SWIPES = 8
        const val MAX_PLAN_INPUT_LENGTH = 2_000
        const val PREDICATE_POLL_MS = 250L
    }
}

private data class ResolvedPlanAction(val action: AgentAction)
private data class ResolvedExecution(val result: AgentToolResult, val after: AgentObservation)

private fun AgentAction.isDeviceAction(): Boolean = when (this) {
    AgentAction.Observe,
    is AgentAction.FindApp,
    is AgentAction.Wait,
    is AgentAction.Finish -> false
    else -> true
}

private fun List<AgentStep>.executedDeviceActionCount(): Int = sumOf { step ->
    if (step.action.isDeviceAction()) step.executedActionCount else 0
}

internal data class AgentPlanExecution(
    val state: AgentTaskUiState,
    val completed: Boolean,
    val failure: String = "",
    val deniedByUser: Boolean = false,
    val policyBlocked: Boolean = false,
    val deterministicGoalProvided: Boolean = false,
    val deterministicGoalMatched: Boolean = false,
    val verifiedObservation: AgentObservation? = null
)
