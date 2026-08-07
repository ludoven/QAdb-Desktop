package com.ludoven.adbtool.agent

import java.util.UUID

internal data class GuardedWorkflowExecution(
    val state: AgentTaskUiState,
    val completed: Boolean,
    val failure: String = ""
)

/** Executes persisted actions only after resolving each selector against a fresh observation. */
internal class GuardedWorkflowExecutor(
    private val deviceGateway: AgentDeviceGateway,
    private val riskEvaluator: AgentRiskEvaluator
) {
    suspend fun execute(
        workflow: AgentWorkflow,
        deviceId: String,
        initialState: AgentTaskUiState,
        onState: (AgentTaskUiState) -> Unit,
        confirmSensitiveAction: suspend (AgentStep) -> Boolean
    ): GuardedWorkflowExecution {
        var state = initialState.copy(executionStrategy = AgentExecutionStrategy.GUARDED_WORKFLOW, phase = AgentRunPhase.EXECUTING)
        for (replayStep in workflow.replaySteps) {
            val beforeObservation = deviceGateway.observeLightweight(deviceId, includeUiHierarchy = true)
            val beforeState = PageSignatureEngine.state(beforeObservation)
            val beforeMatch = WorkflowStateMatcher.match(replayStep.before, beforeState)
            if (!beforeMatch.matches) return fail(state, "Workflow guard failed before a step: ${beforeMatch.reason}", onState)

            val action = when (val replayAction = replayStep.action) {
                is WorkflowReplayAction.KeyEvent -> AgentAction.KeyEvent(replayAction.key)
                is WorkflowReplayAction.LaunchPackage -> AgentAction.LaunchPackage(replayAction.packageName)
                is WorkflowReplayAction.Wait -> AgentAction.Wait(replayAction.durationMs)
                is WorkflowReplayAction.TapSelector -> {
                    val node = SelectorResolver.resolve(replayAction.selector, beforeObservation.uiNodes)
                        ?: return fail(state, "Workflow selector is no longer available", onState)
                    AgentAction.TapElement(beforeObservation.observationId, node.elementId)
                }
            }
            validateAgentAction(action, beforeObservation).getOrElse { return fail(state, it.message ?: "Workflow action is invalid", onState, action) }
            val capabilityReason = deviceGateway.confirmationRequirement(deviceId, action, beforeObservation)
            val risk = riskEvaluator.evaluate(action, beforeObservation, capabilityReason)
            var step = AgentStep(
                id = UUID.randomUUID().toString(), action = action,
                status = if (risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED) AgentStepStatus.AWAITING_CONFIRMATION else AgentStepStatus.RUNNING,
                riskLevel = risk.level, confirmationReason = risk.reason
            )
            state = state.copy(steps = state.steps + step, pendingConfirmation = step.takeIf { risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED }, phase = if (risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED) AgentRunPhase.AWAITING_CONFIRMATION else AgentRunPhase.EXECUTING)
            onState(state)
            if (risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED && !confirmSensitiveAction(step)) {
                step = step.copy(status = AgentStepStatus.DENIED, result = "User denied this workflow action")
                state = state.replaceWorkflowStep(step).copy(pendingConfirmation = null, phase = AgentRunPhase.THINKING)
                onState(state)
                return GuardedWorkflowExecution(state, false, "User denied a workflow action")
            }
            if (risk.level == AgentRiskLevel.CONFIRMATION_REQUIRED) {
                step = step.copy(status = AgentStepStatus.RUNNING)
                state = state.replaceWorkflowStep(step).copy(pendingConfirmation = null, phase = AgentRunPhase.EXECUTING)
                onState(state)
            }
            val result = deviceGateway.execute(deviceId, action)
            if (!result.success) return fail(state.replaceWorkflowStep(step.copy(status = AgentStepStatus.FAILED, result = result.output)), result.output.ifBlank { "ADB action failed" }, onState)
            val afterObservation = deviceGateway.observeLightweight(deviceId, includeUiHierarchy = true)
            val afterMatch = WorkflowStateMatcher.match(replayStep.after, PageSignatureEngine.state(afterObservation))
            if (!afterMatch.matches) return fail(state.replaceWorkflowStep(step.copy(status = AgentStepStatus.FAILED, result = afterMatch.reason)), "Workflow guard failed after a step: ${afterMatch.reason}", onState)
            state = state.replaceWorkflowStep(step.copy(status = AgentStepStatus.COMPLETED, result = result.output.ifBlank { "Completed" })).copy(deviceState = PageSignatureEngine.state(afterObservation))
            onState(state)
        }
        return GuardedWorkflowExecution(state, true)
    }

    private fun fail(state: AgentTaskUiState, reason: String, onState: (AgentTaskUiState) -> Unit, action: AgentAction = AgentAction.Observe): GuardedWorkflowExecution {
        val withFailure = if (state.steps.lastOrNull()?.status == AgentStepStatus.FAILED) state else state.copy(steps = state.steps + AgentStep(UUID.randomUUID().toString(), action, AgentStepStatus.FAILED, reason))
        val next = withFailure.copy(phase = AgentRunPhase.THINKING, executionDetails = (withFailure.executionDetails + "Workflow handoff: $reason").takeLast(40))
        onState(next)
        return GuardedWorkflowExecution(next, false, reason)
    }

    private fun AgentTaskUiState.replaceWorkflowStep(updated: AgentStep): AgentTaskUiState = copy(steps = steps.map { if (it.id == updated.id) updated else it })
}
