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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LogViewModel : ViewModel() {

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

    fun setSelectedDevice(device: String?) {
        _selectedDevice.value = device
    }

    fun updateFilter(filter: LogFilter) {
        _filter.value = filter
    }

    fun startCapture(deviceSerial: String) {
        if (_isCapturing.value) return

        _isCapturing.value = true
        _logs.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val adbPath = AdbPathManager.currentAdbPath ?: throw IllegalStateException("ADB path not set")
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
                    val logRegex = Regex("""^(\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}\.\d{3})\s+([VDIWEF])\/([^\(]+)\(\s*(\d+)\):\s(.*)$""")

                    while (_isCapturing.value) {
                        val line = reader.readLine() ?: break
                        val matchResult = logRegex.find(line.trim())

                        if (matchResult != null) {
                            val (time, levelChar, tag, pidStr, message) = matchResult.destructured
                            val level = when (levelChar) {
                                "V" -> LogLevel.VERBOSE
                                "D" -> LogLevel.DEBUG
                                "I" -> LogLevel.INFO
                                "W" -> LogLevel.WARN
                                "E" -> LogLevel.ERROR
                                "F" -> LogLevel.FATAL
                                else -> LogLevel.INFO
                            }

                            val timestamp = try {
                                dateFormat.parse(time)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }

                            val entry = LogEntry(
                                timestamp = timestamp,
                                level = level,
                                tag = tag.trim(),
                                message = message.trim(),
                                pid = pidStr.trim().toIntOrNull() ?: 0
                            )

                            withContext(Dispatchers.Main) {
                                _logs.update { current ->
                                    (current + entry).takeLast(10000)
                                }
                            }
                        }
                    }
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

    fun getFilteredLogs(): List<LogEntry> {
        val currentFilter = _filter.value
        return _logs.value.filter { entry ->
            (currentFilter.level == null || entry.level == currentFilter.level) &&
            (currentFilter.keyword.isEmpty() || entry.message.contains(currentFilter.keyword, ignoreCase = true)) &&
            (currentFilter.tag.isEmpty() || entry.tag.contains(currentFilter.tag, ignoreCase = true)) &&
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
        _logs.value = emptyList()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopCapture()
    }
}
