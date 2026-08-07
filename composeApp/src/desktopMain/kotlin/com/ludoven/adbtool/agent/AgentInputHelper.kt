package com.ludoven.adbtool.agent

import com.ludoven.adbtool.util.AdbTool
import java.io.File
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class AgentInputHelperStatus(
    val installed: Boolean,
    val enabled: Boolean
)

class AgentInputHelper {
    fun requiresUnicodeHelper(text: String): Boolean =
        !text.matches(SIMPLE_ADB_TEXT_PATTERN)

    suspend fun status(deviceId: String): AgentInputHelperStatus {
        val installedResult = AdbTool.execAdbAsync(
            "-s", deviceId, "shell", "pm", "path", INPUT_HELPER_PACKAGE
        )
        if (!installedResult.success || !installedResult.output.contains("package:")) {
            return AgentInputHelperStatus(installed = false, enabled = false)
        }
        val enabled = AdbTool.execAdbAsync(
            "-s", deviceId, "shell", "ime", "list", "-s"
        ).output.lineSequence().any { it.trim() == INPUT_HELPER_COMPONENT }
        return AgentInputHelperStatus(installed = true, enabled = enabled)
    }

    suspend fun install(deviceId: String): AgentToolResult {
        val apk = locateBundledApk()
            ?: return AgentToolResult(false, "QADB Unicode input helper is missing from this desktop build")
        val result = AdbTool.installApkAsync(apk.absolutePath, deviceId)
        return AgentToolResult(
            success = result.success,
            output = if (result.success) {
                "QADB Unicode input helper installed"
            } else {
                result.errorMessage ?: result.output.ifBlank { "Unable to install QADB input helper" }
            }
        )
    }

    suspend fun uninstall(deviceId: String): AgentToolResult {
        val result = AdbTool.execAdbWithTimeoutAsync(
            INPUT_ACTION_TIMEOUT_MILLIS,
            "-s", deviceId, "uninstall", INPUT_HELPER_PACKAGE
        )
        return AgentToolResult(
            result.success,
            if (result.success) "QADB Unicode input helper removed" else {
                result.errorMessage ?: result.output.ifBlank { "Unable to remove QADB input helper" }
            }
        )
    }

    suspend fun openTestScreen(deviceId: String): AgentToolResult {
        val result = AdbTool.execAdbWithTimeoutAsync(
            INPUT_ACTION_TIMEOUT_MILLIS,
            "-s", deviceId, "shell", "am", "start",
            "-n", "$INPUT_HELPER_PACKAGE/.InputTestActivity"
        )
        return AgentToolResult(result.success, result.output.ifBlank { result.errorMessage.orEmpty() })
    }

    suspend fun input(deviceId: String, text: String, allowInstall: Boolean): AgentToolResult {
        if (!requiresUnicodeHelper(text)) {
            return directInput(deviceId, text)
        }
        var helperStatus = status(deviceId)
        var installedForThisInput = false
        if (!helperStatus.installed) {
            if (!allowInstall) {
                return AgentToolResult(
                    false,
                    "Unicode text input requires QADB's Unicode input helper. Confirm installation or enter the text manually."
                )
            }
            val installResult = install(deviceId)
            if (!installResult.success) return installResult
            installedForThisInput = true
            helperStatus = status(deviceId)
            if (!helperStatus.installed) {
                return AgentToolResult(false, "QADB Unicode input helper was installed but could not be verified")
            }
        }

        val previousIme = AdbTool.execAdbAsync(
            "-s", deviceId, "shell", "settings", "get", "secure", "default_input_method"
        ).output.trim().takeIf { it.contains('/') }
        val wasEnabled = helperStatus.enabled
        return try {
            if (!wasEnabled) {
                val enabled = AdbTool.execAdbWithTimeoutAsync(
                    INPUT_ACTION_TIMEOUT_MILLIS,
                    "-s", deviceId, "shell", "ime", "enable", INPUT_HELPER_COMPONENT
                )
                if (!enabled.success) return AgentToolResult(false, enabled.errorMessage ?: enabled.output)
            }
            val selected = AdbTool.execAdbWithTimeoutAsync(
                INPUT_ACTION_TIMEOUT_MILLIS,
                "-s", deviceId, "shell", "ime", "set", INPUT_HELPER_COMPONENT
            )
            if (!selected.success) return AgentToolResult(false, selected.errorMessage ?: selected.output)
            delay(INPUT_METHOD_SETTLE_MILLIS)
            val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray(Charsets.UTF_8))
            val broadcast = AdbTool.execAdbWithTimeoutAsync(
                INPUT_ACTION_TIMEOUT_MILLIS,
                "-s", deviceId, "shell", "am", "broadcast",
                "-a", INPUT_ACTION,
                "-n", "$INPUT_HELPER_PACKAGE/.InputCommitReceiver",
                "--es", "text64", encoded
            )
            val committed = broadcast.success &&
                broadcast.output.contains("result=0") &&
                broadcast.output.contains("committed", ignoreCase = true)
            AgentToolResult(
                success = committed,
                output = if (committed) {
                    buildString {
                        if (installedForThisInput) append("QADB Unicode input helper installed; ")
                        append("Unicode text committed (${text.codePointCount(0, text.length)} characters)")
                        append("; previous input method restoration attempted")
                    }
                } else {
                    buildString {
                        if (installedForThisInput) append("QADB Unicode input helper installed; ")
                        append(broadcast.errorMessage ?: broadcast.output.ifBlank { "Unicode input was not committed" })
                    }
                }
            )
        } finally {
            previousIme?.let {
                AdbTool.execAdbWithTimeoutAsync(
                    INPUT_ACTION_TIMEOUT_MILLIS,
                    "-s", deviceId, "shell", "ime", "set", it
                )
            }
            if (!wasEnabled) {
                AdbTool.execAdbWithTimeoutAsync(
                    INPUT_ACTION_TIMEOUT_MILLIS,
                    "-s", deviceId, "shell", "ime", "disable", INPUT_HELPER_COMPONENT
                )
            }
        }
    }

    private suspend fun directInput(deviceId: String, text: String): AgentToolResult {
        val result = AdbTool.execAdbWithTimeoutAsync(
            INPUT_ACTION_TIMEOUT_MILLIS,
            "-s", deviceId, "shell", "input", "text", text.replace(" ", "%s")
        )
        return AgentToolResult(
            success = result.success,
            output = if (result.success) {
                "Text input completed (${text.length} characters)"
            } else {
                result.errorMessage ?: result.output.ifBlank { "Text input failed" }
            }
        )
    }

    private suspend fun locateBundledApk(): File? = withContext(Dispatchers.IO) {
        val resource = AgentInputHelper::class.java.getResource(BUNDLED_INPUT_HELPER_RESOURCE)
            ?: return@withContext null
        if (resource.protocol == "file") return@withContext File(URI(resource.toString()))
        val extracted = File(AgentDataPaths.agentDataDirectory(), "helper/qadb-agent-ime.apk")
        if (!extracted.isFile || extracted.length() <= 0L) {
            extracted.parentFile?.mkdirs()
            AgentInputHelper::class.java.getResourceAsStream(BUNDLED_INPUT_HELPER_RESOURCE)?.use { input ->
                extracted.outputStream().use { output -> input.copyTo(output) }
            }
        }
        extracted.takeIf { it.isFile && it.length() > 0L }
    }

    companion object {
        const val INPUT_HELPER_PACKAGE = "com.ludoven.qadb.agentime"
        const val INPUT_HELPER_COMPONENT =
            "com.ludoven.qadb.agentime/.QadbInputMethodService"
        private const val INPUT_ACTION = "com.ludoven.qadb.agentime.COMMIT_TEXT"
        private const val BUNDLED_INPUT_HELPER_RESOURCE = "/qadb/qadb-agent-ime.apk"
    }
}

private val SIMPLE_ADB_TEXT_PATTERN = Regex("[A-Za-z0-9 ._@+\\-]{1,2000}")
private const val INPUT_ACTION_TIMEOUT_MILLIS = 30_000L
private const val INPUT_METHOD_SETTLE_MILLIS = 350L
