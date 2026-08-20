package com.ludoven.adbtool.pages

import com.ludoven.adbtool.agent.AgentMessage
import com.ludoven.adbtool.agent.AgentMessageRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiAgentScreenLayoutTest {
    @Test
    fun `layout breakpoints select single overlay and permanent device panels`() {
        assertEquals(AgentScreenLayout.SINGLE_COLUMN, agentScreenLayout(719.9f))
        assertEquals(AgentScreenLayout.OVERLAY_DEVICE_PANEL, agentScreenLayout(720f))
        assertEquals(AgentScreenLayout.OVERLAY_DEVICE_PANEL, agentScreenLayout(959.9f))
        assertEquals(AgentScreenLayout.PERMANENT_DEVICE_PANEL, agentScreenLayout(960f))
    }

    @Test
    fun `latest conversation item starts at a valid scroll offset`() {
        assertEquals(0, agentLatestItemScrollOffset())
    }

    @Test
    fun `guide remains visible when only internal system messages exist`() {
        assertTrue(shouldShowAgentGuide(emptyList()))
        assertTrue(
            shouldShowAgentGuide(
                listOf(AgentMessage(id = "system", role = AgentMessageRole.SYSTEM, text = "internal"))
            )
        )
        assertFalse(
            shouldShowAgentGuide(
                listOf(AgentMessage(id = "user", role = AgentMessageRole.USER, text = "打开设置"))
            )
        )
    }
}
