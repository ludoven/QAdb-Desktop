package com.ludoven.adbtool.agent

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentObservationCoordinatorTest {
    @Test
    fun `fresh observations are reused until mutation invalidates the frame`() = runBlocking {
        val gateway = CountingObservationGateway()
        var now = 100L
        val coordinator = AgentObservationCoordinator(gateway, cacheTtlMs = 750L) { now }
        val request = AgentObservationRequest(includeUiHierarchy = true)

        val first = coordinator.observe("device", request)
        val cached = coordinator.observe("device", request)

        assertFalse(first.cacheHit)
        assertTrue(cached.cacheHit)
        assertEquals(1, gateway.lightweightCalls)

        coordinator.markMutation("device")
        now += 1
        val afterMutation = coordinator.observe("device", request)

        assertFalse(afterMutation.cacheHit)
        assertEquals(2, gateway.lightweightCalls)
    }

    @Test
    fun `concurrent observations share one device read`() = runBlocking {
        val gateway = CountingObservationGateway()
        val coordinator = AgentObservationCoordinator(gateway)

        List(8) {
            async {
                coordinator.observe(
                    "device",
                    AgentObservationRequest(includeUiHierarchy = true)
                )
            }
        }.awaitAll()

        assertEquals(1, gateway.lightweightCalls)
    }

    @Test
    fun `cache capability must satisfy screenshot request`() = runBlocking {
        val gateway = CountingObservationGateway()
        val coordinator = AgentObservationCoordinator(gateway)

        coordinator.observe("device", AgentObservationRequest(includeUiHierarchy = true))
        val screenshot = coordinator.observe(
            "device",
            AgentObservationRequest(includeUiHierarchy = true, includeScreenshot = true)
        )

        assertFalse(screenshot.cacheHit)
        assertEquals(1, gateway.lightweightCalls)
        assertEquals(1, gateway.fullCalls)
        assertTrue(screenshot.observation.screenshotPng != null)
    }

    @Test
    fun `stable observation waits through an intermediate asynchronous revision`() = runBlocking {
        val gateway = SequencedObservationGateway(2L, 3L, 3L)
        val coordinator = AgentObservationCoordinator(gateway)

        val stable = coordinator.observe(
            "device",
            AgentObservationRequest(
                includeUiHierarchy = true,
                freshness = ObservationFreshness.WAIT_FOR_STABLE,
                baselineRevision = 1L,
                timeoutMs = 1_000L,
                stableForMs = 100L
            )
        )

        assertEquals(3L, stable.observation.revision)
        assertEquals(3, gateway.calls)
    }
}

private class SequencedObservationGateway(vararg revisions: Long) : AgentDeviceGateway {
    private val revisions = ArrayDeque(revisions.toList())
    private var lastRevision = revisions.firstOrNull() ?: 1L
    var calls = 0

    override suspend fun isConnected(deviceId: String): Boolean = true

    override suspend fun observe(deviceId: String): AgentObservation = observeLightweight(deviceId, true)

    override suspend fun observeLightweight(
        deviceId: String,
        includeUiHierarchy: Boolean
    ): AgentObservation {
        calls += 1
        if (revisions.isNotEmpty()) lastRevision = revisions.removeFirst()
        return AgentObservation(
            screenshotPng = null,
            uiHierarchy = if (includeUiHierarchy) "<hierarchy revision='$lastRevision'/>" else "",
            currentActivity = "com.example/.Main",
            screenWidth = 1_080,
            screenHeight = 2_400,
            revision = lastRevision
        )
    }

    override suspend fun execute(deviceId: String, action: AgentAction): AgentToolResult =
        AgentToolResult(true, "ok")
}

private class CountingObservationGateway : AgentDeviceGateway {
    var lightweightCalls = 0
    var fullCalls = 0
    private var revision = 0L

    override suspend fun isConnected(deviceId: String): Boolean = true

    override suspend fun observe(deviceId: String): AgentObservation {
        fullCalls += 1
        revision += 1
        return observation(includeScreenshot = true)
    }

    override suspend fun observeLightweight(
        deviceId: String,
        includeUiHierarchy: Boolean
    ): AgentObservation {
        lightweightCalls += 1
        revision += 1
        return observation(includeScreenshot = false).copy(
            uiHierarchy = if (includeUiHierarchy) "<hierarchy revision='$revision'/>" else "",
            uiNodes = if (includeUiHierarchy) listOf(node()) else emptyList()
        )
    }

    override suspend fun execute(deviceId: String, action: AgentAction): AgentToolResult =
        AgentToolResult(true, "ok")

    private fun observation(includeScreenshot: Boolean) = AgentObservation(
        screenshotPng = byteArrayOf(1).takeIf { includeScreenshot },
        uiHierarchy = "<hierarchy revision='$revision'/>",
        currentActivity = "com.example/.Main",
        screenWidth = 1_080,
        screenHeight = 2_400,
        revision = revision,
        uiNodes = listOf(node())
    )

    private fun node() = UiNodeSnapshot(
        elementId = "node",
        text = "",
        contentDescription = "",
        className = "android.view.View",
        packageName = "com.example",
        bounds = UiBounds(0, 0, 100, 100),
        clickable = false,
        editable = false,
        enabled = true,
        password = false
    )
}
