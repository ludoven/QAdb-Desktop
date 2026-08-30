package com.ludoven.adbtool.viewmodel

import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.agent.AgentBudgetStatus
import com.ludoven.adbtool.agent.AgentMessage
import com.ludoven.adbtool.agent.AgentMessageRole
import com.ludoven.adbtool.agent.AgentPublicActivityReducer
import com.ludoven.adbtool.agent.AgentPublicActivityState
import com.ludoven.adbtool.agent.AgentPublicEvent
import com.ludoven.adbtool.agent.AgentPublicEventPayload
import com.ludoven.adbtool.agent.AgentPublicEventSequencer
import com.ludoven.adbtool.agent.AgentPublicRunStatus
import com.ludoven.adbtool.agent.AgentPublicToolKind
import com.ludoven.adbtool.agent.AgentPublicToolResult
import com.ludoven.adbtool.agent.AgentPublicToolSummary
import com.ludoven.adbtool.agent.AgentRunPhase
import com.ludoven.adbtool.agent.AgentStepStatus
import com.ludoven.adbtool.agent.AgentTaskUiState
import com.ludoven.adbtool.agent.AgentTaskLogEntry
import com.ludoven.adbtool.agent.AgentTaskLogRuntime
import com.ludoven.adbtool.agent.AgentRunMetrics
import com.ludoven.adbtool.agent.AiConfigRepository
import com.ludoven.adbtool.agent.AiConfiguration
import com.ludoven.adbtool.agent.AgentProviderRuntime
import com.ludoven.adbtool.agent.AgentProviderRepository
import com.ludoven.adbtool.agent.AgentProviderAuthType
import com.ludoven.adbtool.agent.AgentModelRole
import com.ludoven.adbtool.agent.AgentCapabilityAttestation
import com.ludoven.adbtool.agent.AgentCapabilityTier
import com.ludoven.adbtool.agent.AgentDeviceGateway
import com.ludoven.adbtool.agent.AgentEngineVersion
import com.ludoven.adbtool.agent.AgentExecutionStrategy
import com.ludoven.adbtool.agent.AgentTaskRunner
import com.ludoven.adbtool.agent.ResolvedAgentProvider
import com.ludoven.adbtool.agent.RealAgentDeviceGateway
import com.ludoven.adbtool.agent.RoutedScreenshotAgentGateway
import com.ludoven.adbtool.agent.ScreenshotAgentEngine
import com.ludoven.adbtool.agent.AgentUsage
import com.ludoven.adbtool.agent.agentFailureFrom
import com.ludoven.adbtool.agent.toPublicMetrics
import com.ludoven.adbtool.agent.toPublicStage
import com.ludoven.adbtool.agent.toPublicToolResult
import com.ludoven.adbtool.agent.toPublicToolSummary
import com.ludoven.adbtool.util.l10n
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal fun ResolvedAgentProvider?.isReadyForAgentResponse(): Boolean =
    this != null && (
        profile.authType == AgentProviderAuthType.NONE ||
            !authSecret.isNullOrBlank()
        )

internal fun ResolvedAgentProvider?.isReadyForVisualAgent(
    attestation: AgentCapabilityAttestation?
): Boolean = isReadyForAgentResponse() &&
    attestation != null &&
    attestation.tier >= AgentCapabilityTier.L3_VISUAL_AGENT

class AiAgentViewModel(
    private val configRepository: AiConfigRepository = AiConfiguration.repository,
    deviceGateway: AgentDeviceGateway = RealAgentDeviceGateway(),
    agentTaskRunner: AgentTaskRunner = ScreenshotAgentEngine(
        model = RoutedScreenshotAgentGateway(),
        deviceGateway = deviceGateway
    ),
    private val providerRepository: AgentProviderRepository = AgentProviderRuntime.repository
) : BaseViewModel() {
    private val agentTaskRunner: AgentTaskRunner = agentTaskRunner
    private val _state = MutableStateFlow(AgentTaskUiState())
    val state: StateFlow<AgentTaskUiState> = _state.asStateFlow()

    val modelConfig = configRepository.config
    val apiKeyAvailable = configRepository.hasApiKeyState
    private val _configurationReady = MutableStateFlow(false)
    val configurationReady: StateFlow<Boolean> = _configurationReady.asStateFlow()
    private val _configurationChecked = MutableStateFlow(false)
    val configurationChecked: StateFlow<Boolean> = _configurationChecked.asStateFlow()
    private val _taskLogs = MutableStateFlow(emptyList<AgentTaskLogEntry>())
    val taskLogs: StateFlow<List<AgentTaskLogEntry>> = _taskLogs.asStateFlow()
    private val _runMetrics = MutableStateFlow(emptyList<AgentRunMetrics>())
    val runMetrics: StateFlow<List<AgentRunMetrics>> = _runMetrics.asStateFlow()

    private val taskRunHandles = AgentTaskRunHandleRegistry()
    private var publicEventAdapter: AgentOrchestratorPublicEventAdapter? = null
    private val publicStateSynchronizer = AgentPublicStateSynchronizer()

    init {
        refreshConfigurationStatus()
        refreshTaskLogs()
        refreshRunMetrics()
    }

    fun refreshConfigurationStatus() {
        _configurationChecked.value = false
        viewModelScope.launch {
            runCatching { configRepository.isReady() }
            val provider = runCatching {
                providerRepository.ensureMigration()
                providerRepository.resolve(AgentModelRole.BRAIN)
            }.getOrNull()
            val attestation = provider?.let { providerRepository.capabilityAttestation(it.profile) }
            _configurationReady.value = provider.isReadyForVisualAgent(attestation)
            _configurationChecked.value = true
        }
    }

    fun startTask(
        prompt: String,
        selectedDeviceId: String?
    ) {
        val task = prompt.trim()
        if (task.isEmpty() || _state.value.isRunning) return
        if (!configurationReady.value) {
            publicStateSynchronizer.serialized {
                _state.value = _state.value.copy(
                    errorMessage = l10n(
                        "请先在设置中配置并测试支持视觉与工具调用的 BRAIN Provider（L3）",
                        "Configure and test a BRAIN provider with L3 vision and tool calling"
                    )
                )
            }
            return
        }
        if (selectedDeviceId.isNullOrBlank()) {
            publicStateSynchronizer.serialized {
                _state.value = _state.value.copy(
                    errorMessage = l10n("请先连接并选择一台设备", "Connect and select a device first")
                )
            }
            return
        }
        val acceptedAtMs = System.currentTimeMillis()
        val executionDeviceId = selectedDeviceId.orEmpty()
        val runId = UUID.randomUUID().toString()
        val taskJob = publicStateSynchronizer.serialized {
            val initialState = _state.value
            if (initialState.isRunning) return@serialized null
            val adapter = AgentOrchestratorPublicEventAdapter(
                runId = runId,
                baseline = initialState
            )
            publicEventAdapter = adapter
            val startedActivity = AgentPublicActivityReducer.reduce(
                initialState.publicActivity,
                adapter.startEvent()
            )
            // The accepted user message must be part of the state handed to the runner.
            // Otherwise the first orchestrator snapshot (built from this pre-send state)
            // would erase the optimistic user message from the conversation as soon as
            // the engine publishes (mergeAgentOrchestratorSnapshot replaces _state).
            val acceptedState = initialState.copy(
                messages = initialState.messages + AgentMessage(
                    id = UUID.randomUUID().toString(),
                    role = AgentMessageRole.USER,
                    text = task,
                    runId = runId
                ),
                steps = emptyList(),
                isRunning = true,
                needsUser = false,
                boundDeviceId = selectedDeviceId?.takeIf(String::isNotBlank),
                pendingConfirmation = null,
                errorMessage = null,
                failure = null,
                phase = AgentRunPhase.OBSERVING,
                usage = AgentUsage(),
                budgetStatus = AgentBudgetStatus(),
                executionDetails = emptyList(),
                deviceState = null,
                pageDiff = null,
                latestScreenshot = null,
                publicActivity = startedActivity
            )
            _state.value = acceptedState
            val firstFeedbackMs = (System.currentTimeMillis() - acceptedAtMs).coerceAtLeast(0)
            val runHandle = taskRunHandles.begin(runId, adapter, firstFeedbackMs)
            viewModelScope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
                executeTask(
                    task = task,
                    executionDeviceId = executionDeviceId,
                    initialState = acceptedState,
                    runHandle = runHandle,
                    acceptedAtMs = acceptedAtMs,
                    firstFeedbackMs = runHandle.firstFeedbackMs
                )
            }.also { job ->
                check(taskRunHandles.attachJob(runHandle, job)) { "Agent run was replaced before launch" }
                job.invokeOnAgentCancellation { completeCancelledRun(runHandle) }
            }
        } ?: return
        taskJob.start()
    }

    private suspend fun executeTask(
        task: String,
        executionDeviceId: String,
        initialState: AgentTaskUiState,
        runHandle: AgentTaskRunHandle,
        acceptedAtMs: Long,
        firstFeedbackMs: Long
    ) {
        try {
            agentTaskRunner.run(
                task = task,
                deviceId = executionDeviceId,
                initialState = initialState,
                runId = runHandle.runId,
                acceptedAtMs = acceptedAtMs,
                firstFeedbackMs = firstFeedbackMs,
                onState = { publishOrchestratorState(runHandle.runId, runHandle.adapter, it) },
                confirmSensitiveAction = {
                    val deferred = CompletableDeferred<Boolean>()
                    if (!taskRunHandles.registerConfirmation(runHandle, deferred)) {
                        deferred.cancel()
                        throw CancellationException("Agent run is no longer active")
                    }
                    try {
                        deferred.await()
                    } finally {
                        taskRunHandles.clearConfirmation(runHandle, deferred)
                    }
                }
            )
        } catch (_: CancellationException) {
            // Also covers cancellation before the orchestrator enters its own cancellation handler.
            completeCancelledRun(runHandle)
        } finally {
            publicStateSynchronizer.serialized {
                releaseRunHandleLocked(runHandle)
            }
            refreshTaskLogs()
            refreshRunMetrics()
        }
    }

    /** Entry point for streaming/model integrations that can emit native public events directly. */
    fun acceptPublicEvent(event: AgentPublicEvent) {
        publicStateSynchronizer.serialized {
            acceptPublicEventLocked(event)
        }
    }

    fun respondToConfirmation(approved: Boolean) {
        taskRunHandles.respondToActiveConfirmation(approved)
    }

    fun cancelTask() {
        val cancellation = publicStateSynchronizer.serialized {
            publicEventAdapter?.next(AgentPublicEventPayload.Cancelling)?.let(::acceptPublicEventLocked)
            taskRunHandles.detachActiveForCancellation().also {
                _state.value = _state.value.copy(pendingConfirmation = null)
            }
        }
        cancellation.confirmation?.cancel()
        cancellation.job?.cancel()
    }

    fun newTask() {
        cancelTask()
        publicStateSynchronizer.serialized {
            taskRunHandles.abandonActive()
            publicEventAdapter = null
            _state.value = AgentTaskUiState()
        }
    }

    fun clearError() {
        publicStateSynchronizer.serialized {
            _state.value = _state.value.copy(errorMessage = null)
        }
    }

    fun refreshTaskLogs() = viewModelScope.launch {
        _taskLogs.value = runCatching { AgentTaskLogRuntime.store.recent() }.getOrDefault(emptyList())
    }

    fun refreshRunMetrics() = viewModelScope.launch {
        _runMetrics.value = runCatching { AgentTaskLogRuntime.store.recentMetrics() }.getOrDefault(emptyList())
    }

    private fun publishOrchestratorState(
        runId: String,
        adapter: AgentOrchestratorPublicEventAdapter,
        snapshot: AgentTaskUiState
    ) {
        publicStateSynchronizer.serialized {
            val current = _state.value
            if (current.publicActivity.activeRunId != runId || publicEventAdapter !== adapter) {
                return@serialized
            }

            var publicActivity = current.publicActivity
            adapter.eventsFor(snapshot).forEach { event ->
                publicActivity = AgentPublicActivityReducer.reduce(publicActivity, event)
            }
            val taggedMessages = snapshot.messages
                .mapIndexed { index, message ->
                    if (index >= adapter.baselineMessageCount) message.copy(runId = runId) else message
                }
                .filterNot { message ->
                    message.role == AgentMessageRole.SYSTEM &&
                        snapshot.phase == AgentRunPhase.FAILED &&
                        message.text == snapshot.errorMessage
                }

            _state.value = mergeAgentOrchestratorSnapshot(
                current = current,
                snapshot = snapshot.copy(messages = taggedMessages),
                publicActivity = publicActivity
            )
        }
    }

    private fun completeCancelledRun(runHandle: AgentTaskRunHandle) {
        publicStateSynchronizer.serialized {
            val current = _state.value
            if (
                current.publicActivity.activeRunId == runHandle.runId &&
                publicEventAdapter === runHandle.adapter
            ) {
                val status = current.publicActivity.activeRun?.status
                _state.value = if (status?.isTerminal == true) {
                    current.copy(pendingConfirmation = null)
                } else {
                    current.applyCancelledEvent(
                        runHandle.adapter.next(AgentPublicEventPayload.Cancelled)
                    )
                }
            }
            releaseRunHandleLocked(runHandle)
        }
    }

    /** Must be called while [publicStateSynchronizer] is held. */
    private fun releaseRunHandleLocked(runHandle: AgentTaskRunHandle) {
        if (taskRunHandles.finish(runHandle) && publicEventAdapter === runHandle.adapter) {
            publicEventAdapter = null
        }
    }

    /** Must be called while [publicStateSynchronizer] is held. */
    private fun acceptPublicEventLocked(event: AgentPublicEvent) {
        val current = _state.value
        val reduced = AgentPublicActivityReducer.reduce(current.publicActivity, event)
        if (reduced != current.publicActivity) {
            _state.value = current.copy(publicActivity = reduced)
        }
    }
}

internal fun mergeAgentOrchestratorSnapshot(
    current: AgentTaskUiState,
    snapshot: AgentTaskUiState,
    publicActivity: AgentPublicActivityState
): AgentTaskUiState {
    val currentStatus = current.publicActivity.activeRun?.status
    val nextStatus = publicActivity.activeRun?.status
    val preserveCurrentSnapshot =
        (currentStatus == AgentPublicRunStatus.CANCELLING &&
            nextStatus == AgentPublicRunStatus.CANCELLING) ||
            (currentStatus?.isTerminal == true && nextStatus == currentStatus)
    return if (preserveCurrentSnapshot) {
        current.copy(pendingConfirmation = null, publicActivity = publicActivity)
    } else {
        snapshot.copy(publicActivity = publicActivity)
    }
}

internal fun AgentTaskUiState.applyCancelledEvent(event: AgentPublicEvent): AgentTaskUiState {
    val reduced = AgentPublicActivityReducer.reduce(publicActivity, event)
    val cancelled = reduced.runs[event.runId]?.status == AgentPublicRunStatus.CANCELLED
    return if (cancelled) {
        copy(
            isRunning = false,
            pendingConfirmation = null,
            errorMessage = null,
            phase = AgentRunPhase.CANCELLED,
            publicActivity = reduced
        )
    } else {
        copy(pendingConfirmation = null, publicActivity = reduced)
    }
}

internal fun Job.invokeOnAgentCancellation(onCancelled: () -> Unit) {
    invokeOnCompletion { cause ->
        if (cause is CancellationException) onCancelled()
    }
}

/** Serializes public run state with adapter sequencing across UI and orchestrator threads. */
internal class AgentPublicStateSynchronizer {
    private val lock = Any()

    fun <T> serialized(block: () -> T): T = synchronized(lock) { block() }
}

internal data class AgentTaskRunCancellation(
    val job: Job?,
    val confirmation: CompletableDeferred<Boolean>?
)

internal class AgentTaskRunHandle internal constructor(
    val runId: String,
    val adapter: AgentOrchestratorPublicEventAdapter,
    val firstFeedbackMs: Long
) {
    internal var job: Job? = null
    internal var confirmation: CompletableDeferred<Boolean>? = null
    internal var cancellationRequested: Boolean = false
}

/** Keeps cancellation and confirmation handles scoped to the run that created them. */
internal class AgentTaskRunHandleRegistry {
    private var active: AgentTaskRunHandle? = null

    @Synchronized
    fun begin(
        runId: String,
        adapter: AgentOrchestratorPublicEventAdapter,
        firstFeedbackMs: Long
    ): AgentTaskRunHandle = AgentTaskRunHandle(runId, adapter, firstFeedbackMs).also { active = it }

    @Synchronized
    fun attachJob(handle: AgentTaskRunHandle, job: Job): Boolean {
        if (active !== handle || handle.cancellationRequested) return false
        handle.job = job
        return true
    }

    @Synchronized
    fun registerConfirmation(
        handle: AgentTaskRunHandle,
        confirmation: CompletableDeferred<Boolean>
    ): Boolean {
        if (active !== handle || handle.cancellationRequested) return false
        handle.confirmation = confirmation
        return true
    }

    @Synchronized
    fun clearConfirmation(
        handle: AgentTaskRunHandle,
        confirmation: CompletableDeferred<Boolean>
    ) {
        if (handle.confirmation === confirmation) handle.confirmation = null
    }

    @Synchronized
    fun respondToActiveConfirmation(approved: Boolean): Boolean =
        active?.confirmation
            ?.takeIf { !it.isCompleted }
            ?.complete(approved) == true

    @Synchronized
    fun detachActiveForCancellation(): AgentTaskRunCancellation {
        val handle = active ?: return AgentTaskRunCancellation(null, null)
        handle.cancellationRequested = true
        val cancellation = AgentTaskRunCancellation(handle.job, handle.confirmation)
        handle.job = null
        handle.confirmation = null
        return cancellation
    }

    @Synchronized
    fun finish(handle: AgentTaskRunHandle): Boolean {
        handle.job = null
        handle.confirmation = null
        if (active !== handle) return false
        active = null
        return true
    }

    @Synchronized
    fun abandonActive() {
        active?.job = null
        active?.confirmation = null
        active = null
    }
}

/**
 * Compatibility adapter for the current snapshot-based orchestrator. New execution code can emit
 * [AgentPublicEvent] directly through [AiAgentViewModel.acceptPublicEvent].
 */
internal class AgentOrchestratorPublicEventAdapter(
    runId: String,
    baseline: AgentTaskUiState,
    clock: () -> Long = System::currentTimeMillis
) {
    private val sequencer = AgentPublicEventSequencer(runId, clock)
    private var previous = baseline
    private val baselineMessageIds = baseline.messages.asSequence().map { it.id }.toHashSet()
    private val emittedAssistantTextById = mutableMapOf<String, String>()
    private var responseCompletedEmitted = false
    val baselineMessageCount: Int = baseline.messages.size

    @Synchronized
    fun startEvent(): AgentPublicEvent = sequencer.next(AgentPublicEventPayload.RunStarted)

    @Synchronized
    fun next(payload: AgentPublicEventPayload): AgentPublicEvent = sequencer.next(payload)

    @Synchronized
    fun eventsFor(snapshot: AgentTaskUiState): List<AgentPublicEvent> {
        val events = mutableListOf<AgentPublicEvent>()
        if (
            snapshot.phase != previous.phase &&
            snapshot.phase !in setOf(
                AgentRunPhase.COMPLETED,
                AgentRunPhase.FAILED,
                AgentRunPhase.CANCELLED
            )
        ) {
            if (snapshot.phase == AgentRunPhase.RETRYING) {
                events += next(AgentPublicEventPayload.Retrying())
            } else {
                events += next(AgentPublicEventPayload.StageChanged(snapshot.phase.toPublicStage()))
            }
        }

        val previousSteps = previous.steps.associateBy { it.id }
        snapshot.steps.forEach { step ->
            val old = previousSteps[step.id]
            val tool = step.action.toPublicToolSummary()
            when {
                step.status == AgentStepStatus.AWAITING_CONFIRMATION &&
                    old?.status != AgentStepStatus.AWAITING_CONFIRMATION -> {
                    events += next(AgentPublicEventPayload.ConfirmationRequested(tool))
                }
                step.status == AgentStepStatus.RUNNING && old?.status != AgentStepStatus.RUNNING -> {
                    events += next(AgentPublicEventPayload.ToolStarted(tool))
                }
                step.status.toPublicToolResult() != null && old?.status != step.status -> {
                    if (old == null) events += next(AgentPublicEventPayload.ToolStarted(tool))
                    events += next(
                        AgentPublicEventPayload.ToolFinished(
                            tool = tool,
                            result = requireNotNull(step.status.toPublicToolResult())
                        )
                    )
                }
            }
        }

        val semanticTool = AgentPublicToolSummary(AgentPublicToolKind.SEMANTIC_GOAL)
        val oldSemantic = previous.semanticActivity
        snapshot.semanticActivity?.let { semantic ->
            when {
                semantic.status == com.ludoven.adbtool.agent.AgentSemanticActivityStatus.RUNNING &&
                    oldSemantic != semantic -> events += next(AgentPublicEventPayload.ToolStarted(semanticTool))
                semantic.status != com.ludoven.adbtool.agent.AgentSemanticActivityStatus.RUNNING &&
                    oldSemantic != semantic -> {
                    if (oldSemantic == null) events += next(AgentPublicEventPayload.ToolStarted(semanticTool))
                    events += next(
                        AgentPublicEventPayload.ToolFinished(
                            semanticTool,
                            if (semantic.status == com.ludoven.adbtool.agent.AgentSemanticActivityStatus.SUCCEEDED) {
                                AgentPublicToolResult.SUCCEEDED
                            } else {
                                AgentPublicToolResult.FAILED
                            }
                        )
                    )
                }
            }
        }

        if (snapshot.budgetStatus != previous.budgetStatus) {
            events += next(
                AgentPublicEventPayload.MetricsUpdated(
                    snapshot.budgetStatus.toPublicMetrics(
                        if (snapshot.executionStrategy == AgentExecutionStrategy.SEMANTIC_V2) {
                            AgentEngineVersion.V2
                        } else {
                            AgentEngineVersion.SCREENSHOT
                        }
                    )
                )
            )
        }

        snapshot.messages
            .asSequence()
            .filter { it.id !in baselineMessageIds && it.role == AgentMessageRole.ASSISTANT }
            .forEach { message ->
                val emittedText = emittedAssistantTextById[message.id]
                val delta = when {
                    emittedText == null -> message.text
                    message.text.startsWith(emittedText) -> message.text.removePrefix(emittedText)
                    else -> ""
                }
                if (delta.isNotEmpty()) {
                    events += next(AgentPublicEventPayload.ResponseDelta(delta))
                }
                if (emittedText == null || message.text.startsWith(emittedText)) {
                    emittedAssistantTextById[message.id] = message.text
                }
            }

        val reachedTerminal = snapshot.phase in setOf(
            AgentRunPhase.COMPLETED,
            AgentRunPhase.FAILED,
            AgentRunPhase.CANCELLED
        )
        if (
            reachedTerminal &&
            !responseCompletedEmitted &&
            emittedAssistantTextById.isNotEmpty()
        ) {
            events += next(AgentPublicEventPayload.ResponseCompleted)
            responseCompletedEmitted = true
        }

        when {
            snapshot.phase == AgentRunPhase.FAILED && previous.phase != AgentRunPhase.FAILED -> {
                events += next(
                    AgentPublicEventPayload.Failed(
                        snapshot.failure ?: agentFailureFrom(snapshot.errorMessage)
                    )
                )
            }
            snapshot.phase == AgentRunPhase.CANCELLED && previous.phase != AgentRunPhase.CANCELLED -> {
                events += next(AgentPublicEventPayload.Cancelled)
            }
            snapshot.phase == AgentRunPhase.COMPLETED && previous.phase != AgentRunPhase.COMPLETED -> {
                events += next(AgentPublicEventPayload.Completed)
            }
        }
        previous = snapshot
        return events
    }
}
