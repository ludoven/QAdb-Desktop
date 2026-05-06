package com.ludoven.adbtool.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.util.AdbTool
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TerminalLogEntry(
    val timestamp: String,
    val command: String,
    val output: String,
    val isError: Boolean
)

class TerminalViewModel : ViewModel() {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private val _commandInput = MutableStateFlow("")
    val commandInput: StateFlow<String> = _commandInput.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _logs = MutableStateFlow<List<TerminalLogEntry>>(emptyList())
    val logs: StateFlow<List<TerminalLogEntry>> = _logs.asStateFlow()

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    fun updateCommandInput(value: String) {
        _commandInput.value = value
    }

    fun applyHistoryCommand(command: String) {
        _commandInput.value = command
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun executeCommand(selectedDevice: String?) {
        val command = _commandInput.value.trim()
        if (command.isBlank() || _isExecuting.value) return

        viewModelScope.launch {
            _isExecuting.value = true

            val result = executeParsedCommand(command, selectedDevice)
            appendLog(
                command = command,
                output = result.output.ifBlank { result.errorMessage ?: "命令执行完成（无输出）" },
                isError = !result.success
            )
            _history.update { current ->
                (listOf(command) + current.filterNot { item -> item == command }).take(20)
            }
            _isExecuting.value = false
        }
    }

    private suspend fun executeParsedCommand(command: String, selectedDevice: String?): AdbTool.AdbResult {
        if (command.startsWith("adb ")) {
            val args = command.removePrefix("adb ").trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
            if (args.isEmpty()) {
                return AdbTool.AdbResult(false, "", "命令为空")
            }
            if (args.first() == "shell") {
                if (selectedDevice.isNullOrBlank()) {
                    return AdbTool.AdbResult(false, "", "未选择设备，无法执行 adb shell 命令。")
                }
                val shellCommand = args.drop(1).joinToString(" ").trim()
                if (shellCommand.isBlank()) {
                    return AdbTool.AdbResult(false, "", "shell 命令为空")
                }
                return AdbTool.execShellAsync(shellCommand, selectedDevice)
            }
            return AdbTool.execAdbAsync(*args.toTypedArray())
        }

        if (selectedDevice.isNullOrBlank()) {
            return AdbTool.AdbResult(false, "", "未选择设备，无法执行命令。")
        }

        return AdbTool.execShellAsync(command, selectedDevice)
    }

    fun runQuickCommand(command: String, selectedDevice: String?) {
        _commandInput.value = command
        executeCommand(selectedDevice)
    }

    private fun appendLog(command: String, output: String, isError: Boolean) {
        val entry = TerminalLogEntry(
            timestamp = LocalDateTime.now().format(timeFormatter),
            command = command,
            output = output,
            isError = isError
        )
        _logs.update { it + entry }
    }
}
