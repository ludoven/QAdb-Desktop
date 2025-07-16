package com.ludoven.adbtool.viewmodel

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.select_a_app
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.entity.AdbFunctionType
import com.ludoven.adbtool.pages.AppInfo
import com.ludoven.adbtool.pages.getInstalledApps
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource


class AppViewModel : ViewModel() {

    private val _appInfo = MutableStateFlow<Map<String, String>>(emptyMap())
    val appInfo: StateFlow<Map<String, String>> = _appInfo.asStateFlow()

    private val _selectedApp = MutableStateFlow<AppInfo?>(null)
    val selectedApp: StateFlow<AppInfo?> = _selectedApp

    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appList: StateFlow<List<AppInfo>> = _appList

    private val _dialogMessage = MutableStateFlow<String?>(null)
    val dialogMessage: StateFlow<String?> = _dialogMessage.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()


    fun getAppList(){
        viewModelScope.launch(Dispatchers.IO) {
            val list = getInstalledApps()
            _appList.value = list
        }
    }

    fun selectApp(app: AppInfo?) {
        _selectedApp.value = app
    }


    fun loadAppInfo(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = AdbTool.exec("dumpsys package $packageName") ?: return@launch

            val info = parseAppInfo(result)
            _appInfo.value = info
        }
    }

    private fun parseAppInfo(dump: String): Map<String, String> {
        val infoMap = mutableMapOf<String, String>()

        infoMap["包名"] = Regex("Package \\[([^]]+)]").find(dump)?.groupValues?.get(1) ?: "未知"
        infoMap["版本号"] = Regex("versionName=([^\\s]+)").find(dump)?.groupValues?.get(1) ?: "未知"
        infoMap["版本码"] = Regex("versionCode=(\\d+)").find(dump)?.groupValues?.get(1) ?: "未知"

        val providers = Regex("Content providers:\n((?:\\s+.+\n)+)").find(dump)
            ?.groupValues?.get(1)
            ?.lines()?.joinToString("\n") { it.trim() } ?: "无"
        infoMap["ContentProviders"] = providers


        infoMap["UID"] = Regex("userId=(\\d+)").find(dump)?.groupValues?.get(1) ?: "未知"

        val isSystemApp = Regex("flags=\\[.*SYSTEM.*]").containsMatchIn(dump)
        infoMap["是否系统应用"] = if (isSystemApp) "是" else "否"

        infoMap["targetSdkVersion"] = Regex("targetSdk=(\\d+)").find(dump)?.groupValues?.get(1) ?: "未知"
        infoMap["minSdkVersion"] = Regex("minSdk=(\\d+)").find(dump)?.groupValues?.get(1) ?: "未知"

        infoMap["首次安装时间"] = Regex("firstInstallTime=([^\n]+)").find(dump)?.groupValues?.get(1) ?: "未知"
        infoMap["更新时间"] = Regex("lastUpdateTime=([^\n]+)").find(dump)?.groupValues?.get(1) ?: "未知"

        infoMap["支持架构"] = Regex("primaryCpuAbi=([^\n]+)").find(dump)?.groupValues?.get(1) ?: "未知"

        return infoMap
    }





    fun showTipDialog(message: String, autoDismiss: Boolean = false, delayMillis: Long = 2000L) {
        viewModelScope.launch {
            _dialogMessage.value = message
            _showDialog.value = true
            if (autoDismiss) {
                delay(delayMillis)
                _showDialog.value = false
                _dialogMessage.value = null
            }
        }
    }

    fun dismissTipDialog() {
        _showDialog.value = false
        _dialogMessage.value = null
    }

    fun executeAdbAction(
        type: AdbFunctionType,
        packageName: String? = selectedApp.value?.packageName // 默认使用当前选中应用的包名
    ) {
        if (packageName == null) {
            showTipDialog("select_a_app")
            return
        }

        viewModelScope.launch {
            try {
                val commandResult: String? = when (type) {
                    AdbFunctionType.UNINSTALL -> AdbTool.exec(type.formatCommand(packageName!!))
                    AdbFunctionType.LAUNCH -> AdbTool.exec(type.formatCommand(packageName!!))
                    AdbFunctionType.FORCE_STOP -> AdbTool.exec(type.formatCommand(packageName!!))
                    AdbFunctionType.RESTART_APP -> {
                        AdbTool.exec(AdbFunctionType.FORCE_STOP.formatCommand(packageName!!))
                        delay(300)
                        AdbTool.exec(AdbFunctionType.LAUNCH.formatCommand(packageName))
                        "已重启应用：$packageName"
                    }
                    AdbFunctionType.CLEAR_DATA -> AdbTool.exec(type.formatCommand(packageName!!))
                    AdbFunctionType.CLEAR_AND_RESTART -> {
                        AdbTool.exec(AdbFunctionType.CLEAR_DATA.formatCommand(packageName!!))
                        delay(300)
                        AdbTool.exec(AdbFunctionType.LAUNCH.formatCommand(packageName))
                        "清除并重启成功：$packageName"
                    }
                    AdbFunctionType.RESET_PERMISSIONS -> AdbTool.exec(type.formatCommand(packageName!!))
                    AdbFunctionType.RESET_PERMISSIONS_AND_RESTART -> {
                        AdbTool.exec(AdbFunctionType.RESET_PERMISSIONS.formatCommand(packageName!!))
                        delay(300)
                        AdbTool.exec(AdbFunctionType.LAUNCH.formatCommand(packageName))
                        "已重置权限并重启：$packageName"
                    }
                    AdbFunctionType.GRANT_ALL_PERMISSIONS -> {
                        val result1 = AdbTool.exec("pm grant $packageName android.permission.READ_EXTERNAL_STORAGE")
                        val result2 = AdbTool.exec("pm grant $packageName android.permission.WRITE_EXTERNAL_STORAGE")
                        if (result1?.contains("Success") == true && result2?.contains("Success") == true) {
                            "授权完成：$packageName"
                        } else {
                            "授权失败"
                        }
                    }
                    AdbFunctionType.GET_PATH -> AdbTool.exec(type.formatCommand(packageName!!))
                    AdbFunctionType.EXPORT_APK -> {
                        val path = AdbTool.exec(AdbFunctionType.GET_PATH.formatCommand(packageName!!))?.split(":")?.getOrNull(1)?.trim()
                        if (path != null) {
                            val folderPath = FileUtils.selectFolder() // 假设 FileUtils.selectFolder() 是一个同步操作
                            if (folderPath != null) {
                                val savePath = "$folderPath/${selectedApp.value?.appName}_${System.currentTimeMillis()}.apk"
                                val success = AdbTool.pullFile(path, savePath)
                                if (success) "导出成功：$savePath" else "导出失败"
                            } else {
                                "未选择导出路径"
                            }
                        } else {
                            "获取安装路径失败"
                        }
                    }
                    AdbFunctionType.GET_APP_SIZE -> AdbTool.exec(type.formatCommand(packageName!!))
                    else -> null
                }

                when (type) {
                    AdbFunctionType.UNINSTALL -> {
                        if (commandResult?.contains("Success") == true) {
                            showTipDialog("卸载成功：$packageName")
                            getAppList() // 刷新列表
                            selectApp(null) // 清空选择
                        } else {
                            showTipDialog("卸载失败: $commandResult")
                        }
                    }
                    AdbFunctionType.LAUNCH, AdbFunctionType.FORCE_STOP,
                    AdbFunctionType.CLEAR_DATA, AdbFunctionType.RESET_PERMISSIONS -> {
                        if (commandResult?.contains("Success") == true) {
                            showTipDialog("操作成功：$packageName")
                        } else {
                            showTipDialog("操作失败: $commandResult")
                        }
                    }
                    // 对于其他多步或复杂操作，直接使用 commandResult 作为消息
                    else -> showTipDialog(commandResult ?: "操作失败")
                }

            } catch (e: Exception) {
                showTipDialog("执行命令时发生错误: ${e.message}")
            }
        }
    }
}
