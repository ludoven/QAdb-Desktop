package com.ludoven.adbtool.domain.adb

data class AdbCommand(
    val rawInput: String,
    val displayCommand: String,
    val executable: String,
    val args: List<String>,
    val requireDevice: Boolean,
    val entersShell: Boolean = false
)

enum class BuiltInCommandType {
    HELP,
    CLEAR,
    HISTORY,
    DEVICES,
    SELECT,
    EXIT
}

sealed interface ParsedCommand {
    data object Empty : ParsedCommand
    data class BuiltIn(
        val type: BuiltInCommandType,
        val args: List<String> = emptyList()
    ) : ParsedCommand

    data class External(
        val command: AdbCommand
    ) : ParsedCommand

    data class ExternalSequence(
        val commands: List<AdbCommand>
    ) : ParsedCommand

    data class Invalid(
        val message: String
    ) : ParsedCommand
}

class AdbCommandParser {

    fun parse(rawInput: String, deviceId: String?): ParsedCommand {
        val input = rawInput.trim()
        if (input.isBlank()) return ParsedCommand.Empty

        val tokens = tokenize(input)
        if (tokens.isEmpty()) return ParsedCommand.Empty

        val head = tokens.first().lowercase()
        return when (head) {
            "help" -> ParsedCommand.BuiltIn(BuiltInCommandType.HELP)
            "history" -> ParsedCommand.BuiltIn(BuiltInCommandType.HISTORY)
            "devices" -> ParsedCommand.BuiltIn(BuiltInCommandType.DEVICES)
            "select", "use" -> {
                val target = tokens.getOrNull(1)
                if (target.isNullOrBlank()) {
                    ParsedCommand.Invalid("用法：select <deviceId>")
                } else {
                    ParsedCommand.BuiltIn(BuiltInCommandType.SELECT, listOf(target))
                }
            }
            "exit" -> ParsedCommand.BuiltIn(BuiltInCommandType.EXIT)
            "clear" -> {
                if (tokens.size == 1) {
                    ParsedCommand.BuiltIn(BuiltInCommandType.CLEAR)
                } else {
                    parseDeviceCommand(tokens, input, deviceId)
                }
            }
            "adb" -> parseRawAdb(tokens, input)
            else -> parseDeviceCommand(tokens, input, deviceId)
        }
    }

    fun commandTemplates(): List<String> {
        return listOf(
            "help",
            "clear",
            "history",
            "devices",
            "select <deviceId>",
            "shell",
            "shell <cmd>",
            "install <apkPath>",
            "uninstall <packageName>",
            "start <packageName>",
            "stop <packageName>",
            "clear <packageName>",
            "restart <packageName>",
            "logcat",
            "logcat clear",
            "logcat error",
            "battery",
            "size",
            "density",
            "activity",
            "screenshot"
        )
    }

    private fun parseRawAdb(tokens: List<String>, rawInput: String): ParsedCommand {
        val args = tokens.drop(1)
        if (args.isEmpty()) {
            return ParsedCommand.Invalid("adb 命令不能为空")
        }
        return ParsedCommand.External(
            AdbCommand(
                rawInput = rawInput,
                displayCommand = rawInput,
                executable = "adb",
                args = args,
                requireDevice = false
            )
        )
    }

    private fun parseDeviceCommand(tokens: List<String>, rawInput: String, deviceId: String?): ParsedCommand {
        val head = tokens.first().lowercase()
        val tail = tokens.drop(1)

        return when (head) {
            "shell" -> {
                if (deviceId.isNullOrBlank()) return missingDevice()
                if (tail.isEmpty()) {
                    ParsedCommand.External(
                        buildAdbCommand(rawInput, deviceId, listOf("shell"), requireDevice = true, entersShell = true)
                    )
                } else {
                    ParsedCommand.External(
                        buildAdbCommand(rawInput, deviceId, listOf("shell") + tail, requireDevice = true)
                    )
                }
            }
            "install" -> {
                val path = tail.firstOrNull() ?: return ParsedCommand.Invalid("用法：install <apkPath>")
                if (deviceId.isNullOrBlank()) return missingDevice()
                ParsedCommand.External(
                    buildAdbCommand(rawInput, deviceId, listOf("install", path), requireDevice = true)
                )
            }
            "uninstall" -> {
                val pkg = tail.firstOrNull() ?: return ParsedCommand.Invalid("用法：uninstall <packageName>")
                if (deviceId.isNullOrBlank()) return missingDevice()
                ParsedCommand.External(
                    buildAdbCommand(rawInput, deviceId, listOf("uninstall", pkg), requireDevice = true)
                )
            }
            "start" -> {
                val pkg = tail.firstOrNull() ?: return ParsedCommand.Invalid("用法：start <packageName>")
                if (deviceId.isNullOrBlank()) return missingDevice()
                ParsedCommand.External(
                    buildAdbCommand(rawInput, deviceId, listOf("shell", "monkey", "-p", pkg, "1"), requireDevice = true)
                )
            }
            "stop" -> {
                val pkg = tail.firstOrNull() ?: return ParsedCommand.Invalid("用法：stop <packageName>")
                if (deviceId.isNullOrBlank()) return missingDevice()
                ParsedCommand.External(
                    buildAdbCommand(rawInput, deviceId, listOf("shell", "am", "force-stop", pkg), requireDevice = true)
                )
            }
            "clear" -> {
                val pkg = tail.firstOrNull() ?: return ParsedCommand.Invalid("用法：clear <packageName>")
                if (deviceId.isNullOrBlank()) return missingDevice()
                ParsedCommand.External(
                    buildAdbCommand(rawInput, deviceId, listOf("shell", "pm", "clear", pkg), requireDevice = true)
                )
            }
            "restart" -> {
                val pkg = tail.firstOrNull() ?: return ParsedCommand.Invalid("用法：restart <packageName>")
                if (deviceId.isNullOrBlank()) return missingDevice()
                ParsedCommand.ExternalSequence(
                    listOf(
                        buildAdbCommand("stop $pkg", deviceId, listOf("shell", "am", "force-stop", pkg), requireDevice = true),
                        buildAdbCommand("start $pkg", deviceId, listOf("shell", "monkey", "-p", pkg, "1"), requireDevice = true)
                    )
                )
            }
            "logcat" -> {
                if (deviceId.isNullOrBlank()) return missingDevice()
                when (tail.firstOrNull()?.lowercase()) {
                    null -> ParsedCommand.External(
                        buildAdbCommand(rawInput, deviceId, listOf("logcat"), requireDevice = true)
                    )
                    "clear" -> ParsedCommand.External(
                        buildAdbCommand(rawInput, deviceId, listOf("logcat", "-c"), requireDevice = true)
                    )
                    "error" -> ParsedCommand.External(
                        buildAdbCommand(rawInput, deviceId, listOf("logcat", "*:E"), requireDevice = true)
                    )
                    else -> ParsedCommand.Invalid("暂不支持该 logcat 子命令")
                }
            }
            "battery" -> {
                if (deviceId.isNullOrBlank()) return missingDevice()
                ParsedCommand.External(
                    buildAdbCommand(rawInput, deviceId, listOf("shell", "dumpsys", "battery"), requireDevice = true)
                )
            }
            "size" -> {
                if (deviceId.isNullOrBlank()) return missingDevice()
                ParsedCommand.External(
                    buildAdbCommand(rawInput, deviceId, listOf("shell", "wm", "size"), requireDevice = true)
                )
            }
            "density" -> {
                if (deviceId.isNullOrBlank()) return missingDevice()
                ParsedCommand.External(
                    buildAdbCommand(rawInput, deviceId, listOf("shell", "wm", "density"), requireDevice = true)
                )
            }
            "activity" -> {
                if (deviceId.isNullOrBlank()) return missingDevice()
                ParsedCommand.External(
                    buildAdbCommand(rawInput, deviceId, listOf("shell", "dumpsys", "activity", "top"), requireDevice = true)
                )
            }
            "screenshot" -> {
                if (deviceId.isNullOrBlank()) return missingDevice()
                ParsedCommand.External(
                    buildAdbCommand(rawInput, deviceId, listOf("exec-out", "screencap", "-p"), requireDevice = true)
                )
            }
            else -> ParsedCommand.Invalid("未知命令：$head\n输入 help 查看支持的命令。")
        }
    }

    private fun buildAdbCommand(
        rawInput: String,
        deviceId: String,
        subArgs: List<String>,
        requireDevice: Boolean,
        entersShell: Boolean = false
    ): AdbCommand {
        val args = listOf("-s", deviceId) + subArgs
        return AdbCommand(
            rawInput = rawInput,
            displayCommand = "adb ${args.joinToString(" ")}",
            executable = "adb",
            args = args,
            requireDevice = requireDevice,
            entersShell = entersShell
        )
    }

    private fun missingDevice(): ParsedCommand.Invalid {
        return ParsedCommand.Invalid("当前未选择设备，请先连接设备或执行 devices 查看设备列表。")
    }

    private fun tokenize(input: String): List<String> {
        val regex = Regex("\"([^\"]*)\"|'([^']*)'|([^\\s]+)")
        return regex.findAll(input)
            .mapNotNull { match ->
                match.groups[1]?.value
                    ?: match.groups[2]?.value
                    ?: match.groups[3]?.value
            }
            .toList()
    }
}
