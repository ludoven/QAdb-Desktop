package com.ludoven.adbtool.viewmodel

import com.ludoven.adbtool.agent.AgentCapabilities
import com.ludoven.adbtool.agent.AgentCapabilityAttestationStore
import com.ludoven.adbtool.agent.AgentCapabilityTier
import com.ludoven.adbtool.agent.AgentMessage
import com.ludoven.adbtool.agent.AgentMessageRole
import com.ludoven.adbtool.agent.AgentProviderAuthType
import com.ludoven.adbtool.agent.AgentProviderProfile
import com.ludoven.adbtool.agent.AgentRunPhase
import com.ludoven.adbtool.agent.AgentStep
import com.ludoven.adbtool.agent.AgentTaskRunner
import com.ludoven.adbtool.agent.AgentTaskUiState
import com.ludoven.adbtool.agent.AiConfigRepository
import com.ludoven.adbtool.agent.AgentProviderRepository
import com.ludoven.adbtool.agent.SecretStore
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking

/**
 * End-to-end regression for the AI Agent conversation chain:
 *
 * startTask → optimistic user message → engine first-frame snapshot publish →
 * the published snapshot must still carry the user message, and the final
 * viewModel state must keep it too.
 *
 * Guards against the historical defect where executeTask received the
 * pre-send initial state, so the engine's first snapshot erased the
 * optimistic user message from the conversation.
 */
class AiAgentViewModelStartTaskTest {

    private val preferenceNodes = mutableListOf<Preferences>()

    @AfterTest
    fun cleanupPreferences() {
        preferenceNodes.forEach { node -> runCatching { node.removeNode() } }
        preferenceNodes.clear()
    }

    @Test
    fun `engine first frame snapshot preserves the accepted user message`() {
        val runner = FirstFrameRecordingRunner()
        val viewModel = newViewModel(runner)
        val task = "打开设置并关闭蓝牙"

        viewModel.startTask(task, "emulator-5554")

        val accepted = runner.awaitInitialState()
        val userMessage = accepted.messages.lastOrNull()
        assertNotNull(userMessage, "Engine initial state must contain the accepted user message")
        assertEquals(AgentMessageRole.USER, userMessage.role)
        assertEquals(task, userMessage.text)
        assertNotNull(userMessage.runId, "Accepted user message must be tagged with the run id")

        assertTrue(
            runner.awaitFirstFramePublished(),
            "First frame snapshot was never handed to onState"
        )
        runner.finishRun()

        val finalState = viewModel.awaitUntil(
            "run completes",
            diagnostics = { "; runnerFailure=${runner.runFailure}" }
        ) {
            it.phase == AgentRunPhase.COMPLETED && !it.isRunning
        }
        assertTrue(
            finalState.messages.any { it.role == AgentMessageRole.USER && it.text == task },
            "Final viewModel state must retain the user message after engine snapshots publish"
        )
        assertTrue(finalState.messages.any { it.role == AgentMessageRole.ASSISTANT })
    }

    @Test
    fun `cancelTask resets isRunning and allows a new run to start`() {
        val runner = GatedBlockingRunner()
        val viewModel = newViewModel(runner)
        val firstTask = "截图并分析当前页面"

        viewModel.startTask(firstTask, "emulator-5554")
        viewModel.awaitUntil("run starts") { it.isRunning }

        viewModel.cancelTask()

        val cancelledState = viewModel.awaitUntil("run cancelled") {
            !it.isRunning && it.phase == AgentRunPhase.CANCELLED
        }
        assertTrue(
            cancelledState.messages.any { it.role == AgentMessageRole.USER && it.text == firstTask },
            "Cancelled run must retain the user message"
        )

        val secondTask = "返回桌面"
        viewModel.startTask(secondTask, "emulator-5554")
        val restarted = viewModel.awaitUntil("second run starts") { it.isRunning }
        assertTrue(
            restarted.messages.any { it.role == AgentMessageRole.USER && it.text == secondTask },
            "Second run state must contain the new user message"
        )

        runner.release()
        val completed = viewModel.awaitUntil(
            "second run completes",
            diagnostics = { "; runnerFailure=${runner.runFailure}" }
        ) {
            it.phase == AgentRunPhase.COMPLETED && !it.isRunning
        }
        assertTrue(
            completed.messages.any { it.role == AgentMessageRole.USER && it.text == secondTask },
            "Completed second run must retain its user message"
        )
    }

    /** Records the initial state handed to the engine, then mirrors it back as the first frame. */
    private class FirstFrameRecordingRunner : AgentTaskRunner {
        private val initialStateLatch = CountDownLatch(1)

        @Volatile
        private var initialStateValue: AgentTaskUiState? = null
        private val firstFramePublished = CountDownLatch(1)
        private val finishRequested = CountDownLatch(1)

        override suspend fun run(
            task: String,
            deviceId: String,
            initialState: AgentTaskUiState,
            runId: String?,
            acceptedAtMs: Long?,
            firstFeedbackMs: Long?,
            onState: (AgentTaskUiState) -> Unit,
            confirmSensitiveAction: suspend (AgentStep) -> Boolean
        ): AgentTaskUiState {
            initialStateValue = initialState
            initialStateLatch.countDown()
            try {
                // First engine frame: an observation snapshot derived from the accepted state.
                onState(initialState.copy(phase = AgentRunPhase.THINKING))
                firstFramePublished.countDown()
                assertTrue(
                    finishRequested.await(10, TimeUnit.SECONDS),
                    "Test never released the runner"
                )
                return completedState(initialState, task).also { onState(it) }
            } catch (t: Throwable) {
                runFailure = t
                throw t
            }
        }

        @Volatile
        var runFailure: Throwable? = null
            private set

        fun awaitInitialState(): AgentTaskUiState {
            assertTrue(
                initialStateLatch.await(10, TimeUnit.SECONDS),
                "Engine run() was never invoked"
            )
            return requireNotNull(initialStateValue)
        }

        fun awaitFirstFramePublished(): Boolean =
            firstFramePublished.await(10, TimeUnit.SECONDS)

        fun finishRun() {
            finishRequested.countDown()
        }
    }

    /** Blocks inside the run until [release], so cancellation can be exercised. */
    private class GatedBlockingRunner : AgentTaskRunner {
        private val gate = CompletableDeferred<Unit>()

        override suspend fun run(
            task: String,
            deviceId: String,
            initialState: AgentTaskUiState,
            runId: String?,
            acceptedAtMs: Long?,
            firstFeedbackMs: Long?,
            onState: (AgentTaskUiState) -> Unit,
            confirmSensitiveAction: suspend (AgentStep) -> Boolean
        ): AgentTaskUiState {
            try {
                onState(initialState.copy(phase = AgentRunPhase.THINKING))
                gate.await()
                return completedState(initialState, task).also { onState(it) }
            } catch (t: Throwable) {
                if (t !is kotlinx.coroutines.CancellationException) runFailure = t
                throw t
            }
        }

        @Volatile
        var runFailure: Throwable? = null
            private set

        fun release() {
            gate.complete(Unit)
        }
    }

    private fun AiAgentViewModel.awaitUntil(
        description: String,
        timeoutMs: Long = 10_000,
        diagnostics: () -> String = { "" },
        condition: (AgentTaskUiState) -> Boolean
    ): AgentTaskUiState {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val snapshot = state.value
            if (condition(snapshot)) return snapshot
            Thread.sleep(20)
        }
        error("Timed out waiting for: $description; last state=${state.value}${diagnostics()}")
    }

    private fun newViewModel(runner: AgentTaskRunner): AiAgentViewModel = runBlocking {
        val configRepository = AiConfigRepository(
            preferences = newPreferencesNode(),
            secretStore = InMemorySecretStore()
        )
        val providerRepository = AgentProviderRepository(
            preferences = newPreferencesNode(),
            secrets = InMemorySecretStore(),
            legacy = configRepository,
            capabilityAttestations = AgentCapabilityAttestationStore(newPreferencesNode())
        )
        val profile = AgentProviderProfile(
            name = "Test L3 Provider",
            authType = AgentProviderAuthType.NONE,
            baseUrl = "https://example.test/v1",
            defaultModel = "test-model",
            capabilities = AgentCapabilities(vision = true)
        )
        providerRepository.upsert(profile, apiKey = null)
        providerRepository.attestCapabilities(profile, AgentCapabilityTier.L3_VISUAL_AGENT)

        val viewModel = AiAgentViewModel(
            configRepository = configRepository,
            agentTaskRunner = runner,
            providerRepository = providerRepository
        )
        val readyDeadline = System.currentTimeMillis() + 10_000
        while (!viewModel.configurationReady.value) {
            check(System.currentTimeMillis() < readyDeadline) {
                "Timed out waiting for configurationReady; " +
                    "state=${viewModel.state.value} provider=${providerRepository.profiles.value}"
            }
            Thread.sleep(20)
        }
        viewModel
    }

    private fun newPreferencesNode(): Preferences =
        Preferences.userRoot()
            .node("adbtool-test-${UUID.randomUUID()}")
            .also { preferenceNodes.add(it) }
}

private fun completedState(initialState: AgentTaskUiState, task: String): AgentTaskUiState =
    initialState.copy(
        phase = AgentRunPhase.COMPLETED,
        isRunning = false,
        messages = initialState.messages + AgentMessage(
            id = "assistant-${UUID.randomUUID()}",
            role = AgentMessageRole.ASSISTANT,
            text = "已完成：$task"
        )
    )

private class InMemorySecretStore : SecretStore {
        private val secrets = ConcurrentHashMap<String, String>()

        override fun write(account: String, secret: String) {
            secrets[account] = secret
        }

        override fun read(account: String): String? = secrets[account]

        override fun delete(account: String) {
            secrets.remove(account)
        }
    }
