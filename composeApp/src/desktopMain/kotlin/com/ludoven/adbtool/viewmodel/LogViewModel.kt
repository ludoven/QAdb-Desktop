package com.ludoven.adbtool.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.entity.LogEntry
import com.ludoven.adbtool.entity.LogFilter
import com.ludoven.adbtool.entity.LogLevel
import com.ludoven.adbtool.util.AdbPathManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LogViewModel : ViewModel() {
    companion object {
        internal const val MAX_LOG_ENTRIES = 10_000
        private const val LOG_UI_BATCH_SIZE = 40

        internal fun appendWithLimit(buffer: ArrayDeque<LogEntry>, entry: LogEntry, maxEntries: Int = MAX_LOG_ENTRIES) {
            if (buffer.size >= maxEntries) {
                buffer.removeFirst()
            }
            buffer.addLast(entry)
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

    private var logProcess: Process? = null
    private val logBuffer = ArrayDeque<LogEntry>(MAX_LOG_ENTRIES)
    private val logBufferLock = Any()
    private var pendingLogUpdates = 0

    fun setSelectedDevice(device: String?) {
        _selectedDevice.value = device
    }

    fun updateFilter(filter: LogFilter) {
        _filter.value = filter
    }

    fun startCapture(deviceSerial: String) {
        if (_isCapturing.value) return

        _isCapturing.value = true
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
                    add(deviceSerial)
                    add("logcat")
                    add("-v")
                    add("time")
                }

                logProcess = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()

                logProcess?.inputStream?.bufferedReader()?.use { reader ->
                    val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault())
                    val structuredRegex = Regex("""^(\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEF])\s+(.+?):\s?(.*)$""")
                    val legacyRegex = Regex("""^(\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}\.\d{3})\s+([VDIWEF])\/([^\(]+)\(\s*(\d+)\):\s(.*)$""")

                    while (_isCapturing.value) {
                        val line = reader.readLine() ?: break
                        val trimmed = line.trim()

                        val entry = when {
                            structuredRegex.matches(trimmed) -> {
                                val (time, pidStr, tidStr, levelChar, tag, message) = structuredRegex.find(trimmed)!!.destructured
                                createLogEntry(
                                    time = time,
                                    levelChar = levelChar,
                                    tag = tag.trim(),
                                    message = message.trim(),
                                    pid = pidStr.trim().toIntOrNull() ?: 0,
                                    tid = tidStr.trim().toIntOrNull() ?: 0,
                                    dateFormat = dateFormat
                                )
                            }
                            legacyRegex.matches(trimmed) -> {
                                val (time, levelChar, tag, pidStr, message) = legacyRegex.find(trimmed)!!.destructured
                                createLogEntry(
                                    time = time,
                                    levelChar = levelChar,
                                    tag = tag.trim(),
                                    message = message.trim(),
                                    pid = pidStr.trim().toIntOrNull() ?: 0,
                                    tid = 0,
                                    dateFormat = dateFormat
                                )
                            }
                            else -> {
                                createLogEntry(
                                    time = "",
                                    levelChar = "I",
                                    tag = "logcat",
                                    message = trimmed,
                                    pid = 0,
                                    tid = 0,
                                    dateFormat = dateFormat
                                )
                            }
                        }

                        val shouldPublish = bufferLogEntry(entry)
                        withContext(Dispatchers.Main) {
                            publishBufferedLogs(force = false, shouldPublish = shouldPublish)
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
                withContext(Dispatchers.Main) {
                    _isCapturing.value = false
                }
            }
        }
    }

    fun stopCapture() {
        _isCapturing.value = false
        logProcess?.destroy()
        logProcess = null
    }

    fun restartCapture(deviceSerial: String) {
        stopCapture()
        startCapture(deviceSerial)
    }

    fun getFilteredLogs(): List<LogEntry> {
        val currentFilter = _filter.value
        val keywordRegex = if (currentFilter.keyword.isNotBlank() && currentFilter.isRegex) {
            runCatching { Regex(currentFilter.keyword, RegexOption.IGNORE_CASE) }.getOrNull()
        } else {
            null
        }
        val keywordLower = if (!currentFilter.isRegex) currentFilter.keyword.trim().lowercase() else ""
        val pidFilter = currentFilter.pid.trim()

        return _logs.value.filter { entry ->
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
        pendingLogUpdates >= LOG_UI_BATCH_SIZE
    }

    private fun publishBufferedLogs(force: Boolean, shouldPublish: Boolean) {
        if (!force && !shouldPublish) return
        val snapshot = synchronized(logBufferLock) {
            if (!force && pendingLogUpdates < LOG_UI_BATCH_SIZE) {
                null
            } else {
                pendingLogUpdates = 0
                logBuffer.toList()
            }
        }
        if (snapshot != null) {
            _logs.value = snapshot
        }
    }

    private fun clearLogBuffer() = synchronized(logBufferLock) {
        logBuffer.clear()
        pendingLogUpdates = 0
    }

    private fun createLogEntry(
        time: String,
        levelChar: String,
        tag: String,
        message: String,
        pid: Int,
        tid: Int,
        dateFormat: SimpleDateFormat
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
            try {
                dateFormat.parse(time)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
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
        val target = "${entry.tag} ${entry.message}"
        return if (isRegex) {
            keywordRegex?.containsMatchIn(target) ?: false
        } else {
            target.lowercase().contains(keywordLower)
        }
    }
}
