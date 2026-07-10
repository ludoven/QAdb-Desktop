package com.ludoven.adbtool.domain.terminal

import com.ludoven.adbtool.domain.adb.AdbCommand
import com.ludoven.adbtool.domain.adb.AdbCommandExecutor
import com.ludoven.adbtool.domain.adb.AdbCommandParser
import com.ludoven.adbtool.domain.adb.BuiltInCommandType
import com.ludoven.adbtool.domain.adb.CommandOutput
import com.ludoven.adbtool.domain.adb.DeviceRepository
import com.ludoven.adbtool.domain.adb.ParsedCommand
import com.ludoven.adbtool.domain.adb.RunningProcess
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TerminalController(
    private val parser: AdbCommandParser = AdbCommandParser(),
    private val executor: AdbCommandExecutor = AdbCommandExecutor(),
    private val deviceRepository: DeviceRepository = DeviceRepository(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val maxLines = 5000
    private val maxHistory = 200
    private val outputBatchSize = 40
    private val outputFlushDelayMs = 80L
    private val lineCounter = AtomicLong(0)
    private val outputBufferLock = Any()
    private val pendingOutputLines = ArrayDeque<TerminalLine>()

    private var historyCursor = -1
    private var historyDraft = ""
    private var runningProcess: RunningProcess? = null
    private var runningJob: Job? = null
    private var pendingOutputFlushJob: Job? = null

    private val _session = MutableStateFlow(
        TerminalSession(
            id = UUID.randomUUID().toString(),
            title = "ADB Console",
            deviceId = null,
            mode = TerminalMode.QADB,
            history = emptyList(),
            isRunning = false,
            runningCommand = null,
            lines = emptyList(),
            devices = emptyList(),
            deviceDisplayNames = emptyMap()
        )
    )
    val session: StateFlow<TerminalSession> = _session.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    fun bindDeviceState(devices: List<String>, displayNames: Map<String, String>, selectedDevice: String?) {
        deviceRepository.update(devices, displayNames)
        _session.update { current ->
            val boundDevice = chooseBoundDevice(
                current = current.deviceId,
                selectedDevice = selectedDevice,
                available = devices
            )
            current.copy(
                deviceId = boundDevice,
                devices = devices,
                deviceDisplayNames = displayNames
            )
        }
    }

    fun updateInput(text: String) {
        _input.value = text
        historyCursor = -1
    }

    fun clearOutput() {
        clearPendingOutput()
        _session.update { it.copy(lines = emptyList()) }
    }

    fun clearInput() {
        _input.value = ""
        historyCursor = -1
        historyDraft = ""
    }

    fun updateSearchQuery(query: String) {
        _session.update { it.copy(searchQuery = query) }
    }

    fun setFollowOutput(enabled: Boolean) {
        _session.update { it.copy(followOutput = enabled) }
    }

    fun previousHistory() {
        val history = _session.value.history
        if (history.isEmpty()) return

        if (historyCursor == -1) {
            historyDraft = _input.value
            historyCursor = 0
        } else if (historyCursor < history.lastIndex) {
            historyCursor += 1
        }

        _input.value = history[historyCursor]
    }

    fun nextHistory() {
        val history = _session.value.history
        if (history.isEmpty() || historyCursor == -1) return

        if (historyCursor == 0) {
            historyCursor = -1
            _input.value = historyDraft
            return
        }

        historyCursor -= 1
        _input.value = history[historyCursor]
    }

    fun executeCurrentInput() {
        executeInput(_input.value)
    }

    fun executeInput(rawInput: String) {
        val input = rawInput.trim()
        if (input.isBlank()) return

        appendLine(TerminalLineType.INPUT, "${currentPrompt()} $input")
        addToHistory(input)
        _input.value = ""

        if (_session.value.mode == TerminalMode.ADB_SHELL) {
            val sent = runningProcess?.writeLine(input) == true
            if (!sent) {
                appendLine(TerminalLineType.ERROR, "Shell 会话已断开，请重新执行 shell。")
                switchToQadbMode()
            }
            return
        }

        if (_session.value.isRunning) {
            appendLine(TerminalLineType.STATUS, "当前已有命令在执行，可按 Ctrl+C 终止。")
            return
        }

        when (val parsed = parser.parse(input, _session.value.deviceId)) {
            ParsedCommand.Empty -> Unit
            is ParsedCommand.Invalid -> appendLine(TerminalLineType.ERROR, enrichErrorMessage(parsed.message))
            is ParsedCommand.BuiltIn -> executeBuiltIn(parsed)
            is ParsedCommand.External -> runExternalCommand(parsed.command)
            is ParsedCommand.ExternalSequence -> runExternalSequence(parsed.commands)
        }
    }

    fun interruptRunningCommand() {
        val current = _session.value
        if (!current.isRunning) return

        if (current.mode == TerminalMode.ADB_SHELL) {
            runningProcess?.interrupt()
            appendLine(TerminalLineType.STATUS, "已发送 Ctrl+C 到 shell 会话。")
        } else {
            runningProcess?.cancel()
            runningJob?.cancel()
            runningProcess = null
            _session.update {
                it.copy(
                    isRunning = false,
                    runningCommand = null
                )
            }
            appendLine(TerminalLineType.STATUS, "已中断当前命令。")
        }
    }

    fun dispose() {
        flushPendingOutput()
        runningProcess?.cancel()
        runningJob?.cancel()
        pendingOutputFlushJob?.cancel()
    }

    private fun executeBuiltIn(parsed: ParsedCommand.BuiltIn) {
        when (parsed.type) {
            BuiltInCommandType.HELP -> {
                appendLine(TerminalLineType.OUTPUT, helpText())
            }
            BuiltInCommandType.CLEAR -> {
                clearOutput()
            }
            BuiltInCommandType.HISTORY -> {
                if (_session.value.history.isEmpty()) {
                    appendLine(TerminalLineType.STATUS, "暂无历史命令。")
                } else {
                    val text = _session.value.history.withIndex()
                        .joinToString("\n") { (index, cmd) -> "${index + 1}. $cmd" }
                    appendLine(TerminalLineType.OUTPUT, text)
                }
            }
            BuiltInCommandType.DEVICES -> {
                runExternalCommand(
                    AdbCommand(
                        rawInput = "devices",
                        displayCommand = "adb devices",
                        executable = "adb",
                        args = listOf("devices"),
                        requireDevice = false
                    ),
                    onExit = { code, stdout, _ ->
                        if (code == 0) {
                            val devices = DeviceRepository.parseDevices(stdout.joinToString("\n"))
                            val currentNames = _session.value.deviceDisplayNames
                            bindDeviceState(devices, currentNames, _session.value.deviceId)
                        }
                    }
                )
            }
            BuiltInCommandType.SELECT -> {
                val deviceId = parsed.args.firstOrNull().orEmpty()
                val devices = _session.value.devices
                if (deviceId !in devices) {
                    appendLine(TerminalLineType.ERROR, "未找到设备：$deviceId\n请先执行 devices 查看可用设备。")
                } else {
                    _session.update { it.copy(deviceId = deviceId) }
                    appendLine(TerminalLineType.SUCCESS, "已切换设备：${deviceLabel(deviceId)}")
                }
            }
            BuiltInCommandType.EXIT -> {
                if (_session.value.mode == TerminalMode.ADB_SHELL) {
                    val sent = runningProcess?.writeLine("exit") == true
                    if (!sent) {
                        switchToQadbMode()
                        appendLine(TerminalLineType.STATUS, "Shell 会话已结束。")
                    }
                } else {
                    appendLine(TerminalLineType.STATUS, "当前不在 shell 会话中。")
                }
            }
        }
    }

    private fun runExternalSequence(commands: List<AdbCommand>) {
        if (commands.isEmpty()) return
        runExternalSequenceInternal(commands, index = 0)
    }

    private fun runExternalSequenceInternal(commands: List<AdbCommand>, index: Int) {
        if (index >= commands.size) return
        runExternalCommand(
            command = commands[index],
            onExit = { code, _, _ ->
                if (code == 0) {
                    runExternalSequenceInternal(commands, index + 1)
                }
            }
        )
    }

    private fun runExternalCommand(
        command: AdbCommand,
        onExit: ((code: Int, stdout: List<String>, stderr: List<String>) -> Unit)? = null
    ) {
        if (_session.value.isRunning) {
            appendLine(TerminalLineType.STATUS, "当前已有命令在执行，可按 Ctrl+C 终止。")
            return
        }

        val timeoutMs = if (command.entersShell || command.args.firstOrNull() == "logcat") null else 120_000L
        val process = if (command.entersShell) {
            val currentDevice = _session.value.deviceId
            if (currentDevice.isNullOrBlank()) {
                appendLine(TerminalLineType.ERROR, "当前未选择设备，请先连接设备或执行 devices 查看设备列表。")
                return
            }
            executor.startInteractiveShell(currentDevice)
        } else {
            executor.start(command, timeoutMs = timeoutMs)
        }

        runningProcess = process
        _session.update { it.copy(isRunning = true, runningCommand = command.rawInput) }

        val stdoutLines = mutableListOf<String>()
        val stderrLines = mutableListOf<String>()

        if (command.entersShell) {
            _session.update { it.copy(mode = TerminalMode.ADB_SHELL) }
            appendLine(TerminalLineType.STATUS, "已进入 adb shell 会话，输入 exit 退出。")
        }

        runningJob = scope.launch {
            try {
                process.outputs.collect { output ->
                    when (output) {
                        is CommandOutput.Command -> appendLine(TerminalLineType.COMMAND, output.text)
                        is CommandOutput.Stdout -> {
                            stdoutLines += output.text
                            appendLine(TerminalLineType.OUTPUT, output.text)
                        }
                        is CommandOutput.Stderr -> {
                            stderrLines += output.text
                            appendLine(TerminalLineType.ERROR, output.text)
                        }
                        is CommandOutput.Status -> appendLine(TerminalLineType.STATUS, output.text)
                        is CommandOutput.Exit -> {
                            _session.update {
                                it.copy(
                                    isRunning = false,
                                    runningCommand = null
                                )
                            }
                            runningProcess = null

                            val lineType = if (output.code == 0) TerminalLineType.SUCCESS else TerminalLineType.ERROR
                            appendLine(lineType, "[exit code: ${output.code}, ${output.durationMs}ms]")
                            if (output.code != 0 && shouldShowFailureHint(command.rawInput)) {
                                appendLine(
                                    TerminalLineType.ERROR,
                                    "命令执行失败，可能原因：\n1. 当前设备未授权\n2. 应用不存在\n3. 系统限制了该操作\n4. 设备不是 root，无法执行该命令"
                                )
                            }

                            if (command.entersShell) {
                                switchToQadbMode()
                                appendLine(TerminalLineType.STATUS, "shell 会话已结束，已返回 qadb 模式。")
                            }

                            onExit?.invoke(output.code, stdoutLines.toList(), stderrLines.toList())
                        }
                    }
                }
            } finally {
                if (_session.value.isRunning && runningProcess === process) {
                    runningProcess = null
                    if (command.entersShell || _session.value.mode == TerminalMode.ADB_SHELL) {
                        switchToQadbMode()
                        appendLine(TerminalLineType.STATUS, "shell 会话已结束，已返回 qadb 模式。")
                    } else {
                        _session.update {
                            it.copy(
                                isRunning = false,
                                runningCommand = null
                            )
                        }
                    }
                }
            }
        }
    }

    private fun switchToQadbMode() {
        _session.update { current ->
            current.copy(
                mode = TerminalMode.QADB,
                isRunning = false,
                runningCommand = null
            )
        }
    }

    private fun addToHistory(command: String) {
        _session.update { current ->
            val newHistory = (listOf(command) + current.history.filterNot { it == command }).take(maxHistory)
            current.copy(history = newHistory)
        }
        historyCursor = -1
        historyDraft = ""
    }

    private fun appendLine(type: TerminalLineType, text: String) {
        if (text.isBlank()) return
        val line = TerminalLine(
            id = lineCounter.incrementAndGet().toString(),
            type = type,
            text = text
        )
        if (type.shouldBatchOutput()) {
            val shouldPublish = synchronized(outputBufferLock) {
                pendingOutputLines.addLast(line)
                pendingOutputLines.size >= outputBatchSize
            }
            if (shouldPublish) {
                flushPendingOutput()
            } else {
                schedulePendingOutputFlush()
            }
            return
        }

        flushPendingOutput()
        publishLines(listOf(line))
    }

    private fun TerminalLineType.shouldBatchOutput(): Boolean {
        return this == TerminalLineType.OUTPUT || this == TerminalLineType.ERROR
    }

    private fun schedulePendingOutputFlush() {
        synchronized(outputBufferLock) {
            if (pendingOutputFlushJob?.isActive == true) return
            pendingOutputFlushJob = scope.launch {
                delay(outputFlushDelayMs)
                flushPendingOutput()
            }
        }
    }

    private fun flushPendingOutput() {
        val lines = synchronized(outputBufferLock) {
            if (pendingOutputLines.isEmpty()) return
            pendingOutputFlushJob?.cancel()
            pendingOutputFlushJob = null
            pendingOutputLines.toList().also {
                pendingOutputLines.clear()
            }
        }
        publishLines(lines)
    }

    private fun clearPendingOutput() = synchronized(outputBufferLock) {
        pendingOutputFlushJob?.cancel()
        pendingOutputFlushJob = null
        pendingOutputLines.clear()
    }

    private fun publishLines(lines: List<TerminalLine>) {
        if (lines.isEmpty()) return
        _session.update { current ->
            val overflow = current.lines.size + lines.size - maxLines
            val retainedCurrentLines = if (overflow > 0) {
                current.lines.drop(overflow.coerceAtMost(current.lines.size))
            } else {
                current.lines
            }
            val newLines = ArrayList<TerminalLine>(
                (retainedCurrentLines.size + lines.size).coerceAtMost(maxLines)
            ).apply {
                addAll(retainedCurrentLines)
                if (size + lines.size <= maxLines) {
                    addAll(lines)
                } else {
                    addAll(lines.takeLast(maxLines - size))
                }
            }
            current.copy(lines = newLines)
        }
    }

    private fun currentPrompt(): String {
        val current = _session.value
        return when (current.mode) {
            TerminalMode.QADB -> "qadb:${deviceLabel(current.deviceId)}>"
            TerminalMode.ADB_SHELL -> "device:${deviceLabel(current.deviceId)}\$"
        }
    }

    private fun deviceLabel(deviceId: String?): String {
        if (deviceId.isNullOrBlank()) return "no-device"
        val display = deviceRepository.findBestName(deviceId)
        return if (display.isNullOrBlank()) deviceId else display
    }

    private fun chooseBoundDevice(current: String?, selectedDevice: String?, available: List<String>): String? {
        return when {
            !current.isNullOrBlank() && current in available -> current
            !selectedDevice.isNullOrBlank() && selectedDevice in available -> selectedDevice
            available.size == 1 -> available.first()
            else -> null
        }
    }

    private fun helpText(): String {
        return buildString {
            appendLine("QADB 内置命令：")
            appendLine("  help")
            appendLine("  clear")
            appendLine("  history")
            appendLine("  devices")
            appendLine("  select <deviceId>")
            appendLine("  exit")
            appendLine()
            appendLine("ADB 简化命令（MVP）：")
            appendLine("  shell")
            appendLine("  shell <cmd>")
            appendLine("  install <apkPath>")
            appendLine("  uninstall <packageName>")
            appendLine("  start <packageName>")
            appendLine("  stop <packageName>")
            appendLine("  clear <packageName>")
            appendLine("  restart <packageName>")
            appendLine("  logcat")
            appendLine("  logcat clear")
            appendLine("  logcat error")
            appendLine("  battery / size / density / activity / screenshot")
            appendLine()
            appendLine("原始 adb 命令：")
            appendLine("  adb <args...>")
        }
    }

    private fun enrichErrorMessage(message: String): String {
        if (!message.contains("当前未选择设备")) return message
        return if (_session.value.devices.size > 1) {
            "检测到多个设备，请先使用 select <deviceId> 选择一个设备。"
        } else {
            message
        }
    }

    private fun shouldShowFailureHint(rawInput: String): Boolean {
        val head = rawInput.trim().substringBefore(" ").lowercase()
        return head in setOf("clear", "uninstall", "reboot", "restart")
    }
}
