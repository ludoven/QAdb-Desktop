package com.ludoven.adbtool.viewmodel

import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.agent.AgentModelClient
import com.ludoven.adbtool.agent.AgentOrchestrator
import com.ludoven.adbtool.agent.AgentTaskUiState
import com.ludoven.adbtool.agent.AgentTaskSource
import com.ludoven.adbtool.agent.AgentMemoryPreferences
import com.ludoven.adbtool.agent.AgentMemoryRuntime
import com.ludoven.adbtool.agent.AiConfigRepository
import com.ludoven.adbtool.agent.AiConfiguration
import com.ludoven.adbtool.agent.OpenAiCompatibleClient
import com.ludoven.adbtool.agent.RealAgentDeviceGateway
import com.ludoven.adbtool.util.l10n
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiAgentViewModel(
    private val configRepository: AiConfigRepository = AiConfiguration.repository,
    modelClient: AgentModelClient = OpenAiCompatibleClient(),
    orchestrator: AgentOrchestrator = AgentOrchestrator(
        modelClient = modelClient,
        deviceGateway = RealAgentDeviceGateway()
    ),
    private val memoryPreferences: AgentMemoryPreferences = AgentMemoryRuntime.preferences
) : BaseViewModel() {
    private val agentOrchestrator = orchestrator
    private val _state = MutableStateFlow(AgentTaskUiState())
    val state: StateFlow<AgentTaskUiState> = _state.asStateFlow()

    val modelConfig = configRepository.config
    val apiKeyAvailable = configRepository.hasApiKeyState
    val configurationReady: StateFlow<Boolean> = configRepository.ready
    val memoryEnabled: StateFlow<Boolean> = memoryPreferences.enabled
    private val _memoryNeedsConsent = MutableStateFlow(memoryPreferences.needsConsent)
    val memoryNeedsConsent: StateFlow<Boolean> = _memoryNeedsConsent.asStateFlow()

    private var taskJob: Job? = null
    private var pendingConfirmation: CompletableDeferred<Boolean>? = null

    init {
        refreshConfigurationStatus()
    }

    fun refreshConfigurationStatus() {
        viewModelScope.launch {
            configRepository.isReady()
        }
    }

    fun acceptMemoryConsent() {
        memoryPreferences.acceptConsent()
        _memoryNeedsConsent.value = false
    }

    fun declineMemoryConsent() {
        memoryPreferences.declineConsent()
        _memoryNeedsConsent.value = false
    }

    fun startTask(
        prompt: String,
        selectedDeviceId: String?,
        taskSource: AgentTaskSource = AgentTaskSource.NATURAL_LANGUAGE
    ) {
        val task = prompt.trim()
        if (task.isEmpty() || _state.value.isRunning) return
        if (selectedDeviceId.isNullOrBlank()) {
            _state.value = _state.value.copy(
                errorMessage = l10n("请先连接并选择设备", "Connect and select a device first")
            )
            return
        }
        taskJob = viewModelScope.launch {
            val config = configRepository.config.value
            val apiKey = configRepository.loadApiKey()
            if (apiKey.isNullOrBlank()) {
                _state.value = _state.value.copy(
                    errorMessage = l10n("请先在设置中完成 AI 模型配置", "Configure the AI model in Settings first")
                )
                return@launch
            }
            try {
                agentOrchestrator.run(
                    task = task,
                    deviceId = selectedDeviceId,
                    config = config,
                    apiKey = apiKey,
                    taskSource = taskSource,
                    initialState = _state.value,
                    onState = { _state.value = it },
                    confirmSensitiveAction = {
                        CompletableDeferred<Boolean>().also { deferred ->
                            pendingConfirmation = deferred
                        }.await()
                    }
                )
            } catch (_: CancellationException) {
                // Orchestrator publishes the final cancelled state.
            } finally {
                pendingConfirmation = null
                taskJob = null
            }
        }
    }

    fun respondToConfirmation(approved: Boolean) {
        pendingConfirmation?.takeIf { !it.isCompleted }?.complete(approved)
    }

    fun cancelTask() {
        pendingConfirmation?.cancel()
        pendingConfirmation = null
        taskJob?.cancel()
        taskJob = null
    }

    fun newTask() {
        cancelTask()
        _state.value = AgentTaskUiState()
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}
