package com.ludoven.adbtool.agent

import java.util.prefs.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** Multi-provider store. Existing single-model settings are imported once as the default profile. */
class AgentProviderRepository(
    private val preferences: Preferences = Preferences.userNodeForPackage(AgentProviderRepository::class.java),
    private val secrets: SecretStore = PlatformSecretStoreFactory.create(),
    private val legacy: AiConfigRepository = AiConfiguration.repository
) {
    private val _profiles = MutableStateFlow(loadProfiles())
    val profiles: StateFlow<List<AgentProviderProfile>> = _profiles.asStateFlow()
    private val _bindings = MutableStateFlow(loadBindings())
    val bindings: StateFlow<AgentRoleBindings> = _bindings.asStateFlow()

    suspend fun ensureMigration() = withContext(Dispatchers.IO) {
        if (_profiles.value.isNotEmpty()) return@withContext
        val config = legacy.config.value
        if (config.model.isBlank()) return@withContext
        val profile = AgentProviderProfile(
            id = LEGACY_PROFILE_ID,
            name = "Default",
            baseUrl = config.baseUrl,
            defaultModel = config.model,
            capabilities = AgentCapabilities(vision = config.visionMode != VisionMode.DISABLED),
            limits = AgentProviderLimits(contextWindowTokens = config.contextWindowTokens)
        )
        legacy.loadApiKey()?.let { secrets.write(providerSecretAccount(profile.id), it) }
        saveProfiles(listOf(profile))
        saveBindings(AgentRoleBindings(AgentModelRole.entries.associateWith { profile.id }))
    }

    /** Keeps the existing single-provider settings dialog backward compatible. */
    suspend fun syncLegacy(config: AiModelConfig, apiKey: String?) = withContext(Dispatchers.IO) {
        ensureMigration()
        val existing = _profiles.value.firstOrNull { it.id == LEGACY_PROFILE_ID } ?: return@withContext
        val updated = existing.copy(
            baseUrl = config.baseUrl,
            defaultModel = config.model,
            capabilities = existing.capabilities.copy(vision = config.visionMode != VisionMode.DISABLED),
            limits = existing.limits.copy(contextWindowTokens = config.contextWindowTokens)
        )
        saveProfiles(_profiles.value.map { if (it.id == updated.id) updated else it })
        apiKey?.trim()?.takeIf { it.isNotEmpty() }?.let { secrets.write(providerSecretAccount(updated.id), it) }
    }

    suspend fun upsert(profile: AgentProviderProfile, apiKey: String?, secretHeadersJson: String? = null): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(profile.name.isNotBlank()) { "Provider name is required" }
                validateAiModelConfig(profile.toLegacyConfig()).getOrThrow()
                val all = _profiles.value.filterNot { it.id == profile.id } + profile
                saveProfiles(all)
                apiKey?.trim()?.takeIf { it.isNotEmpty() }?.let { secrets.write(providerSecretAccount(profile.id), it) }
                secretHeadersJson?.takeIf { it.isNotBlank() }?.let { secrets.write(providerHeaderSecretAccount(profile.id), it) }
                if (_bindings.value.providers.isEmpty()) {
                    saveBindings(AgentRoleBindings(AgentModelRole.entries.associateWith { profile.id }))
                }
            }
        }

    suspend fun delete(profileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(_profiles.value.size > 1) { "At least one provider must remain" }
            saveProfiles(_profiles.value.filterNot { it.id == profileId })
            secrets.delete(providerSecretAccount(profileId))
            secrets.delete(providerHeaderSecretAccount(profileId))
            val fallback = _profiles.value.first().id
            saveBindings(AgentRoleBindings(_bindings.value.providers.mapValues { (_, id) -> if (id == profileId) fallback else id }))
        }
    }

    fun bind(role: AgentModelRole, providerId: String): Result<Unit> = runCatching {
        require(_profiles.value.any { it.id == providerId && it.enabled }) { "Selected provider is unavailable" }
        saveBindings(_bindings.value.copy(providers = _bindings.value.providers + (role to providerId)))
    }

    suspend fun apiKey(profileId: String): String? = withContext(Dispatchers.IO) { secrets.read(providerSecretAccount(profileId)) }
    suspend fun secretHeaders(profileId: String): String? = withContext(Dispatchers.IO) { secrets.read(providerHeaderSecretAccount(profileId)) }
    fun providerFor(role: AgentModelRole): AgentProviderProfile? {
        val default = _profiles.value.firstOrNull { it.enabled } ?: return null
        val id = _bindings.value.providerIdFor(role, default.id)
        return _profiles.value.firstOrNull { it.id == id && it.enabled } ?: default
    }

    private fun loadProfiles(): List<AgentProviderProfile> = runCatching {
        Json.parseToJsonElement(preferences.get(KEY_PROFILES, "[]")).jsonArray.map(::decodeProfile)
    }.getOrDefault(emptyList())

    private fun loadBindings(): AgentRoleBindings = runCatching {
        val values = Json.parseToJsonElement(preferences.get(KEY_BINDINGS, "{}")).jsonObject
        AgentRoleBindings(values.mapNotNull { (role, provider) ->
            runCatching { AgentModelRole.valueOf(role) to provider.jsonPrimitive.content }.getOrNull()
        }.toMap())
    }.getOrDefault(AgentRoleBindings())

    private fun saveProfiles(value: List<AgentProviderProfile>) {
        preferences.put(KEY_PROFILES, JsonArray(value.map(::encodeProfile)).toString())
        preferences.flush(); _profiles.value = value
    }

    private fun saveBindings(value: AgentRoleBindings) {
        preferences.put(KEY_BINDINGS, buildJsonObject {
            value.providers.forEach { (role, id) -> put(role.name, id) }
        }.toString())
        preferences.flush(); _bindings.value = value
    }

    private fun encodeProfile(value: AgentProviderProfile): JsonObject = buildJsonObject {
        put("id", value.id); put("name", value.name); put("baseUrl", value.baseUrl); put("model", value.defaultModel)
        put("enabled", value.enabled); put("protocol", value.protocol.name)
        put("capabilities", buildJsonObject {
            put("text", value.capabilities.text); put("vision", value.capabilities.vision); put("tool", value.capabilities.toolCalling)
            put("structured", value.capabilities.structuredOutput); put("reasoning", value.capabilities.reasoning)
            put("cache", value.capabilities.promptCache); put("usage", value.capabilities.usageReporting)
        })
        put("limits", buildJsonObject {
            value.limits.contextWindowTokens?.let { put("context", it) }; put("output", value.limits.maxOutputTokens)
            put("timeout", value.limits.timeoutMs); put("retries", value.limits.maxRetries)
        })
        put("extraBody", value.requestOptions.extraBodyJson)
        put("headerNames", buildJsonArray { value.requestOptions.secretHeaderNames.forEach { add(JsonPrimitive(it)) } })
        put("pricing", buildJsonObject { put("input", value.pricing.inputPerMillion); put("cached", value.pricing.cachedInputPerMillion); put("output", value.pricing.outputPerMillion); put("currency", value.pricing.currency) })
    }

    private fun decodeProfile(value: kotlinx.serialization.json.JsonElement): AgentProviderProfile {
        val obj = value.jsonObject; fun text(key: String) = obj[key]?.jsonPrimitive?.contentOrNull.orEmpty()
        val capabilities = obj["capabilities"]?.jsonObject
        val limits = obj["limits"]?.jsonObject
        val pricing = obj["pricing"]?.jsonObject
        return AgentProviderProfile(
            id = text("id"), name = text("name"), baseUrl = text("baseUrl"), defaultModel = text("model"),
            protocol = runCatching { AgentModelProtocol.valueOf(text("protocol")) }.getOrDefault(AgentModelProtocol.OPENAI_COMPATIBLE),
            enabled = obj["enabled"]?.jsonPrimitive?.booleanOrNull ?: true,
            capabilities = AgentCapabilities(
                text = capabilities?.get("text")?.jsonPrimitive?.booleanOrNull ?: true, vision = capabilities?.get("vision")?.jsonPrimitive?.booleanOrNull ?: false,
                toolCalling = capabilities?.get("tool")?.jsonPrimitive?.booleanOrNull ?: true, structuredOutput = capabilities?.get("structured")?.jsonPrimitive?.booleanOrNull ?: false,
                reasoning = capabilities?.get("reasoning")?.jsonPrimitive?.booleanOrNull ?: false, promptCache = capabilities?.get("cache")?.jsonPrimitive?.booleanOrNull ?: false,
                usageReporting = capabilities?.get("usage")?.jsonPrimitive?.booleanOrNull ?: false
            ),
            limits = AgentProviderLimits(limits?.get("context")?.jsonPrimitive?.intOrNull, limits?.get("output")?.jsonPrimitive?.intOrNull ?: 8192, limits?.get("timeout")?.jsonPrimitive?.longOrNull ?: 30_000, limits?.get("retries")?.jsonPrimitive?.intOrNull ?: 2),
            requestOptions = AgentRequestOptions(text("extraBody"), obj["headerNames"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()),
            pricing = AgentPricing(pricing?.get("input")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0, pricing?.get("cached")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0, pricing?.get("output")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0, pricing?.get("currency")?.jsonPrimitive?.contentOrNull ?: "CNY")
        )
    }

    private companion object { const val KEY_PROFILES = "agent.provider.profiles"; const val KEY_BINDINGS = "agent.provider.bindings"; const val LEGACY_PROFILE_ID = "legacy-default" }
}

fun AgentProviderProfile.toLegacyConfig() = AiModelConfig(baseUrl, defaultModel, if (capabilities.vision) VisionMode.AUTO else VisionMode.DISABLED, limits.contextWindowTokens)

object AgentProviderRuntime { val repository: AgentProviderRepository by lazy { AgentProviderRepository() } }
