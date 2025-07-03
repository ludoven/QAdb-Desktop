package com.ludoven.adbtool.util

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.openFilePicker
import java.io.File

import javax.swing.JFileChooser


object AdbPathManager {

    private val configFile = File(System.getProperty("user.home"), ".adb_path_config")

    // 供外部访问的当前路径，只读
    val currentAdbPath: String?
        get() = cachedAdbPath

    // 内部缓存路径
    private var cachedAdbPath: String? = null

    private val commonAdbPaths = listOf(
        "/Users/${System.getProperty("user.name")}/Library/Android/sdk/platform-tools/adb",
        "/opt/homebrew/bin/adb",
        "/usr/local/bin/adb",
        "C:\\Users\\${System.getProperty("user.name")}\\AppData\\Local\\Android\\sdk\\platform-tools\\adb",
        "C:\\Program Files (x86)\\Android\\android-sdk\\platform-tools\\adb"
    )

    /**
     * 获取有效的 adb 路径，按以下顺序：
     * 1. 缓存路径
     * 2. 本地配置文件路径
     * 3. 系统 PATH 中的 adb
     * 4. 常见路径列表
     * 5. 用户手动选择
     */
   suspend fun getAdbPath(): String? {
        // 1. 使用缓存
        cachedAdbPath?.takeIf { isValidAdb(it) }?.let { return it }

        // 2. 读取配置文件
        if (configFile.exists()) {
            val saved = configFile.readText().trim()
            if (isValidAdb(saved)) {
                setAdbPath(saved)
                return saved
            }
        }

        // 3. 系统 PATH 中查找
        getAdbFromSystemPath()?.let {
            setAdbPath(it)
            return it
        }

        // 4. 常见路径
        commonAdbPaths.firstOrNull { isValidAdb(it) }?.let {
            setAdbPath(it)
            return it
        }

        // 5. 用户手动选择
        showAdbPickerDialog()?.let {
            setAdbPath(it)
            return it
        }

        return null
    }

    fun setAdbPath(path: String) {
        if (isValidAdb(path)) {
            cachedAdbPath = path
            configFile.writeText(path)
        }
    }

    fun reset() {
        cachedAdbPath = null
        configFile.delete()
    }

    private fun isValidAdb(path: String): Boolean {
        val file = File(path)
        return file.exists() && file.canExecute()
    }

    private fun getAdbFromSystemPath(): String? {
        return try {
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")
            val cmd = if (isWindows) arrayOf("where", "adb") else arrayOf("which", "adb")
            val process = ProcessBuilder(*cmd).start()
            val result = process.inputStream.bufferedReader().readLine()
            result?.takeIf { isValidAdb(it) }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun showAdbPickerDialog(): String? {
        val openFilePicker = FileKit.openFilePicker()
        return openFilePicker?.absolutePath().takeIf { isValidAdb(it.toString()) }
    }
}

