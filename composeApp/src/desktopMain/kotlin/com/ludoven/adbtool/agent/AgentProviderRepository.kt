package com.ludoven.adbtool.agent

import java.util.prefs.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
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
    private val legacy: AiConfigRepository = AiConfiguration.repository,
    private val capabilityAttestations: AgentCapabilityAttestationStore = AgentCapabilityAttestationStore()
) {
    private val repositoryMutex = Mutex()
    private val _profiles = MutableStateFlow(loadProfiles())
    val profiles: StateFlow<List<AgentProviderProfile>> = _profiles.asStateFlow()
    private val _bindings = MutableStateFlow(loadBindings())
    val bindings: StateFlow<AgentRoleBindings> = _bindings.asStateFlow()

    suspend fun ensureMigration() = withContext(Dispatchers.IO) {
        repositoryMutex.withLock { ensureMigrationLocked() }
    }

    private suspend fun ensureMigrationLocked() {
        if (_profiles.value.isNotEmpty()) return
        val config = legacy.config.value
        if (config.model.isBlank()) return
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
        val tier = if (profile.capabilities.vision) AgentCapabilityTier.L3_VISUAL_AGENT else AgentCapabilityTier.L2_SEMANTIC_AGENT
        capabilityAttestations.save(profile, tier)
    }

    /** Keeps the existing single-provider settings dialog backward compatible. */
    suspend fun syncLegacy(config: AiModelConfig, apiKey: String?) = withContext(Dispatchers.IO) {
        repositoryMutex.withLock {
            ensureMigrationLocked()
            val existing = _profiles.value.firstOrNull { it.id == LEGACY_PROFILE_ID } ?: return@withLock
            val updated = existing.copy(
                baseUrl = config.baseUrl,
                defaultModel = config.model,
                capabilities = existing.capabilities.copy(vision = config.visionMode != VisionMode.DISABLED),
                limits = existing.limits.copy(contextWindowTokens = config.contextWindowTokens)
            )
            // The legacy dialog treats a blank key as "keep the stored key".
            upsertLocked(updated, apiKey?.trim()?.takeIf { it.isNotEmpty() }, secretHeadersJson = null)
            val tier = if (updated.capabilities.vision) AgentCapabilityTier.L3_VISUAL_AGENT else AgentCapabilityTier.L2_SEMANTIC_AGENT
            capabilityAttestations.save(updated, tier)
        }
    }

    suspend fun upsert(profile: AgentProviderProfile, apiKey: String?, secretHeadersJson: String? = null): Result<Unit> =
        withContext(Dispatchers.IO) {
            repositoryMutex.withLock { runCatching { upsertLocked(profile, apiKey, secretHeadersJson) } }
        }

    private fun upsertLocked(profile: AgentProviderProfile, apiKey: String?, secretHeadersJson: String?) {
        validateAgentProviderProfile(profile).getOrThrow()
        val previousProfile = _profiles.value.firstOrNull { it.id == profile.id }

        val authAccount = providerSecretAccount(profile.id)
        val headerAccount = providerHeaderSecretAccount(profile.id)
        val authUpdate = secretUpdate(
            value = apiKey,
            forceDelete = profile.authType == AgentProviderAuthType.NONE
        )
        val headerUpdate = secretUpdate(
            value = secretHeadersJson,
            forceDelete = profile.requestOptions.secretHeaderNames.isEmpty()
        )
        val previousAuthSecret = snapshotSecret(authAccount)
        val previousHeaderSecret = snapshotSecret(headerAccount)
        if (authUpdate != SecretUpdate.Keep) previousAuthSecret.valueOrThrow()
        previousHeaderSecret.valueOrThrow()
        val credentialsChanged = authUpdate.changes(previousAuthSecret) ||
            headerUpdate.changes(previousHeaderSecret)
        if (headerUpdate != SecretUpdate.Delete) {
            val effectiveHeaders = when (headerUpdate) {
                SecretUpdate.Keep -> previousHeaderSecret.valueOrThrow()
                is SecretUpdate.Replace -> headerUpdate.value
                SecretUpdate.Delete -> null
            }
            validateSecretHeaders(profile, effectiveHeaders).getOrThrow()
        }

        val previousProfiles = _profiles.value
        val all = previousProfiles.filterNot { it.id == profile.id } + profile
        val previousBindings = _bindings.value
        val initializedBindings = previousBindings.providers.isEmpty()
        val attemptedSecrets = mutableListOf<Pair<String, SecretSnapshot>>()
        var bindingsPublishAttempted = false
        var profilesPublishAttempted = false
        try {
            applySecretUpdate(authAccount, authUpdate, previousAuthSecret, attemptedSecrets)
            applySecretUpdate(headerAccount, headerUpdate, previousHeaderSecret, attemptedSecrets)
            if (initializedBindings) {
                bindingsPublishAttempted = true
                saveBindings(AgentRoleBindings(AgentModelRole.entries.associateWith { profile.id }))
            }
            // Publish the profile only after every SecretStore operation succeeds.
            profilesPublishAttempted = true
            saveProfiles(all)
            if (previousProfile != null && (
                    previousProfile.capabilityFingerprint() != profile.capabilityFingerprint() || credentialsChanged
                )
            ) {
                capabilityAttestations.invalidate(profile.id)
            }
        } catch (failure: Throwable) {
            if (profilesPublishAttempted) restoreProfiles(previousProfiles, failure)
            if (bindingsPublishAttempted) restoreBindings(previousBindings, failure)
            attemptedSecrets.asReversed().forEach { (account, snapshot) ->
                restoreSecret(account, snapshot, failure)
            }
            throw failure
        }
    }

    suspend fun delete(profileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        repositoryMutex.withLock { runCatching {
            val previousProfiles = _profiles.value
            require(previousProfiles.size > 1) { "At least one provider must remain" }
            val remainingProfiles = previousProfiles.filterNot { it.id == profileId }
            require(remainingProfiles.size < previousProfiles.size) { "Provider not found: $profileId" }
            val previousBindings = _bindings.value
            val fallback = remainingProfiles.firstOrNull { it.enabled }?.id ?: remainingProfiles.first().id
            val remainingBindings = AgentRoleBindings(
                previousBindings.providers.mapValues { (_, id) -> if (id == profileId) fallback else id }
            )
            val authAccount = providerSecretAccount(profileId)
            val headerAccount = providerHeaderSecretAccount(profileId)
            val authSnapshot = snapshotSecret(authAccount)
            val headerSnapshot = snapshotSecret(headerAccount)
            authSnapshot.valueOrThrow()
            headerSnapshot.valueOrThrow()
            val attemptedSecrets = mutableListOf<Pair<String, SecretSnapshot>>()
            var bindingsPublishAttempted = false
            var profilesPublishAttempted = false
            try {
                attemptedSecrets += authAccount to authSnapshot
                secrets.delete(authAccount)
                attemptedSecrets += headerAccount to headerSnapshot
                secrets.delete(headerAccount)
                bindingsPublishAttempted = true
                saveBindings(remainingBindings)
                profilesPublishAttempted = true
                saveProfiles(remainingProfiles)
                capabilityAttestations.invalidate(profileId)
            } catch (failure: Throwable) {
                if (profilesPublishAttempted) restoreProfiles(previousProfiles, failure)
                if (bindingsPublishAttempted) restoreBindings(previousBindings, failure)
                attemptedSecrets.asReversed().forEach { (account, snapshot) ->
                    restoreSecret(account, snapshot, failure)
                }
                throw failure
            }
        } }
    }

    suspend fun bind(role: AgentModelRole, providerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        repositoryMutex.withLock { runCatching {
            require(_profiles.value.any { it.id == providerId && it.enabled }) { "Selected provider is unavailable" }
            saveBindings(_bindings.value.copy(providers = _bindings.value.providers + (role to providerId)))
        } }
    }

    suspend fun apiKey(profileId: String): String? = withContext(Dispatchers.IO) {
        repositoryMutex.withLock { secrets.read(providerSecretAccount(profileId)) }
    }
    suspend fun secretHeaders(profileId: String): String? = withContext(Dispatchers.IO) {
        repositoryMutex.withLock { secrets.read(providerHeaderSecretAccount(profileId)) }
    }

    suspend fun resolve(role: AgentModelRole): ResolvedAgentProvider? = withContext(Dispatchers.IO) {
        repositoryMutex.withLock {
            val profile = providerForLocked(role) ?: return@withLock null
            val authSecret = if (profile.authType == AgentProviderAuthType.NONE) {
                null
            } else {
                secrets.read(providerSecretAccount(profile.id))
            }
            val headersJson = secrets.read(providerHeaderSecretAccount(profile.id))
            val headers = decodeSecretHeaders(profile, headersJson).getOrThrow()
            ResolvedAgentProvider(
                role = role,
                profile = profile,
                authSecret = authSecret,
                secretHeaders = headers
            )
        }
    }

    suspend fun legacyPreview(config: AiModelConfig, apiKey: String): ResolvedAgentProvider =
        withContext(Dispatchers.IO) {
            repositoryMutex.withLock {
                val existing = _profiles.value.firstOrNull { it.id == LEGACY_PROFILE_ID }
                val profile = (existing ?: AgentProviderProfile(
                    id = LEGACY_PROFILE_ID,
                    name = "Default",
                    baseUrl = config.baseUrl,
                    defaultModel = config.model
                )).copy(
                    baseUrl = config.baseUrl,
                    defaultModel = config.model,
                    capabilities = (existing?.capabilities ?: AgentCapabilities()).copy(
                        vision = config.visionMode != VisionMode.DISABLED
                    ),
                    limits = (existing?.limits ?: AgentProviderLimits()).copy(
                        contextWindowTokens = config.contextWindowTokens
                    )
                )
                ResolvedAgentProvider(AgentModelRole.BRAIN, profile, apiKey.trim())
            }
        }

    fun capabilityAttestation(profile: AgentProviderProfile): AgentCapabilityAttestation? =
        capabilityAttestations.load(profile)

    fun attestCapabilities(
        profile: AgentProviderProfile,
        tier: AgentCapabilityTier
    ): AgentCapabilityAttestation = capabilityAttestations.save(profile, tier)

    fun providerFor(role: AgentModelRole): AgentProviderProfile? = providerForLocked(role)

    private fun providerForLocked(role: AgentModelRole): AgentProviderProfile? {
        val default = _profiles.value.firstOrNull { it.enabled } ?: return null
        fun bound(candidate: AgentModelRole): AgentProviderProfile? {
            val id = _bindings.value.providers[candidate] ?: return null
            return _profiles.value.firstOrNull { it.id == id && it.enabled }
        }
        return when (role) {
            AgentModelRole.BRAIN -> bound(AgentModelRole.BRAIN)
                ?: bound(AgentModelRole.EXECUTOR)
                ?: default
            AgentModelRole.RESPONDER -> bound(AgentModelRole.RESPONDER)
                ?: bound(AgentModelRole.SUMMARIZER)
                ?: default
            else -> bound(role) ?: default
        }
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

    private fun applySecretUpdate(
        account: String,
        update: SecretUpdate,
        snapshot: SecretSnapshot,
        attemptedSecrets: MutableList<Pair<String, SecretSnapshot>>
    ) {
        when (update) {
            SecretUpdate.Keep -> Unit
            SecretUpdate.Delete -> {
                attemptedSecrets += account to snapshot
                secrets.delete(account)
            }
            is SecretUpdate.Replace -> {
                attemptedSecrets += account to snapshot
                secrets.write(account, update.value)
            }
        }
    }

    private fun snapshotSecret(account: String): SecretSnapshot = runCatching { secrets.read(account) }
        .fold(
            onSuccess = { SecretSnapshot(value = it) },
            onFailure = { SecretSnapshot(readFailure = it) }
        )

    private fun restoreSecret(account: String, snapshot: SecretSnapshot, failure: Throwable) {
        snapshot.readFailure?.let { readFailure ->
            failure.addSuppressed(IllegalStateException("Secret snapshot was unavailable for rollback", readFailure))
            return
        }
        runCatching {
            if (snapshot.value == null) secrets.delete(account) else secrets.write(account, snapshot.value)
        }.exceptionOrNull()?.let(failure::addSuppressed)
    }

    private fun restoreProfiles(previousValue: List<AgentProviderProfile>, failure: Throwable) {
        runCatching { saveProfiles(previousValue) }.exceptionOrNull()?.let(failure::addSuppressed)
    }

    private fun restoreBindings(previousValue: AgentRoleBindings, failure: Throwable) {
        runCatching { saveBindings(previousValue) }.exceptionOrNull()?.let(failure::addSuppressed)
    }

    private fun encodeProfile(value: AgentProviderProfile): JsonObject = buildJsonObject {
        put("id", value.id); put("name", value.name); put("baseUrl", value.baseUrl); put("model", value.defaultModel)
        put("schemaVersion", value.schemaVersion); put("enabled", value.enabled); put("protocol", value.protocol.name)
        put("authType", value.authType.name); put("authHeaderName", value.authHeaderName)
        put("streamingMode", value.streamingMode.name)
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
            schemaVersion = obj["schemaVersion"]?.jsonPrimitive?.intOrNull ?: 1,
            protocol = runCatching { AgentModelProtocol.valueOf(text("protocol")) }.getOrDefault(AgentModelProtocol.OPENAI_COMPATIBLE),
            authType = runCatching { AgentProviderAuthType.valueOf(text("authType")) }.getOrDefault(AgentProviderAuthType.BEARER),
            authHeaderName = text("authHeaderName").ifBlank {
                runCatching { AgentProviderAuthType.valueOf(text("authType")) }
                    .getOrDefault(AgentProviderAuthType.BEARER)
                    .defaultHeaderName
            },
            streamingMode = runCatching { AgentStreamingMode.valueOf(text("streamingMode")) }.getOrDefault(AgentStreamingMode.AUTO),
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

fun validateAgentProviderProfile(profile: AgentProviderProfile): Result<Unit> = runCatching {
    require(profile.schemaVersion in 1..CURRENT_AGENT_PROVIDER_SCHEMA_VERSION) {
        "Unsupported provider schema version: ${profile.schemaVersion}"
    }
    require(profile.name.isNotBlank()) { "Provider name is required" }
    validateAiModelConfig(profile.toLegacyConfig()).getOrThrow()
    validateAgentProviderEndpointSecurity(profile).getOrThrow()
    require(profile.limits.maxOutputTokens in 1..1_000_000) { "Maximum output tokens are invalid" }
    require(profile.limits.timeoutMs in 1..300_000) { "Provider timeout must be between 1 and 300000 ms" }
    require(profile.limits.maxRetries in 0..5) { "Provider retries must be between 0 and 5" }
    if (profile.authType != AgentProviderAuthType.NONE) {
        validateHeaderName(profile.authHeaderName, allowAuthorization = true)
    }
    val managedAuthHeader = profile.authHeaderName.lowercase()
    val normalizedNames = profile.requestOptions.secretHeaderNames.map { name ->
        validateHeaderName(name, allowAuthorization = false)
        name.lowercase()
    }
    require(normalizedNames.distinct().size == normalizedNames.size) { "Secret header names must be unique" }
    require(managedAuthHeader.isBlank() || managedAuthHeader !in normalizedNames) {
        "Secret headers cannot override the managed authentication header"
    }
    parseAgentExtraBody(profile.requestOptions.extraBodyJson).getOrThrow()
}

internal fun parseAgentExtraBody(value: String): Result<JsonObject> = runCatching {
    if (value.isBlank()) return@runCatching JsonObject(emptyMap())
    val body = Json.parseToJsonElement(value) as? JsonObject
        ?: error("Provider extra body must be a JSON object")
    val forbidden = body.keys.filter { it.lowercase() in CORE_PROVIDER_BODY_FIELDS }
    require(forbidden.isEmpty()) {
        "Provider extra body cannot override core fields: ${forbidden.sorted().joinToString()}"
    }
    val secretFields = mutableListOf<String>()
    collectSecretLikeFields(body, path = "", pathTokens = emptyList(), destination = secretFields)
    require(secretFields.isEmpty()) {
        "Provider extra body cannot contain secret-like fields: ${secretFields.sorted().joinToString()}"
    }
    body
}

private fun collectSecretLikeFields(
    element: JsonElement,
    path: String,
    pathTokens: List<String>,
    destination: MutableList<String>
) {
    when (element) {
        is JsonObject -> element.forEach { (key, child) ->
            val childPath = if (path.isEmpty()) key else "$path.$key"
            val childTokens = pathTokens + providerConfigKeyTokens(key)
            if (isSecretLikeProviderConfigPath(childTokens)) destination += childPath
            collectSecretLikeFields(child, childPath, childTokens, destination)
        }
        is JsonArray -> element.forEachIndexed { index, child ->
            collectSecretLikeFields(child, "$path[$index]", pathTokens, destination)
        }
        else -> Unit
    }
}

internal fun validateSecretHeaders(profile: AgentProviderProfile, value: String?): Result<Unit> =
    decodeSecretHeaders(profile, value).map { Unit }

internal fun decodeSecretHeaders(
    profile: AgentProviderProfile,
    value: String?
): Result<Map<String, String>> = runCatching {
    val configured = profile.requestOptions.secretHeaderNames
    if (configured.isEmpty()) {
        if (value.isNullOrBlank()) return@runCatching emptyMap()
        val body = Json.parseToJsonElement(value) as? JsonObject
            ?: error("Secret header values must be a JSON object")
        require(body.isEmpty()) { "Secret header values were supplied without configured header names" }
        return@runCatching emptyMap()
    }
    require(!value.isNullOrBlank()) { "Secret header values are missing" }
    val body = Json.parseToJsonElement(value) as? JsonObject
        ?: error("Secret header values must be a JSON object")
    val configuredByLowerName = configured.associateBy { it.lowercase() }
    val unknown = body.keys.filter { it.lowercase() !in configuredByLowerName }
    require(unknown.isEmpty()) { "Unconfigured secret headers: ${unknown.sorted().joinToString()}" }
    val valuesByLowerName = body.mapKeys { (name, _) -> name.lowercase() }
    configured.associateWith { configuredName ->
        val element = valuesByLowerName[configuredName.lowercase()]
            ?: error("Secret header value is missing: $configuredName")
        require(element is JsonPrimitive && element.isString) {
            "Secret header value must be a string: $configuredName"
        }
        element.content.also { secret ->
            require(secret.isNotEmpty()) { "Secret header value is empty: $configuredName" }
            require('\r' !in secret && '\n' !in secret) { "Secret header value contains a line break: $configuredName" }
        }
    }
}

private fun validateHeaderName(name: String, allowAuthorization: Boolean) {
    require(HTTP_HEADER_NAME.matches(name)) { "Invalid HTTP header name: $name" }
    val normalized = name.lowercase()
    require(normalized !in DANGEROUS_PROVIDER_HEADERS || (allowAuthorization && normalized == "authorization")) {
        "Provider header is managed or unsafe: $name"
    }
    require(!normalized.startsWith("proxy-") && !normalized.startsWith("sec-") && !normalized.startsWith("x-forwarded-")) {
        "Provider header is unsafe: $name"
    }
}

private val CORE_PROVIDER_BODY_FIELDS = setOf(
    "model",
    "messages",
    "tools",
    "tool_choice",
    "stream",
    "stream_options",
    "max_tokens",
    "max_completion_tokens"
)

private sealed interface SecretUpdate {
    data object Keep : SecretUpdate
    data object Delete : SecretUpdate
    data class Replace(val value: String) : SecretUpdate
}

private fun secretUpdate(value: String?, forceDelete: Boolean): SecretUpdate = when {
    forceDelete -> SecretUpdate.Delete
    value == null -> SecretUpdate.Keep
    value.isBlank() -> SecretUpdate.Delete
    else -> SecretUpdate.Replace(value.trim())
}

private fun SecretUpdate.changes(snapshot: SecretSnapshot): Boolean = when (this) {
    SecretUpdate.Keep -> false
    SecretUpdate.Delete -> snapshot.value != null
    is SecretUpdate.Replace -> snapshot.value != value
}

private data class SecretSnapshot(
    val value: String? = null,
    val readFailure: Throwable? = null
) {
    fun valueOrThrow(): String? {
        readFailure?.let { throw it }
        return value
    }
}

private val DANGEROUS_PROVIDER_HEADERS = setOf(
    "authorization",
    "content-type",
    "content-length",
    "host",
    "connection",
    "transfer-encoding",
    "upgrade",
    "cookie",
    "set-cookie",
    "forwarded"
)

private val HTTP_HEADER_NAME = Regex("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$")

object AgentProviderRuntime { val repository: AgentProviderRepository by lazy { AgentProviderRepository() } }
