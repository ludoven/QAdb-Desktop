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
        fun parseDevices(adbDevicesOutput: String): List<String> {
            return adbDevicesOutput
                .lineSequence()
                .drop(1)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val cols = line.split(Regex("\\s+"))
                    if (cols.size >= 2 && cols[1] == "device") cols[0] else null
                }
                .toList()
        }
    }
}
