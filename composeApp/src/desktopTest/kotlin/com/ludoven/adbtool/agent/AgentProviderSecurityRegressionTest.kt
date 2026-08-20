package com.ludoven.adbtool.agent

import java.util.UUID
import java.util.prefs.Preferences
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentProviderSecurityRegressionTest {
    @Test
    fun `provider endpoint rejects embedded credentials fragments and secret query names`() {
        listOf(
            "https://user:token@provider.example/v1",
            "https://provider.example/v1#debug",
            "https://provider.example/v1?api_key=secret",
            "https://provider.example/v1?api%2Ekey=secret",
            "https://provider.example/v1?auth.key=secret",
            "https://provider.example/v1?access-token=secret",
            "https://provider.example/v1?credential=secret",
            "https://provider.example/v1?password=secret"
        ).forEach { baseUrl ->
            assertTrue(
                runCatching { normalizeChatCompletionsEndpoint(baseUrl) }.isFailure,
                "Expected unsafe provider URL to be rejected: $baseUrl"
            )
        }
    }

    @Test
    fun `provider endpoint appends path before preserving safe query`() {
        assertEquals(
            "https://provider.example/v1/chat/completions?api-version=2026-01-01&region=cn",
            normalizeChatCompletionsEndpoint(
                "https://provider.example/v1?api-version=2026-01-01&region=cn"
            )
        )
        assertEquals(
            "https://provider.example/v1/chat/completions?api-version=2026-01-01",
            normalizeChatCompletionsEndpoint(
                "https://provider.example/v1/chat/completions?api-version=2026-01-01"
            )
        )
    }

    @Test
    fun `extra body rejects secret names split across nested paths`() {
        listOf(
            """{"api":{"key":"secret"}}""",
            """{"vendor":{"auth":{"key":"secret"}}}""",
            """{"options":[{"access":{"key":"secret"}}]}"""
        ).forEach { body ->
            val result = parseAgentExtraBody(body)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("secret-like fields"))
        }
    }

    @Test
    fun `secret provider transport allows loopback HTTP and requires HTTPS remotely`() {
        listOf(
            "http://localhost:8080/v1",
            "http://worker.localhost:8080/v1",
            "http://127.0.0.1:8080/v1",
            "http://127.23.4.5:8080/v1",
            "http://[::1]:8080/v1",
            "http://[0:0:0:0:0:0:0:1]:8080/v1"
        ).forEach { baseUrl ->
            assertTrue(validateAgentProviderEndpointSecurity(testProfile(baseUrl = baseUrl)).isSuccess)
        }

        assertTrue(
            validateAgentProviderEndpointSecurity(testProfile(baseUrl = "https://provider.example/v1")).isSuccess
        )
        assertTrue(
            validateAgentProviderEndpointSecurity(
                testProfile(
                    baseUrl = "http://provider.example/v1",
                    authType = AgentProviderAuthType.NONE,
                    secretHeaderNames = emptyList()
                )
            ).isSuccess
        )
        assertTrue(
            validateAgentProviderEndpointSecurity(testProfile(baseUrl = "http://provider.example/v1")).isFailure
        )
        assertTrue(
            validateAgentProviderEndpointSecurity(
                testProfile(
                    baseUrl = "http://provider.example/v1",
                    authType = AgentProviderAuthType.NONE,
                    secretHeaderNames = listOf("X-Tenant-Secret")
                )
            ).isFailure
        )
    }

    @Test
    fun `forced cleanup stops before delete when rollback snapshot cannot be read`() = runBlocking {
        val node = testPreferences()
        val secrets = RegressionSecretStore()
        try {
            val repository = repository(node, secrets)
            val original = testProfile(baseUrl = "https://provider.example/v1", id = "cleanup-target")
            repository.upsert(
                original,
                apiKey = "original-key",
                secretHeadersJson = """{"X-Tenant-Secret":"original-header"}"""
            ).getOrThrow()
            val authAccount = providerSecretAccount(original.id)
            val headerAccount = providerHeaderSecretAccount(original.id)
            secrets.deleteAttempts.clear()
            secrets.failReadAccounts += headerAccount

            val result = repository.upsert(
                original.copy(
                    name = "Must not publish",
                    authType = AgentProviderAuthType.NONE,
                    authHeaderName = "",
                    requestOptions = original.requestOptions.copy(secretHeaderNames = emptyList())
                ),
                apiKey = null,
                secretHeadersJson = null
            )

            assertTrue(result.isFailure)
            assertTrue(secrets.deleteAttempts.isEmpty())
            assertEquals("Original", repository.profiles.value.single().name)
            assertEquals("original-key", secrets.value(authAccount))
            assertEquals("""{"X-Tenant-Secret":"original-header"}""", secrets.value(headerAccount))
        } finally {
            node.removeNode()
        }
    }

    @Test
    fun `forced cleanup restores old secrets when a delete mutates before failing`() = runBlocking {
        val node = testPreferences()
        val secrets = RegressionSecretStore()
        try {
            val repository = repository(node, secrets)
            val original = testProfile(baseUrl = "https://provider.example/v1", id = "rollback-target")
            repository.upsert(
                original,
                apiKey = "original-key",
                secretHeadersJson = """{"X-Tenant-Secret":"original-header"}"""
            ).getOrThrow()
            val authAccount = providerSecretAccount(original.id)
            val headerAccount = providerHeaderSecretAccount(original.id)
            secrets.failDeleteAfterMutationAccount = headerAccount

            val result = repository.upsert(
                original.copy(
                    name = "Must not publish",
                    authType = AgentProviderAuthType.NONE,
                    authHeaderName = "",
                    requestOptions = original.requestOptions.copy(secretHeaderNames = emptyList())
                ),
                apiKey = null,
                secretHeadersJson = null
            )

            assertTrue(result.isFailure)
            assertEquals("Original", repository.profiles.value.single().name)
            assertEquals("original-key", secrets.value(authAccount))
            assertEquals("""{"X-Tenant-Secret":"original-header"}""", secrets.value(headerAccount))
        } finally {
            node.removeNode()
        }
    }

    @Test
    fun `provider delete stops before mutation when any secret snapshot fails`() = runBlocking {
        val node = testPreferences()
        val secrets = RegressionSecretStore()
        try {
            val repository = repository(node, secrets)
            val fallback = testProfile(
                baseUrl = "https://fallback.example/v1",
                id = "fallback",
                authType = AgentProviderAuthType.NONE,
                secretHeaderNames = emptyList()
            )
            repository.upsert(fallback, apiKey = null).getOrThrow()
            val target = testProfile(baseUrl = "https://provider.example/v1", id = "delete-target")
            repository.upsert(
                target,
                apiKey = "target-key",
                secretHeadersJson = """{"X-Tenant-Secret":"target-header"}"""
            ).getOrThrow()
            repository.bind(AgentModelRole.EXECUTOR, target.id).getOrThrow()
            val authAccount = providerSecretAccount(target.id)
            val headerAccount = providerHeaderSecretAccount(target.id)
            secrets.deleteAttempts.clear()
            secrets.failReadAccounts += headerAccount

            val result = repository.delete(target.id)

            assertTrue(result.isFailure)
            assertTrue(secrets.deleteAttempts.isEmpty())
            assertTrue(repository.profiles.value.any { it.id == target.id })
            assertEquals(target.id, repository.bindings.value.providers[AgentModelRole.EXECUTOR])
            assertEquals("target-key", secrets.value(authAccount))
            assertEquals("""{"X-Tenant-Secret":"target-header"}""", secrets.value(headerAccount))
        } finally {
            node.removeNode()
        }
    }

    private fun testProfile(
        baseUrl: String,
        id: String = "security-${UUID.randomUUID()}",
        authType: AgentProviderAuthType = AgentProviderAuthType.BEARER,
        secretHeaderNames: List<String> = listOf("X-Tenant-Secret")
    ) = AgentProviderProfile(
        id = id,
        name = "Original",
        baseUrl = baseUrl,
        defaultModel = "security-model",
        authType = authType,
        authHeaderName = authType.defaultHeaderName,
        requestOptions = AgentRequestOptions(secretHeaderNames = secretHeaderNames)
    )

    private fun repository(node: Preferences, secrets: SecretStore) = AgentProviderRepository(
        preferences = node,
        secrets = secrets,
        legacy = AiConfigRepository(node.node("legacy"), secrets)
    )

    private fun testPreferences(): Preferences =
        Preferences.userRoot().node("/qadb-provider-security-tests/${UUID.randomUUID()}")
}

private class RegressionSecretStore : SecretStore {
    private val values = mutableMapOf<String, String>()
    val failReadAccounts = mutableSetOf<String>()
    val deleteAttempts = mutableListOf<String>()
    var failDeleteAfterMutationAccount: String? = null

    override fun write(account: String, secret: String) {
        values[account] = secret
    }

    override fun read(account: String): String? {
        check(account !in failReadAccounts) { "Injected SecretStore read failure" }
        return values[account]
    }

    override fun delete(account: String) {
        deleteAttempts += account
        values.remove(account)
        check(account != failDeleteAfterMutationAccount) { "Injected SecretStore delete failure" }
    }

    fun value(account: String): String? = values[account]
}
