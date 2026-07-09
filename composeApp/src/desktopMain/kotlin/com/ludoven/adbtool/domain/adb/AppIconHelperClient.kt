package com.ludoven.adbtool.domain.adb

import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.util.ChildProcessRegistry
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URI
import java.util.Base64
import java.util.UUID

data class DeviceIconResult(
    val packageName: String,
    val label: String?,
    val localPath: String,
    val remotePath: String,
    val source: String,
    val cacheHit: Boolean,
    val elapsedMs: Long
)

data class DeviceIconBatchResult(
    val successes: Map<String, DeviceIconResult>,
    val failures: Map<String, String>
)

class AppIconHelperClient(
    private val localCacheRoot: File,
    private val helperJarOverride: File? = null
) {
    private val installedDevices = mutableSetOf<String>()
    private val sessions = mutableMapOf<String, IconHelperSession>()

    suspend fun fetchIcon(packageName: String, deviceId: String?, sizePx: Int = 192): Result<DeviceIconResult> {
        val batch = fetchIcons(listOf(packageName), deviceId, sizePx)
            .getOrElse { return Result.failure(it) }
        batch.successes[packageName]?.let { return Result.success(it) }
        return Result.failure(IllegalStateException(batch.failures[packageName] ?: "Icon helper returned no result"))
    }

    suspend fun fetchIcons(packageNames: List<String>, deviceId: String?, sizePx: Int = 192): Result<DeviceIconBatchResult> {
        if (deviceId.isNullOrBlank()) return Result.failure(IllegalArgumentException("Device ID is required"))
        val distinctPackages = packageNames.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (distinctPackages.isEmpty()) {
            return Result.success(DeviceIconBatchResult(emptyMap(), emptyMap()))
        }

        val helperJar = helperJarOverride ?: locateHelperJar()
        if (!helperJar.isFile) {
            return Result.failure(IllegalStateException("qadb-icon-helper jar not found. Run :qadb-icon-helper:assembleIconHelperDex."))
        }

        runCatching { pushHelperIfNeeded(helperJar, deviceId) }
            .onFailure { return Result.failure(it) }

        val startedAt = System.currentTimeMillis()
        val result = requestRemoteIcons(distinctPackages, deviceId, sizePx)
        val output = AdbTool.outputText(result)
        val parsedResults = parseHelperOutputs(output)
        if (parsedResults.isEmpty()) {
            return Result.failure(IllegalStateException(output.ifBlank { "Icon helper returned no result" }))
        }

        val deviceCacheDir = File(localCacheRoot, safeFileToken(deviceId)).also { it.mkdirs() }
        val successes = linkedMapOf<String, DeviceIconResult>()
        val failures = linkedMapOf<String, String>()
        for (parsed in parsedResults) {
            val parsedPackageName = parsed.packageName
            if (parsedPackageName.isBlank()) continue
            if (!parsed.ok) {
                failures[parsedPackageName] = parsed.reason.ifBlank { output }
                continue
            }
            val stableLocalFile = File(deviceCacheDir, localIconCacheFileName(parsedPackageName))
            val inlinePng = decodeBase64Bytes(parsed.dataBase64)
            val localPath = if (inlinePng != null) {
                val writeResult = runCatching {
                    stableLocalFile.parentFile?.mkdirs()
                    stableLocalFile.writeBytes(inlinePng)
                }
                val writeError = writeResult.exceptionOrNull()
                if (writeError != null) {
                    failures[parsedPackageName] = writeError.message ?: "Failed to write inline icon data"
                    continue
                }
                stableLocalFile.absolutePath
            } else {
                val localFile = File(deviceCacheDir, File(parsed.path).name)
                if (!localFile.isFile || localFile.length() <= 0L) {
                    val pullResult = pullRemotePng(parsed.path, localFile, deviceId)
                    if (!pullResult.success || !localFile.isFile || localFile.length() <= 0L) {
                        failures[parsedPackageName] = AdbTool.outputText(pullResult)
                        continue
                    }
                }
                if (!stableLocalFile.isFile || stableLocalFile.length() != localFile.length()) {
                    runCatching { localFile.copyTo(stableLocalFile, overwrite = true) }
                }
                stableLocalFile.takeIf { it.isFile && it.length() > 0L }?.absolutePath
                    ?: localFile.absolutePath
            }
            successes[parsedPackageName] = DeviceIconResult(
                packageName = parsedPackageName,
                label = parsed.label,
                localPath = localPath,
                remotePath = parsed.path,
                source = parsed.source,
                cacheHit = parsed.cacheHit,
                elapsedMs = parsed.elapsedMs.takeIf { it > 0 } ?: (System.currentTimeMillis() - startedAt)
            )
        }

        distinctPackages
            .filterNot { successes.containsKey(it) || failures.containsKey(it) }
            .forEach { failures[it] = "Icon helper returned no result" }

        return Result.success(DeviceIconBatchResult(successes, failures))
    }

    suspend fun clearRemoteCache(deviceId: String?): Result<Unit> {
        if (deviceId.isNullOrBlank()) return Result.failure(IllegalArgumentException("Device ID is required"))
        val helperJar = helperJarOverride ?: locateHelperJar()
        if (!helperJar.isFile) {
            return Result.failure(IllegalStateException("qadb-icon-helper jar not found. Run :qadb-icon-helper:assembleIconHelperDex."))
        }
        runCatching { pushHelperIfNeeded(helperJar, deviceId) }
            .onFailure { return Result.failure(it) }
        val command = helperCommand("clear-cache")
        val result = AdbTool.execShellAsync(command, deviceId)
        return if (result.success) Result.success(Unit) else Result.failure(IllegalStateException(AdbTool.outputText(result)))
    }

    private suspend fun pushHelperIfNeeded(helperJar: File, deviceId: String) {
        synchronized(installedDevices) {
            if (installedDevices.contains(deviceId)) return
        }
        AdbTool.execShellAsync("mkdir -p ${AdbTool.shellQuote(REMOTE_BASE_DIR)} ${AdbTool.shellQuote(REMOTE_ICON_DIR)}", deviceId)
        val pushResult = AdbTool.execAdbAsync("-s", deviceId, "push", helperJar.absolutePath, REMOTE_HELPER_JAR)
        if (!pushResult.success) {
            throw IllegalStateException(AdbTool.outputText(pushResult))
        }
        synchronized(installedDevices) {
            installedDevices.add(deviceId)
        }
    }

    private suspend fun requestRemoteIcon(packageName: String, deviceId: String, sizePx: Int): AdbTool.AdbResult {
        return AdbTool.execShellAsync(helperCommand("get-icon", packageName, sizePx.toString()), deviceId)
    }

    private suspend fun requestRemoteIcons(packageNames: List<String>, deviceId: String, sizePx: Int): AdbTool.AdbResult {
        if (isLongSessionEnabled()) {
            runCatching {
                return sessionFor(deviceId, sizePx).request(packageNames)
            }.onFailure { error ->
                closeSession(deviceId)
                println("QADB icon helper session failed device=$deviceId reason=${error.message}")
            }
        }
        return if (packageNames.size == 1) {
            requestRemoteIcon(packageNames.first(), deviceId, sizePx)
        } else {
            AdbTool.execShellAsync(helperCommand("get-icons", sizePx.toString(), *packageNames.toTypedArray()), deviceId)
        }
    }

    private suspend fun pullRemotePng(remotePath: String, localFile: File, deviceId: String): AdbTool.AdbResult {
        localFile.parentFile?.mkdirs()
        return AdbTool.pullFileAsync(remotePath, localFile.absolutePath, deviceId)
    }

    private fun helperCommand(vararg args: String): String {
        return buildList {
            add("CLASSPATH=${AdbTool.shellQuote(REMOTE_HELPER_JAR)}")
            add("app_process")
            add("/system/bin")
            add(AdbTool.shellQuote(HELPER_MAIN_CLASS))
            args.forEach { add(AdbTool.shellQuote(it)) }
        }.joinToString(" ")
    }

    private suspend fun sessionFor(deviceId: String, sizePx: Int): IconHelperSession {
        synchronized(sessions) {
            sessions[deviceId]?.takeIf { it.isAlive && it.sizePx == sizePx }?.let { return it }
            sessions.remove(deviceId)?.close()
        }
        val adbPath = AdbPathManager.getAdbPath()
            ?: throw IllegalStateException(AdbPathManager.adbEnvironment.value.message ?: "ADB path not found")
        val command = helperCommand("serve-icons", sizePx.toString())
        val process = ProcessBuilder(adbPath, "-s", deviceId, "shell", command)
            .redirectErrorStream(true)
            .start()
        ChildProcessRegistry.register(process)
        val session = IconHelperSession(deviceId, sizePx, process)
        synchronized(sessions) {
            sessions[deviceId] = session
        }
        return session
    }

    private fun closeSession(deviceId: String) {
        synchronized(sessions) {
            sessions.remove(deviceId)?.close()
        }
    }

    private fun isLongSessionEnabled(): Boolean =
        System.getProperty("qadb.icon.longSession") != "false"

    private fun locateHelperJar(): File {
        System.getProperty("qadb.icon.helper.path")?.takeIf { it.isNotBlank() }?.let { return File(it) }

        val cwd = File(System.getProperty("user.dir"))
        val candidates = listOf(
            cwd.resolve("qadb-icon-helper/build/outputs/qadb-icon-helper.jar"),
            cwd.parentFile?.resolve("qadb-icon-helper/build/outputs/qadb-icon-helper.jar"),
            File("qadb-icon-helper/build/outputs/qadb-icon-helper.jar")
        ).filterNotNull()
        candidates.firstOrNull { it.isFile }?.let { return it }

        locateBundledHelperJar()?.let { return it }
        return candidates.first()
    }

    private fun locateBundledHelperJar(): File? {
        val resource = AppIconHelperClient::class.java.getResource(BUNDLED_HELPER_RESOURCE) ?: return null
        if (resource.protocol == "file") {
            return File(URI(resource.toString()))
        }

        val extracted = File(localCacheRoot, "helper/qadb-icon-helper.jar")
        if (!extracted.isFile || extracted.length() <= 0L) {
            extracted.parentFile?.mkdirs()
            AppIconHelperClient::class.java.getResourceAsStream(BUNDLED_HELPER_RESOURCE)?.use { input ->
                extracted.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return extracted.takeIf { it.isFile && it.length() > 0L }
    }

    private fun parseHelperOutputs(output: String): List<HelperOutput> {
        return output.lineSequence().mapNotNull { line ->
            if (line.startsWith("OK ") || line.startsWith("ERR ")) parseHelperLine(line) else null
        }.toList()
    }

    private fun parseHelperLine(line: String): HelperOutput? {
        val values = line.substringAfter(' ').split(' ')
            .mapNotNull { token ->
                val index = token.indexOf('=')
                if (index <= 0) null else token.substring(0, index) to token.substring(index + 1)
            }.toMap()
        return HelperOutput(
            ok = line.startsWith("OK "),
            packageName = values["package"].orEmpty(),
            label = decodeBase64(values["label64"]),
            path = values["path"].orEmpty(),
            source = values["source"].orEmpty(),
            cacheHit = values["cache"] == "hit",
            elapsedMs = values["elapsedMs"]?.toLongOrNull() ?: 0L,
            dataBase64 = values["data64"].orEmpty(),
            reason = if (line.startsWith("ERR ")) line.substringAfter("reason=", "").substringBefore(" source=") else ""
        )
    }

    private fun safeFileToken(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun decodeBase64(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8).trim().takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    private fun decodeBase64Bytes(value: String?): ByteArray? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            Base64.getUrlDecoder().decode(value).takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    private data class HelperOutput(
        val ok: Boolean,
        val packageName: String,
        val label: String?,
        val path: String,
        val source: String,
        val cacheHit: Boolean,
        val elapsedMs: Long,
        val dataBase64: String,
        val reason: String
    )

    private class IconHelperSession(
        private val deviceId: String,
        val sizePx: Int,
        private val process: Process
    ) {
        private val reader = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))
        private val writer = BufferedWriter(OutputStreamWriter(process.outputStream, Charsets.UTF_8))
        private val lock = Any()

        val isAlive: Boolean
            get() = process.isAlive

        fun request(packageNames: List<String>): AdbTool.AdbResult = synchronized(lock) {
            if (!process.isAlive) {
                throw IllegalStateException("icon helper session is not alive")
            }
            val batchId = UUID.randomUUID().toString()
            for (packageName in packageNames) {
                writer.write(packageName)
                writer.newLine()
            }
            writer.write("__qadb_end_batch__ $batchId")
            writer.newLine()
            writer.flush()

            val lines = mutableListOf<String>()
            while (true) {
                val line = reader.readLine() ?: throw IllegalStateException("icon helper session closed")
                if (line.startsWith("DONE ") && line.contains("batch=$batchId")) {
                    break
                }
                lines.add(line)
            }
            AdbTool.AdbResult(success = true, output = lines.joinToString("\n"))
        }

        fun close() {
            runCatching {
                writer.write("__qadb_exit__")
                writer.newLine()
                writer.flush()
            }
            runCatching { process.destroy() }
            ChildProcessRegistry.unregister(process)
        }
    }

    companion object {
        const val REMOTE_BASE_DIR = "/data/local/tmp/qadb"
        const val REMOTE_ICON_DIR = "$REMOTE_BASE_DIR/icons"
        const val REMOTE_HELPER_JAR = "$REMOTE_BASE_DIR/qadb-icon-helper.jar"
        const val HELPER_MAIN_CLASS = "com.ludoven.qadb.icon.IconHelperMain"
        private const val BUNDLED_HELPER_RESOURCE = "/qadb/qadb-icon-helper.jar"
        private const val LOCAL_ICON_CACHE_VERSION = 5

        fun localIconCacheFileName(packageName: String): String =
            "${packageName.replace(Regex("[^A-Za-z0-9._-]"), "_")}.v$LOCAL_ICON_CACHE_VERSION.png"
    }
}
