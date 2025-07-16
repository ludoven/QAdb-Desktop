package com.ludoven.adbtool.entity

import androidx.compose.ui.graphics.vector.ImageVector

/*data class AdbFunction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)*/

// 定义 AdbFunctionType，用于区分不同操作
enum class AdbFunctionType(val commandTemplate: String? = null) {
    // 新增类型 (注意有些类型可能不需要 commandTemplate，因为它们的操作更复杂，直接在 ViewModel 中实现)
    INSTALL_APK(null), // 安装需要文件选择器
    INPUT_TEXT(null), // 输入文本需要弹窗
    SCREENSHOT(null), // 截图需要文件选择器
    VIEW_CURRENT_ACTIVITY(null), // 查看当前 Activity，结果需要解析

    // 系统类操作
    REBOOT_DEVICE("reboot"),
    IS_ROOTED("su -c id"), // 判断是否 root
    WIFI_INFO("dumpsys wifi"),
    CPU_INFO("top -n 1"),
    NETWORK_STATUS("dumpsys connectivity"),
    BATTERY_STATUS("dumpsys battery"),
    SCREEN_RESOLUTION("wm size"),
    DEVELOPER_OPTIONS("am start -a android.settings.APPLICATION_DEVELOPMENT_SETTINGS"),

    //APP
    UNINSTALL("pm uninstall %s"),
    LAUNCH("monkey -p %s -c android.intent.category.LAUNCHER 1"),
    FORCE_STOP("am force-stop %s"),
    RESTART_APP(null), // 需要多步操作
    CLEAR_DATA("pm clear %s"),
    CLEAR_AND_RESTART(null), // 需要多步操作
    RESET_PERMISSIONS("pm reset-permissions %s"),
    RESET_PERMISSIONS_AND_RESTART(null), // 需要多步操作
    GRANT_ALL_PERMISSIONS(null), // 需要多步操作
    GET_PATH("pm path %s"),
    EXPORT_APK(null), // 复杂操作
    GET_APP_SIZE("dumpsys package %s | grep 'codePath\\|dataDir'");

    fun formatCommand(packageName: String): String {
        return commandTemplate?.format(packageName) ?: throw IllegalArgumentException("Command template not defined for $this")
    }
}

// 统一的 AdbFunction 数据类，包含类型和 UI 信息
data class AdbFunction(
    val title: String,
    val icon: ImageVector,
    val type: AdbFunctionType,
    val requiresPackage: Boolean = true // 标记是否需要选中应用
)
