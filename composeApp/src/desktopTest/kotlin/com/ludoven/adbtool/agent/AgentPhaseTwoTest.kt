package com.ludoven.adbtool.agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AgentPhaseTwoTest {
    @Test
    fun `selector keeps exact text when a repeated resource id is available`() {
        val selector = SelectorResolver.from(
            node(
                resourceId = "com.tencent.mm:id/odf",
                text = "奶娃",
                role = "TextView"
            )
        )

        assertEquals("com.tencent.mm:id/odf", selector.resourceId)
        assertEquals(listOf("奶娃"), selector.textAny)
        assertEquals("TextView", selector.role)
    }

    @Test
    fun `default task budget supports multi step execution`() {
        val budget = AgentBudget()
        assertEquals(16, budget.maxModelCalls)
        assertEquals(100_000, budget.maxInputTokens)
        assertEquals(10_000, budget.maxOutputTokens)
    }

    @Test
    fun `page signatures ignore volatile element ids and diff exposes changed controls`() {
        val first = observation("Save")
        val second = observation("Save")
        val changed = observation("Send")
        assertEquals(PageSignatureEngine.signature(first), PageSignatureEngine.signature(second))
        val diff = PageSignatureEngine.diff(PageSignatureEngine.state(first), PageSignatureEngine.state(changed))
        assertTrue(diff.changed)
        assertTrue(diff.addedElementIds.isNotEmpty())
    }

    @Test
    fun `selector favors resource id over matching text`() {
        val node = SelectorResolver.resolve(
            Selector(resourceId = "app:id/send", textAny = listOf("Send")),
            listOf(node("e1", "Send", "app:id/cancel"), node("e2", "Send", "app:id/send"))
        )
        assertNotNull(node)
        assertEquals("e2", node.elementId)
    }

    @Test
    fun `budget degrades progressively and stops at token cap`() {
        val tracker = AgentBudgetTracker(AgentBudget(maxModelCalls = 8, maxInputTokens = 100, maxOutputTokens = 100))
        repeat(4) { tracker.record(AgentUsage(promptTokens = 13), false) }
        assertEquals(AgentBudgetMode.DIFF_ONLY, tracker.current().mode)
        tracker.record(AgentUsage(promptTokens = 50), false)
        assertEquals(AgentBudgetMode.EXHAUSTED, tracker.current().mode)
        assertFalse(tracker.canRequest(false, false))
    }

    @Test
    fun `budget reserves the last model call for a terminal result`() {
        val tracker = AgentBudgetTracker(AgentBudget(maxModelCalls = 8))
        repeat(7) { tracker.record(AgentUsage(), false) }

        assertEquals(1, tracker.remainingModelCalls())
        assertTrue(tracker.shouldForceFinalResponse())
        assertEquals(AgentBudgetMode.FINAL_RECOVERY_ONLY, tracker.current().mode)
        assertFalse(tracker.canSpendOnCompaction())

        tracker.record(AgentUsage(), false)
        assertFalse(tracker.canRequest(false, false))
        assertEquals("Model call budget reached (8/8)", tracker.current().stopReason)
    }

    @Test
    fun `disabled task budget keeps recording usage without blocking requests`() {
        val tracker = AgentBudgetTracker(
            AgentBudget(
                maxModelCalls = 1,
                maxInputTokens = 1,
                maxOutputTokens = 1,
                maxVisionCalls = 0,
                maxReplans = 0,
                enforceLimits = false
            )
        )

        repeat(3) {
            assertTrue(tracker.canRequest(includeVision = true, isReplan = true))
            tracker.record(
                AgentUsage(promptTokens = 10, completionTokens = 10, totalTokens = 20),
                usedVision = true,
                isReplan = true
            )
        }

        assertEquals(3, tracker.current().modelCalls)
        assertEquals(60, tracker.current().usage.totalTokens)
        assertEquals(AgentBudgetMode.NORMAL, tracker.current().mode)
        assertEquals(0, tracker.current().modelCallLimit)
        assertEquals(null, tracker.current().stopReason)
    }

    @Test
    fun `cached selector only becomes reusable after proven success`() {
        val cached = CachedSelector("page", "send", Selector(textAny = listOf("Send")), successCount = 3)
        assertTrue(cached.reusable())
        assertFalse(cached.copy(failureCount = 1).reusable())
    }

    private fun observation(text: String) = AgentObservation(
        screenshotPng = null, uiHierarchy = "", currentActivity = "app/.Main", screenWidth = 100, screenHeight = 100,
        uiNodes = listOf(node("volatile", text, "app:id/action"))
    )

    private fun node(
        id: String = "element",
        text: String,
        resourceId: String,
        role: String = "button"
    ) = UiNodeSnapshot(
        elementId = id, text = text, contentDescription = "", className = role, packageName = "app",
        bounds = UiBounds(0, 0, 20, 20), clickable = true, editable = false, enabled = true, password = false,
        resourceId = resourceId, role = role
    )
}
