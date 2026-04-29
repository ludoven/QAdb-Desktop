package com.ludoven.adbtool.entity

import org.jetbrains.compose.resources.StringResource
import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.*

/**
 * 设备信息字段枚举
 */
enum class DeviceInfoField(val stringResource: StringResource) {
    ANDROID_VERSION(Res.string.android_version),
    SDK_VERSION(Res.string.sdk_version),
    KERNEL_VERSION(Res.string.kernel_version),
    DEVICE_MODEL(Res.string.device_model),
    MANUFACTURER(Res.string.manufacturer),
    ROM_VERSION(Res.string.rom_version),
    SCREEN_RESOLUTION(Res.string.screen_resolution),
    FONT_SCALE(Res.string.font_scale),
    BUILD_FINGERPRINT(Res.string.build_fingerprint),
    IP_ADDRESS(Res.string.ip_address),
    MAC_ADDRESS(Res.string.mac_address)
}

/**
 * 设备中心信息字段枚举
 */
enum class DeviceCenterInfoField(val stringResource: StringResource) {
    CPU_USAGE(Res.string.cpu_usage),
    MEMORY_USAGE(Res.string.memory_usage),
    STORAGE_USAGE(Res.string.storage_usage),
    BATTERY_LEVEL(Res.string.battery_level)
}

enum class BatteryStatus(val stringResource: StringResource) {
    CHARGING(Res.string.charging_status),
    DISCHARGING(Res.string.discharging_status),
    FULL(Res.string.full_status),
    UNKNOWN(Res.string.unknown_status);
    
    companion object {
        fun fromStatusCode(statusCode: String?): BatteryStatus {
            return when (statusCode) {
                "2" -> CHARGING
                "3" -> DISCHARGING
                "5" -> FULL
                else -> UNKNOWN
            }
        }
    }
}

/**
 * 设备信息数据类
 */
data class DeviceInfoData(
    val androidVersion: String = "",
    val sdkVersion: String = "",
    val kernelVersion: String = "",
    val deviceModel: String = "",
    val manufacturer: String = "",
    val romVersion: String = "",
    val screenResolution: String = "",
    val fontScale: String = "",
    val buildFingerprint: String = "",
    val ipAddress: String = "",
    val macAddress: String = ""
) {
    /**
     * 转换为显示用的Map，key为DeviceInfoField，value为对应的值
     */
    fun toDisplayMap(): Map<DeviceInfoField, String> {
        return mapOf(
            DeviceInfoField.ANDROID_VERSION to androidVersion,
            DeviceInfoField.SDK_VERSION to sdkVersion,
            DeviceInfoField.KERNEL_VERSION to kernelVersion,
            DeviceInfoField.DEVICE_MODEL to deviceModel,
            DeviceInfoField.MANUFACTURER to manufacturer,
            DeviceInfoField.ROM_VERSION to romVersion,
            DeviceInfoField.SCREEN_RESOLUTION to screenResolution,
            DeviceInfoField.FONT_SCALE to fontScale,
            DeviceInfoField.BUILD_FINGERPRINT to buildFingerprint,
            DeviceInfoField.IP_ADDRESS to ipAddress,
            DeviceInfoField.MAC_ADDRESS to macAddress
        ).filter { it.value.isNotEmpty() }
    }
}

/**
 * 设备中心信息数据类
 */
data class DeviceCenterInfoData(
    val cpuUsage: String = "",
    val memoryUsage: String = "",
    val storageUsage: String = "",
    val batteryLevel: String = "",
    val batteryStatus: BatteryStatus = BatteryStatus.UNKNOWN
) {
    /**
     * 转换为显示用的Map，key为DeviceCenterInfoField，value为对应的值
     */
    fun toDisplayMap(): Map<DeviceCenterInfoField, String> {
        return mapOf(
            DeviceCenterInfoField.CPU_USAGE to cpuUsage,
            DeviceCenterInfoField.MEMORY_USAGE to memoryUsage,
            DeviceCenterInfoField.STORAGE_USAGE to storageUsage,
            DeviceCenterInfoField.BATTERY_LEVEL to batteryLevel
        ).filter { it.value.isNotEmpty() }
    }
    
    fun getBatteryDisplayText(): String {
        return if (batteryLevel.isNotEmpty()) {
            batteryLevel
        } else {
            ""
        }
    }
}
