package com.ludoven.adbtool.agent

import java.util.prefs.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AiConfigRepository(
    private val preferences: Preferences = Preferences.userNodeForPackage(AiConfigRepository::class.java),
    private val secretStore: SecretStore = PlatformSecretStoreFactory.create()
) {
    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<AiModelConfig> = _config.asStateFlow()
    private val _hasApiKey = MutableStateFlow(preferences.getBoolean(KEY_SECRET_CONFIGURED, false))
    val hasApiKeyState: StateFlow<Boolean> = _hasApiKey.asStateFlow()
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        updateStatus(hasKey = _hasApiKey.value)
    }

    fun loadConfig(): AiModelConfig = AiModelConfig(
        baseUrl = preferences.get(KEY_BASE_URL, AiModelConfig.DEFAULT_OPENAI_BASE_URL),
        model = preferences.get(KEY_MODEL, ""),
        visionMode = runCatching {
            VisionMode.valueOf(preferences.get(KEY_VISION_MODE, VisionMode.AUTO.name))
        }.getOrDefault(VisionMode.AUTO),
        contextWindowTokens = preferences.getInt(KEY_CONTEXT_WINDOW, 0).takeIf { it > 0 }
    )

    suspend fun hasApiKey(): Boolean = withContext(Dispatchers.IO) {
        val available = preferences.getBoolean(KEY_SECRET_CONFIGURED, false)
        updateStatus(available)
        available
    }

    suspend fun loadApiKey(): String? = withContext(Dispatchers.IO) {
        secretStore.read(API_KEY_ACCOUNT).also { secret ->
            val hasKey = secret != null
            preferences.putBoolean(KEY_SECRET_CONFIGURED, hasKey)
            preferences.flush()
            updateStatus(hasKey)
        }
    }

    suspend fun save(config: AiModelConfig, apiKey: String?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            validateAiModelConfig(config).getOrThrow()
            val normalizedKey = apiKey?.trim().orEmpty()
            if (normalizedKey.isNotEmpty()) {
                secretStore.write(API_KEY_ACCOUNT, normalizedKey)
                preferences.putBoolean(KEY_SECRET_CONFIGURED, true)
            } else {
                val hasExisting = preferences.getBoolean(KEY_SECRET_CONFIGURED, false) || secretStore.read(API_KEY_ACCOUNT) != null
                require(hasExisting) { "API key is required" }
                preferences.putBoolean(KEY_SECRET_CONFIGURED, true)
            }
            preferences.put(KEY_BASE_URL, config.baseUrl.trim())
            preferences.put(KEY_MODEL, config.model.trim())
            preferences.put(KEY_VISION_MODE, config.visionMode.name)
            config.contextWindowTokens?.let {
                preferences.putInt(KEY_CONTEXT_WINDOW, it)
            } ?: preferences.remove(KEY_CONTEXT_WINDOW)
            preferences.flush()
            _config.value = config.copy(
                baseUrl = config.baseUrl.trim(),
                model = config.model.trim()
            )
            updateStatus(hasKey = true)
        }
    }

    suspend fun clearApiKey(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            secretStore.delete(API_KEY_ACCOUNT)
            preferences.putBoolean(KEY_SECRET_CONFIGURED, false)
            preferences.flush()
            updateStatus(hasKey = false)
        }
    }

    suspend fun isReady(): Boolean {
        val hasKey = hasApiKey()
        return validateAiModelConfig(_config.value).isSuccess && hasKey
    }

    private fun updateStatus(hasKey: Boolean) {
        _hasApiKey.value = hasKey
        _ready.value = hasKey && validateAiModelConfig(_config.value).isSuccess
    }

    companion object {
        private const val KEY_BASE_URL = "ai.base_url"
        private const val KEY_MODEL = "ai.model"
        private const val KEY_VISION_MODE = "ai.vision_mode"
        private const val KEY_CONTEXT_WINDOW = "ai.context_window_tokens"
        private const val KEY_SECRET_CONFIGURED = "ai.secret_configured"
        private const val API_KEY_ACCOUNT = "openai-compatible-api-key"
    }
}

object AiConfiguration {
    val repository: AiConfigRepository by lazy { AiConfigRepository() }
}
