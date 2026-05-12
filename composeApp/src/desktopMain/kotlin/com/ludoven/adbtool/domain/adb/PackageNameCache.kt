package com.ludoven.adbtool.domain.adb

class PackageNameCache(
    private val ttlMs: Long = 60_000L
) {
    private data class Entry(
        val packageNames: List<String>,
        val timestamp: Long
    )

    private val cache = mutableMapOf<String, Entry>()

    fun get(deviceId: String): List<String>? {
        val entry = cache[deviceId] ?: return null
        val expired = (System.currentTimeMillis() - entry.timestamp) > ttlMs
        return if (expired) {
            cache.remove(deviceId)
            null
        } else {
            entry.packageNames
        }
    }

    fun put(deviceId: String, packageNames: List<String>) {
        cache[deviceId] = Entry(packageNames = packageNames, timestamp = System.currentTimeMillis())
    }

    fun clear(deviceId: String? = null) {
        if (deviceId == null) {
            cache.clear()
        } else {
            cache.remove(deviceId)
        }
    }
}
