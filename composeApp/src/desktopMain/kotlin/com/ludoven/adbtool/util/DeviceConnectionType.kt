package com.ludoven.adbtool.util

private val ADB_HOST_PORT_PATTERN = Regex("""^[A-Za-z0-9.-]+:\d+$""")
private val ADB_IPV6_HOST_PORT_PATTERN = Regex("""^\[[0-9A-Fa-f:%]+\]:\d+$""")

fun isWirelessAdbConnection(deviceId: String?, ipAddress: String? = null): Boolean {
    val normalized = deviceId?.trim().orEmpty()
    if (normalized.isNotEmpty()) {
        val lower = normalized.lowercase()
        if (lower.contains("tls-connect")) return true
        if (ADB_HOST_PORT_PATTERN.matches(normalized)) return true
        if (ADB_IPV6_HOST_PORT_PATTERN.matches(normalized)) return true
        return false
    }
    return !ipAddress.isNullOrBlank()
}
