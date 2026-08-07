package com.ludoven.adbtool.agent

import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.ChildProcessRegistry
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class RealAgentDeviceGateway(
    private val appCatalog: InstalledAppCatalog = RealInstalledAppCatalog(),
    private val uiParser: UiHierarchyParser = UiHierarchyParser(),
    private val screenshotProcessor: AgentScreenshotProcessor = AgentScreenshotProcessor(),
    private val inputHelper: AgentInputHelper = AgentInputHelper()
) : AgentDeviceGateway {
    private val observations = ConcurrentHashMap<String, AgentObservation>()

    override suspend fun isConnected(deviceId: String): Boolean =
        AdbTool.getConnectedDevices().contains(deviceId)

    override suspend fun observe(deviceId: String): AgentObservation = coroutineScope {
        if (!isConnected(deviceId)) {
            throw AgentException("The selected device is no longer connected")
        }
        val screenshotTask = async { runCatching { captureScreenshotPng(deviceId) } }
        val hierarchyTask = async { runCatching { loadUiHierarchy(deviceId) } }
        val activityTask = async { runCatching { loadCurrentActivity(deviceId) } }
        val displayTask = async { runCatching { loadDisplaySize(deviceId) } }

        val warnings = mutableListOf<String>()
        val screenshot = screenshotTask.await().getOrElse {
            warnings += "Screenshot unavailable"
            null
        }?.let(screenshotProcessor::process).also {
            if (it == null) warnings += "Visual observation unavailable"
        }
        val displaySize = screenshot?.let { it.width to it.height }
            ?: displayTask.await().getOrElse {
                warnings += "Display size unavailable"
                null
            }
            ?: throw AgentException("Unable to read the device display size")
        val rawHierarchy = hierarchyTask.await().getOrElse {
            warnings += "UI hierarchy unavailable"
            ""
        }
        val parsedHierarchy = uiParser.parse(rawHierarchy, displaySize.first, displaySize.second)
        val currentActivity = activityTask.await().getOrElse {
            warnings += "Current Activity unavailable"
            ""
        }
        val observation = AgentObservation(
            screenshotPng = screenshot?.bytes,
            screenshotMimeType = screenshot?.mimeType ?: "image/jpeg",
            uiHierarchy = parsedHierarchy.compactText,
            uiNodes = parsedHierarchy.nodes,
            currentActivity = currentActivity,
            screenWidth = displaySize.first,
            screenHeight = displaySize.second,
            warnings = warnings.distinct()
        )
        observations[deviceId] = observation
        observation
    }

    override suspend fun observeLightweight(
        deviceId: String,
        includeUiHierarchy: Boolean
    ): AgentObservation = coroutineScope {
        if (!isConnected(deviceId)) {
            throw AgentException("The selected device is no longer connected")
        }
        val activityTask = async { runCatching { loadCurrentActivity(deviceId) } }
        val displayTask = async { runCatching { loadDisplaySize(deviceId) } }
        val hierarchyTask = if (includeUiHierarchy) {
            async { runCatching { loadUiHierarchy(deviceId) } }
        } else {
            null
        }
        val displaySize = displayTask.await().getOrNull()
            ?: throw AgentException("Unable to read the device display size")
        val hierarchy = hierarchyTask?.await()?.getOrDefault("").orEmpty()
        val parsedHierarchy = uiParser.parse(hierarchy, displaySize.first, displaySize.second)
        AgentObservation(
            screenshotPng = null,
            uiHierarchy = parsedHierarchy.compactText,
            uiNodes = parsedHierarchy.nodes,
            currentActivity = activityTask.await().getOrDefault(""),
            screenWidth = displaySize.first,
            screenHeight = displaySize.second,
            warnings = buildList {
                if (activityTask.await().isFailure) add("Current Activity unavailable")
                if (includeUiHierarchy && hierarchyTask?.await()?.isFailure == true) add("UI hierarchy unavailable")
            }
        )
    }

    override suspend fun confirmationRequirement(
        deviceId: String,
        action: AgentAction,
        observation: AgentObservation
    ): String? {
        val input = action as? AgentAction.InputText ?: return null
        if (!inputHelper.requiresUnicodeHelper(input.text)) return null

        val helperInstalled = runCatching { inputHelper.status(deviceId).installed }
            .getOrDefault(false)
        return if (helperInstalled) {
            null
        } else {
            "Chinese or emoji input requires installing QADB's Unicode input helper on this device. " +
                "QADB will switch to it only for this input, restore the previous input method, and record the result in the task log."
        }
    }

    override suspend fun execute(deviceId: String, action: AgentAction): AgentToolResult {
        if (!isConnected(deviceId)) {
            return AgentToolResult(false, "The selected device is no longer connected")
        }
        return when (action) {
            AgentAction.Observe -> AgentToolResult(true, "Observation refreshed")
            is AgentAction.FindApp -> findApp(deviceId, action.query)
            is AgentAction.Tap -> tap(deviceId, action.x, action.y)
            is AgentAction.TapElement -> {
                val node = resolveNode(deviceId, action.observationId, action.elementId)
                    ?: return AgentToolResult(false, "The element reference is stale; observe again")
                tap(deviceId, node.bounds.centerX, node.bounds.centerY)
            }
            is AgentAction.Swipe -> adbResult(
                AdbTool.execAdbWithTimeoutAsync(
                    ADB_ACTION_TIMEOUT_MILLIS,
                    "-s", deviceId, "shell", "input", "swipe",
                    action.startX.toString(), action.startY.toString(),
                    action.endX.toString(), action.endY.toString(),
                    action.durationMs.toString()
                )
            )
            is AgentAction.InputText -> {
                action.elementId?.let { elementId ->
                    val observationId = action.observationId
                        ?: return AgentToolResult(false, "Input element requires an observation ID")
                    val node = resolveNode(deviceId, observationId, elementId)
                        ?: return AgentToolResult(false, "The input element reference is stale; observe again")
                    val focusResult = tap(deviceId, node.bounds.centerX, node.bounds.centerY)
                    if (!focusResult.success) return focusResult
                    delay(INPUT_FOCUS_SETTLE_MILLIS)
                }
                inputHelper.input(deviceId, action.text, allowInstall = true)
            }
            is AgentAction.KeyEvent -> adbResult(
                AdbTool.execAdbWithTimeoutAsync(
                    ADB_ACTION_TIMEOUT_MILLIS,
                    "-s", deviceId, "shell", "input", "keyevent", action.key.androidKeyCode
                )
            )
            is AgentAction.LaunchPackage -> {
                if (!appCatalog.isInstalled(deviceId, action.packageName)) {
                    AgentToolResult(false, "Package is not in the installed application catalog")
                } else {
                    adbResult(AdbTool.startAppAsync(action.packageName, deviceId))
                }
            }
            is AgentAction.Wait -> {
                delay(action.durationMs.toLong())
                AgentToolResult(true, "Waited ${action.durationMs} ms")
            }
            is AgentAction.ForceStopPackage -> adbResult(AdbTool.stopAppAsync(action.packageName, deviceId))
            is AgentAction.ClearAppData -> adbResult(AdbTool.clearAppDataAsync(action.packageName, deviceId))
            is AgentAction.UninstallPackage -> adbResult(
                AdbTool.execAdbWithTimeoutAsync(
                    ADB_ACTION_TIMEOUT_MILLIS, "-s", deviceId, "uninstall", action.packageName
                )
            ).also { appCatalog.invalidate(deviceId) }
            AgentAction.RebootDevice -> adbResult(
                AdbTool.execAdbWithTimeoutAsync(
                    ADB_ACTION_TIMEOUT_MILLIS, "-s", deviceId, "reboot"
                )
            )
            is AgentAction.Finish -> AgentToolResult(true, action.summary)
        }
    }

    private suspend fun findApp(deviceId: String, query: String): AgentToolResult {
        val matches = appCatalog.find(deviceId, query)
        if (matches.isEmpty()) {
            return AgentToolResult(false, "No installed launchable application matched the query")
        }
        val output = matches.joinToString("\n") {
            "label=${it.label.take(100)} package=${it.packageName}"
        }
        return AgentToolResult(
            success = true,
            output = "Installed app matches (untrusted labels):\n$output",
            ambiguous = matches.size > 1,
            resolvedPackages = matches.map { it.packageName }
        )
    }

    private suspend fun tap(deviceId: String, x: Int, y: Int): AgentToolResult = adbResult(
        AdbTool.execAdbWithTimeoutAsync(
            ADB_ACTION_TIMEOUT_MILLIS,
            "-s", deviceId, "shell", "input", "tap", x.toString(), y.toString()
        )
    )

    private fun resolveNode(deviceId: String, observationId: String, elementId: String): UiNodeSnapshot? =
        observations[deviceId]
            ?.takeIf { it.observationId == observationId }
            ?.uiNodes
            ?.firstOrNull { it.elementId == elementId }

    private suspend fun loadUiHierarchy(deviceId: String): String {
        val remotePath = "/sdcard/qadb_agent_window.xml"
        return try {
            val dump = AdbTool.execAdbAsync(
                "-s", deviceId, "shell", "uiautomator", "dump", remotePath
            )
            if (!dump.success) throw AgentException("Unable to capture UI hierarchy")
            AdbTool.execAdbOutputAsync("-s", deviceId, "shell", "cat", remotePath)
                .take(MAX_RAW_UI_HIERARCHY_LENGTH)
        } finally {
            runCatching { AdbTool.execAdbAsync("-s", deviceId, "shell", "rm", "-f", remotePath) }
        }
    }

    private suspend fun loadCurrentActivity(deviceId: String): String =
        parseCurrentActivity(
            AdbTool.execAdbOutputAsync(
                "-s", deviceId, "shell", "dumpsys", "activity", "activities"
            )
        )

    private suspend fun loadDisplaySize(deviceId: String): Pair<Int, Int>? {
        val output = AdbTool.execAdbOutputAsync("-s", deviceId, "shell", "wm", "size")
        val match = SCREEN_SIZE_PATTERN.findAll(output).lastOrNull() ?: return null
        return match.groupValues[1].toInt() to match.groupValues[2].toInt()
    }

    private suspend fun captureScreenshotPng(deviceId: String): ByteArray? = withContext(Dispatchers.IO) {
        val adbPath = AdbPathManager.getAdbPath() ?: return@withContext null
        val process = runCatching {
            ProcessBuilder(adbPath, "-s", deviceId, "exec-out", "screencap", "-p")
                .redirectErrorStream(false)
                .start()
        }.getOrNull() ?: return@withContext null
        ChildProcessRegistry.register(process)
        val outputFuture = CompletableFuture.supplyAsync {
            process.inputStream.use { it.readBytes() }
        }
        try {
            val completed = process.waitFor(ADB_ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return@withContext null
            }
            outputFuture.get(2, TimeUnit.SECONDS)
                .takeIf { process.exitValue() == 0 && it.isNotEmpty() }
        } catch (_: Exception) {
            null
        } finally {
            if (process.isAlive) process.destroyForcibly()
            outputFuture.cancel(true)
            ChildProcessRegistry.unregister(process)
        }
    }

    private fun parseCurrentActivity(output: String): String =
        output.lineSequence()
            .firstOrNull { line ->
                line.contains("mResumedActivity") ||
                    line.contains("topResumedActivity") ||
                    line.contains("ResumedActivity")
            }
            ?.substringAfter('{')
            ?.substringBefore('}')
            ?.trim()
            .orEmpty()

    private fun adbResult(result: AdbTool.AdbResult): AgentToolResult = AgentToolResult(
        success = result.success,
        output = if (result.success) {
            result.output.ifBlank { "Completed" }
        } else {
            result.errorMessage ?: result.output.ifBlank { "ADB action failed" }
        }
    )
}

private val SCREEN_SIZE_PATTERN = Regex("(\\d+)x(\\d+)")
private const val MAX_RAW_UI_HIERARCHY_LENGTH = 120_000
private const val ADB_ACTION_TIMEOUT_SECONDS = 30L
private const val ADB_ACTION_TIMEOUT_MILLIS = ADB_ACTION_TIMEOUT_SECONDS * 1_000L
private const val INPUT_FOCUS_SETTLE_MILLIS = 250L
