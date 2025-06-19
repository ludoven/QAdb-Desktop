package com.ludoven.adbtool.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object AdbTool {

    private var adbPath: String? = null
    var selectDeviceId: String? = null

    fun setAdbPath(path: String) {
        adbPath = path
    }

    fun getAdbPath(): String? = adbPath

    fun getSystemAdbPath(): String? {
        return try {
            val cmd = if (System.getProperty("os.name").startsWith("Windows")) "where adb" else "which adb"
            Runtime.getRuntime().exec(cmd).inputStream.bufferedReader().readLine()
        } catch (e: Exception) {
            null
        }
    }

    private fun runCommand(vararg args: String): String {
        val fullCmd = mutableListOf(adbPath ?: "adb").apply { addAll(args) }
        return try {
            val process = ProcessBuilder(fullCmd).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.trim()
        } catch (e: Exception) {
            "执行失败：${e.message}"
        }
    }

    fun execShell(command: String, deviceId: String? = selectDeviceId): String {
        val args = mutableListOf<String>()
        if (!deviceId.isNullOrBlank()) args += listOf("-s", deviceId)
        args += listOf("shell", command)
        return runCommand(*args.toTypedArray())
    }

    fun exec(command: String): String = execShell(command)

    suspend fun getConnectedDevices(): List<String> {
        return runCommand("devices").lines().drop(1).mapNotNull {
            val parts = it.trim().split("\\s+".toRegex())
            if (parts.size >= 2 && parts[1] == "device") parts[0] else null
        }
    }

    fun takeScreenshot(savePath: String, deviceId: String? = selectDeviceId): Boolean {
        if (deviceId.isNullOrBlank()) return false
        return try {
            val remote = "/sdcard/screen.png"
            runCommand("-s", deviceId, "shell", "screencap", "-p", remote)
            runCommand("-s", deviceId, "pull", remote, savePath)
            runCommand("-s", deviceId, "shell", "rm", remote)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun pullFile(devicePath: String, localPath: String, deviceId: String? = selectDeviceId): Boolean {
        if (deviceId.isNullOrBlank()) return false
        val result = runCommand("-s", deviceId, "pull", devicePath, localPath)
        return result.contains("pulled")
    }

    fun installApk(apkPath: String, deviceId: String? = selectDeviceId): Boolean {
        if (deviceId.isNullOrBlank()) return false
        val output = runCommand("-s", deviceId, "install", "-r", apkPath)
        return output.contains("Success")
    }

    fun uninstallApp(packageName: String, deviceId: String? = selectDeviceId): Boolean {
        if (deviceId.isNullOrBlank()) return false
        val output = runCommand("-s", deviceId, "uninstall", packageName)
        return output.contains("Success")
    }

    fun inputText(text: String, deviceId: String? = selectDeviceId): Boolean {
        if (deviceId.isNullOrBlank()) return false
        val processedText = text.replace(" ", "%s")
        val output = runCommand("-s", deviceId, "shell", "input", "text", processedText)
        return output.isNotBlank()
    }

    fun getCurrentActivity(deviceId: String? = selectDeviceId): String? {
        val result = runCommand("-s", deviceId ?: return null, "shell", "dumpsys", "activity", "activities")
        val regex = Regex("""ResumedActivity:.* ([\w\.]+\/[\w\.\$]+)""")
        return regex.find(result)?.groups?.get(1)?.value
    }

    fun startApp(packageName: String, deviceId: String? = selectDeviceId): Boolean {
        val component = execShell("cmd package resolve-activity --brief $packageName", deviceId).lines().lastOrNull()
        return if (!component.isNullOrBlank()) {
            execShell("am start -n $component", deviceId)
            true
        } else false
    }

    fun stopApp(packageName: String, deviceId: String? = selectDeviceId): Boolean {
        execShell("am force-stop $packageName", deviceId)
        return true
    }

    fun clearAppData(packageName: String, deviceId: String? = selectDeviceId): Boolean {
        return execShell("pm clear $packageName", deviceId).contains("Success")
    }

    fun resetPermissions(packageName: String, deviceId: String? = selectDeviceId): Boolean {
        execShell("pm reset-permissions $packageName", deviceId)
        return true
    }

    fun grantAllPermissions(packageName: String, deviceId: String? = selectDeviceId): Boolean {
        val result = execShell("dumpsys package $packageName", deviceId)
        val permissions = Regex("android.permission.[A-Z_\\.]+").findAll(result).map { it.value }.toSet()
        permissions.forEach { perm ->
            execShell("pm grant $packageName $perm", deviceId)
        }
        return true
    }

    fun getAppPath(packageName: String, deviceId: String? = selectDeviceId): String {
        return execShell("pm path $packageName", deviceId)
    }

    fun getInstallTime(packageName: String, deviceId: String? = selectDeviceId): String {
        return execShell("dumpsys package $packageName | grep firstInstallTime", deviceId)
    }

    fun getUpdateTime(packageName: String, deviceId: String? = selectDeviceId): String {
        return execShell("dumpsys package $packageName | grep lastUpdateTime", deviceId)
    }

    fun isSystemApp(packageName: String, deviceId: String? = selectDeviceId): Boolean {
        return execShell("dumpsys package $packageName", deviceId).contains("flags=[ SYSTEM")
    }

    fun getSupportedAbis(deviceId: String? = selectDeviceId): String {
        return execShell("getprop ro.product.cpu.abilist", deviceId)
    }

    fun getTargetSdkVersion(packageName: String, deviceId: String? = selectDeviceId): String {
        return execShell("dumpsys package $packageName | grep targetSdk", deviceId)
    }

    fun getMinSdkVersion(packageName: String, deviceId: String? = selectDeviceId): String {
        return execShell("dumpsys package $packageName | grep minSdk", deviceId)
    }

    fun runAdbAndShowResult(
        coroutineScope: CoroutineScope,
        command: String,
        setDialogText: (String) -> Unit,
        setShowDialog: (Boolean) -> Unit
    ) {
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) { exec(command) }
            setDialogText(result.ifBlank { "无返回结果" })
            setShowDialog(true)
        }
    }


    suspend fun executeAdbCommand(vararg args: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val command = mutableListOf("adb") + args
                val process = ProcessBuilder(command).redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                output.trim()
            } catch (e: Exception) {
                e.printStackTrace()
                "执行失败：${e.message}"
            }
        }
    }

    suspend fun disconnectDevice(deviceId: String): String {
        return executeAdbCommand("disconnect", deviceId)
    }
/*
    suspend fun getConnectedDevices(): List<String> {
        val output = executeAdbCommand("devices")
        return output.lines().drop(1)
            .mapNotNull { line -> line.split("\\s+".toRegex()).takeIf { it.size >= 2 && it[1] == "device" }?.get(0) }
    }*/
}
