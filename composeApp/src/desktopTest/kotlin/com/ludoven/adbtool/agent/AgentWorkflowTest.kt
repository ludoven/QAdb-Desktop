package com.ludoven.adbtool.agent

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentWorkflowTest {
    private val node = UiNodeSnapshot(
        elementId = "ephemeral-1", text = "设置", contentDescription = "", className = "android.widget.Button",
        packageName = "com.example.app", bounds = UiBounds(0, 0, 30, 30), clickable = true, editable = false,
        enabled = true, password = false, resourceId = "com.example.app:id/settings", role = "button"
    )
    private val state = DeviceState("com.example.app", "com.example.app/.Main", PageSignature("page", "com.example.app", "Main"), listOf(node))

    @Test fun `fully verified safe execution creates reviewable draft`() {
        val draft = WorkflowDraftFactory.fromRecordedTask(
            "打开设置", state,
            listOf(WorkflowRecordedStep(AgentAction.TapElement("obs-1", node.elementId), state, state))
        )
        assertNotNull(draft)
        assertTrue(draft.status == WorkflowStatus.DRAFT)
        assertTrue(WorkflowSanitizer.valid(draft))
        assertTrue(draft.canEnable)
        assertTrue((draft.replaySteps.single().action as WorkflowReplayAction.TapSelector).selector.resourceId != null)
    }

    @Test fun `failed or sensitive tasks never create automatic workflow`() {
        val failed = WorkflowDraftFactory.fromRecordedTask("x", null, emptyList())
        val secret = WorkflowDraftFactory.fromRecordedTask("x", state, listOf(WorkflowRecordedStep(AgentAction.InputText("password=secret"), state, state)))
        assertNull(failed); assertNull(secret)
        assertFalse(WorkflowSanitizer.safe(AgentAction.InputText("Bearer token")))
    }

    @Test fun `state guards ignore transient element ids and node ordering`() {
        val reordered = state.copy(nodes = listOf(node.copy(elementId = "ephemeral-2")))
        val match = WorkflowStateMatcher.match(WorkflowStateMatcher.guard(state), reordered)
        assertTrue(match.matches)
    }

    @Test fun `knowledge cards reject secrets and oversized guides`() {
        assertFalse(WorkflowSanitizer.validKnowledge(AgentAppKnowledgeCard(packageName = "com.example.app", title = "x", guide = "token=secret")))
        assertFalse(WorkflowSanitizer.validKnowledge(AgentAppKnowledgeCard(packageName = "com.example.app", title = "x", guide = "a".repeat(4_001))))
        assertTrue(WorkflowSanitizer.validKnowledge(AgentAppKnowledgeCard(packageName = "com.example.app", title = "设置", guide = "先打开设置页面")))
    }
}
