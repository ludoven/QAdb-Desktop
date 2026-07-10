package com.ludoven.adbtool.domain.adb

data class DeviceState(
    val devices: List<String> = emptyList(),
    val displayNames: Map<String, String> = emptyMap()
)

class DeviceRepository {
    private var state: DeviceState = DeviceState()

    fun update(devices: List<String>, displayNames: Map<String, String>) {
        state = DeviceState(devices = devices, displayNames = displayNames)
    }

    fun currentState(): DeviceState = state

    fun findBestName(deviceId: String?): String? {
        if (deviceId.isNullOrBlank()) return null
        return state.displayNames[deviceId].takeIf { !it.isNullOrBlank() } ?: deviceId
    }

    companion object {
        private val adbDeviceLineRegex = Regex(
            """^(.+?)\s+(device|offline|unauthorized|recovery|sideload|bootloader|no permissions)(?:\s+.*)?$"""
        )

        fun parseDevices(adbDevicesOutput: String): List<String> {
            return adbDevicesOutput
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val match = adbDeviceLineRegex.matchEntire(line) ?: return@mapNotNull null
                    val (deviceId, state) = match.destructured
                    deviceId.trim().takeIf { state == "device" && it.isNotEmpty() }
                }
                .toList()
        }
    }
}
