package com.ludoven.adbtool.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ADB工具类，提供统一的ADB命令执行接口
 * 优化了错误处理、代码复用和性能
 */
object AdbTool {
    
    var selectDeviceId: String? = null
    
    /**
 * 执行结果封装类
 */
    data class AdbResult(
        val success: Boolean,
        val output: String,
        val errorMessage: String? = null
    )
    
    /**
     * 获取系统PATH中的ADB路径
     */
    fun getSystemAdbPath(): String? {
        return try {
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")
            val cmd = if (isWindows) arrayOf("where", "adb") else arrayOf("which", "adb")
            
            val process = ProcessBuilder(*cmd)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readLine()
            process.waitFor()
            output?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            println("Failed to get system ADB path: ${e.message}")
            null
        }
    }
    
    /**
     * 统一的命令执行方法，支持更好的错误处理
     */
    private suspend fun executeCommand(vararg args: String): AdbResult {
        return withContext(Dispatchers.IO) {
            try {
                val adbPath = AdbPathManager.getAdbPath() 
                    ?: return@withContext AdbResult(false, "", "ADB path not found")
                
                val fullCmd = mutableListOf(adbPath).apply { addAll(args) }
                val process = ProcessBuilder(fullCmd)
                    .redirectErrorStream(true)
                    .start()
                
                val output = process.inputStream.bufferedReader().readText().trim()
                val exitCode = process.waitFor()
                
                if (exitCode == 0) {
                    AdbResult(true, output)
                } else {
                    AdbResult(false, output, "Command failed with exit code: $exitCode")
                }
            } catch (e: Exception) {
                AdbResult(false, "", "Execution failed: ${e.message}")
            }
        }
    }
    
    /**
     * 同步版本的命令执行（向后兼容）
     */
    private fun runCommand(vararg args: String): String {
        return try {
            val adbPath = AdbPathManager.currentAdbPath ?: "adb"
            val fullCmd = mutableListOf(adbPath).apply { addAll(args) }
            val process = ProcessBuilder(fullCmd).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.trim()
        } catch (e: Exception) {
            "Execution failed: ${e.message}"
        }
    }

    /**
     * 执行Shell命令
     */
    fun execShell(command: String, deviceId: String? = selectDeviceId): String {
        val args = buildDeviceArgs(deviceId) + listOf("shell", command)
        return runCommand(*args.toTypedArray())
    }
    
    /**
     * 执行Shell命令的异步版本
     */
    suspend fun execShellAsync(command: String, deviceId: String? = selectDeviceId): AdbResult {
        val args = buildDeviceArgs(deviceId) + listOf("shell", command)
        return executeCommand(*args.toTypedArray())
    }
    
    /**
     * 向后兼容的exec方法
     */
    fun exec(command: String): String = execShell(command)
    
    /**
     * 构建设备参数
     */
    private fun buildDeviceArgs(deviceId: String?): List<String> {
        return if (!deviceId.isNullOrBlank()) listOf("-s", deviceId) else emptyList()
    }
    
    /**
     * 获取已连接的设备列表
     */
    suspend fun getConnectedDevices(): List<String> {
        val result = executeCommand("devices")
        return if (result.success) {
            result.output.lines().drop(1).mapNotNull {
                val parts = it.trim().split("\\s+".toRegex())
                if (parts.size >= 2 && parts[1] == "device") parts[0] else null
            }
        } else {
            emptyList()
        }
    }

    /**
     * 截图功能 - 异步版本
     */
    suspend fun takeScreenshotAsync(savePath: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        
        return try {
            val remote = "/sdcard/screen_${System.currentTimeMillis()}.png"
            val args = buildDeviceArgs(deviceId)
            
            // 截图
            val screencapResult = executeCommand(*(args + listOf("shell", "screencap", "-p", remote)).toTypedArray())
            if (!screencapResult.success) {
                return screencapResult
            }
            
            // 拉取文件
            val pullResult = executeCommand(*(args + listOf("pull", remote, savePath)).toTypedArray())
            
            // 清理临时文件
            executeCommand(*(args + listOf("shell", "rm", remote)).toTypedArray())
            
            pullResult
        } catch (e: Exception) {
            AdbResult(false, "", "Screenshot failed: ${e.message}")
        }
    }
    
    /**
     * 截图功能 - 同步版本（向后兼容）
     */
    fun takeScreenshot(savePath: String, deviceId: String? = selectDeviceId): Boolean {
        if (deviceId.isNullOrBlank()) return false
        return try {
            val remote = "/sdcard/screen.png"
            val args = buildDeviceArgs(deviceId)
            runCommand(*(args + listOf("shell", "screencap", "-p", remote)).toTypedArray())
            val result = runCommand(*(args + listOf("pull", remote, savePath)).toTypedArray())
            runCommand(*(args + listOf("shell", "rm", remote)).toTypedArray())
            result.contains("pulled") || result.contains("1 file pulled")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 拉取文件
     */
    suspend fun pullFileAsync(devicePath: String, localPath: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        val args = buildDeviceArgs(deviceId) + listOf("pull", devicePath, localPath)
        return executeCommand(*args.toTypedArray())
    }
    
    /**
     * 拉取文件 - 同步版本（向后兼容）
     */
    fun pullFile(devicePath: String, localPath: String, deviceId: String? = selectDeviceId): Boolean {
        if (deviceId.isNullOrBlank()) return false
        val args = buildDeviceArgs(deviceId) + listOf("pull", devicePath, localPath)
        val result = runCommand(*args.toTypedArray())
        return result.contains("pulled") || result.contains("1 file pulled")
    }
    
    /**
     * 安装APK - 异步版本
     */
    suspend fun installApkAsync(apkPath: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        val args = buildDeviceArgs(deviceId) + listOf("install", "-r", apkPath)
        return executeCommand(*args.toTypedArray())
    }
    
    /**
     * 安装APK - 同步版本（向后兼容）
     */
    fun installApk(apkPath: String, deviceId: String? = selectDeviceId): Boolean {
        if (deviceId.isNullOrBlank()) return false
        val args = buildDeviceArgs(deviceId) + listOf("install", "-r", apkPath)
        val output = runCommand(*args.toTypedArray())
        return output.contains("Success")
    }
    
    /**
     * 卸载应用 - 异步版本
     */
    suspend fun uninstallAppAsync(packageName: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        val args = buildDeviceArgs(deviceId) + listOf("uninstall", packageName)
        return executeCommand(*args.toTypedArray())
    }
    
    /**
     * 卸载应用 - 同步版本（向后兼容）
     */
    fun uninstallApp(packageName: String, deviceId: String? = selectDeviceId): Boolean {
        if (deviceId.isNullOrBlank()) return false
        val args = buildDeviceArgs(deviceId) + listOf("uninstall", packageName)
        val output = runCommand(*args.toTypedArray())
        return output.contains("Success")
    }

    /**
     * 输入文本 - 异步版本
     */
    suspend fun inputTextAsync(text: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        val processedText = text.replace(" ", "%s")
        val args = buildDeviceArgs(deviceId) + listOf("shell", "input", "text", processedText)
        return executeCommand(*args.toTypedArray())
    }
    
    /**
     * 输入文本 - 同步版本（向后兼容）
     */
    fun inputText(text: String, deviceId: String? = selectDeviceId): Boolean {
        if (deviceId.isNullOrBlank()) return false
        val processedText = text.replace(" ", "%s")
        val args = buildDeviceArgs(deviceId) + listOf("shell", "input", "text", processedText)
        val output = runCommand(*args.toTypedArray())
        return output.isNotBlank() && !output.contains("error")
    }
    
    /**
     * 获取当前活动 - 异步版本
     */
    suspend fun getCurrentActivityAsync(deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        val args = buildDeviceArgs(deviceId) + listOf("shell", "dumpsys", "activity", "activities")
        val result = executeCommand(*args.toTypedArray())
        
        if (result.success) {
            val regex = Regex("""ResumedActivity:.* ([\w\.]+\/[\w\.\$]+)""")
            val activity = regex.find(result.output)?.groups?.get(1)?.value
            return AdbResult(true, activity ?: "", null)
        }
        return result
    }
    
    /**
     * 获取当前活动 - 同步版本（向后兼容）
     */
    fun getCurrentActivity(deviceId: String? = selectDeviceId): String? {
        val result = runCommand("-s", deviceId ?: return null, "shell", "dumpsys", "activity", "activities")
        val regex = Regex("""ResumedActivity:.* ([\w\.]+\/[\w\.\$]+)""")
        return regex.find(result)?.groups?.get(1)?.value
    }
    
    /**
     * 启动应用 - 异步版本
     */
    suspend fun startAppAsync(packageName: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        
        // 首先获取启动组件
        val componentResult = execShellAsync("cmd package resolve-activity --brief $packageName", deviceId)
        if (!componentResult.success) {
            return componentResult
        }
        
        val component = componentResult.output.lines().lastOrNull()
        if (component.isNullOrBlank()) {
            return AdbResult(false, "", "No launch activity found for package: $packageName")
        }
        
        // 启动应用
        return execShellAsync("am start -n $component", deviceId)
    }
    
    /**
     * 启动应用 - 同步版本（向后兼容）
     */
    fun startApp(packageName: String, deviceId: String? = selectDeviceId): Boolean {
        val component = execShell("cmd package resolve-activity --brief $packageName", deviceId).lines().lastOrNull()
        return if (!component.isNullOrBlank()) {
            execShell("am start -n $component", deviceId)
            true
        } else false
    }
    
    /**
     * 停止应用 - 异步版本
     */
    suspend fun stopAppAsync(packageName: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        return execShellAsync("am force-stop $packageName", deviceId)
    }
    
    /**
     * 停止应用 - 同步版本（向后兼容）
     */
    fun stopApp(packageName: String, deviceId: String? = selectDeviceId): Boolean {
        execShell("am force-stop $packageName", deviceId)
        return true
    }
    
    /**
     * 清除应用数据 - 异步版本
     */
    suspend fun clearAppDataAsync(packageName: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        return execShellAsync("pm clear $packageName", deviceId)
    }
    
    /**
     * 清除应用数据 - 同步版本（向后兼容）
     */
    fun clearAppData(packageName: String, deviceId: String? = selectDeviceId): Boolean {
        return execShell("pm clear $packageName", deviceId).contains("Success")
    }

    /**
     * 重置应用权限 - 异步版本
     */
    suspend fun resetPermissionsAsync(packageName: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        return execShellAsync("pm reset-permissions $packageName", deviceId)
    }
    
    /**
     * 重置应用权限 - 同步版本（向后兼容）
     */
    fun resetPermissions(packageName: String, deviceId: String? = selectDeviceId): Boolean {
        if (deviceId.isNullOrBlank()) return false
        val output = execShell("pm reset-permissions $packageName", deviceId)
        return !output.contains("error") && !output.contains("Exception")
    }
    
    /**
     * 授予所有权限 - 异步版本
     */
    suspend fun grantAllPermissionsAsync(packageName: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        
        // 获取应用的所有权限
        val permissionsResult = execShellAsync("dumpsys package $packageName", deviceId)
        if (!permissionsResult.success) {
            return permissionsResult
        }
        
        val permissions = Regex("android\\.permission\\.[A-Z_\\.]+").findAll(permissionsResult.output)
            .map { it.value }.toSet()
        
        if (permissions.isEmpty()) {
            return AdbResult(true, "No permissions found to grant", null)
        }
        
        // 逐个授予权限
        val results = mutableListOf<String>()
        for (permission in permissions) {
            val grantResult = execShellAsync("pm grant $packageName $permission", deviceId)
            results.add("$permission: ${if (grantResult.success) "granted" else "failed"}")
        }
        
        return AdbResult(true, results.joinToString("\n"), null)
    }
    
    /**
     * 授予所有权限 - 同步版本（向后兼容）
     */
    fun grantAllPermissions(packageName: String, deviceId: String? = selectDeviceId): Boolean {
        if (deviceId.isNullOrBlank()) return false
        
        val result = execShell("dumpsys package $packageName", deviceId)
        val permissions = Regex("android\\.permission\\.[A-Z_\\.]+").findAll(result).map { it.value }.toSet()
        
        return permissions.all { permission ->
            val grantResult = execShell("pm grant $packageName $permission", deviceId)
            !grantResult.contains("Exception") && !grantResult.contains("error")
        }
    }
    
    /**
     * 获取应用路径 - 异步版本
     */
    suspend fun getAppPathAsync(packageName: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        val result = execShellAsync("pm path $packageName", deviceId)
        if (result.success) {
            val path = result.output.substringAfter("package:").trim()
            return AdbResult(true, path, null)
        }
        return result
    }
    
    /**
     * 获取应用路径 - 同步版本（向后兼容）
     */
    fun getAppPath(packageName: String, deviceId: String? = selectDeviceId): String {
        if (deviceId.isNullOrBlank()) return ""
        val output = execShell("pm path $packageName", deviceId)
        return output.substringAfter("package:").trim()
    }
    
    /**
     * 获取安装时间 - 异步版本
     */
    suspend fun getInstallTimeAsync(packageName: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        val result = execShellAsync("dumpsys package $packageName | grep firstInstallTime", deviceId)
        if (result.success) {
            val time = result.output.substringAfter("firstInstallTime=").trim()
            return AdbResult(true, time, null)
        }
        return result
    }
    
    /**
     * 获取安装时间 - 同步版本（向后兼容）
     */
    fun getInstallTime(packageName: String, deviceId: String? = selectDeviceId): String {
        if (deviceId.isNullOrBlank()) return ""
        val output = execShell("dumpsys package $packageName | grep firstInstallTime", deviceId)
        return output.substringAfter("firstInstallTime=").trim()
    }
    
    /**
     * 获取更新时间 - 异步版本
     */
    suspend fun getUpdateTimeAsync(packageName: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        val result = execShellAsync("dumpsys package $packageName | grep lastUpdateTime", deviceId)
        if (result.success) {
            val time = result.output.substringAfter("lastUpdateTime=").trim()
            return AdbResult(true, time, null)
        }
        return result
    }
    
    /**
     * 获取更新时间 - 同步版本（向后兼容）
     */
    fun getUpdateTime(packageName: String, deviceId: String? = selectDeviceId): String {
        if (deviceId.isNullOrBlank()) return ""
        val output = execShell("dumpsys package $packageName | grep lastUpdateTime", deviceId)
        return output.substringAfter("lastUpdateTime=").trim()
    }

    /**
     * 检查是否为系统应用 - 异步版本
     */
    suspend fun isSystemAppAsync(packageName: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        val result = execShellAsync("dumpsys package $packageName", deviceId)
        if (result.success) {
            val isSystem = result.output.contains("flags=[ SYSTEM") || result.output.contains("SYSTEM")
            return AdbResult(true, isSystem.toString(), null)
        }
        return result
    }
    
    /**
     * 检查是否为系统应用 - 同步版本（向后兼容）
     */
    fun isSystemApp(packageName: String, deviceId: String? = selectDeviceId): Boolean {
        if (deviceId.isNullOrBlank()) return false
        val output = execShell("dumpsys package $packageName", deviceId)
        return output.contains("flags=[ SYSTEM") || output.contains("SYSTEM")
    }
    
    /**
     * 获取支持的ABI - 异步版本
     */
    suspend fun getSupportedAbisAsync(deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        return execShellAsync("getprop ro.product.cpu.abilist", deviceId)
    }
    
    /**
     * 获取支持的ABI - 同步版本（向后兼容）
     */
    fun getSupportedAbis(deviceId: String? = selectDeviceId): String {
        if (deviceId.isNullOrBlank()) return ""
        return execShell("getprop ro.product.cpu.abilist", deviceId)
    }
    
    /**
     * 获取目标SDK版本 - 异步版本
     */
    suspend fun getTargetSdkVersionAsync(packageName: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        val result = execShellAsync("dumpsys package $packageName", deviceId)
        if (result.success) {
            val targetSdk = result.output.lines()
                .find { it.contains("targetSdk") }
                ?.substringAfter("targetSdk=")
                ?.substringBefore(" ")
                ?.trim() ?: ""
            return AdbResult(true, targetSdk, null)
        }
        return result
    }
    
    /**
     * 获取目标SDK版本 - 同步版本（向后兼容）
     */
    fun getTargetSdkVersion(packageName: String, deviceId: String? = selectDeviceId): String {
        if (deviceId.isNullOrBlank()) return ""
        val output = execShell("dumpsys package $packageName", deviceId)
        return output.lines()
            .find { it.contains("targetSdk") }
            ?.substringAfter("targetSdk=")
            ?.substringBefore(" ")
            ?.trim() ?: ""
    }
    
    /**
     * 获取最小SDK版本 - 异步版本
     */
    suspend fun getMinSdkVersionAsync(packageName: String, deviceId: String? = selectDeviceId): AdbResult {
        if (deviceId.isNullOrBlank()) {
            return AdbResult(false, "", "Device ID is required")
        }
        val result = execShellAsync("dumpsys package $packageName", deviceId)
        if (result.success) {
            val minSdk = result.output.lines()
                .find { it.contains("minSdk") }
                ?.substringAfter("minSdk=")
                ?.substringBefore(" ")
                ?.trim() ?: ""
            return AdbResult(true, minSdk, null)
        }
        return result
    }
    
    /**
     * 获取最小SDK版本 - 同步版本（向后兼容）
     */
    fun getMinSdkVersion(packageName: String, deviceId: String? = selectDeviceId): String {
        if (deviceId.isNullOrBlank()) return ""
        val output = execShell("dumpsys package $packageName", deviceId)
        return output.lines()
            .find { it.contains("minSdk") }
            ?.substringAfter("minSdk=")
            ?.substringBefore(" ")
            ?.trim() ?: ""
    }
    
    /**
     * 运行ADB命令并显示结果（UI辅助方法）
     */
    fun runAdbAndShowResult(
        coroutineScope: CoroutineScope,
        command: String,
        setDialogText: (String) -> Unit,
        setShowDialog: (Boolean) -> Unit
    ) {
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) { 
                try {
                    exec(command)
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }
            }
            setDialogText(result.ifBlank { "No result returned" })
            setShowDialog(true)
        }
    }
    
    /**
     * 执行ADB命令（已弃用，建议使用executeCommand）
     */
    @Deprecated("Use executeCommand instead", ReplaceWith("executeCommand(*args)"))
    suspend fun executeAdbCommand(vararg args: String): String {
        val result = executeCommand(*args)
        return if (result.success) result.output else result.errorMessage ?: "Unknown error"
    }
    
    /**
     * 断开设备连接
     */
    suspend fun disconnectDevice(deviceId: String): AdbResult {
        return executeCommand("disconnect", deviceId)
    }

}
