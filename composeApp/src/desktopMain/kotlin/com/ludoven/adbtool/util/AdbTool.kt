package com.ludoven.adbtool.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object AdbTool {

    // 假设 adb 已经添加到系统 PATH 中，或者你需要提供 adb 可执行文件的完整路径
    // 例如：val adbPath = "C:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe"
    // 或者在 macOS/Linux 上：val adbPath = "/Users/YourUser/Library/Android/sdk/platform-tools/adb"
    private const val ADB_COMMAND = "adb"

    fun executeAdbCommand(vararg args: String): String {
        val command = mutableListOf(ADB_COMMAND).apply { addAll(args) }
        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(true) // 合并标准错误流到标准输出流

        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
        }

        val exitCode = process.waitFor() // 等待命令执行完成
        if (exitCode != 0) {
            System.err.println("ADB command failed with exit code $exitCode: ${command.joinToString(" ")}")
        }
        return output.toString()
    }

    // 示例：获取已连接设备列表
    fun getConnectedDevices(): List<String> {
        val output = executeAdbCommand("devices")
        // 解析输出，通常第一行是 "List of devices attached"，需要跳过
        return output.lines()
            .drop(1) // 跳过标题行
            .mapNotNull { line ->
                // 示例输出：emulator-5554    device
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 2 && parts[1] == "device") {
                    parts[0] // 返回设备ID
                } else {
                    null
                }
            }
            .filter { it.isNotBlank() } // 过滤空行
    }

    // 示例：断开指定设备连接
    fun disconnectDevice(deviceId: String): String {
        // ADB本身没有直接的"disconnect device"命令，通常是"adb kill-server"和"adb start-server"来重置
        // 或者对于TCP连接，可以使用 adb disconnect host:port
        // 如果是USB设备，拔掉就是断开
        // 这里只是一个示例，如果需要精确控制某个设备断开，可能需要更复杂的逻辑或用户手动操作
        return executeAdbCommand("disconnect", deviceId) // 假设这是一个TCP/IP连接的设备
    }

    // 示例：向指定设备输入文本
    fun inputTextToDevice(deviceId: String, text: String): String {
        val escapedText = text.replace(" ", "%s").replace("'", "\\'") // 简单的转义处理
        return executeAdbCommand("-s", deviceId, "shell", "input", "text", escapedText)
    }



    fun installApk(apkPath: String): Boolean {
        return try {
            val process = ProcessBuilder("adb", "install", "-r", apkPath)
                .redirectErrorStream(true)
                .start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val exitCode = process.waitFor()
            println("ADB Output: $output")
            exitCode == 0 && output.contains("Success")
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    fun inputText(text: String, escapeSpaces: Boolean = true): Boolean {
        println("尝试输入文本: '$text'")
        println("是否转义空格: $escapeSpaces")

        // 处理转义空格
        val processedText = if (escapeSpaces) text.replace(" ", "%s") else text
        println("处理后的文本: '$processedText'")

        return try {
            // 执行 adb 命令
            val process = ProcessBuilder("adb", "shell", "input", "text", processedText)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            println("adb 执行输出:\n$output")
            println("adb 返回码: $exitCode")

            exitCode == 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }



    fun takeScreenshot(savePath: String): Boolean {
        try {
            // 1. 截图到设备
            val screenshotCmd = listOf("adb", "shell", "screencap", "-p", "/sdcard/screen.png")
            val screenshotResult = ProcessBuilder(screenshotCmd).start().waitFor()
            if (screenshotResult != 0) return false

            // 2. 拉取到电脑
            val pullCmd = listOf("adb", "pull", "/sdcard/screen.png", savePath)
            val pullResult = ProcessBuilder(pullCmd).start().waitFor()
            if (pullResult != 0) return false

            // 3. 可选：删除设备上的临时文件
            ProcessBuilder("adb", "shell", "rm", "/sdcard/screen.png").start()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }


    fun getCurrentActivity(): String? {
        val result = Runtime.getRuntime().exec("adb shell dumpsys activity activities").inputStream
            .bufferedReader().use { it.readText() }

        // 示例行：ResumedActivity: ActivityRecord{... com.example/.MainActivity}
        val regex = Regex("""ResumedActivity:.* ([\w\.]+\/[\w\.\$]+)""")
        val match = regex.find(result)
        return match?.groups?.get(1)?.value
    }

    fun exec(command: String): String {
        return try {
            val process = ProcessBuilder("adb", "shell", command)
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().use { it.readText() }.trim()
        } catch (e: Exception) {
            "执行失败：${e.message}"
        }
    }

    suspend fun execSuspend(command: String): String = withContext(Dispatchers.IO) {
        exec(command)
    }

    fun runAdbAndShowResult(
        coroutineScope: CoroutineScope,
        command: String,
        setDialogText: (String) -> Unit,
        setShowDialog: (Boolean) -> Unit
    ) {
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                AdbTool.exec(command)
            }
            setDialogText(result.ifEmpty { "无返回结果" })
            setShowDialog(true)
        }
    }


    fun pullFile(devicePath: String, localPath: String): Boolean {
        val result = exec("adb pull \"$devicePath\" \"$localPath\"")
        return result?.contains("1 file pulled") == true || result?.contains("pulled") == true
    }

    // 更多ADB命令的包装...
    // adb -s <deviceId> shell screencap /sdcard/screen.png
    // adb -s <deviceId> pull /sdcard/screen.png .
    // adb -s <deviceId> shell pm list packages
}