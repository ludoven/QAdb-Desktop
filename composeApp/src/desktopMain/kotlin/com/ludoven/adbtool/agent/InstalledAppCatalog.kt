package com.ludoven.adbtool.agent

import com.ludoven.adbtool.domain.adb.AppIconHelperClient
import com.ludoven.adbtool.domain.adb.InstalledAppLabel
import com.ludoven.adbtool.util.AdbTool
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAgentApp(
    val packageName: String,
    val label: String,
    val enabled: Boolean,
    val launchable: Boolean = true
)

interface InstalledAppCatalog {
    suspend fun list(deviceId: String, forceRefresh: Boolean = false): List<InstalledAgentApp>
    suspend fun find(deviceId: String, query: String): List<InstalledAgentApp>
    suspend fun isInstalled(deviceId: String, packageName: String): Boolean
    fun invalidate(deviceId: String)
}

class RealInstalledAppCatalog(
    private val helperClient: AppIconHelperClient = AppIconHelperClient(
        File(AgentDataPaths.agentDataDirectory(), "app-catalog")
    )
) : InstalledAppCatalog {
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    override suspend fun list(deviceId: String, forceRefresh: Boolean): List<InstalledAgentApp> {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            cache[deviceId]?.takeIf { now - it.loadedAt < CACHE_TTL_MILLIS }?.let { return it.apps }
        }
        val labels = helperClient.fetchInstalledAppLabels(deviceId).getOrElse {
            fallbackPackages(deviceId)
        }
        val apps = labels.map {
            InstalledAgentApp(
                packageName = it.packageName,
                label = it.label.ifBlank { it.packageName },
                enabled = it.enabled
            )
        }.filter { it.enabled }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
        cache[deviceId] = CacheEntry(now, apps)
        return apps
    }

    override suspend fun find(deviceId: String, query: String): List<InstalledAgentApp> {
        return rankInstalledApps(list(deviceId), query)
    }

    override suspend fun isInstalled(deviceId: String, packageName: String): Boolean =
        list(deviceId).any { it.packageName == packageName && it.enabled }

    override fun invalidate(deviceId: String) {
        cache.remove(deviceId)
    }

    private suspend fun fallbackPackages(deviceId: String): List<InstalledAppLabel> = withContext(Dispatchers.IO) {
        val result = AdbTool.execAdbAsync("-s", deviceId, "shell", "pm", "list", "packages")
        if (!result.success) return@withContext emptyList()
        result.output.lineSequence().mapNotNull { line ->
            line.substringAfter("package:", "").trim().takeIf { it.isNotEmpty() }?.let {
                InstalledAppLabel(packageName = it, label = it, enabled = true)
            }
        }.toList()
    }

    private data class CacheEntry(val loadedAt: Long, val apps: List<InstalledAgentApp>)
}

internal fun rankInstalledApps(
    apps: List<InstalledAgentApp>,
    query: String
): List<InstalledAgentApp> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return emptyList()
    return apps
        .map { app ->
            app to when {
                app.packageName.equals(normalized, ignoreCase = true) -> 4
                app.label.equals(normalized, ignoreCase = true) -> 3
                app.label.contains(normalized, ignoreCase = true) -> 2
                app.packageName.contains(normalized, ignoreCase = true) -> 1
                else -> 0
            }
        }
        .filter { it.second > 0 }
        .sortedWith(compareByDescending<Pair<InstalledAgentApp, Int>> { it.second }.thenBy { it.first.label })
        .take(MAX_APP_RESULTS)
        .map { it.first }
}

private const val CACHE_TTL_MILLIS = 10L * 60L * 1_000L
private const val MAX_APP_RESULTS = 8
