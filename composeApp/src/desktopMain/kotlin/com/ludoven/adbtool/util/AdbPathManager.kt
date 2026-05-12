package com.ludoven.adbtool.util

import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.Properties
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * ADB 路径管理器。
 *
 * 默认自动检测系统 ADB；系统不可用时回退到 QADB 内置 ADB。
 */
object AdbPathManager {

    enum class AdbSource {
        SYSTEM,
        BUNDLED,
        CUSTOM,
        NONE
    }

    data class AdbEnvironment(
        val path: String? = null,
        val source: AdbSource = AdbSource.NONE,
        val version: String? = null,
        val isReady: Boolean = false,
        val message: String? = null
    )

    private data class SavedPreference(
        val source: AdbSource?,
        val path: String?
    )

    private data class ValidationResult(
        val isValid: Boolean,
        val version: String? = null,
        val error: String? = null
    )

    private val configFile = File(System.getProperty("user.home"), ".adb_path_config")
    private const val HELP_URL = "https://ludoven.github.io/QADB/guide/getting-started.html"
    private const val FRIENDLY_INIT_ERROR = "ADB 初始化失败，请检查 QADB 是否有执行权限，或手动选择 adb 路径。"

    private val _adbEnvironment = MutableStateFlow(
        AdbEnvironment(message = "正在检测 ADB 环境...")
    )
    val adbEnvironment: StateFlow<AdbEnvironment> = _adbEnvironment.asStateFlow()

    val currentAdbPath: String?
        get() = _adbEnvironment.value.path

    private val commonAdbPaths: List<String>
        get() {
            val userName = System.getProperty("user.name")
            val osName = System.getProperty("os.name").lowercase()

            return when {
                osName.contains("mac") -> listOf(
                    "/Users/$userName/Library/Android/sdk/platform-tools/adb",
                    "/opt/homebrew/bin/adb",
                    "/usr/local/bin/adb"
                )
                osName.contains("windows") -> listOf(
                    "C:\\Users\\$userName\\AppData\\Local\\Android\\sdk\\platform-tools\\adb.exe",
                    "C:\\Program Files (x86)\\Android\\android-sdk\\platform-tools\\adb.exe",
                    "C:\\Android\\sdk\\platform-tools\\adb.exe"
                )
                else -> listOf(
                    "/home/$userName/Android/Sdk/platform-tools/adb",
                    "/usr/local/bin/adb",
                    "/usr/bin/adb"
                )
            }
        }

    suspend fun getAdbPath(): String? = withContext(Dispatchers.IO) {
        detectAdbEnvironment(preferSavedPreference = true).path
    }

    suspend fun detectAdbEnvironment(preferSavedPreference: Boolean = true): AdbEnvironment = withContext(Dispatchers.IO) {
        val environment = resolveEnvironment(preferSavedPreference)
        _adbEnvironment.value = environment
        environment
    }

    suspend fun autoDetect(): AdbEnvironment = withContext(Dispatchers.IO) {
        deleteSavedPreference()
        val environment = resolveEnvironment(preferSavedPreference = false)
        _adbEnvironment.value = environment
        environment
    }

    suspend fun useBundledAdb(): AdbEnvironment = withContext(Dispatchers.IO) {
        val environment = bundledAdbEnvironment()
        if (environment.isReady) {
            writeSavedPreference(AdbSource.BUNDLED, environment.path)
        }
        _adbEnvironment.value = environment
        environment
    }

    fun setAdbPath(path: String): Boolean {
        val validation = validateAdb(path)
        return if (validation.isValid) {
            writeSavedPreference(AdbSource.CUSTOM, path)
            _adbEnvironment.value = AdbEnvironment(
                path = path,
                source = AdbSource.CUSTOM,
                version = validation.version,
                isReady = true
            )
            true
        } else {
            _adbEnvironment.value = AdbEnvironment(
                path = path.takeIf { it.isNotBlank() },
                source = AdbSource.CUSTOM,
                isReady = false,
                message = friendlyInitializationError(validation.error)
            )
            false
        }
    }

    fun reset() {
        deleteSavedPreference()
        _adbEnvironment.value = AdbEnvironment(message = "已恢复自动检测 ADB。")
    }

    fun friendlyInitializationError(rawError: String?): String {
        return FRIENDLY_INIT_ERROR
    }

    fun sourceDisplayName(source: AdbSource): String {
        return when (source) {
            AdbSource.SYSTEM -> "系统 ADB"
            AdbSource.BUNDLED -> "QADB 内置 ADB"
            AdbSource.CUSTOM -> "自定义 ADB"
            AdbSource.NONE -> "未就绪"
        }
    }

    fun openHelp(): Result<Unit> = runCatching {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(HELP_URL))
        } else {
            throw IllegalStateException("当前系统不支持打开浏览器。")
        }
    }

    fun resolveBundledAdbPath(
        resourceRoot: File,
        osName: String = System.getProperty("os.name"),
        osArch: String = System.getProperty("os.arch")
    ): File? {
        val normalizedOs = osName.lowercase()
        val normalizedArch = osArch.lowercase()
        val relativePath = when {
            normalizedOs.contains("mac") -> {
                val archDir = if (
                    normalizedArch.contains("aarch64") ||
                    normalizedArch.contains("arm64")
                ) {
                    "arm64"
                } else {
                    "x64"
                }
                "adb/macos/$archDir/adb"
            }
            normalizedOs.contains("windows") -> "adb/windows/adb.exe"
            normalizedOs.contains("linux") -> "adb/linux/adb"
            else -> return null
        }
        return File(resourceRoot, relativePath)
    }

    private fun resolveEnvironment(preferSavedPreference: Boolean): AdbEnvironment {
        if (preferSavedPreference) {
            resolveSavedPreference()?.let { return it }
        }

        systemAdbEnvironment()?.let { return it }
        return bundledAdbEnvironment()
    }

    private fun resolveSavedPreference(): AdbEnvironment? {
        val saved = readSavedPreference() ?: return null
        return when (saved.source) {
            AdbSource.CUSTOM -> {
                val path = saved.path ?: return null
                val validation = validateAdb(path)
                if (validation.isValid) {
                    AdbEnvironment(path, AdbSource.CUSTOM, validation.version, isReady = true)
                } else {
                    AdbEnvironment(
                        path = path,
                        source = AdbSource.CUSTOM,
                        isReady = false,
                        message = friendlyInitializationError(validation.error)
                    )
                }
            }
            AdbSource.BUNDLED -> bundledAdbEnvironment()
            else -> null
        }
    }

    private fun systemAdbEnvironment(): AdbEnvironment? {
        val systemPath = getAdbFromSystemPath()
            ?: findAdbInCommonPaths()
            ?: return null
        val validation = validateAdb(systemPath)
        return if (validation.isValid) {
            AdbEnvironment(systemPath, AdbSource.SYSTEM, validation.version, isReady = true)
        } else {
            null
        }
    }

    private fun bundledAdbEnvironment(): AdbEnvironment {
        val bundledPath = findBundledResourceRoots()
            .mapNotNull { resolveBundledAdbPath(it) }
            .firstOrNull { it.exists() }

        if (bundledPath == null) {
            return AdbEnvironment(
                source = AdbSource.NONE,
                isReady = false,
                message = friendlyInitializationError("Bundled ADB not found")
            )
        }

        ensureExecutableIfPossible(bundledPath)
        val validation = validateAdb(bundledPath.absolutePath)
        return if (validation.isValid) {
            AdbEnvironment(
                path = bundledPath.absolutePath,
                source = AdbSource.BUNDLED,
                version = validation.version,
                isReady = true
            )
        } else {
            AdbEnvironment(
                path = bundledPath.absolutePath,
                source = AdbSource.BUNDLED,
                isReady = false,
                message = friendlyInitializationError(validation.error)
            )
        }
    }

    private fun findBundledResourceRoots(): List<File> {
        val roots = mutableListOf<File>()
        val resource = Thread.currentThread().contextClassLoader.getResource("adb")
        if (resource != null && resource.protocol == "file") {
            runCatching { File(resource.toURI()).parentFile }
                .getOrNull()
                ?.let { roots += it }
        }
        roots += File(System.getProperty("user.dir"), "composeApp/src/desktopMain/resources")
        roots += File(System.getProperty("user.dir"), "src/desktopMain/resources")
        return roots.distinctBy { it.absolutePath }
    }

    private fun ensureExecutableIfPossible(file: File) {
        if (isWindows()) return
        if (file.exists() && !file.canExecute()) {
            runCatching { file.setExecutable(true, false) }
        }
    }

    private fun findAdbInCommonPaths(): String? {
        return commonAdbPaths.firstOrNull { path ->
            validateAdb(path).isValid
        }
    }

    private fun validateAdb(path: String): ValidationResult {
        return try {
            if (path.isBlank()) return ValidationResult(false, error = "ADB path is blank")

            val file = File(path)
            val isExecutableFile = file.exists() &&
                file.canExecute() &&
                (file.name == "adb" || file.name == "adb.exe")

            if (!isExecutableFile) {
                return ValidationResult(false, error = "ADB file is missing or not executable")
            }

            val process = ProcessBuilder(path, "version")
                .redirectErrorStream(true)
                .start()
            val completed = process.waitFor(5, TimeUnit.SECONDS)
            val output = process.inputStream.bufferedReader().readText()

            if (!completed) {
                process.destroyForcibly()
                return ValidationResult(false, error = "ADB version check timed out")
            }

            if (process.exitValue() == 0 && output.contains("Android Debug Bridge")) {
                ValidationResult(true, version = output.lines().firstOrNull { it.isNotBlank() }?.trim())
            } else {
                ValidationResult(false, error = output.ifBlank { "ADB version check failed" })
            }
        } catch (e: Exception) {
            ValidationResult(false, error = e.message)
        }
    }

    private fun getAdbFromSystemPath(): String? {
        return try {
            val cmd = if (isWindows()) arrayOf("where", "adb") else arrayOf("which", "adb")
            val process = ProcessBuilder(*cmd)
                .redirectErrorStream(true)
                .start()
            val completed = process.waitFor(5, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return null
            }
            if (process.exitValue() == 0) {
                process.inputStream.bufferedReader()
                    .readLines()
                    .firstOrNull { it.isNotBlank() }
                    ?.trim()
                    ?.takeIf { validateAdb(it).isValid }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun readSavedPreference(): SavedPreference? {
        return try {
            if (!configFile.exists()) return null
            val raw = configFile.readText().trim()
            if (raw.isBlank()) return null

            if (!raw.contains("=")) {
                return SavedPreference(AdbSource.CUSTOM, raw)
            }

            val properties = Properties()
            properties.load(raw.byteInputStream())
            SavedPreference(
                source = properties.getProperty("source")?.let { runCatching { AdbSource.valueOf(it) }.getOrNull() },
                path = properties.getProperty("path")?.takeIf { it.isNotBlank() }
            )
        } catch (e: IOException) {
            null
        }
    }

    private fun writeSavedPreference(source: AdbSource, path: String?) {
        try {
            val properties = Properties()
            properties.setProperty("source", source.name)
            if (!path.isNullOrBlank()) {
                properties.setProperty("path", path)
            }
            configFile.writer().use { writer ->
                properties.store(writer, "QADB ADB environment")
            }
        } catch (e: IOException) {
            println("保存 ADB 配置失败: ${e.message}")
        }
    }

    private fun deleteSavedPreference() {
        try {
            if (configFile.exists()) {
                configFile.delete()
            }
        } catch (e: IOException) {
            println("重置 ADB 配置失败: ${e.message}")
        }
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase().contains("windows")
    }
}
