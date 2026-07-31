package com.ludoven.adbtool.agent

import java.util.UUID
import java.util.prefs.Preferences
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AgentRealModelSmokeTest {
    @Test
    fun `configured model opens settings on real device`() = runBlocking {
        val deviceId = System.getProperty(DEVICE_PROPERTY)?.takeIf { it.isNotBlank() }
            ?: return@runBlocking
        val repository = AiConfiguration.repository
        val config = repository.config.value
        val apiKey = repository.loadApiKey()
        require(validateAiModelConfig(config).isSuccess && !apiKey.isNullOrBlank()) {
            "A saved AI model and API key are required for the opt-in real-model smoke test"
        }
        val gateway = RealAgentDeviceGateway()
        val memoryPreferences = AgentMemoryPreferences(
            Preferences.userRoot().node("/qadb-tests/real-model/${UUID.randomUUID()}")
        ).also { it.declineConsent() }

        val result = AgentOrchestrator(
            modelClient = OpenAiCompatibleClient(),
            deviceGateway = gateway,
            maxActions = 10,
            memoryPreferences = memoryPreferences
        ).run(
            task = "返回桌面，然后按应用名称查找并打开系统设置。看到设置页面后结束任务。",
            deviceId = deviceId,
            config = config,
            apiKey = apiKey,
            onState = {},
            confirmSensitiveAction = { false }
        )

        assertFalse(result.isRunning)
        val trace = result.steps.joinToString(" -> ") {
            "${it.action.toolName}:${it.status.name.lowercase()}"
        }
        assertNull(result.errorMessage, "${result.errorMessage}; steps=$trace")
        assertContains(gateway.observe(deviceId).currentActivity, "com.android.settings")
    }
}

private const val DEVICE_PROPERTY = "qadb.agent.realModelDevice"
