package com.ludoven.adbtool.viewmodel

import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.util.AdbTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class CpuProcess(val name: String, val pid: String, val cpuPercent: Float)
data class MemProcess(val name: String, val pid: String, val memMb: Long)
data class StoragePartition(val mount: String, val totalGb: Double, val usedGb: Double, val availGb: Double)
data class StorageApp(val name: String, val sizeMb: Long)
data class PrimaryStorageSummary(
    val mount: String,
    val totalGb: Double,
    val usedGb: Double,
    val availGb: Double,
    val usedPercent: Float
)

internal fun parsePackageDuSizeMb(output: String): Long? {
    val sizeKb = output.lineSequence()
        .mapNotNull { line -> line.trim().split(Regex("\\s+")).firstOrNull()?.toLongOrNull() }
        .firstOrNull { it > 0L }
        ?: return null
    return sizeKb / 1024
}

internal fun calculateMemUsedMb(totalMb: Long, availableMb: Long): Long {
    return (totalMb - availableMb).coerceAtLeast(0L)
}

internal fun calculateMemUsedPercent(totalMb: Long, availableMb: Long): Float {
    if (totalMb <= 0L) return 0f
    return calculateMemUsedMb(totalMb, availableMb).toFloat() / totalMb * 100f
}

internal fun selectPrimaryStorageSummary(partitions: List<StoragePartition>): PrimaryStorageSummary? {
    val primary = partitions.firstOrNull { it.mount == "/data" }
        ?: partitions.firstOrNull { partition ->
            partition.mount.contains("sdcard", ignoreCase = true) ||
                partition.mount.contains("emulated", ignoreCase = true) ||
                partition.mount.contains("self", ignoreCase = true) ||
                partition.mount.contains("/storage", ignoreCase = true)
        }
        ?: partitions.maxByOrNull { it.usedGb }

    return primary?.let {
        PrimaryStorageSummary(
            mount = it.mount,
            totalGb = it.totalGb,
            usedGb = it.usedGb,
            availGb = it.availGb,
            usedPercent = if (it.totalGb > 0) ((it.usedGb / it.totalGb) * 100.0).toFloat().coerceIn(0f, 100f) else 0f
        )
    }
}

internal fun appendStorageHistory(
    history: List<Float>,
    summary: PrimaryStorageSummary?,
    maxHistory: Int
): List<Float> {
    if (summary == null) return history
    return (history + summary.usedPercent).takeLast(maxHistory)
}

internal fun resourceRefreshShouldCancelForDeviceChange(activeRefreshDeviceId: String?, nextDeviceId: String?): Boolean {
    val active = activeRefreshDeviceId?.trim().orEmpty()
    if (active.isEmpty()) return false
    return active != nextDeviceId?.trim().orEmpty()
}

class ResourceViewModel : BaseViewModel() {

    companion object {
        private const val MAX_HISTORY = 60
        private const val MAX_PROCESSES = 15

        internal fun resourceDeviceActionsEnabled(deviceId: String?): Boolean =
            !deviceId.isNullOrBlank()

        internal fun normalizedResourceDeviceId(deviceId: String?): String? =
            deviceId?.trim()?.takeIf { resourceDeviceActionsEnabled(it) }
    }

    private val _cpuHistory = MutableStateFlow<List<Float>>(emptyList())
    val cpuHistory: StateFlow<List<Float>> = _cpuHistory.asStateFlow()

    private val _currentCpuPercent = MutableStateFlow(0f)
    val currentCpuPercent: StateFlow<Float> = _currentCpuPercent.asStateFlow()

    private val _cpuProcesses = MutableStateFlow<List<CpuProcess>>(emptyList())
    val cpuProcesses: StateFlow<List<CpuProcess>> = _cpuProcesses.asStateFlow()

    private val _memHistory = MutableStateFlow<List<Float>>(emptyList())
    val memHistory: StateFlow<List<Float>> = _memHistory.asStateFlow()

    private val _memTotalMb = MutableStateFlow(0L)
    val memTotalMb: StateFlow<Long> = _memTotalMb.asStateFlow()

    private val _memAvailableMb = MutableStateFlow(0L)
    val memAvailableMb: StateFlow<Long> = _memAvailableMb.asStateFlow()

    private val _memUsedMb = MutableStateFlow(0L)
    val memUsedMb: StateFlow<Long> = _memUsedMb.asStateFlow()

    private val _memUsedPercent = MutableStateFlow(0f)
    val memUsedPercent: StateFlow<Float> = _memUsedPercent.asStateFlow()

    private val _memProcesses = MutableStateFlow<List<MemProcess>>(emptyList())
    val memProcesses: StateFlow<List<MemProcess>> = _memProcesses.asStateFlow()

    private val _storagePartitions = MutableStateFlow<List<StoragePartition>>(emptyList())
    val storagePartitions: StateFlow<List<StoragePartition>> = _storagePartitions.asStateFlow()

    private val _storageApps = MutableStateFlow<List<StorageApp>>(emptyList())
    val storageApps: StateFlow<List<StorageApp>> = _storageApps.asStateFlow()

    private val _primaryStorageSummary = MutableStateFlow<PrimaryStorageSummary?>(null)
    val primaryStorageSummary: StateFlow<PrimaryStorageSummary?> = _primaryStorageSummary.asStateFlow()

    private val _storageHistory = MutableStateFlow<List<Float>>(emptyList())
    val storageHistory: StateFlow<List<Float>> = _storageHistory.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _lastUpdatedAtMillis = MutableStateFlow<Long?>(null)
    val lastUpdatedAtMillis: StateFlow<Long?> = _lastUpdatedAtMillis.asStateFlow()

    private var monitoringJob: Job? = null
    private var refreshOnceJob: Job? = null
    private var refreshOnceDeviceId: String? = null
    private var currentDeviceId: String? = null

    fun setDevice(deviceId: String?) {
        val normalizedDeviceId = normalizedResourceDeviceId(deviceId)
        val changed = currentDeviceId != normalizedDeviceId
        if (resourceRefreshShouldCancelForDeviceChange(refreshOnceDeviceId, normalizedDeviceId)) {
            refreshOnceJob?.cancel()
            refreshOnceJob = null
            refreshOnceDeviceId = null
        }
        currentDeviceId = normalizedDeviceId
        if (changed) {
            stopMonitoring()
            clearResourceState()
        }
    }

    fun startMonitoring(deviceId: String) {
        val normalizedDeviceId = normalizedResourceDeviceId(deviceId)
        if (normalizedDeviceId == null) {
            stopMonitoring()
            clearResourceState()
            currentDeviceId = null
            return
        }
        stopMonitoring()
        currentDeviceId = normalizedDeviceId
        _isMonitoring.value = true

        monitoringJob = viewModelScope.launch {
            var storageTick = 0
            while (isActive) {
                try {
                    coroutineScope {
                        val cpuJob = async(Dispatchers.IO) { refreshCpuData(normalizedDeviceId) }
                        val memJob = async(Dispatchers.IO) { refreshMemData(normalizedDeviceId) }
                        cpuJob.await()
                        memJob.await()
                    }
                    storageTick++
                    if (storageTick >= 5) {
                        storageTick = 0
                        withContext(Dispatchers.IO) {
                            refreshStorageData(normalizedDeviceId)
                        }
                    }
                    _lastUpdatedAtMillis.value = System.currentTimeMillis()
                } catch (e: Exception) {
                    if (!isActive) break
                    _isMonitoring.value = false
                    break
                }
                delay(1000)
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        refreshOnceJob?.cancel()
        refreshOnceJob = null
        refreshOnceDeviceId = null
        _isMonitoring.value = false
    }

    fun refreshOnce(deviceId: String) {
        val normalizedDeviceId = normalizedResourceDeviceId(deviceId)
        if (normalizedDeviceId == null) {
            stopMonitoring()
            clearResourceState()
            currentDeviceId = null
            return
        }
        refreshOnceJob?.cancel()
        refreshOnceDeviceId = normalizedDeviceId
        refreshOnceJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    refreshCpuData(normalizedDeviceId)
                    refreshMemData(normalizedDeviceId)
                    refreshStorageData(normalizedDeviceId)
                }
                _lastUpdatedAtMillis.value = System.currentTimeMillis()
            } catch (_: Exception) {
                // Silently ignore one-shot refresh errors
            }
        }
    }

    private fun clearResourceState() {
        _cpuHistory.value = emptyList()
        _currentCpuPercent.value = 0f
        _cpuProcesses.value = emptyList()
        _memHistory.value = emptyList()
        _memTotalMb.value = 0L
        _memAvailableMb.value = 0L
        _memUsedMb.value = 0L
        _memUsedPercent.value = 0f
        _memProcesses.value = emptyList()
        _storagePartitions.value = emptyList()
        _storageApps.value = emptyList()
        _primaryStorageSummary.value = null
        _storageHistory.value = emptyList()
        _lastUpdatedAtMillis.value = null
    }

    private suspend fun refreshCpuData(deviceId: String) {
        val (statResult, topResult) = coroutineScope {
            val stat = async { AdbTool.execAdbOutputAsync("-s", deviceId, "shell", "cat", "/proc/stat") }
            val top = async {
                AdbTool.execAdbOutputAsync(
                    "-s", deviceId, "shell", "top", "-n", "1", "-b", "-q"
                )
            }
            stat.await() to top.await()
        }

        val cpuPercent = parseOverallCpu(statResult)
        if (cpuPercent >= 0f) {
            _currentCpuPercent.value = cpuPercent
            _cpuHistory.value = (_cpuHistory.value + cpuPercent).takeLast(MAX_HISTORY)
        }

        _cpuProcesses.value = parseCpuProcesses(topResult)
    }

    private fun parseOverallCpu(output: String): Float {
        val cpuLine = output.lines().firstOrNull { it.startsWith("cpu ") } ?: return -1f
        val parts = cpuLine.trim().split(Regex("\\s+"))
        if (parts.size < 5) return -1f

        val values = parts.drop(1).mapNotNull { it.toLongOrNull() }
        if (values.size < 4) return -1f

        val idle = values[3] + if (values.size > 4) values[4] else 0L
        val total = values.sum()
        return if (total > 0) ((total - idle).toFloat() / total * 100f) else 0f
    }

    private fun parseCpuProcesses(output: String): List<CpuProcess> {
        val lines = output.lines().filter { it.isNotBlank() }
        val headerIdx = lines.indexOfFirst {
            val upper = it.uppercase()
            upper.contains("PID") && (upper.contains("CPU") || upper.contains("%CPU"))
        }
        if (headerIdx < 0) return emptyList()

        val header = lines[headerIdx].split(Regex("\\s+"))
        val pidIdx = header.indexOfFirst { it.equals("PID", ignoreCase = true) }
        val cpuIdx = header.indexOfFirst {
            it.equals("%CPU", ignoreCase = true) || it.equals("CPU%", ignoreCase = true) ||
                it.equals("CPU", ignoreCase = true)
        }
        val nameIdx = header.indexOfFirst {
            it.equals("NAME", ignoreCase = true) || it.equals("CMD", ignoreCase = true) ||
                it.equals("COMMAND", ignoreCase = true)
        }
        if (pidIdx < 0 || cpuIdx < 0 || nameIdx < 0) return emptyList()

        return lines.drop(headerIdx + 1)
            .mapNotNull { line ->
                val tokens = line.split(Regex("\\s+"))
                if (tokens.size <= maxOf(pidIdx, cpuIdx, nameIdx)) return@mapNotNull null
                val cpu = tokens[cpuIdx].removeSuffix("%").toFloatOrNull() ?: 0f
                CpuProcess(
                    name = tokens.drop(nameIdx).joinToString(" ").ifBlank { "-" },
                    pid = tokens[pidIdx],
                    cpuPercent = cpu
                )
            }
            .filter { it.cpuPercent > 0f }
            .sortedByDescending { it.cpuPercent }
            .take(MAX_PROCESSES)
    }

    private suspend fun refreshMemData(deviceId: String) {
        val (meminfoResult, psResult) = coroutineScope {
            val meminfo = async { AdbTool.execAdbOutputAsync("-s", deviceId, "shell", "cat", "/proc/meminfo") }
            val ps = async {
                AdbTool.execAdbOutputAsync("-s", deviceId, "shell", "ps", "-eo", "pid,user,rss,comm")
            }
            meminfo.await() to ps.await()
        }

        parseMemInfo(meminfoResult)
        _memProcesses.value = parseMemProcesses(psResult)
    }

    private fun parseMemInfo(output: String) {
        val map = mutableMapOf<String, Long>()
        output.lines().forEach { line ->
            val parts = line.split(":")
            if (parts.size == 2) {
                val key = parts[0].trim()
                val valueParts = parts[1].trim().split(Regex("\\s+"))
                val kb = valueParts.firstOrNull()?.toLongOrNull()
                if (kb != null) {
                    map[key] = kb / 1024
                }
            }
        }
        val total = map["MemTotal"] ?: 0L
        val available = map["MemAvailable"] ?: 0L
        val usedMb = calculateMemUsedMb(total, available)
        val usedPercent = calculateMemUsedPercent(total, available)

        _memTotalMb.value = total
        _memAvailableMb.value = available
        _memUsedMb.value = usedMb
        _memUsedPercent.value = usedPercent

        if (total > 0) {
            _memHistory.value = (_memHistory.value + usedPercent).takeLast(MAX_HISTORY)
        }
    }

    private fun parseMemProcesses(output: String): List<MemProcess> {
        val lines = output.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()

        val header = lines.first().split(Regex("\\s+"))
        val pidIdx = header.indexOfFirst { it.equals("PID", ignoreCase = true) }.takeIf { it >= 0 } ?: 0
        val rssIdx = header.indexOfFirst { it.equals("RSS", ignoreCase = true) }.takeIf { it >= 0 } ?: 2
        val commIdx = header.indexOfFirst {
            it.equals("COMM", ignoreCase = true) || it.equals("CMD", ignoreCase = true) ||
                it.equals("NAME", ignoreCase = true) || it.equals("COMMAND", ignoreCase = true)
        }.takeIf { it >= 0 } ?: (header.size - 1)

        return lines.drop(1)
            .mapNotNull { line ->
                val tokens = line.split(Regex("\\s+"))
                if (tokens.size <= maxOf(pidIdx, rssIdx, commIdx)) return@mapNotNull null
                val rssKb = tokens[rssIdx].toLongOrNull() ?: 0L
                MemProcess(
                    name = tokens.drop(commIdx).joinToString(" ").ifBlank { tokens.lastOrNull().orEmpty() },
                    pid = tokens[pidIdx],
                    memMb = rssKb / 1024
                )
            }
            .filter { it.memMb > 0 }
            .sortedByDescending { it.memMb }
            .take(MAX_PROCESSES)
    }

    private suspend fun refreshStorageData(deviceId: String) {
        val dfResult = AdbTool.execAdbOutputAsync("-s", deviceId, "shell", "df", "-h")
        val partitions = parseDfOutput(dfResult)
        val primarySummary = selectPrimaryStorageSummary(partitions)

        _storagePartitions.value = partitions
        _primaryStorageSummary.value = primarySummary
        _storageHistory.value = appendStorageHistory(_storageHistory.value, primarySummary, MAX_HISTORY)

        val pkgResult = AdbTool.execAdbOutputAsync("-s", deviceId, "shell", "pm", "list", "packages", "-f")
        val apkPaths = parseApkPaths(pkgResult)
        _storageApps.value = estimateAppSizes(deviceId, apkPaths)
    }

    private fun parseDfOutput(output: String): List<StoragePartition> {
        val lines = output.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()

        val headerIdx = lines.indexOfFirst { it.contains("Filesystem") || it.contains("Mounted") }
        if (headerIdx < 0) return emptyList()

        val header = lines[headerIdx].split(Regex("\\s+"))
        val mountIdx = header.indexOfFirst {
            it.equals("Mounted", ignoreCase = true) || it.equals("MountedOn", ignoreCase = true)
        }.takeIf { it >= 0 } ?: (header.size - 1)
        val sizeIdx = header.indexOfFirst { it.equals("Size", ignoreCase = true) }.takeIf { it >= 0 } ?: 1
        val usedIdx = header.indexOfFirst { it.equals("Used", ignoreCase = true) }.takeIf { it >= 0 } ?: 2
        val availIdx = header.indexOfFirst {
            it.equals("Avail", ignoreCase = true) || it.equals("Available", ignoreCase = true)
        }.takeIf { it >= 0 } ?: 3

        return lines.drop(headerIdx + 1)
            .mapNotNull { line ->
                val tokens = line.split(Regex("\\s+"))
                if (tokens.size <= maxOf(mountIdx, sizeIdx, usedIdx, availIdx)) return@mapNotNull null
                val mount = tokens[mountIdx]
                if (mount == "none" || mount.startsWith("tmpfs")) return@mapNotNull null
                StoragePartition(
                    mount = mount,
                    totalGb = parseSizeToGb(tokens[sizeIdx]),
                    usedGb = parseSizeToGb(tokens[usedIdx]),
                    availGb = parseSizeToGb(tokens[availIdx])
                )
            }
            .filter { it.totalGb > 0 }
            .sortedByDescending { it.totalGb }
    }

    private fun parseSizeToGb(raw: String): Double {
        val cleaned = raw.trim()
        val value = cleaned.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        return when {
            cleaned.endsWith("G", ignoreCase = true) -> value
            cleaned.endsWith("M", ignoreCase = true) -> value / 1024.0
            cleaned.endsWith("K", ignoreCase = true) -> value / (1024.0 * 1024.0)
            cleaned.endsWith("T", ignoreCase = true) -> value * 1024.0
            else -> value / (1024.0 * 1024.0)
        }
    }

    private fun parseApkPaths(output: String): List<Pair<String, String>> {
        return output.lines()
            .filter { it.startsWith("package:") }
            .mapNotNull { line ->
                val pathPart = line.removePrefix("package:").substringBefore("=").trim()
                val pkgName = line.substringAfterLast("=").trim()
                if (pathPart.isNotBlank() && pkgName.isNotBlank()) {
                    pkgName to pathPart
                } else {
                    null
                }
            }
    }

    private suspend fun estimateAppSizes(
        deviceId: String,
        packages: List<Pair<String, String>>
    ): List<StorageApp> {
        if (packages.isEmpty()) return emptyList()

        val samplePackages = packages.take(50)
        val semaphore = Semaphore(6)
        return coroutineScope {
            samplePackages.map { (name, path) ->
                async {
                    semaphore.withPermit {
                        val duResult = AdbTool.execAdbOutputAsync("-s", deviceId, "shell", "du", "-k", path)
                        parsePackageDuSizeMb(duResult)?.let { sizeMb ->
                            StorageApp(name = name, sizeMb = sizeMb)
                        }
                    }
                }
            }
                .awaitAll()
                .filterNotNull()
                .sortedByDescending { it.sizeMb }
                .take(MAX_PROCESSES)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}
