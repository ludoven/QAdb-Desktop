package com.ludoven.adbtool.agent

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentOrchestratorTest {
    @Test
    fun `safe device actions execute and finish`() = runBlocking {
        val model = QueueModelClient(
            AgentAction.Tap(12, 20),
            AgentAction.Swipe(10, 10, 20, 30, 200),
            AgentAction.InputText("hello"),
            AgentAction.Finish("done")
        )
        val gateway = FakeDeviceGateway()
        val result = orchestrator(model, gateway).runTask()

        assertEquals(3, gateway.executed.size)
        assertTrue(result.steps.all { it.status == AgentStepStatus.COMPLETED })
        assertEquals("done", result.messages.last().text)
    }

    @Test
    fun `ordered navigation verifies settings before success finish`() = runBlocking {
        val gateway = FakeDeviceGateway(currentActivity = "com.android.launcher/.Launcher")
        val result = orchestrator(
            QueueModelClient(
                AgentAction.KeyEvent(AgentKey.HOME),
                AgentAction.FindApp("settings"),
                AgentAction.LaunchPackage("com.android.settings"),
                AgentAction.Finish("Settings is open")
            ),
            gateway
        ).runTask()

        assertEquals(3, gateway.executed.size)
        assertTrue(result.steps.all { it.status == AgentStepStatus.COMPLETED })
        assertEquals(AgentRunPhase.COMPLETED, result.phase)
        assertEquals("com.android.settings/.Settings", gateway.currentActivity)
    }

    @Test
    fun `repeated navigation is blocked once then stops`() = runBlocking {
        val gateway = FakeDeviceGateway(
            currentActivity = "com.android.launcher/.Launcher",
            navigationStateStable = true
        )
        val result = orchestrator(
            QueueModelClient(
                AgentAction.LaunchPackage("com.android.settings"),
                AgentAction.KeyEvent(AgentKey.HOME),
                AgentAction.LaunchPackage("com.android.settings"),
                AgentAction.LaunchPackage("com.android.settings")
            ),
            gateway
        ).runTask()

        assertEquals(2, gateway.executed.size)
        assertEquals(AgentStepStatus.FAILED, result.steps.last().status)
        assertContains(result.errorMessage.orEmpty(), "navigation loop")
    }

    @Test
    fun `launch verification distinguishes mismatched and unavailable activities`() = runBlocking {
        val mismatched = orchestrator(
            QueueModelClient(
                AgentAction.LaunchPackage("com.android.settings"),
                AgentAction.Finish("done")
            ),
            FakeDeviceGateway(launchActivity = "com.example.other/.MainActivity")
        ).runTask()
        assertEquals(AgentStepStatus.FAILED, mismatched.steps.single().status)

        val unavailable = orchestrator(
            QueueModelClient(
                AgentAction.LaunchPackage("com.android.settings"),
                AgentAction.Finish("done")
            ),
            FakeDeviceGateway(activityUnavailable = true)
        ).runTask()
        assertEquals(AgentStepStatus.UNVERIFIED, unavailable.steps.single().status)
    }

    @Test
    fun `finish requires latest observation and blocked finish fails task`() = runBlocking {
        val stale = orchestrator(
            QueueModelClient(AgentAction.Finish("done", observationId = "stale")),
            FakeDeviceGateway()
        ).runTask()
        assertContains(stale.errorMessage.orEmpty(), "latest observation")

        val blocked = orchestrator(
            QueueModelClient(AgentAction.Finish("Could not open settings", AgentFinishOutcome.BLOCKED)),
            FakeDeviceGateway()
        ).runTask()
        assertEquals(AgentRunPhase.FAILED, blocked.phase)
        assertContains(blocked.errorMessage.orEmpty(), "Task blocked")
    }

    @Test
    fun `directly selected settings template completes without any model request`() = runBlocking {
        val model = PlanningModelClient()
        val gateway = FakeDeviceGateway(currentActivity = "com.android.launcher/.Launcher")
        val result = orchestrator(model, gateway).run(
            task = "返回桌面并打开系统设置",
            deviceId = "device-1",
            config = AiModelConfig(model = "test"),
            apiKey = "secret",
            taskSource = AgentTaskSource.DIRECT_TEMPLATE,
            onState = {},
            confirmSensitiveAction = { true }
        )

        assertEquals(0, model.planCalls)
        assertEquals(0, model.nextActionCalls)
        assertEquals(AgentExecutionStrategy.FAST_TEMPLATE, result.executionStrategy)
        assertEquals(AgentRunPhase.COMPLETED, result.phase)
        assertEquals(2, gateway.executed.size)
        assertTrue(gateway.lightweightObservations > 0)
    }

    @Test
    fun `natural language always asks AI for a plan instead of matching a template`() = runBlocking {
        val model = PlanningModelClient(
            plan = AgentTaskPlan(
                mode = AgentPlanMode.INTERACTIVE,
                summary = "Continue in the UI"
            )
        )
        val gateway = FakeDeviceGateway(currentActivity = "com.android.launcher/.Launcher")
        val result = orchestrator(model, gateway).run(
            task = "返回桌面打开设置后再返回桌面再打开 chrome 搜索 今日新闻",
            deviceId = "device-1",
            config = AiModelConfig(model = "test"),
            apiKey = "secret",
            onState = {},
            confirmSensitiveAction = { true }
        )

        assertEquals(1, model.planCalls)
        assertEquals(1, model.nextActionCalls)
        assertEquals(AgentExecutionStrategy.INTERACTIVE, result.executionStrategy)
        assertTrue(gateway.executed.isEmpty())
    }

    @Test
    fun `batch plan resolves apps locally and repair stays bounded`() = runBlocking {
        val initialPlan = AgentTaskPlan(
            mode = AgentPlanMode.BATCH,
            summary = "Open settings",
            steps = listOf(
                AgentPlanStep("find", AgentPlanAction.FindApp("settings")),
                AgentPlanStep(
                    "launch",
                    AgentPlanAction.LaunchResolvedApp("find"),
                    AgentVerification.ForegroundPackage(sourceStepId = "find")
                )
            )
        )
        val batchModel = PlanningModelClient(plan = initialPlan)
        val batchResult = orchestrator(batchModel, FakeDeviceGateway()).runTask()
        assertEquals(1, batchModel.planCalls)
        assertEquals(0, batchModel.nextActionCalls)
        assertEquals(AgentExecutionStrategy.BATCH_PLAN, batchResult.executionStrategy)
        assertEquals(AgentRunPhase.COMPLETED, batchResult.phase)

        val repairPlan = AgentTaskPlan(
            mode = AgentPlanMode.BATCH,
            summary = "Recovered by opening settings",
            steps = initialPlan.steps
        )
        val repairModel = PlanningModelClient(
            plan = AgentTaskPlan(
                mode = AgentPlanMode.BATCH,
                summary = "Try HOME",
                steps = listOf(
                    AgentPlanStep("home", AgentPlanAction.KeyEvent(AgentKey.HOME), AgentVerification.ActivityChanged)
                )
            ),
            repair = repairPlan
        )
        val repaired = orchestrator(
            repairModel,
            FakeDeviceGateway(currentActivity = "com.android.launcher/.Launcher")
        ).runTask()
        assertEquals(1, repairModel.repairCalls)
        assertEquals(0, repairModel.nextActionCalls)
        assertEquals(AgentExecutionStrategy.REPAIR_PLAN, repaired.executionStrategy)
        assertEquals(AgentRunPhase.COMPLETED, repaired.phase)
    }

    @Test
    fun `sensitive action pauses and approval executes`() = runBlocking {
        val gateway = FakeDeviceGateway()
        var confirmations = 0
        val result = orchestrator(
            QueueModelClient(
                AgentAction.ClearAppData("com.example.app"),
                AgentAction.Finish("cleared")
            ),
            gateway
        ).runTask(confirm = {
            confirmations += 1
            true
        })

        assertEquals(1, confirmations)
        assertEquals(1, gateway.executed.size)
        assertTrue(result.pendingConfirmation == null)
    }

    @Test
    fun `rejected action is returned to model and planning continues`() = runBlocking {
        val model = QueueModelClient(
            AgentAction.UninstallPackage("com.example.app"),
            AgentAction.Finish("kept app")
        )
        val gateway = FakeDeviceGateway()
        val result = orchestrator(model, gateway).runTask(confirm = { false })

        assertTrue(gateway.executed.isEmpty())
        assertEquals(AgentStepStatus.DENIED, result.steps.single().status)
        assertContains(model.contexts.last().completedSteps.single().result, "denied", ignoreCase = true)
        assertEquals("kept app", result.messages.last().text)
    }

    @Test
    fun `auto vision mode retries once with text observation`() = runBlocking {
        val model = VisionFallbackModelClient()
        val gateway = FakeDeviceGateway()
        var latest = AgentTaskUiState()
        val result = orchestrator(model, gateway).run(
            task = "inspect",
            deviceId = "device-1",
            config = AiModelConfig(model = "test", visionMode = VisionMode.AUTO),
            apiKey = "secret",
            onState = { latest = it },
            confirmSensitiveAction = { true }
        )

        assertEquals(listOf(true, false), model.includeScreenshot)
        assertEquals(AgentObservationMode.TEXT_ONLY, result.observationMode)
        assertEquals(result, latest)
    }

    @Test
    fun `invalid action device disconnect and max actions stop clearly`() = runBlocking {
        val invalid = orchestrator(
            QueueModelClient(AgentAction.Tap(5_000, 1)),
            FakeDeviceGateway()
        ).runTask()
        assertContains(invalid.errorMessage.orEmpty(), "outside")

        val disconnected = orchestrator(
            QueueModelClient(AgentAction.Finish("never")),
            FakeDeviceGateway(connected = false)
        ).runTask()
        assertContains(disconnected.errorMessage.orEmpty(), "no longer connected")

        val limited = orchestrator(
            model = QueueModelClient(AgentAction.Observe, AgentAction.Observe),
            gateway = FakeDeviceGateway(),
            maxActions = 2
        ).runTask()
        assertContains(limited.errorMessage.orEmpty(), "2-action")
    }

    @Test
    fun `cancelling task publishes stopped state`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val model = object : AgentModelClient {
            override suspend fun nextAction(
                config: AiModelConfig,
                apiKey: String,
                context: AgentModelContext,
                includeScreenshot: Boolean
            ): AgentModelDecision {
                started.complete(Unit)
                delay(Long.MAX_VALUE)
                error("unreachable")
            }

            override suspend fun testConnection(config: AiModelConfig, apiKey: String) = Unit
        }
        var latest = AgentTaskUiState()
        val job = launch {
            orchestrator(model, FakeDeviceGateway()).run(
                task = "wait",
                deviceId = "device-1",
                config = AiModelConfig(model = "test"),
                apiKey = "secret",
                onState = { latest = it },
                confirmSensitiveAction = { true }
            )
        }
        started.await()
        job.cancelAndJoin()
        yield()

        assertFalse(latest.isRunning)
        assertContains(latest.messages.last().text, "cancelled", ignoreCase = true)
    }

    @Test
    fun `send semantics require confirmation and unchanged UI is reported`() = runBlocking {
        val action = AgentAction.Tap(
            x = 12,
            y = 20,
            meta = AgentActionMeta(
                intent = "send the message",
                target = "send button",
                operationKind = AgentOperationKind.SEND
            )
        )
        var confirmationReason = ""
        val result = orchestrator(
            QueueModelClient(action, AgentAction.Finish("done")),
            FakeDeviceGateway(changesAfterExecution = false)
        )
            .runTask(confirm = {
                confirmationReason = it.confirmationReason
                true
            })

        assertContains(confirmationReason, "send", ignoreCase = true)
        assertEquals(AgentStepStatus.UNVERIFIED, result.steps.single().status)
        assertContains(result.steps.single().result, "No visible device change")
    }

    @Test
    fun `oversized deterministic context uses bounded model compaction and counts usage`() = runBlocking {
        val model = CompactingModelClient()
        val gateway = object : AgentDeviceGateway {
            override suspend fun isConnected(deviceId: String): Boolean = true

            override suspend fun observe(deviceId: String): AgentObservation = AgentObservation(
                screenshotPng = null,
                uiHierarchy = "界".repeat(7_000),
                currentActivity = "com.example/.MainActivity",
                screenWidth = 1_080,
                screenHeight = 2_400
            )

            override suspend fun execute(
                deviceId: String,
                action: AgentAction
            ): AgentToolResult = AgentToolResult(true, "错".repeat(2_000))
        }

        val result = orchestrator(model, gateway).runTask()

        assertEquals(1, model.compactionCalls)
        assertEquals(2, result.compactionCount)
        assertEquals(7, result.usage.totalTokens)
    }
}

private class QueueModelClient(vararg actions: AgentAction) : AgentModelClient {
    private val queue = ArrayDeque(actions.toList())
    val contexts = mutableListOf<AgentModelContext>()

    override suspend fun nextAction(
        config: AiModelConfig,
        apiKey: String,
        context: AgentModelContext,
        includeScreenshot: Boolean
    ): AgentModelDecision {
        contexts += context
        val action = queue.removeFirst().let { planned ->
            if (planned is AgentAction.Finish && planned.observationId.isBlank()) {
                planned.copy(observationId = context.observation.observationId)
            } else {
                planned
            }
        }
        return AgentModelDecision(action, includeScreenshot)
    }

    override suspend fun testConnection(config: AiModelConfig, apiKey: String) = Unit
}

private class VisionFallbackModelClient : AgentModelClient {
    val includeScreenshot = mutableListOf<Boolean>()

    override suspend fun nextAction(
        config: AiModelConfig,
        apiKey: String,
        context: AgentModelContext,
        includeScreenshot: Boolean
    ): AgentModelDecision {
        this.includeScreenshot += includeScreenshot
        if (includeScreenshot) throw UnsupportedVisionException("image unsupported")
        return AgentModelDecision(
            AgentAction.Finish("text mode", observationId = context.observation.observationId),
            false
        )
    }

    override suspend fun testConnection(config: AiModelConfig, apiKey: String) = Unit
}

private class PlanningModelClient(
    private val plan: AgentTaskPlan? = null,
    private val repair: AgentTaskPlan? = null
) : AgentModelClient {
    var planCalls = 0
    var repairCalls = 0
    var nextActionCalls = 0

    override suspend fun nextAction(
        config: AiModelConfig,
        apiKey: String,
        context: AgentModelContext,
        includeScreenshot: Boolean
    ): AgentModelDecision {
        nextActionCalls += 1
        return AgentModelDecision(
            AgentAction.Finish("fallback", observationId = context.observation.observationId),
            includeScreenshot
        )
    }

    override suspend fun testConnection(config: AiModelConfig, apiKey: String) = Unit

    override suspend fun planTask(
        config: AiModelConfig,
        apiKey: String,
        task: String,
        observation: AgentObservation
    ): AgentPlanDecision? {
        planCalls += 1
        return plan?.let { AgentPlanDecision(it) }
    }

    override suspend fun repairPlan(
        config: AiModelConfig,
        apiKey: String,
        task: String,
        observation: AgentObservation,
        completedSteps: List<AgentStep>,
        failure: String
    ): AgentPlanDecision? {
        repairCalls += 1
        return repair?.let { AgentPlanDecision(it) }
    }
}

private class CompactingModelClient : AgentModelClient {
    var compactionCalls = 0
    private var decisionCalls = 0

    override suspend fun nextAction(
        config: AiModelConfig,
        apiKey: String,
        context: AgentModelContext,
        includeScreenshot: Boolean
    ): AgentModelDecision = AgentModelDecision(
        action = if (decisionCalls++ < 7) {
            AgentAction.Tap(10 + decisionCalls, 20)
        } else {
            AgentAction.Finish("done", observationId = context.observation.observationId)
        },
        usedVision = false
    )

    override suspend fun testConnection(config: AiModelConfig, apiKey: String) = Unit

    override suspend fun compactContext(
        config: AiModelConfig,
        apiKey: String,
        context: AgentModelContext
    ): AgentCompactionResult {
        compactionCalls += 1
        return AgentCompactionResult(
            summary = "activity=com.example/.MainActivity; unresolved=open target",
            usage = AgentUsage(promptTokens = 5, completionTokens = 2, totalTokens = 7)
        )
    }
}

private class FakeDeviceGateway(
    var connected: Boolean = true,
    var currentActivity: String = "com.example/.MainActivity",
    private val changesAfterExecution: Boolean = true,
    private val launchActivity: String? = "com.android.settings/.Settings",
    private val activityUnavailable: Boolean = false,
    private val navigationStateStable: Boolean = false
) : AgentDeviceGateway {
    val executed = mutableListOf<AgentAction>()
    var lightweightObservations = 0
    private var observationVersion = 0

    override suspend fun isConnected(deviceId: String): Boolean = connected

    override suspend fun observe(deviceId: String): AgentObservation = AgentObservation(
        screenshotPng = byteArrayOf(1, 2, 3),
        uiHierarchy = if (navigationStateStable) {
            "<hierarchy activity='$currentActivity'/>"
        } else {
            "<hierarchy version='$observationVersion'/>"
        },
        currentActivity = if (activityUnavailable) "" else currentActivity,
        screenWidth = 1080,
        screenHeight = 2400
    )

    override suspend fun observeLightweight(deviceId: String, includeUiHierarchy: Boolean): AgentObservation {
        lightweightObservations += 1
        return observe(deviceId).copy(screenshotPng = null)
    }

    override suspend fun execute(deviceId: String, action: AgentAction): AgentToolResult {
        executed += action
        if (changesAfterExecution) observationVersion += 1
        when (action) {
            is AgentAction.LaunchPackage -> currentActivity = launchActivity ?: currentActivity
            is AgentAction.KeyEvent -> if (action.key == AgentKey.HOME) {
                currentActivity = "com.android.launcher/.Launcher"
            }
            else -> Unit
        }
        return if (action is AgentAction.FindApp) {
            AgentToolResult(true, "label=Settings package=com.android.settings", resolvedPackages = listOf("com.android.settings"))
        } else {
            AgentToolResult(true, "ok")
        }
    }
}

private fun orchestrator(
    model: AgentModelClient,
    gateway: AgentDeviceGateway,
    maxActions: Int = 20
): AgentOrchestrator {
    val node = Preferences.userRoot().node("/qadb-tests/orchestrator/${UUID.randomUUID()}")
    val memoryPreferences = AgentMemoryPreferences(node).also { it.declineConsent() }
    return AgentOrchestrator(
        modelClient = model,
        deviceGateway = gateway,
        maxActions = maxActions,
        memoryPreferences = memoryPreferences
    )
}

private suspend fun AgentOrchestrator.runTask(
    confirm: suspend (AgentStep) -> Boolean = { true }
): AgentTaskUiState = run(
    task = "test task",
    deviceId = "device-1",
    config = AiModelConfig(model = "test"),
    apiKey = "secret",
    onState = {},
    confirmSensitiveAction = confirm
)
