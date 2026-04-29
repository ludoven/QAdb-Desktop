package com.ludoven.adbtool.entity

import org.jetbrains.compose.resources.StringResource
import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.first_install_time
import adbtool_desktop.composeapp.generated.resources.is_system_app
import adbtool_desktop.composeapp.generated.resources.last_update_time
import adbtool_desktop.composeapp.generated.resources.min_sdk
import adbtool_desktop.composeapp.generated.resources.package_name
import adbtool_desktop.composeapp.generated.resources.supported_abi
import adbtool_desktop.composeapp.generated.resources.target_sdk
import adbtool_desktop.composeapp.generated.resources.uid
import adbtool_desktop.composeapp.generated.resources.version_code
import adbtool_desktop.composeapp.generated.resources.version_name

/**
 * 应用信息字段枚举
 */
enum class AppInfoField(val stringResource: StringResource) {
    PACKAGE_NAME(Res.string.package_name),
    VERSION_NAME(Res.string.version_name),
    VERSION_CODE(Res.string.version_code),
    UID(Res.string.uid),
    IS_SYSTEM_APP(Res.string.is_system_app),
    TARGET_SDK(Res.string.target_sdk),
    MIN_SDK(Res.string.min_sdk),
    FIRST_INSTALL_TIME(Res.string.first_install_time),
    LAST_UPDATE_TIME(Res.string.last_update_time),
    SUPPORTED_ABI(Res.string.supported_abi)
}

/**
 * 应用信息数据类
 */
data class AppInfoData(
    val appName: String = "",
    val packageName: String = "",
    val versionName: String = "",
    val versionCode: String = "",
    val uid: String = "",
    val isSystemApp: Boolean = false,
    val isRunning: Boolean = false,
    val targetSdk: String = "",
    val minSdk: String = "",
    val firstInstallTime: String = "",
    val lastUpdateTime: String = "",
    val supportedAbi: String = "",
    val apkPath: String = "",
    val dataDir: String = "",
    val installLocation: String = "",
    val appSize: String = "-",
    val dataSize: String = "-",
    val cacheSize: String = "-",
    val totalSize: String = "-",
    val processId: String = "-",
    val memoryUsage: String = "-",
    val startTime: String = "-",
    val dangerousPermissionCount: Int = 0,
    val privacyPermissionCount: Int = 0,
    val normalPermissionCount: Int = 0,
    val totalPermissionCount: Int = 0,
    val contentProviders: String = ""
) {
    /**
     * 转换为显示用的Map，key为AppInfoField，value为对应的值
     */
    fun toDisplayMap(): Map<AppInfoField, String> {
        return mapOf(
            AppInfoField.PACKAGE_NAME to packageName,
            AppInfoField.VERSION_NAME to versionName,
            AppInfoField.VERSION_CODE to versionCode,
            AppInfoField.UID to uid,
            AppInfoField.IS_SYSTEM_APP to if (isSystemApp) "Yes" else "No", // 这里会在UI层本地化
            AppInfoField.TARGET_SDK to targetSdk,
            AppInfoField.MIN_SDK to minSdk,
            AppInfoField.FIRST_INSTALL_TIME to firstInstallTime,
            AppInfoField.LAST_UPDATE_TIME to lastUpdateTime,
            AppInfoField.SUPPORTED_ABI to supportedAbi
        ).filter { it.value.isNotEmpty() }
    }
}
