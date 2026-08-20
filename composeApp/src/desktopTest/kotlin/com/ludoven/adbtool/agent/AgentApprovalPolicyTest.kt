package com.ludoven.adbtool.agent

import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class AgentApprovalPolicyTest {
    @Test
    fun `smart approval keeps navigation input and search clearing uninterrupted`() {
        val preferences = preferences(AgentApprovalPolicy.SMART)
        val evaluator = AgentRiskEvaluator(preferences)
        val observation = observation("清除搜索", "clear search")

        assertSafe(evaluator.evaluate(AgentAction.TapElement(observation.observationId, "target"), observation))
        assertSafe(evaluator.evaluate(AgentAction.InputText("hello"), observation))
        assertSafe(evaluator.evaluate(AgentAction.LaunchPackage("com.example.app"), observation))
        assertSafe(evaluator.evaluate(AgentAction.KeyEvent(AgentKey.HOME), observation))
    }

    @Test
    fun `smart approval allows routine actions and prompts only for destructive data changes`() {
        val evaluator = AgentRiskEvaluator(preferences(AgentApprovalPolicy.SMART))
        val sendObservation = observation("发送", "send")

        assertSafe(evaluator.evaluate(AgentAction.TapElement(sendObservation.observationId, "target"), sendObservation))
        assertSafe(evaluator.evaluate(
            AgentAction.LaunchPackage(
                "com.tencent.mm",
                AgentActionMeta(
                    intent = "打开微信以发送消息",
                    target = "微信",
                    operationKind = AgentOperationKind.SEND
                )
            ),
            observation()
        ))
        assertSafe(evaluator.evaluate(
            AgentAction.Tap(10, 10, meta = AgentActionMeta(operationKind = AgentOperationKind.PERMISSION)),
            observation()
        ))
        assertSafe(evaluator.evaluate(AgentAction.ForceStopPackage("com.example.app"), observation()))
        assertSafe(evaluator.evaluate(AgentAction.RebootDevice, observation()))

        val clearDataObservation = observation("清除数据", "clear data")
        assertPrompt(evaluator.evaluate(AgentAction.TapElement(clearDataObservation.observationId, "target"), clearDataObservation))
        assertPrompt(evaluator.evaluate(
            AgentAction.Tap(10, 10, meta = AgentActionMeta(operationKind = AgentOperationKind.DELETE)),
            observation()
        ))
        assertPrompt(evaluator.evaluate(AgentAction.ClearAppData("com.example.app"), observation()))
        assertPrompt(evaluator.evaluate(AgentAction.UninstallPackage("com.example.app"), observation()))
        assertPrompt(evaluator.evaluate(AgentAction.InputText("你好"), observation(), "Installing the local Unicode input helper requires approval"))
    }

    @Test
    fun `cautious approval retains broad clear text review`() {
        val observation = observation("清除搜索", "clear search")
        assertPrompt(
            AgentRiskEvaluator(preferences(AgentApprovalPolicy.CAUTIOUS)).evaluate(
                AgentAction.TapElement(observation.observationId, "target"), observation
            )
        )
    }

    @Test
    fun `approval policy is persisted locally`() {
        val node = Preferences.userRoot().node("/qadb-tests/approval/${UUID.randomUUID()}")
        try {
            AgentApprovalPreferences(node).setPolicy(AgentApprovalPolicy.CAUTIOUS)
            assertEquals(AgentApprovalPolicy.CAUTIOUS, AgentApprovalPreferences(node).policy.value)
        } finally {
            node.removeNode()
        }
    }

    private fun preferences(policy: AgentApprovalPolicy): AgentApprovalPreferences {
        val node = Preferences.userRoot().node("/qadb-tests/approval/${UUID.randomUUID()}")
        return AgentApprovalPreferences(node).also { it.setPolicy(policy) }
    }

    private fun observation(text: String = "设置", description: String = "settings"): AgentObservation = AgentObservation(
        screenshotPng = null,
        uiHierarchy = "",
        currentActivity = "com.example.app/.MainActivity",
        screenWidth = 100,
        screenHeight = 100,
        observationId = "observation",
        uiNodes = listOf(
            UiNodeSnapshot(
                elementId = "target", text = text, contentDescription = description,
                className = "android.widget.Button", packageName = "com.example.app",
                bounds = UiBounds(0, 0, 20, 20), clickable = true, editable = false,
                enabled = true, password = false
            )
        )
    )

    private fun assertSafe(assessment: AgentRiskAssessment) = assertEquals(AgentRiskLevel.SAFE, assessment.level)
    private fun assertPrompt(assessment: AgentRiskAssessment) = assertEquals(AgentRiskLevel.CONFIRMATION_REQUIRED, assessment.level)
}
