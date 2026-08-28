package com.ludoven.adbtool.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.entity.LogEntry
import com.ludoven.adbtool.entity.LogFilter
import com.ludoven.adbtool.entity.LogLevel
import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.util.ChildProcessRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@OptIn(FlowPreview::class)
class LogViewModel : ViewModel() {
    companion object {
        internal const val MAX_LOG_ENTRIES = 10_000
        private const val LOG_UI_BATCH_SIZE = 40
        private const val LOG_UI_PUBLISH_INTERVAL_MS = 100L

        private val PACKAGE_PATTERN = Regex("""([a-zA-Z][a-zA-Z0-9_]*(?:\.[a-zA-Z0-9_]+){2,})""")
        private val STRUCTURED_REGEX = Regex("""^(\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEF])\s+(.+?):\s?(.*)$""")
        private val LEGACY_REGEX = Regex("""^(\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}\.\d{3})\s+([VDIWEF])\/([^\(]+)\(\s*(\d+)\):\s(.*)$""")
        private val SYSTEM_ZONE = ZoneId.systemDefault()
        private val CURRENT_YEAR = LocalDateTime.now().year

        internal fun appendWithLimit(buffer: ArrayDeque<LogEntry>, entry: LogEntry, maxEntries: Int = MAX_LOG_ENTRIES) {
            if (buffer.size >= maxEntries) {
                buffer.removeFirst()
            }
            buffer.addLast(entry)
        }

        internal fun normalizedCaptureDevice(device: String?): String? =
            device?.takeIf { it.isNotBlank() }

        internal fun shouldPublishUiBatch(pendingUpdates: Int, elapsedMs: Long): Boolean =
            pendingUpdates >= LOG_UI_BATCH_SIZE ||
                (pendingUpdates > 0 && elapsedMs >= LOG_UI_PUBLISH_INTERVAL_MS)

        internal fun shouldStopCaptureForDeviceChange(
            isCapturing: Boolean,
            activeCaptureDevice: String?,
            nextSelectedDevice: String?
        ): Boolean {
            if (!isCapturing) return false
            val activeDevice = normalizedCaptureDevice(activeCaptureDevice) ?: return false
            return activeDevice != normalizedCaptureDevice(nextSelectedDevice)
        }

        internal fun detectLikelyPackage(logs: List<LogEntry>): String? {
            if (logs.isEmpty()) return null
            val score = HashMap<String, Int>()
            val sampleSize = 1200
            val startIndex = (logs.size - sampleSize).coerceAtLeast(0)
            for (i in startIndex until logs.size) {
                val entry = logs[i]
                PACKAGE_PATTERN.findAll(entry.tag).forEach { match ->
                    val candidate = match.value
                    if (!candidate.startsWith("android.") && !candidate.startsWith("java.")) {
                        score[candidate] = (score[candidate] ?: 0) + 1
                    }
                }
                PACKAGE_PATTERN.findAll(entry.message).forEach { match ->
                    val candidate = match.value
                    if (!candidate.startsWith("android.") && !candidate.startsWith("java.")) {
                        score[candidate] = (score[candidate] ?: 0) + 1
                    }
                }
            }
            return score.maxByOrNull { it.value }?.key
        }

        internal fun parseLogcatTimestamp(time: String): Long {
            if (time.length < 18) return System.currentTimeMillis()
            return try {
                val month = time.substring(0, 2).toInt()
                val day = time.substring(3, 5).toInt()
                val hour = time.substring(6, 8).toInt()
                val minute = time.substring(9, 11).toInt()
                val second = time.substring(12, 14).toInt()
                val millis = time.substring(15, 18).toInt()
                LocalDateTime.of(CURRENT_YEAR, month, day, hour, minute, second, millis * 1_000_000)
                    .atZone(SYSTEM_ZONE)
                    .toInstant()
                    .toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _filter = MutableStateFlow(LogFilter())
    val filter: StateFlow<LogFilter> = _filter.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedDevice = MutableStateFlow<String?>(null)
    val selectedDevice: StateFlow<String?> = _selectedDevice.asStateFlow()

    val filteredLogs: StateFlow<List<LogEntry>> = combine(_logs, _filter) { logs, filter ->
        applyFilters(logs, filter)
    }.debounce(100).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likelyCurrentPackage: StateFlow<String?> = _logs
        .debounce(300)
        .map { logs -> detectLikelyPackage(logs) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var logProcess: Process? = null
    private var activeCaptureDevice: String? = null
    private val logBuffer = ArrayDeque<LogEntry>(MAX_LOG_ENTRIES)
    private val logBufferLock = Any()
    private var pendingLogUpdates = 0
    private var lastLogPublishAtMillis = System.currentTimeMillis()
    private var pendingUiPublishJob: Job? = null

    fun setSelectedDevice(device: String?) {
        val normalizedDevice = normalizedCaptureDevice(device)
        _selectedDevice.value = normalizedDevice
        if (shouldStopCaptureForDeviceChange(_isCapturing.value, activeCaptureDevice, normalizedDevice)) {
            stopCapture()
        }
    }

    fun updateFilter(filter: LogFilter) {
        _filter.value = filter
    }

    fun startCapture(deviceSerial: String) {
        val normalizedDevice = normalizedCaptureDevice(deviceSerial) ?: return
        if (_isCapturing.value) return

        _isCapturing.value = true
        activeCaptureDevice = normalizedDevice
        clearLogBuffer()
        _logs.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val adbPath = AdbPathManager.currentAdbPath
                    ?: throw IllegalStateException(
                        AdbPathManager.adbEnvironment.value.message
                            ?: AdbPathManager.friendlyInitializationError("ADB path not set")
                    )
                val command = buildList {
                    add(adbPath)
                    add("-s")
                    add(normalizedDevice)
                    add("logcat")
                    add("-v")
                    add("time")
                }

                logProcess = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
                ChildProcessRegistry.register(logProcess!!)

                logProcess?.inputStream?.bufferedReader()?.use { reader ->
                    while (_isCapturing.value) {
                        val line = reader.readLine() ?: break
                        val trimmed = line.trim()

                        val entry = run {
                            val structuredMatch = STRUCTURED_REGEX.find(trimmed)
                            if (structuredMatch != null) {
                                val (time, pidStr, tidStr, levelChar, tag, message) = structuredMatch.destructured
                                createLogEntry(
                                    time = time,
                                    levelChar = levelChar,
                                    tag = tag.trim(),
                                    message = message.trim(),
                                    pid = pidStr.trim().toIntOrNull() ?: 0,
                                    tid = tidStr.trim().toIntOrNull() ?: 0
                                )
                            } else {
                                val legacyMatch = LEGACY_REGEX.find(trimmed)
                                if (legacyMatch != null) {
                                    val (time, levelChar, tag, pidStr, message) = legacyMatch.destructured
                                    createLogEntry(
                                        time = time,
                                        levelChar = levelChar,
                                        tag = tag.trim(),
                                        message = message.trim(),
                                        pid = pidStr.trim().toIntOrNull() ?: 0,
                                        tid = 0
                                    )
                                } else {
                                    createLogEntry(
                                        time = "",
                                        levelChar = "I",
                                        tag = "logcat",
                                        message = trimmed,
                                        pid = 0,
                                        tid = 0
                                    )
                                }
                            }
                        }

                        val shouldPublish = bufferLogEntry(entry)
                        if (shouldPublish) {
                            pendingUiPublishJob?.cancel()
                            pendingUiPublishJob = null
                            withContext(Dispatchers.Main) {
                                publishBufferedLogs(force = false, shouldPublish = true)
                            }
                        } else {
                            schedulePendingUiPublish()
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    publishBufferedLogs(force = true, shouldPublish = true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Log capture failed: ${e.message}"
                }
            } finally {
                ChildProcessRegistry.unregister(logProcess)
                logProcess = null
                activeCaptureDevice = null
                withContext(Dispatchers.Main) {
                    _isCapturing.value = false
                }
            }
        }
    }

    fun stopCapture() {
        _isCapturing.value = false
        pendingUiPublishJob?.cancel()
        pendingUiPublishJob = null
        logProcess?.destroy()
        ChildProcessRegistry.unregister(logProcess)
        logProcess = null
        activeCaptureDevice = null
    }

    fun restartCapture(deviceSerial: String) {
        if (normalizedCaptureDevice(deviceSerial) == null) return
        stopCapture()
        startCapture(deviceSerial)
    }

    fun getFilteredLogs(): List<LogEntry> = applyFilters(_logs.value, _filter.value)

    private fun applyFilters(logs: List<LogEntry>, currentFilter: LogFilter): List<LogEntry> {
        val keywordRegex = if (currentFilter.keyword.isNotBlank() && currentFilter.isRegex) {
            runCatching { Regex(currentFilter.keyword, RegexOption.IGNORE_CASE) }.getOrNull()
        } else {
            null
        }
        val keywordLower = if (!currentFilter.isRegex) currentFilter.keyword.trim().lowercase() else ""
        val pidFilter = currentFilter.pid.trim()

        return logs.filter { entry ->
            (currentFilter.level == null || entry.level == currentFilter.level) &&
            (!currentFilter.onlyErrors || entry.level == LogLevel.ERROR || entry.level == LogLevel.FATAL) &&
            (currentFilter.packageName.isEmpty() ||
                entry.tag.contains(currentFilter.packageName, ignoreCase = true) ||
                entry.message.contains(currentFilter.packageName, ignoreCase = true)) &&
            (currentFilter.tag.isEmpty() || entry.tag.contains(currentFilter.tag, ignoreCase = true)) &&
            (pidFilter.isEmpty() || entry.pid.toString().contains(pidFilter)) &&
            matchesKeywordFilter(entry, currentFilter.keyword, keywordLower, currentFilter.isRegex, keywordRegex) &&
            (currentFilter.startTime == null || entry.timestamp >= currentFilter.startTime) &&
            (currentFilter.endTime == null || entry.timestamp <= currentFilter.endTime)
        }
    }

    fun exportLogs(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                val filteredLogs = getFilteredLogs()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

                file.bufferedWriter().use { writer ->
                    writer.write("ADB Log Export - ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                    writer.write("Total entries: ${filteredLogs.size}\n")
                    writer.write("=".repeat(80) + "\n\n")

                    filteredLogs.forEach { entry ->
                        val time = dateFormat.format(Date(entry.timestamp))
                        writer.write("[$time] ${entry.level.displayName}/${entry.tag} (PID: ${entry.pid}): ${entry.message}\n")
                    }
                }

                withContext(Dispatchers.Main) {
                    _errorMessage.value = null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Export failed: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun clearLogs() {
        clearLogBuffer()
        _logs.value = emptyList()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopCapture()
    }

    private fun bufferLogEntry(entry: LogEntry): Boolean = synchronized(logBufferLock) {
        appendWithLimit(logBuffer, entry)
        pendingLogUpdates += 1
        shouldPublishUiBatch(
            pendingUpdates = pendingLogUpdates,
            elapsedMs = System.currentTimeMillis() - lastLogPublishAtMillis
        )
    }

    private fun publishBufferedLogs(force: Boolean, shouldPublish: Boolean) {
        if (!force && !shouldPublish) return
        val snapshot = synchronized(logBufferLock) {
            if (!force && (!shouldPublish || pendingLogUpdates == 0)) {
                null
            } else {
                pendingLogUpdates = 0
                lastLogPublishAtMillis = System.currentTimeMillis()
                logBuffer.toList()
            }
        }
        if (snapshot != null) {
            _logs.value = snapshot
        }
    }

    private fun schedulePendingUiPublish() {
        if (pendingUiPublishJob?.isActive == true) return
        pendingUiPublishJob = viewModelScope.launch {
            delay(LOG_UI_PUBLISH_INTERVAL_MS)
            publishBufferedLogs(force = false, shouldPublish = true)
            pendingUiPublishJob = null
        }
    }

    private fun clearLogBuffer() {
        pendingUiPublishJob?.cancel()
        pendingUiPublishJob = null
        synchronized(logBufferLock) {
            logBuffer.clear()
            pendingLogUpdates = 0
            lastLogPublishAtMillis = System.currentTimeMillis()
        }
    }

    private fun createLogEntry(
        time: String,
        levelChar: String,
        tag: String,
        message: String,
        pid: Int,
        tid: Int
    ): LogEntry {
        val level = when (levelChar) {
            "V" -> LogLevel.VERBOSE
            "D" -> LogLevel.DEBUG
            "I" -> LogLevel.INFO
            "W" -> LogLevel.WARN
            "E" -> LogLevel.ERROR
            "F" -> LogLevel.FATAL
            else -> LogLevel.INFO
        }

        val timestamp = if (time.isBlank()) {
            System.currentTimeMillis()
        } else {
            parseLogcatTimestamp(time)
        }

        return LogEntry(
            timestamp = timestamp,
            level = level,
            tag = tag,
            message = message,
            pid = pid,
            tid = tid
        )
    }

    private fun matchesKeywordFilter(
        entry: LogEntry,
        keywordRaw: String,
        keywordLower: String,
        isRegex: Boolean,
        keywordRegex: Regex?
    ): Boolean {
        if (keywordRaw.isBlank()) return true
        return if (isRegex) {
            keywordRegex?.let { it.containsMatchIn(entry.tag) || it.containsMatchIn(entry.message) } ?: false
        } else {
            entry.tag.contains(keywordLower, ignoreCase = true) || entry.message.contains(keywordLower, ignoreCase = true)
        }
    }
}
