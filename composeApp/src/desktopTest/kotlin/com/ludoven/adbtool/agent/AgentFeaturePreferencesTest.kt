package com.ludoven.adbtool.agent

import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentFeaturePreferencesTest {
    @Test
    fun `agent beta is disabled by default and explicit opt in persists`() {
        val node = Preferences.userRoot().node("/qadb-tests/agent-feature/${UUID.randomUUID()}")
        try {
            val preferences = AgentFeaturePreferences(node)
            assertFalse(preferences.enabled.value)
            preferences.setEnabled(true)
            assertTrue(AgentFeaturePreferences(node).enabled.value)
        } finally {
            node.removeNode()
        }
    }

    @Test
    fun `reduced motion is disabled by default and persists an explicit preference`() {
        val node = Preferences.userRoot().node("/qadb-tests/agent-motion/${UUID.randomUUID()}")
        try {
            val preferences = AgentFeaturePreferences(node)
            assertFalse(preferences.reduceMotion.value)

            preferences.setReduceMotion(true)
            assertTrue(preferences.reduceMotion.value)
            assertTrue(AgentFeaturePreferences(node).reduceMotion.value)
        } finally {
            node.removeNode()
        }
    }
}
