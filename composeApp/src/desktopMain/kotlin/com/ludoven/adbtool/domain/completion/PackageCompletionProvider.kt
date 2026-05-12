package com.ludoven.adbtool.domain.completion

import com.ludoven.adbtool.domain.adb.PackageNameCache

class PackageCompletionProvider(
    private val cache: PackageNameCache
) {
    fun complete(deviceId: String?, input: String): List<String> {
        if (deviceId.isNullOrBlank()) return emptyList()
        val packages = cache.get(deviceId) ?: return emptyList()
        val prefix = input.trim().lowercase()
        if (prefix.isBlank()) return packages
        return packages.filter { it.lowercase().startsWith(prefix) }
    }
}
