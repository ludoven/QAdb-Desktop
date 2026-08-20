package com.ludoven.adbtool.agent

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

enum class ObservationFreshness {
    ALLOW_CACHE,
    REQUIRE_FRESH,
    WAIT_FOR_CHANGE,
    WAIT_FOR_STABLE
}

data class AgentObservationRequest(
    val includeUiHierarchy: Boolean,
    val includeScreenshot: Boolean = false,
    val freshness: ObservationFreshness = ObservationFreshness.ALLOW_CACHE,
    val baselineRevision: Long? = null,
    val timeoutMs: Long = AGENT_DEFAULT_CHANGE_TIMEOUT_MS,
    val stableForMs: Long = DEFAULT_STABLE_WINDOW_MS
)

data class AgentObservationFrame(
    val observation: AgentObservation,
    val state: DeviceState,
    val diff: PageDiff?,
    val cacheHit: Boolean
)

/**
 * Shares observations within one Agent run and serializes concurrent reads per device.
 * It deliberately keeps selector knowledge and live screen caching separate.
 */
class AgentObservationCoordinator(
    private val deviceGateway: AgentDeviceGateway,
    private val cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS,
    private val clockMs: () -> Long = System::currentTimeMillis
) {
    private val guard = Any()
    private val locks = mutableMapOf<String, Mutex>()
    private val cache = mutableMapOf<String, CachedObservation>()
    private val mutationEpochs = mutableMapOf<String, Long>()

    suspend fun observe(deviceId: String, request: AgentObservationRequest): AgentObservationFrame {
        val mutex = synchronized(guard) { locks.getOrPut(deviceId) { Mutex() } }
        return mutex.withLock {
            when (request.freshness) {
                ObservationFreshness.ALLOW_CACHE -> cachedFrame(deviceId, request) ?: capture(deviceId, request)
                ObservationFreshness.REQUIRE_FRESH -> capture(deviceId, request)
                ObservationFreshness.WAIT_FOR_CHANGE -> waitForChange(deviceId, request)
                ObservationFreshness.WAIT_FOR_STABLE -> waitForStable(deviceId, request)
            }
        }
    }

    fun markMutation(deviceId: String) = synchronized(guard) {
        mutationEpochs[deviceId] = currentEpoch(deviceId) + 1L
    }

    fun invalidate(deviceId: String) {
        synchronized(guard) {
            cache.remove(deviceId)
        }
    }

    private suspend fun waitForChange(
        deviceId: String,
        request: AgentObservationRequest
    ): AgentObservationFrame {
        val timeoutMs = request.timeoutMs.coerceIn(MIN_CHANGE_TIMEOUT_MS, MAX_CHANGE_TIMEOUT_MS)
        val startedAtNanos = System.nanoTime()
        suspend fun captureWithinDeadline(): AgentObservationFrame? {
            val elapsedMs = (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND
            val remainingMs = timeoutMs - elapsedMs
            if (remainingMs <= 0) return null
            return withTimeoutOrNull(remainingMs) {
                capture(deviceId, request.copy(freshness = ObservationFreshness.REQUIRE_FRESH))
            }
        }

        var latest = captureWithinDeadline()
            ?: throw AgentException("Device observation timed out after ${timeoutMs}ms")
        val baselineRevision = request.baselineRevision
        while (baselineRevision != null && latest.observation.revision == baselineRevision) {
            val elapsedMs = (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND
            val remainingMs = timeoutMs - elapsedMs
            if (remainingMs <= 0) break
            delay(minOf(CHANGE_POLL_INTERVAL_MS, remainingMs))
            latest = captureWithinDeadline() ?: break
        }
        return latest
    }

    private suspend fun waitForStable(
        deviceId: String,
        request: AgentObservationRequest
    ): AgentObservationFrame {
        val timeoutMs = request.timeoutMs.coerceIn(MIN_CHANGE_TIMEOUT_MS, MAX_CHANGE_TIMEOUT_MS)
        val stableForMs = request.stableForMs.coerceIn(MIN_STABLE_WINDOW_MS, MAX_STABLE_WINDOW_MS)
        val startedAtNanos = System.nanoTime()
        var latest = waitForChange(deviceId, request.copy(freshness = ObservationFreshness.WAIT_FOR_CHANGE))
        var stableRevision = latest.observation.revision
        var stableSinceNanos = System.nanoTime()

        while (true) {
            val nowNanos = System.nanoTime()
            val stableElapsedMs = (nowNanos - stableSinceNanos) / NANOS_PER_MILLISECOND
            if (stableElapsedMs >= stableForMs) return latest

            val elapsedMs = (nowNanos - startedAtNanos) / NANOS_PER_MILLISECOND
            val remainingMs = timeoutMs - elapsedMs
            if (remainingMs <= 0L) return latest
            delay(minOf(STABLE_POLL_INTERVAL_MS, stableForMs - stableElapsedMs, remainingMs))

            val captured = withTimeoutOrNull(remainingMs) {
                capture(deviceId, request.copy(freshness = ObservationFreshness.REQUIRE_FRESH))
            } ?: return latest
            latest = captured
            if (captured.observation.revision != stableRevision) {
                stableRevision = captured.observation.revision
                stableSinceNanos = System.nanoTime()
            }
        }
    }

    private fun cachedFrame(
        deviceId: String,
        request: AgentObservationRequest
    ): AgentObservationFrame? {
        val cached = synchronized(guard) { cache[deviceId] } ?: return null
        if (cached.mutationEpoch != currentEpoch(deviceId)) return null
        if (clockMs() - cached.capturedAtMs > cacheTtlMs) return null
        if (request.includeScreenshot && cached.frame.observation.screenshotPng == null) return null
        if (request.includeUiHierarchy && cached.frame.observation.uiHierarchy.isBlank()) return null
        return cached.frame.copy(cacheHit = true)
    }

    private suspend fun capture(
        deviceId: String,
        request: AgentObservationRequest
    ): AgentObservationFrame {
        val previous = synchronized(guard) { cache[deviceId]?.frame?.state }
        val observation = if (request.includeScreenshot) {
            deviceGateway.observe(deviceId)
        } else {
            deviceGateway.observeLightweight(deviceId, request.includeUiHierarchy)
        }
        val state = PageSignatureEngine.state(observation)
        val frame = AgentObservationFrame(
            observation = observation,
            state = state,
            diff = PageSignatureEngine.diff(previous, state),
            cacheHit = false
        )
        synchronized(guard) {
            cache[deviceId] = CachedObservation(
                frame = frame,
                capturedAtMs = clockMs(),
                mutationEpoch = currentEpoch(deviceId)
            )
        }
        return frame
    }

    private fun currentEpoch(deviceId: String): Long = synchronized(guard) {
        mutationEpochs[deviceId] ?: 0L
    }

    private data class CachedObservation(
        val frame: AgentObservationFrame,
        val capturedAtMs: Long,
        val mutationEpoch: Long
    )

    private companion object {
        const val DEFAULT_CACHE_TTL_MS = 750L
        const val MIN_CHANGE_TIMEOUT_MS = 100L
        const val MAX_CHANGE_TIMEOUT_MS = 15_000L
        const val CHANGE_POLL_INTERVAL_MS = 250L
        const val STABLE_POLL_INTERVAL_MS = 150L
        const val MIN_STABLE_WINDOW_MS = 100L
        const val MAX_STABLE_WINDOW_MS = 2_000L
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}

private const val AGENT_DEFAULT_CHANGE_TIMEOUT_MS = 8_000L
private const val DEFAULT_STABLE_WINDOW_MS = 450L
