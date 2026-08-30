package com.ludoven.adbtool.agent

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Fetches the model id list from an OpenAI-compatible provider's `GET /models` endpoint. */
object AgentModelCatalog {
    private val json = Json { ignoreUnknownKeys = true }

    /** The provider must already be resolved via [AgentProviderRepository.legacyPreview] or [AgentProviderRepository.resolve]. */
    suspend fun fetchModelIds(provider: ResolvedAgentProvider): List<String> = withContext(Dispatchers.IO) {
        validateAgentProviderEndpointSecurity(provider.profile).getOrThrow()
        val modelsUrl = normalizeChatCompletionsEndpoint(provider.profile.baseUrl)
            .removeSuffix("/chat/completions") + "/models"
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()
        val builder = HttpRequest.newBuilder(URI.create(modelsUrl))
            .timeout(Duration.ofSeconds(15))
            .GET()
        when (provider.profile.authType) {
            AgentProviderAuthType.BEARER -> builder.header(
                provider.profile.authHeaderName,
                "Bearer ${requireNotNull(provider.authSecret).trim()}"
            )
            AgentProviderAuthType.API_KEY_HEADER -> builder.header(
                provider.profile.authHeaderName,
                requireNotNull(provider.authSecret).trim()
            )
            AgentProviderAuthType.NONE -> Unit
        }
        provider.secretHeaders.forEach { (name, value) -> builder.header(name, value) }
        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        require(response.statusCode() in 200..299) { "HTTP ${response.statusCode()} from $modelsUrl" }
        parseModelIds(response.body())
    }

    internal fun parseModelIds(body: String): List<String> {
        val element = runCatching { json.parseToJsonElement(body) }
            .getOrElse { error("Malformed model catalog response") }
        val array: JsonArray = when {
            element is JsonArray -> element
            element is JsonObject && element.containsKey("data") ->
                element["data"] as? JsonArray ?: error("Malformed model catalog response")
            else -> error("Malformed model catalog response")
        }
        return array.mapNotNull { entry ->
            (entry as? JsonObject)?.get("id")?.jsonPrimitive?.contentOrNull
        }.filter { it.isNotBlank() }.distinct().sorted()
    }
}
