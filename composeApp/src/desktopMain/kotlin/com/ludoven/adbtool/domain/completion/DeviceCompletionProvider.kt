package com.ludoven.adbtool.domain.completion

class DeviceCompletionProvider {
    fun complete(input: String, devices: List<String>): List<String> {
        val prefix = input.trim().lowercase()
        if (prefix.isBlank()) return devices
        return devices.filter { it.lowercase().startsWith(prefix) }
    }
}
