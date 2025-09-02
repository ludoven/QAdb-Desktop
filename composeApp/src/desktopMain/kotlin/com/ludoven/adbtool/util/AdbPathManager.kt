package com.ludoven.adbtool.util

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.openFilePicker
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * ADB路径管理器
 * 负责自动检测、缓存和管理ADB可执行文件的路径
 */
object AdbPathManager {

    private val configFile = File(System.getProperty("user.home"), ".adb_path_config")

    /**
     * 供外部访问的当前ADB路径，只读
     */
    val currentAdbPath: String?
        get() = cachedAdbPath

    // 内部缓存路径
    private var cachedAdbPath: String? = null

    /**
     * 常见的ADB安装路径列表
     */
    private val commonAdbPaths: List<String>
        get() {
            val userName = System.getProperty("user.name")
            val osName = System.getProperty("os.name").lowercase()
            
            return when {
                osName.contains("mac") -> listOf(
                    "/Users/$userName/Library/Android/sdk/platform-tools/adb",
                    "/opt/homebrew/bin/adb",
                    "/usr/local/bin/adb",
                    "/Applications/Android Studio.app/Contents/plugins/android/lib/android.jar/../../../bin/adb"
                )
                osName.contains("windows") -> listOf(
                    "C:\\Users\\$userName\\AppData\\Local\\Android\\sdk\\platform-tools\\adb.exe",
                    "C:\\Program Files (x86)\\Android\\android-sdk\\platform-tools\\adb.exe",
                    "C:\\Android\\sdk\\platform-tools\\adb.exe"
                )
                else -> listOf( // Linux
                    "/home/$userName/Android/Sdk/platform-tools/adb",
                    "/usr/local/bin/adb",
                    "/usr/bin/adb"
                )
            }
        }

    /**
     * 获取有效的ADB路径，按以下优先级顺序：
     * 1. 缓存路径（内存中已验证的路径）
     * 2. 本地配置文件路径（用户之前设置的路径）
     * 3. 系统PATH环境变量中的adb
     * 4. 常见安装路径列表
     * 5. 用户手动选择文件
     * 
     * @return 有效的ADB路径，如果未找到则返回null
     */
    suspend fun getAdbPath(): String? = withContext(Dispatchers.IO) {
        try {
            // 1. 检查缓存路径
            cachedAdbPath?.takeIf { isValidAdb(it) }?.let { 
                println("使用缓存的ADB路径: $it")
                return@withContext it 
            }

            // 2. 读取配置文件
            val savedPath = readSavedPath()
            if (savedPath != null && isValidAdb(savedPath)) {
                println("使用配置文件中的ADB路径: $savedPath")
                setAdbPath(savedPath)
                return@withContext savedPath
            }

            // 3. 系统PATH中查找
            val systemPath = getAdbFromSystemPath()
            if (systemPath != null) {
                println("在系统PATH中找到ADB: $systemPath")
                setAdbPath(systemPath)
                return@withContext systemPath
            }

            // 4. 常见路径检查
            val commonPath = findAdbInCommonPaths()
            if (commonPath != null) {
                println("在常见路径中找到ADB: $commonPath")
                setAdbPath(commonPath)
                return@withContext commonPath
            }

            // 5. 用户手动选择
            val userSelectedPath = showAdbPickerDialog()
            if (userSelectedPath != null) {
                println("用户选择的ADB路径: $userSelectedPath")
                setAdbPath(userSelectedPath)
                return@withContext userSelectedPath
            }

            println("未找到有效的ADB路径")
            null
        } catch (e: Exception) {
            println("获取ADB路径时发生错误: ${e.message}")
            null
        }
    }

    /**
     * 设置ADB路径并保存到配置文件
     * @param path ADB可执行文件的路径
     * @return 是否设置成功
     */
    fun setAdbPath(path: String): Boolean {
        return try {
            if (isValidAdb(path)) {
                cachedAdbPath = path
                configFile.writeText(path)
                println("ADB路径已设置: $path")
                true
            } else {
                println("无效的ADB路径: $path")
                false
            }
        } catch (e: IOException) {
            println("保存ADB路径配置失败: ${e.message}")
            false
        }
    }

    /**
     * 重置ADB路径配置
     */
    fun reset() {
        try {
            cachedAdbPath = null
            if (configFile.exists()) {
                configFile.delete()
                println("ADB路径配置已重置")
            }
        } catch (e: IOException) {
            println("重置ADB路径配置失败: ${e.message}")
        }
    }

    /**
     * 从配置文件读取保存的路径
     */
    private fun readSavedPath(): String? {
        return try {
            if (configFile.exists()) {
                configFile.readText().trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
        } catch (e: IOException) {
            println("读取ADB路径配置失败: ${e.message}")
            null
        }
    }

    /**
     * 在常见路径中查找ADB
     */
    private fun findAdbInCommonPaths(): String? {
        return commonAdbPaths.firstOrNull { path ->
            try {
                isValidAdb(path)
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * 验证给定路径是否为有效的ADB可执行文件
     * @param path 要验证的文件路径
     * @return 是否为有效的ADB文件
     */
    private fun isValidAdb(path: String): Boolean {
        return try {
            if (path.isBlank()) return false
            
            val file = File(path)
            val isValid = file.exists() && file.canExecute() && 
                         (file.name == "adb" || file.name == "adb.exe")
            
            if (isValid) {
                // 进一步验证：尝试执行adb version命令
                val process = ProcessBuilder(path, "version")
                    .redirectErrorStream(true)
                    .start()
                
                val exitCode = process.waitFor()
                val output = process.inputStream.bufferedReader().readText()
                
                exitCode == 0 && output.contains("Android Debug Bridge")
            } else {
                false
            }
        } catch (e: Exception) {
            println("验证ADB路径时发生错误: ${e.message}")
            false
        }
    }

    /**
     * 从系统PATH环境变量中查找ADB
     * @return 找到的ADB路径，如果未找到则返回null
     */
    private fun getAdbFromSystemPath(): String? {
        return try {
            val osName = System.getProperty("os.name").lowercase()
            val isWindows = osName.contains("windows")
            
            val cmd = if (isWindows) {
                arrayOf("where", "adb")
            } else {
                arrayOf("which", "adb")
            }
            
            val process = ProcessBuilder(*cmd)
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                val result = process.inputStream.bufferedReader().readLine()?.trim()
                result?.takeIf { it.isNotEmpty() && isValidAdb(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            println("从系统PATH查找ADB时发生错误: ${e.message}")
            null
        }
    }

    /**
     * 显示文件选择对话框让用户手动选择ADB文件
     * @return 用户选择的ADB路径，如果取消则返回null
     */
    private suspend fun showAdbPickerDialog(): String? {
        return try {
            withContext(Dispatchers.Main) {
                val selectedFile = FileKit.openFilePicker()
                selectedFile?.absolutePath()?.toString()?.takeIf { path ->
                    isValidAdb(path)
                }
            }
        } catch (e: Exception) {
            println("文件选择对话框错误: ${e.message}")
            null
        }
    }
}

