package com.ludoven.adbtool.entity

import androidx.compose.ui.graphics.vector.ImageVector

/*data class AdbFunction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)*/

// 定义 AdbFunctionType，用于区分不同操作
enum class AdbFunctionType() {
    // 新增类型 (注意有些类型可能不需要 commandTemplate，因为它们的操作更复杂，直接在 ViewModel 中实现)
    INSTALL_APK, // 安装需要文件选择器
    INPUT_TEXT, // 输入文本需要弹窗
    SCREENSHOT, // 截图需要文件选择器
    VIEW_CURRENT_ACTIVITY, // 查看当前 Activity，结果需要解析

    // 系统类操作
    REBOOT_DEVICE,
    IS_ROOTED, // 判断是否 root
    WIFI_INFO,
    CPU_INFO,
    NETWORK_STATUS,
    BATTERY_STATUS,
    SCREEN_RESOLUTION,
    DEVELOPER_OPTIONS,

    //APP
    UNINSTALL,
    LAUNCH,
    FORCE_STOP,
    RESTART_APP, // 需要多步操作
    CLEAR_DATA,
    CLEAR_AND_RESTART, // 需要多步操作
    RESET_PERMISSIONS,
    RESET_PERMISSIONS_AND_RESTART, // 需要多步操作
    GRANT_ALL_PERMISSIONS, // 需要多步操作
    GET_PATH,
    EXPORT_APK, // 复杂操作
    GET_APP_SIZE;

}

// 统一的 AdbFunction 数据类，包含类型和 UI 信息
data class AdbFunction(
    val title: String,
    val icon: ImageVector,
    val type: AdbFunctionType,
    val requiresPackage: Boolean = true // 标记是否需要选中应用
)
