package com.ludoven.adbtool.agent

import java.util.UUID

internal object AgentTaskTemplateMatcher {
    fun match(task: String): AgentTaskPlan? {
        val normalized = task
            .lowercase()
            .replace(Regex("[\\s，,。.!！]+"), "")
        if (normalized !in SETTINGS_TEMPLATE_TASKS) return null
        return AgentTaskPlan(
            mode = AgentPlanMode.BATCH,
            summary = "Return home and open system Settings",
            steps = listOf(
                AgentPlanStep("home", AgentPlanAction.KeyEvent(AgentKey.HOME)),
                AgentPlanStep(
                    "settings",
                    AgentPlanAction.LaunchKnownPackage(SETTINGS_PACKAGE),
                    AgentVerification.ForegroundPackage(packageName = SETTINGS_PACKAGE)
                )
            )
        )
    }

    private val SETTINGS_TEMPLATE_TASKS = setOf(
        "返回桌面并打开系统设置",
        "回到桌面并打开系统设置",
        "returnhomeandopensystemsettings"
    )
    private const val SETTINGS_PACKAGE = "com.android.settings"
}

internal class AgentPlanExecutor(
    private val deviceGateway: AgentDeviceGateway,
    private val riskEvaluator: AgentRiskEvaluator
) {
    suspend fun execute(
        plan: AgentTaskPlan,
        deviceId: String,
        initialState: AgentTaskUiState,
        onState: (AgentTaskUiState) -> Unit,
        confirmSensitiveAction: suspend (AgentStep) -> Boolean,
        strategy: AgentExecutionStrategy,
        maxSteps: Int
    ): AgentPlanExecution {
        validatePlan(plan, strategy, maxSteps).getOrElse {
            return AgentPlanExecution(initialState, false, it.message ?: "Invalid batch plan")
        }
        var state = initialState.copy(executionStrategy = strategy, phase = AgentRunPhase.EXECUTING)
        val resolvedPackages = mutableMapOf<String, String>()
        var previousObservation = deviceGateway.observeLightweight(deviceId)
        onState(state)

        for (plannedStep in plan.steps) {
            val action = resolveAction(plannedStep, resolvedPackages)
                ?: return fail(state, plannedStep, "Referenced app resolution is unavailable", onState)
            validateAgentAction(action, previousObservation).getOrElse {
                return fail(state, plannedStep, it.message ?: "Invalid batch action", onState, action)
            }
            val capabilityReason = deviceGateway.confirmationRequirement(deviceId, action, previousObservation)
            val risk = riskEvaluator.evaluate(action, previousObservation, capabilityReason)
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
                pendingConfirmation = step.takeIf { risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED },
                phase = if (risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED) {
                    AgentRunPhase.AWAITING_CONFIRMATION
                } else {
                    AgentRunPhase.EXECUTING
                }
            )
            onState(state)
            if (risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED) {
                if (!confirmSensitiveAction(step)) {
                    step = step.copy(status = AgentStepStatus.DENIED, result = "User denied this action")
                    state = state.replaceStep(step).copy(pendingConfirmation = null, phase = AgentRunPhase.THINKING)
                    onState(state)
                    return AgentPlanExecution(state, false, "User denied a batch action")
                }
                step = step.copy(status = AgentStepStatus.RUNNING)
                state = state.replaceStep(step).copy(pendingConfirmation = null, phase = AgentRunPhase.EXECUTING)
                onState(state)
            }

            val result = deviceGateway.execute(deviceId, action)
            if (action is AgentAction.FindApp && result.success && result.resolvedPackages.size == 1) {
                resolvedPackages[plannedStep.id] = result.resolvedPackages.single()
            }
            val needsUi = plannedStep.verification is AgentVerification.UiElementPresent
            val afterObservation = if (plannedStep.verification == AgentVerification.None ||
                plannedStep.verification == AgentVerification.WaitCompleted
            ) {
                previousObservation
            } else {
                deviceGateway.observeLightweight(deviceId, includeUiHierarchy = needsUi)
            }
            val failure = verify(plannedStep.verification, result, previousObservation, afterObservation, resolvedPackages)
            if (failure != null) {
                step = step.copy(
                    status = AgentStepStatus.FAILED,
                    result = "${result.output.ifBlank { "ADB action failed" }}\n$FAILURE_PREFIX$failure"
                )
                state = state.replaceStep(step).copy(phase = AgentRunPhase.THINKING)
                onState(state)
                return AgentPlanExecution(state, false, failure)
            }
            step = step.copy(status = AgentStepStatus.COMPLETED, result = result.output.ifBlank { "Completed" })
            state = state.replaceStep(step).copy(phase = AgentRunPhase.EXECUTING)
            previousObservation = afterObservation
            onState(state)
        }
        return AgentPlanExecution(state, true)
    }

    private fun validatePlan(
        plan: AgentTaskPlan,
        strategy: AgentExecutionStrategy,
        maxSteps: Int
    ): Result<Unit> = runCatching {
        require(plan.mode == AgentPlanMode.BATCH) { "Only batch plans can be executed directly" }
        require(plan.steps.isNotEmpty() && plan.steps.size <= maxSteps) { "Batch plan step count is invalid" }
        require(plan.steps.map { it.id }.all { it.matches(STEP_ID_PATTERN) }) { "Batch plan has an invalid step ID" }
        require(plan.steps.map { it.id }.distinct().size == plan.steps.size) { "Batch plan has duplicate step IDs" }
        val seen = mutableMapOf<String, AgentPlanAction>()
        val navigationActions = mutableSetOf<String>()
        plan.steps.forEach { step ->
            when (val action = step.action) {
                is AgentPlanAction.KeyEvent -> Unit
                is AgentPlanAction.FindApp -> require(action.query.trim().length in 1..100)
                is AgentPlanAction.LaunchResolvedApp -> require(seen[action.sourceStepId] is AgentPlanAction.FindApp) {
                    "Launch must reference an earlier find_app step"
                }
                is AgentPlanAction.LaunchKnownPackage -> {
                    require(strategy == AgentExecutionStrategy.FAST_TEMPLATE) { "Known package launch is template-only" }
                    require(action.packageName == SETTINGS_PACKAGE) { "Unknown template package" }
                }
                is AgentPlanAction.Wait -> require(action.durationMs in 100..3_000)
            }
            val actionLabel = step.action.navigationLabel()
            if (actionLabel != null) require(navigationActions.add(actionLabel)) {
                "Batch plan repeats the same navigation action"
            }
            val verification = step.verification
            if (verification is AgentVerification.ForegroundPackage && verification.sourceStepId != null) {
                require(seen[verification.sourceStepId] is AgentPlanAction.FindApp || verification.sourceStepId == step.id) {
                    "Foreground verification references an unknown step"
                }
            }
            seen[step.id] = step.action
        }
    }

    private fun resolveAction(
        step: AgentPlanStep,
        resolvedPackages: Map<String, String>
    ): AgentAction? = when (val action = step.action) {
        is AgentPlanAction.KeyEvent -> AgentAction.KeyEvent(action.key)
        is AgentPlanAction.FindApp -> AgentAction.FindApp(action.query)
        is AgentPlanAction.LaunchResolvedApp -> resolvedPackages[action.sourceStepId]?.let(AgentAction::LaunchPackage)
        is AgentPlanAction.LaunchKnownPackage -> AgentAction.LaunchPackage(action.packageName)
        is AgentPlanAction.Wait -> AgentAction.Wait(action.durationMs)
    }

    private fun AgentPlanAction.navigationLabel(): String? = when (this) {
        is AgentPlanAction.KeyEvent -> "key:${key.name}"
        is AgentPlanAction.FindApp -> "find:${query.trim().lowercase()}"
        is AgentPlanAction.LaunchResolvedApp -> "launch:$sourceStepId"
        is AgentPlanAction.LaunchKnownPackage -> "launch:$packageName"
        is AgentPlanAction.Wait -> null
    }

    private fun verify(
        verification: AgentVerification,
        result: AgentToolResult,
        before: AgentObservation,
        after: AgentObservation,
        resolvedPackages: Map<String, String>
    ): String? {
        if (!result.success) return result.output.ifBlank { "ADB action failed" }
        return when (verification) {
            AgentVerification.None,
            AgentVerification.WaitCompleted -> null
            AgentVerification.ActivityChanged -> if (before.currentActivity != after.currentActivity) {
                null
            } else {
                "Expected the foreground Activity to change"
            }
            is AgentVerification.ForegroundPackage -> {
                val expected = verification.packageName ?: verification.sourceStepId?.let(resolvedPackages::get)
                    ?: return "Foreground package verification has no resolved package"
                if (after.currentActivity.belongsToPackage(expected)) null
                else "Expected foreground package $expected, observed ${after.currentActivity.ifBlank { "unknown" }}"
            }
            is AgentVerification.UiElementPresent -> if (after.uiNodes.any { it.elementId == verification.elementId }) {
                null
            } else {
                "Expected UI element ${verification.elementId} is not present"
            }
        }
    }

    private fun fail(
        state: AgentTaskUiState,
        plannedStep: AgentPlanStep,
        failure: String,
        onState: (AgentTaskUiState) -> Unit,
        action: AgentAction = AgentAction.FindApp("batch-plan")
    ): AgentPlanExecution {
        val step = AgentStep(newId(), action, AgentStepStatus.FAILED, failure)
        val updated = state.copy(steps = state.steps + step, phase = AgentRunPhase.THINKING)
        onState(updated)
        return AgentPlanExecution(updated, false, failure)
    }

    private fun AgentTaskUiState.replaceStep(updated: AgentStep): AgentTaskUiState = copy(
        steps = steps.map { if (it.id == updated.id) updated else it }
    )

    private fun String.belongsToPackage(packageName: String): Boolean =
        contains("$packageName/") || contains("$packageName ") || trim() == packageName

    private fun newId(): String = UUID.randomUUID().toString()

    private companion object {
        val STEP_ID_PATTERN = Regex("^[a-z][a-z0-9_-]{0,31}$")
        const val SETTINGS_PACKAGE = "com.android.settings"
        const val FAILURE_PREFIX = "Verification failed: "
    }
}

internal data class AgentPlanExecution(
    val state: AgentTaskUiState,
    val completed: Boolean,
    val failure: String = ""
)
