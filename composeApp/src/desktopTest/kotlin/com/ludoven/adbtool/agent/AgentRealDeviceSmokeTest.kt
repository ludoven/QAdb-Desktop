package com.ludoven.adbtool.agent

import java.util.UUID
import java.util.prefs.Preferences
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentRealDeviceSmokeTest {
    @Test
    fun `real device opens settings and confirms sensitive action`() = runBlocking {
        val deviceId = System.getProperty(DEVICE_PROPERTY)?.takeIf { it.isNotBlank() } ?: return@runBlocking
        val model = RecordingQueueModelClient(
            AgentAction.KeyEvent(AgentKey.HOME),
            AgentAction.FindApp("设置"),
            AgentAction.LaunchPackage(SETTINGS_PACKAGE),
            AgentAction.ForceStopPackage(SETTINGS_PACKAGE),
            AgentAction.Finish("real-device smoke completed")
        )
        val gateway = RealAgentDeviceGateway()
        var confirmations = 0
        val memoryPreferences = AgentMemoryPreferences(
            Preferences.userRoot().node("/qadb-tests/real-device/${UUID.randomUUID()}")
        ).also { it.declineConsent() }

        val result = AgentOrchestrator(
            modelClient = model,
            deviceGateway = gateway,
            memoryPreferences = memoryPreferences
        ).run(
            task = "return home, open settings, then force stop settings after confirmation",
            deviceId = deviceId,
            config = AiModelConfig(model = "local-smoke"),
            apiKey = "not-sent-by-fake-client",
            onState = {},
            confirmSensitiveAction = {
                confirmations += 1
                true
            }
        )

        assertFalse(result.isRunning)
        assertTrue(result.errorMessage == null, result.errorMessage)
        assertEquals(1, confirmations)
        assertEquals(4, result.steps.size)
        assertTrue(result.steps.all { it.status == AgentStepStatus.COMPLETED })
        assertContains(model.contexts[3].observation.currentActivity, SETTINGS_PACKAGE)
    }
}

private class RecordingQueueModelClient(vararg actions: AgentAction) : AgentModelClient {
    private val queue = ArrayDeque(actions.toList())
    val contexts = mutableListOf<AgentModelContext>()

    override suspend fun nextAction(
        config: AiModelConfig,
        apiKey: String,
        context: AgentModelContext,
        includeScreenshot: Boolean
    ): AgentModelDecision {
        contexts += context
        val action = queue.removeFirst().let { planned ->
            if (planned is AgentAction.Finish && planned.observationId.isBlank()) {
                planned.copy(observationId = context.observation.observationId)
            } else {
                planned
            }
        }
        return AgentModelDecision(action, includeScreenshot)
    }

    override suspend fun testConnection(config: AiModelConfig, apiKey: String) = Unit
}

private const val DEVICE_PROPERTY = "qadb.agent.device"
private const val SETTINGS_PACKAGE = "com.android.settings"
