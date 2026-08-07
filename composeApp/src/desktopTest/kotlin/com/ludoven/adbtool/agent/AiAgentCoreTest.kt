package com.ludoven.adbtool.agent

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.http.HttpClient
import java.time.Duration
import java.util.UUID
import java.util.prefs.Preferences
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiAgentCoreTest {
    @Test
    fun `endpoint normalization accepts base url and complete endpoint`() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            normalizeChatCompletionsEndpoint("https://api.openai.com/v1/")
        )
        assertEquals(
            "http://127.0.0.1:8080/chat/completions",
            normalizeChatCompletionsEndpoint("http://127.0.0.1:8080/chat/completions")
        )
        assertFailsWith<IllegalArgumentException> {
            normalizeChatCompletionsEndpoint("file:///tmp/model")
        }
        assertFailsWith<IllegalArgumentException> {
            normalizeChatCompletionsEndpoint("https://")
        }
    }

    @Test
    fun `configuration validation requires model`() {
        assertTrue(validateAiModelConfig(AiModelConfig(model = "gpt-4.1")).isSuccess)
        assertTrue(validateAiModelConfig(AiModelConfig(model = " ")).isFailure)
    }

    @Test
    fun `preferences never contain api key`() = runBlocking {
        val node = Preferences.userRoot().node("/qadb-tests/${UUID.randomUUID()}")
        val secretStore = RecordingSecretStore()
        try {
            val repository = AiConfigRepository(node, secretStore)
            repository.save(
                AiModelConfig(
                    baseUrl = "https://example.com/v1",
                    model = "test-model",
                    contextWindowTokens = 32_000
                ),
                "super-secret-key"
            ).getOrThrow()

            assertEquals("super-secret-key", secretStore.secret)
            assertTrue(repository.hasApiKeyState.value)
            assertTrue(repository.ready.value)
            assertEquals("test-model", repository.config.value.model)
            assertEquals(32_000, repository.config.value.contextWindowTokens)
            assertFalse(node.keys().any { it.contains("key", ignoreCase = true) })
            assertFalse(node.keys().any { node.get(it, "").contains("super-secret-key") })

            repository.save(
                AiModelConfig(baseUrl = "https://example.com/v1", model = "next-model"),
                ""
            ).getOrThrow()
            assertEquals("super-secret-key", secretStore.secret)
            assertTrue(repository.ready.value)
            assertEquals("next-model", repository.config.value.model)
            assertNull(repository.config.value.contextWindowTokens)

            repository.clearApiKey().getOrThrow()
            assertNull(secretStore.secret)
            assertFalse(repository.hasApiKeyState.value)
            assertFalse(repository.ready.value)
        } finally {
            node.removeNode()
        }
    }

    @Test
    fun `action validation checks coordinates package names and text length`() {
        val observation = observation()
        assertTrue(validateAgentAction(AgentAction.Tap(10, 20), observation).isSuccess)
        assertTrue(validateAgentAction(AgentAction.Tap(1080, 20), observation).isFailure)
        assertTrue(validateAgentAction(AgentAction.LaunchPackage("com.example.app"), observation).isSuccess)
        assertTrue(validateAgentAction(AgentAction.LaunchPackage("bad package"), observation).isFailure)
        assertTrue(validateAgentAction(AgentAction.InputText("x".repeat(2_001)), observation).isFailure)
        assertTrue(AgentAction.ClearAppData("com.example.app").requiresConfirmation)
        assertFalse(AgentAction.Swipe(0, 0, 10, 10, 300).requiresConfirmation)
    }

    @Test
    fun `request serializes tools screenshot and automatic tool choice without api key`() {
        val client = OpenAiCompatibleClient()
        val payload = client.buildAgentPayload(
            config = AiModelConfig(model = "agent-model"),
            context = AgentModelContext(
                task = "open settings",
                observation = observation(screenshot = byteArrayOf(1, 2, 3)),
                completedSteps = emptyList()
            ),
            includeScreenshot = true
        )
        val serialized = payload.toString()

        assertContains(serialized, "\"tool_choice\":\"auto\"")
        assertContains(serialized, "data:image/png;base64,AQID")
        assertContains(serialized, "\"clear_app_data\"")
        assertFalse(serialized.contains("api-key"))
    }

    @Test
    fun `structured tool call parser rejects free text and invalid arguments`() {
        val client = OpenAiCompatibleClient()
        val action = client.parseSingleAction(
            response(
                toolName = "tap",
                arguments = """{"x":120,"y":340}"""
            )
        )
        assertEquals(AgentAction.Tap(120, 340), action)

        assertFailsWith<ModelProtocolException> {
            client.parseSingleAction(
                Json.parseToJsonElement(
                    """{"choices":[{"message":{"content":"adb shell input tap 1 2"}}]}"""
                ).jsonObject
            )
        }
        assertFailsWith<ModelProtocolException> {
            client.parseSingleAction(response("tap", """{"x":"bad","y":2}"""))
        }
    }

    @Test
    fun `structured tool call parser unwraps compatible generic wrapper without widening the action whitelist`() {
        val client = OpenAiCompatibleClient()
        val action = client.parseSingleAction(
            response(
                toolName = "tool_call",
                arguments = """{"name":"find_app","arguments":{"query":"微信"}}"""
            )
        )
        assertEquals(AgentAction.FindApp("微信"), action)

        assertFailsWith<ModelProtocolException> {
            client.parseSingleAction(
                response(
                    toolName = "tool_call",
                    arguments = """{"name":"adb_shell","arguments":{"command":"input tap 1 2"}}"""
                )
            )
        }
        assertFailsWith<ModelProtocolException> {
            client.parseSingleAction(response("tool_call", """{"name":"find_app"}"""))
        }
    }

    @Test
    fun `finish without observation id binds the latest runtime observation`() {
        val client = OpenAiCompatibleClient()
        val action = client.parseSingleAction(
            response("finish", """{"summary":"done","outcome":"SUCCESS"}""")
        ) as AgentAction.Finish

        assertEquals("", action.observationId)
        val bound = action.bindLatestObservation("observation-latest") as AgentAction.Finish
        assertEquals("observation-latest", bound.observationId)
    }

    @Test
    fun `http errors and timeout become explicit failures`() = runBlocking {
        withServer(status = 401, body = """{"error":{"message":"invalid credential"}}""") { endpoint ->
            val error = runCatching {
                OpenAiCompatibleClient().testConnection(
                    AiModelConfig(baseUrl = endpoint, model = "test"),
                    "secret"
                )
            }.exceptionOrNull()
            assertNotNull(error)
            assertContains(error.message.orEmpty(), "HTTP 401")
            assertFalse(error.message.orEmpty().contains("secret"))
        }
        withServer(status = 429, body = """{"error":{"message":"rate limited"}}""") { endpoint ->
            val error = runCatching {
                OpenAiCompatibleClient().testConnection(
                    AiModelConfig(baseUrl = endpoint, model = "test"),
                    "secret"
                )
            }.exceptionOrNull()
            assertNotNull(error)
            assertContains(error.message.orEmpty(), "HTTP 429")
        }
        withServer(
            status = 200,
            body = response("finish", """{"summary":"ok"}""").toString(),
            delayMillis = 200
        ) { endpoint ->
            val error = runCatching {
                OpenAiCompatibleClient(
                    httpClient = HttpClient.newHttpClient(),
                    requestTimeout = Duration.ofMillis(50)
                ).testConnection(
                    AiModelConfig(baseUrl = endpoint, model = "test"),
                    "secret"
                )
            }.exceptionOrNull()
            assertNotNull(error)
        }
    }
}

private class RecordingSecretStore : SecretStore {
    var secret: String? = null

    override fun write(account: String, secret: String) {
        this.secret = secret
    }

    override fun read(account: String): String? = secret

    override fun delete(account: String) {
        secret = null
    }
}

private fun observation(screenshot: ByteArray? = null) = AgentObservation(
    screenshotPng = screenshot,
    uiHierarchy = "<hierarchy/>",
    currentActivity = "com.example/.MainActivity",
    screenWidth = 1080,
    screenHeight = 2400
)

private fun response(toolName: String, arguments: String) =
    Json.parseToJsonElement(
        """
        {
          "choices": [{
            "message": {
              "tool_calls": [{
                "type": "function",
                "function": {
                  "name": "$toolName",
                  "arguments": ${JsonPrimitive(arguments)}
                }
              }]
            }
          }]
        }
        """.trimIndent()
    ).jsonObject

private suspend fun withServer(
    status: Int,
    body: String,
    delayMillis: Long = 0,
    block: suspend (String) -> Unit
) {
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/chat/completions") { exchange ->
        if (delayMillis > 0) Thread.sleep(delayMillis)
        val bytes = body.toByteArray()
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
        block("http://127.0.0.1:${server.address.port}/chat/completions")
    } finally {
        server.stop(0)
    }
}
