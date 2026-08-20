package com.ludoven.adbtool.agent

import com.ludoven.adbtool.util.AdbTool
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

enum class DeviceStatusProbe {
    PROPERTIES,
    KERNEL,
    DISPLAY_SIZE,
    DISPLAY_DENSITY,
    FONT_SCALE,
    NETWORK,
    WIFI_STATUS,
    CPU,
    MEMORY,
    STORAGE,
    BATTERY,
    FOREGROUND_APP,
    ADB_LATENCY
}

enum class DeviceBatteryState {
    CHARGING,
    DISCHARGING,
    FULL,
    NOT_CHARGING,
    UNKNOWN
}

enum class DeviceConnectionType {
    USB,
    WIFI
}

data class DeviceIdentityStatus(
    val model: String?,
    val manufacturer: String?,
    val androidVersion: String?,
    val sdkVersion: Int?,
    val kernelVersion: String?,
    val romVersion: String?,
    val buildFingerprint: String?
)

data class DeviceDisplayStatus(
    val widthPx: Int?,
    val heightPx: Int?,
    val densityDpi: Int?,
    val fontScale: Float?
)

data class DeviceBatteryStatus(
    val levelPercent: Int?,
    val state: DeviceBatteryState,
    val temperatureCelsius: Double?
)

data class DeviceCpuStatus(val usagePercent: Double)

data class DeviceMemoryStatus(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedPercent: Double
)

data class DeviceStorageStatus(
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long,
    val usedPercent: Double
)

data class DeviceNetworkStatus(
    val connectionType: DeviceConnectionType,
    val ipAddress: String?,
    val macAddress: String?,
    val wifiEnabled: Boolean? = null,
    val wifiConnected: Boolean? = null
)

data class DeviceForegroundStatus(
    val packageName: String,
    val activityName: String?,
    val rawComponent: String
)

data class DeviceStatusProbeFailure(
    val probe: DeviceStatusProbe,
    val reason: String,
    val timedOut: Boolean = false
)

data class DeviceStatusSnapshot(
    val deviceId: String,
    val observedAtMs: Long,
    val identity: DeviceIdentityStatus?,
    val display: DeviceDisplayStatus?,
    val battery: DeviceBatteryStatus?,
    val cpu: DeviceCpuStatus?,
    val memory: DeviceMemoryStatus?,
    val storage: DeviceStorageStatus?,
    val network: DeviceNetworkStatus,
    val foreground: DeviceForegroundStatus?,
    val adbLatencyMs: Long?,
    val failures: List<DeviceStatusProbeFailure>
) {
    fun hasFreshEvidence(): Boolean = listOfNotNull(
        identity,
        display,
        battery,
        cpu,
        memory,
        storage,
        foreground,
        adbLatencyMs
    ).isNotEmpty()
}

interface AgentDeviceStatusGateway {
    suspend fun readStatus(deviceId: String, forceRefresh: Boolean = false): DeviceStatusSnapshot
}

interface DeviceStatusCommandRunner {
    suspend fun execute(deviceId: String, vararg shellArgs: String): String
}

internal sealed interface DeviceStatusProbeOutcome {
    data class Success(val outputs: List<String>, val elapsedMs: Long? = null) : DeviceStatusProbeOutcome
    data class Failure(val reason: String, val timedOut: Boolean = false) : DeviceStatusProbeOutcome
}

internal class AdbDeviceStatusCommandRunner : DeviceStatusCommandRunner {
    override suspend fun execute(deviceId: String, vararg shellArgs: String): String {
        val args = arrayOf("-s", deviceId, "shell") + shellArgs
        val result = AdbTool.execAdbWithTimeoutAsync(DEVICE_STATUS_PROBE_TIMEOUT_MS, *args)
        if (!result.success) {
            throw AgentException(result.errorMessage ?: result.output.ifBlank { "ADB status probe failed" })
        }
        return result.output
    }
}

object DeviceStatusRuntime {
    val repository: DeviceStatusRepository by lazy { DeviceStatusRepository() }
}

class DeviceStatusRepository internal constructor(
    private val commandRunner: DeviceStatusCommandRunner,
    private val clockMillis: () -> Long,
    private val sleeper: suspend (Long) -> Unit,
    private val cacheTtlMillis: Long,
    private val probeTimeoutMillis: Long,
    private val totalTimeoutMillis: Long
) {
    constructor() : this(
        commandRunner = AdbDeviceStatusCommandRunner(),
        clockMillis = System::currentTimeMillis,
        sleeper = { delay(it) },
        cacheTtlMillis = DEVICE_STATUS_CACHE_TTL_MS,
        probeTimeoutMillis = DEVICE_STATUS_PROBE_TIMEOUT_MS,
        totalTimeoutMillis = DEVICE_STATUS_TOTAL_TIMEOUT_MS
    )

    private data class CacheEntry(val snapshot: DeviceStatusSnapshot, val storedAtMs: Long)

    private val stateMutex = Mutex()
    private val cache = mutableMapOf<String, CacheEntry>()
    private val inFlight = mutableMapOf<String, CompletableDeferred<Result<DeviceStatusSnapshot>>>()

    suspend fun readStatus(deviceId: String, forceRefresh: Boolean = false): DeviceStatusSnapshot {
        val normalizedDeviceId = deviceId.trim()
        require(normalizedDeviceId.isNotEmpty()) { "Device ID is required" }

        var ownsCollection = false
        val pending = stateMutex.withLock {
            if (!forceRefresh) {
                cache[normalizedDeviceId]
                    ?.takeIf { entry -> isFresh(entry, clockMillis()) }
                    ?.let { return it.snapshot }
            }
            inFlight[normalizedDeviceId] ?: CompletableDeferred<Result<DeviceStatusSnapshot>>().also {
                inFlight[normalizedDeviceId] = it
                ownsCollection = true
            }
        }

        if (ownsCollection) {
            val result = try {
                Result.success(collectSnapshot(normalizedDeviceId))
            } catch (cancelled: CancellationException) {
                pending.cancel(cancelled)
                stateMutex.withLock { inFlight.remove(normalizedDeviceId, pending) }
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }

            stateMutex.withLock {
                result.getOrNull()?.let { snapshot ->
                    cache[normalizedDeviceId] = CacheEntry(snapshot, clockMillis())
                }
                inFlight.remove(normalizedDeviceId, pending)
            }
            pending.complete(result)
        }

        return pending.await().getOrThrow()
    }

    suspend fun invalidate(deviceId: String) {
        stateMutex.withLock { cache.remove(deviceId.trim()) }
    }

    private fun isFresh(entry: CacheEntry, nowMs: Long): Boolean {
        val age = nowMs - entry.storedAtMs
        return age in 0 until cacheTtlMillis
    }

    private suspend fun collectSnapshot(deviceId: String): DeviceStatusSnapshot = supervisorScope {
        val outcomes = ConcurrentHashMap<DeviceStatusProbe, DeviceStatusProbeOutcome>()
        val jobs = DeviceStatusProbe.entries.associateWith { probe ->
            launch {
                outcomes[probe] = if (probe == DeviceStatusProbe.CPU) {
                    collectCpuProbe(deviceId)
                } else {
                    collectProbe(deviceId, probe)
                }
            }
        }

        val completed = withTimeoutOrNull(totalTimeoutMillis) {
            jobs.values.joinAll()
            true
        } ?: false
        if (!completed) {
            jobs.forEach { (probe, job) ->
                if (!job.isCompleted) {
                    job.cancel()
                    outcomes.putIfAbsent(probe, DeviceStatusProbeOutcome.Failure("Total status deadline exceeded", true))
                }
            }
            jobs.values.joinAll()
        }

        buildSnapshot(deviceId, outcomes, clockMillis())
    }

    private suspend fun collectProbe(deviceId: String, probe: DeviceStatusProbe): DeviceStatusProbeOutcome {
        val command = PROBE_COMMANDS[probe]
            ?: return DeviceStatusProbeOutcome.Failure("No command is configured for $probe")
        val startedAt = System.nanoTime()
        return executeProbe(deviceId, command).let { outcome ->
            if (probe == DeviceStatusProbe.ADB_LATENCY && outcome is DeviceStatusProbeOutcome.Success) {
                outcome.copy(elapsedMs = nanosToMillis(System.nanoTime() - startedAt))
            } else {
                outcome
            }
        }
    }

    private suspend fun collectCpuProbe(deviceId: String): DeviceStatusProbeOutcome {
        val command = requireNotNull(PROBE_COMMANDS[DeviceStatusProbe.CPU])
        val first = executeProbe(deviceId, command)
        if (first !is DeviceStatusProbeOutcome.Success) return first
        sleeper(CPU_SAMPLE_INTERVAL_MS)
        val second = executeProbe(deviceId, command)
        if (second !is DeviceStatusProbeOutcome.Success) return second
        return DeviceStatusProbeOutcome.Success(listOf(first.outputs.single(), second.outputs.single()))
    }

    private suspend fun executeProbe(deviceId: String, command: Array<String>): DeviceStatusProbeOutcome {
        return try {
            val output = withTimeoutOrNull(probeTimeoutMillis) {
                commandRunner.execute(deviceId, *command)
            }
            if (output == null) {
                DeviceStatusProbeOutcome.Failure("Probe timed out after ${probeTimeoutMillis}ms", timedOut = true)
            } else {
                DeviceStatusProbeOutcome.Success(listOf(output))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            DeviceStatusProbeOutcome.Failure(safeFailureReason(error))
        }
    }
}

private fun buildSnapshot(
    deviceId: String,
    outcomes: Map<DeviceStatusProbe, DeviceStatusProbeOutcome>,
    observedAtMs: Long
): DeviceStatusSnapshot {
    fun output(probe: DeviceStatusProbe): String? =
        (outcomes[probe] as? DeviceStatusProbeOutcome.Success)?.outputs?.firstOrNull()

    val properties = parseProperties(output(DeviceStatusProbe.PROPERTIES).orEmpty())
    val identity = DeviceIdentityStatus(
        model = properties["ro.product.model"].cleanOrNull(),
        manufacturer = properties["ro.product.manufacturer"].cleanOrNull(),
        androidVersion = properties["ro.build.version.release"].cleanOrNull(),
        sdkVersion = properties["ro.build.version.sdk"]?.trim()?.toIntOrNull(),
        kernelVersion = output(DeviceStatusProbe.KERNEL).cleanOrNull(),
        romVersion = properties["ro.build.display.id"].cleanOrNull(),
        buildFingerprint = properties["ro.build.fingerprint"].cleanOrNull()
    ).takeIf { status ->
        listOf(
            status.model,
            status.manufacturer,
            status.androidVersion,
            status.sdkVersion,
            status.kernelVersion,
            status.romVersion,
            status.buildFingerprint
        ).any { it != null }
    }

    val display = parseDisplay(
        output(DeviceStatusProbe.DISPLAY_SIZE),
        output(DeviceStatusProbe.DISPLAY_DENSITY),
        output(DeviceStatusProbe.FONT_SCALE)
    )
    val networkOutput = output(DeviceStatusProbe.NETWORK).orEmpty()
    val wifiStatusOutput = output(DeviceStatusProbe.WIFI_STATUS).orEmpty()
    val connectionType = if (isNetworkDeviceId(deviceId)) DeviceConnectionType.WIFI else DeviceConnectionType.USB
    val network = DeviceNetworkStatus(
        connectionType = connectionType,
        ipAddress = Regex("\\binet\\s+([0-9.]+)").find(networkOutput)?.groupValues?.getOrNull(1),
        macAddress = Regex("\\blink/ether\\s+([0-9a-f:]+)", RegexOption.IGNORE_CASE)
            .find(networkOutput)?.groupValues?.getOrNull(1),
        wifiEnabled = parseWifiEnabled(wifiStatusOutput),
        wifiConnected = parseWifiConnected(wifiStatusOutput)
    )

    val failures = outcomes.entries.mapNotNull { (probe, outcome) ->
        (outcome as? DeviceStatusProbeOutcome.Failure)?.let {
            DeviceStatusProbeFailure(probe, it.reason, it.timedOut)
        }
    }.sortedBy { it.probe.ordinal }

    return DeviceStatusSnapshot(
        deviceId = deviceId,
        observedAtMs = observedAtMs,
        identity = identity,
        display = display,
        battery = parseBattery(output(DeviceStatusProbe.BATTERY)),
        cpu = parseCpu(outcomes[DeviceStatusProbe.CPU]),
        memory = parseMemory(output(DeviceStatusProbe.MEMORY)),
        storage = parseStorage(output(DeviceStatusProbe.STORAGE)),
        network = network,
        foreground = parseForeground(output(DeviceStatusProbe.FOREGROUND_APP)),
        adbLatencyMs = (outcomes[DeviceStatusProbe.ADB_LATENCY] as? DeviceStatusProbeOutcome.Success)
            ?.elapsedMs,
        failures = failures
    )
}

private fun parseProperties(output: String): Map<String, String> = buildMap {
    output.lineSequence().forEach { line ->
        PROPERTY_PATTERN.find(line)?.let { match -> put(match.groupValues[1], match.groupValues[2]) }
    }
}

private fun parseDisplay(sizeOutput: String?, densityOutput: String?, fontScaleOutput: String?): DeviceDisplayStatus? {
    val size = parseWmMetric(sizeOutput.orEmpty(), "size")
        ?.let { SCREEN_SIZE_PATTERN.matchEntire(it) }
    val density = parseWmMetric(densityOutput.orEmpty(), "density")
        ?.substringBefore(' ')
        ?.toIntOrNull()
    val fontScale = fontScaleOutput.cleanOrNull()
        ?.takeUnless { it.equals("null", ignoreCase = true) }
        ?.toFloatOrNull()
    return DeviceDisplayStatus(
        widthPx = size?.groupValues?.getOrNull(1)?.toIntOrNull(),
        heightPx = size?.groupValues?.getOrNull(2)?.toIntOrNull(),
        densityDpi = density,
        fontScale = fontScale
    ).takeIf { it.widthPx != null || it.heightPx != null || it.densityDpi != null || it.fontScale != null }
}

private fun parseBattery(output: String?): DeviceBatteryStatus? {
    val text = output.orEmpty()
    val level = Regex("(?m)^\\s*level:\\s*(\\d+)").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
    val statusCode = Regex("(?m)^\\s*status:\\s*(\\d+)").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
    val temperatureTenths = Regex("(?m)^\\s*temperature:\\s*(-?\\d+)")
        .find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (level == null && statusCode == null && temperatureTenths == null) return null
    return DeviceBatteryStatus(
        levelPercent = level?.coerceIn(0, 100),
        state = when (statusCode) {
            2 -> DeviceBatteryState.CHARGING
            3 -> DeviceBatteryState.DISCHARGING
            4 -> DeviceBatteryState.NOT_CHARGING
            5 -> DeviceBatteryState.FULL
            else -> DeviceBatteryState.UNKNOWN
        },
        temperatureCelsius = temperatureTenths?.div(10.0)
    )
}

private fun parseCpu(outcome: DeviceStatusProbeOutcome?): DeviceCpuStatus? {
    val outputs = (outcome as? DeviceStatusProbeOutcome.Success)?.outputs ?: return null
    if (outputs.size != 2) return null
    val first = parseCpuCounters(outputs[0]) ?: return null
    val second = parseCpuCounters(outputs[1]) ?: return null
    val totalDelta = second.total - first.total
    val idleDelta = second.idle - first.idle
    if (totalDelta <= 0L || idleDelta < 0L) return null
    val usage = ((totalDelta - idleDelta).coerceAtLeast(0L) * 100.0 / totalDelta).coerceIn(0.0, 100.0)
    return DeviceCpuStatus(usage)
}

private data class CpuCounters(val idle: Long, val total: Long)

private fun parseCpuCounters(output: String): CpuCounters? {
    val values = output.lineSequence()
        .firstOrNull { it.startsWith("cpu ") }
        ?.trim()
        ?.split(Regex("\\s+"))
        ?.drop(1)
        ?.mapNotNull(String::toLongOrNull)
        ?: return null
    if (values.size < 4) return null
    val idle = values[3] + values.getOrElse(4) { 0L }
    val total = values.take(8).sum()
    return CpuCounters(idle, total)
}

private fun parseMemory(output: String?): DeviceMemoryStatus? {
    val text = output.orEmpty()
    val totalKb = Regex("(?m)^MemTotal:\\s+(\\d+)").find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()
        ?: return null
    val availableKb = Regex("(?m)^MemAvailable:\\s+(\\d+)").find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()
        ?: return null
    if (totalKb <= 0) return null
    val available = availableKb.coerceIn(0, totalKb)
    return DeviceMemoryStatus(
        totalBytes = totalKb * KIBIBYTE,
        availableBytes = available * KIBIBYTE,
        usedPercent = ((totalKb - available) * 100.0 / totalKb).coerceIn(0.0, 100.0)
    )
}

private fun parseStorage(output: String?): DeviceStorageStatus? {
    val parts = output.orEmpty().lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map { it.split(Regex("\\s+")) }
        .firstOrNull { tokens -> tokens.size >= 4 && tokens[1].toLongOrNull() != null }
        ?: return null
    val totalKb = parts[1].toLongOrNull() ?: return null
    val usedKb = parts[2].toLongOrNull() ?: return null
    val availableKb = parts[3].toLongOrNull() ?: return null
    if (totalKb <= 0) return null
    return DeviceStorageStatus(
        totalBytes = totalKb * KIBIBYTE,
        usedBytes = usedKb.coerceAtLeast(0) * KIBIBYTE,
        availableBytes = availableKb.coerceAtLeast(0) * KIBIBYTE,
        usedPercent = (usedKb * 100.0 / totalKb).coerceIn(0.0, 100.0)
    )
}

private fun parseForeground(output: String?): DeviceForegroundStatus? {
    val line = output.orEmpty().lineSequence().firstOrNull {
        it.contains("mResumedActivity") || it.contains("topResumedActivity") || it.contains("ResumedActivity")
    } ?: return null
    val component = COMPONENT_PATTERN.find(line)?.value ?: return null
    val packageName = component.substringBefore('/')
    val activityName = component.substringAfter('/', "").takeIf(String::isNotBlank)
    return DeviceForegroundStatus(packageName, activityName, component)
}

private fun parseWmMetric(output: String, metric: String): String? {
    val override = Regex("Override $metric:\\s*([^\\n\\r]+)").find(output)?.groupValues?.getOrNull(1)?.trim()
    if (!override.isNullOrBlank()) return override
    return Regex("Physical $metric:\\s*([^\\n\\r]+)").find(output)?.groupValues?.getOrNull(1)?.trim()
}

private fun String?.cleanOrNull(): String? = this
    ?.lineSequence()
    ?.firstOrNull { it.isNotBlank() }
    ?.trim()
    ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

private fun parseWifiEnabled(output: String): Boolean? = when {
    WIFI_ENABLED_PATTERN.containsMatchIn(output) -> true
    WIFI_DISABLED_PATTERN.containsMatchIn(output) -> false
    else -> null
}

private fun parseWifiConnected(output: String): Boolean? = when {
    WIFI_NOT_CONNECTED_PATTERN.containsMatchIn(output) -> false
    WIFI_CONNECTED_PATTERN.containsMatchIn(output) -> true
    else -> null
}

internal fun isNetworkDeviceId(deviceId: String): Boolean =
    deviceId.contains(':') ||
        IPV4_IN_DEVICE_ID.containsMatchIn(deviceId) ||
        MDNS_WIFI_DEVICE_ID.containsMatchIn(deviceId)

private fun safeFailureReason(error: Exception): String = error.message
    ?.replace(Regex("[\\r\\n\\t]+"), " ")
    ?.trim()
    ?.take(MAX_FAILURE_REASON_LENGTH)
    ?.takeIf(String::isNotEmpty)
    ?: error::class.simpleName.orEmpty().ifBlank { "Status probe failed" }

private fun nanosToMillis(nanos: Long): Long = (nanos / 1_000_000.0).roundToInt().toLong().coerceAtLeast(0L)

private val PROPERTY_PATTERN = Regex("\\[(.*?)]\\s*:\\s*\\[(.*?)]")
private val SCREEN_SIZE_PATTERN = Regex("(\\d+)x(\\d+)")
private val COMPONENT_PATTERN = Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+/[A-Za-z0-9_.$]+")
private val IPV4_IN_DEVICE_ID = Regex("(?:\\d{1,3}\\.){3}\\d{1,3}")
private val MDNS_WIFI_DEVICE_ID = Regex("\\._adb(?:-tls-connect)?\\._tcp$", RegexOption.IGNORE_CASE)
private val WIFI_ENABLED_PATTERN = Regex("(?im)^\\s*Wi-?Fi is enabled\\s*$")
private val WIFI_DISABLED_PATTERN = Regex("(?im)^\\s*Wi-?Fi is disabled\\s*$")
private val WIFI_CONNECTED_PATTERN = Regex("(?im)^\\s*Wi-?Fi is connected(?:\\s+to)?(?:\\s|$)")
private val WIFI_NOT_CONNECTED_PATTERN = Regex("(?im)^\\s*Wi-?Fi is not connected(?:\\s|$)")
private const val KIBIBYTE = 1_024L
private const val CPU_SAMPLE_INTERVAL_MS = 150L
private const val MAX_FAILURE_REASON_LENGTH = 160
internal const val DEVICE_STATUS_CACHE_TTL_MS = 3_000L
internal const val DEVICE_STATUS_PROBE_TIMEOUT_MS = 3_000L
internal const val DEVICE_STATUS_TOTAL_TIMEOUT_MS = 5_000L

private val PROBE_COMMANDS = mapOf(
    DeviceStatusProbe.PROPERTIES to arrayOf("getprop"),
    DeviceStatusProbe.KERNEL to arrayOf("uname", "-r"),
    DeviceStatusProbe.DISPLAY_SIZE to arrayOf("wm", "size"),
    DeviceStatusProbe.DISPLAY_DENSITY to arrayOf("wm", "density"),
    DeviceStatusProbe.FONT_SCALE to arrayOf("settings", "get", "system", "font_scale"),
    DeviceStatusProbe.NETWORK to arrayOf("ip", "addr", "show", "wlan0"),
    DeviceStatusProbe.WIFI_STATUS to arrayOf("cmd", "wifi", "status"),
    DeviceStatusProbe.CPU to arrayOf("cat", "/proc/stat"),
    DeviceStatusProbe.MEMORY to arrayOf("cat", "/proc/meminfo"),
    DeviceStatusProbe.STORAGE to arrayOf("df", "-k", "/data"),
    DeviceStatusProbe.BATTERY to arrayOf("dumpsys", "battery"),
    DeviceStatusProbe.FOREGROUND_APP to arrayOf("dumpsys", "activity", "activities"),
    DeviceStatusProbe.ADB_LATENCY to arrayOf("echo", "ping")
)
