package com.ludoven.adbtool.agent

import kotlin.test.Test
import kotlin.test.assertEquals

class AgentReadinessTest {
    @Test
    fun `device guidance has priority over model configuration`() {
        assertEquals(
            AgentReadiness.DEVICE_REQUIRED,
            resolveAgentReadiness(
                deviceConnected = false,
                configurationChecked = false,
                modelReady = false
            )
        )
    }

    @Test
    fun `connected device waits for configuration check`() {
        assertEquals(
            AgentReadiness.CHECKING,
            resolveAgentReadiness(
                deviceConnected = true,
                configurationChecked = false,
                modelReady = false
            )
        )
    }

    @Test
    fun `model is required after configuration check`() {
        assertEquals(
            AgentReadiness.MODEL_REQUIRED,
            resolveAgentReadiness(
                deviceConnected = true,
                configurationChecked = true,
                modelReady = false
            )
        )
    }

    @Test
    fun `ready requires both device and tested model`() {
        assertEquals(
            AgentReadiness.READY,
            resolveAgentReadiness(
                deviceConnected = true,
                configurationChecked = true,
                modelReady = true
            )
        )
    }
}
