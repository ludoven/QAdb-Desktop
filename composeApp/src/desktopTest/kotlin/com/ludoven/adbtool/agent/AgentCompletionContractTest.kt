package com.ludoven.adbtool.agent

import kotlin.test.Test
import kotlin.test.assertIs

class AgentCompletionContractTest {
    private val contract = CompletionContract()

    @Test
    fun `read only completion requires fresh evidence and zero device actions`() {
        val intent = AgentTaskIntent(
            kind = AgentTaskIntentKind.DEVICE_STATUS,
            accessLevel = AgentTaskAccessLevel.STATUS_ONLY,
            reason = "test"
        )

        assertIs<CompletionContractResult.Passed>(
            contract.evaluate(readOnlyInput(intent, hasFreshEvidence = true, deviceActionCount = 0))
        )
        assertIs<CompletionContractResult.Rejected>(
            contract.evaluate(readOnlyInput(intent, hasFreshEvidence = false, deviceActionCount = 0))
        )
        assertIs<CompletionContractResult.Rejected>(
            contract.evaluate(readOnlyInput(intent, hasFreshEvidence = true, deviceActionCount = 1))
        )
    }

    @Test
    fun `device operation cannot complete without a matched deterministic goal`() {
        val intent = AgentTaskIntent(
            kind = AgentTaskIntentKind.DEVICE_OPERATION,
            accessLevel = AgentTaskAccessLevel.MUTATING,
            hasExplicitMutation = true,
            reason = "test"
        )

        assertIs<CompletionContractResult.Rejected>(
            contract.evaluate(operationInput(intent, goalProvided = false, goalMatched = false))
        )
        assertIs<CompletionContractResult.Rejected>(
            contract.evaluate(operationInput(intent, goalProvided = true, goalMatched = false))
        )
        assertIs<CompletionContractResult.Passed>(
            contract.evaluate(operationInput(intent, goalProvided = true, goalMatched = true))
        )
    }

    @Test
    fun `failed unverified or denied step always blocks completion`() {
        val intent = AgentTaskIntent(
            kind = AgentTaskIntentKind.DEVICE_OPERATION,
            accessLevel = AgentTaskAccessLevel.MUTATING,
            hasExplicitMutation = true,
            reason = "test"
        )
        listOf(
            AgentStepStatus.FAILED,
            AgentStepStatus.UNVERIFIED,
            AgentStepStatus.DENIED
        ).forEach { status ->
            val result = contract.evaluate(
                operationInput(intent, goalProvided = true, goalMatched = true).copy(
                    steps = listOf(AgentStep("step", AgentAction.Tap(10, 10), status))
                )
            )
            assertIs<CompletionContractResult.Rejected>(result)
        }
    }

    @Test
    fun `typed mutation evidence binds fresh revision goal and executed action`() {
        val intent = AgentTaskIntent(
            kind = AgentTaskIntentKind.DEVICE_OPERATION,
            accessLevel = AgentTaskAccessLevel.MUTATING,
            hasExplicitMutation = true,
            reason = "test"
        )
        val base = CompletionEvidence(
            freshRevision = 9,
            deviceActionCount = 1,
            deterministicGoalFingerprint = "goal-fingerprint",
            verification = CompletionVerificationVerdict.MATCHED,
            actionRevisions = listOf(CompletionActionRevision(8, 9, executed = true))
        )

        assertIs<CompletionContractResult.Passed>(
            contract.evaluate(CompletionContractInput(intent, base, emptyList()))
        )
        assertIs<CompletionContractResult.Rejected>(
            contract.evaluate(CompletionContractInput(intent, base.copy(freshRevision = null), emptyList()))
        )
        assertIs<CompletionContractResult.Rejected>(
            contract.evaluate(CompletionContractInput(intent, base.copy(actionRevisions = emptyList()), emptyList()))
        )
        assertIs<CompletionContractResult.Rejected>(
            contract.evaluate(
                CompletionContractInput(
                    intent,
                    base.copy(actionRevisions = listOf(CompletionActionRevision(9, 8, executed = true))),
                    emptyList()
                )
            )
        )
    }

    @Test
    fun `navigation read completion accepts only fully bound effects inside authority`() {
        val intent = AgentTaskIntent(
            kind = AgentTaskIntentKind.APP_CONTENT_READ,
            accessLevel = AgentTaskAccessLevel.NAVIGATION_READ_ONLY,
            hasExplicitDeviceAction = true,
            reason = "test"
        )
        val allowed = CompletionEvidence(
            freshRevision = 12,
            deviceActionCount = 3,
            deterministicGoalFingerprint = "read-goal",
            verification = CompletionVerificationVerdict.MATCHED,
            actionRevisions = listOf(
                CompletionActionRevision(8, 9, executed = true, effect = AgentActionEffect.LAUNCH_APP),
                CompletionActionRevision(9, 10, executed = true, effect = AgentActionEffect.NAVIGATION),
                CompletionActionRevision(10, 12, executed = true, effect = AgentActionEffect.SCROLL)
            )
        )

        assertIs<CompletionContractResult.Passed>(
            contract.evaluate(CompletionContractInput(intent, allowed, emptyList()))
        )
        assertIs<CompletionContractResult.Rejected>(
            contract.evaluate(
                CompletionContractInput(
                    intent,
                    allowed.copy(
                        actionRevisions = allowed.actionRevisions.dropLast(1) +
                            CompletionActionRevision(10, 12, executed = true, effect = AgentActionEffect.DATA_ENTRY)
                    ),
                    emptyList()
                )
            )
        )
        assertIs<CompletionContractResult.Rejected>(
            contract.evaluate(
                CompletionContractInput(
                    intent,
                    allowed.copy(
                        actionRevisions = allowed.actionRevisions.dropLast(1) +
                            CompletionActionRevision(10, 12, executed = true)
                    ),
                    emptyList()
                )
            )
        )
        assertIs<CompletionContractResult.Rejected>(
            contract.evaluate(
                CompletionContractInput(intent, allowed.copy(actionRevisions = allowed.actionRevisions.dropLast(1)), emptyList())
            )
        )
    }

    @Test
    fun `legacy navigation read accepts fresh matched evidence without typed action revisions`() {
        val intent = AgentTaskIntent(
            kind = AgentTaskIntentKind.APP_CONTENT_READ,
            accessLevel = AgentTaskAccessLevel.NAVIGATION_READ_ONLY,
            hasExplicitDeviceAction = true,
            reason = "test"
        )

        assertIs<CompletionContractResult.Passed>(
            contract.evaluate(
                CompletionContractInput(
                    intent = intent,
                    hasFreshEvidence = true,
                    deviceActionCount = 3,
                    deterministicGoalProvided = true,
                    deterministicGoalMatched = true,
                    steps = emptyList()
                )
            )
        )
        assertIs<CompletionContractResult.Rejected>(
            contract.evaluate(
                CompletionContractInput(
                    intent = intent,
                    hasFreshEvidence = false,
                    deviceActionCount = 3,
                    deterministicGoalProvided = true,
                    deterministicGoalMatched = false,
                    steps = emptyList()
                )
            )
        )
    }

    private fun readOnlyInput(
        intent: AgentTaskIntent,
        hasFreshEvidence: Boolean,
        deviceActionCount: Int
    ) = CompletionContractInput(
        intent = intent,
        hasFreshEvidence = hasFreshEvidence,
        deviceActionCount = deviceActionCount,
        deterministicGoalProvided = false,
        deterministicGoalMatched = false,
        steps = emptyList()
    )

    private fun operationInput(
        intent: AgentTaskIntent,
        goalProvided: Boolean,
        goalMatched: Boolean
    ) = CompletionContractInput(
        intent = intent,
        hasFreshEvidence = goalMatched,
        deviceActionCount = 1,
        deterministicGoalProvided = goalProvided,
        deterministicGoalMatched = goalMatched,
        steps = emptyList()
    )
}
