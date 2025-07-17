package com.ludoven.adbtool.viewmodel

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.dialog_clear_and_restart_success
import adbtool_desktop.composeapp.generated.resources.dialog_export_failed
import adbtool_desktop.composeapp.generated.resources.dialog_export_success
import adbtool_desktop.composeapp.generated.resources.dialog_get_install_path_failed
import adbtool_desktop.composeapp.generated.resources.dialog_no_export_path
import adbtool_desktop.composeapp.generated.resources.dialog_operation_failed
import adbtool_desktop.composeapp.generated.resources.dialog_reset_permissions_and_restart_success
import adbtool_desktop.composeapp.generated.resources.dialog_restart_success
import adbtool_desktop.composeapp.generated.resources.dialog_uninstall_failed
import adbtool_desktop.composeapp.generated.resources.dialog_uninstall_success
import adbtool_desktop.composeapp.generated.resources.select_a_app
import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.entity.AdbFunctionType
import com.ludoven.adbtool.entity.MsgContent
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
import kotlinx.coroutines.withContext


class AppViewModel : BaseViewModel() {

    private val _appInfo = MutableStateFlow<Map<String, String>>(emptyMap())
    val appInfo: StateFlow<Map<String, String>> = _appInfo.asStateFlow()

    private val _selectedApp = MutableStateFlow<AppInfo?>(null)
    val selectedApp: StateFlow<AppInfo?> = _selectedApp

    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appList: StateFlow<List<AppInfo>> = _appList


    fun getAppList() {
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

        infoMap["targetSdkVersion"] =
            Regex("targetSdk=(\\d+)").find(dump)?.groupValues?.get(1) ?: "未知"
        infoMap["minSdkVersion"] = Regex("minSdk=(\\d+)").find(dump)?.groupValues?.get(1) ?: "未知"

        infoMap["首次安装时间"] =
            Regex("firstInstallTime=([^\n]+)").find(dump)?.groupValues?.get(1) ?: "未知"
        infoMap["更新时间"] =
            Regex("lastUpdateTime=([^\n]+)").find(dump)?.groupValues?.get(1) ?: "未知"

        infoMap["支持架构"] =
            Regex("primaryCpuAbi=([^\n]+)").find(dump)?.groupValues?.get(1) ?: "未知"

        return infoMap
    }


    fun executeAdbAction(
        type: AdbFunctionType,
        packageName: String? = selectedApp.value?.packageName // 默认使用当前选中应用的包名
    ) {
        if (packageName == null) {
            showTipDialog(MsgContent.Resource(Res.string.select_a_app))
            return
        }

        viewModelScope.launch {
            try {
                when (type) {
                    AdbFunctionType.UNINSTALL -> {
                        withContext(Dispatchers.IO){
                            val string = AdbTool.exec("pm uninstall $packageName")
                            val resource = if (string.contains("Success")) {
                                getAppList() // 刷新列表
                                selectApp(null) // 清空选择
                                MsgContent.Resource(Res.string.dialog_uninstall_success)
                            } else {
                                MsgContent.Resource(Res.string.dialog_uninstall_failed)
                            }
                            showTipDialog(resource)
                        }
                    }
                    AdbFunctionType.LAUNCH -> execResult("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
                    AdbFunctionType.FORCE_STOP -> execResult("am force-stop $packageName")
                    AdbFunctionType.RESTART_APP -> {
                        withContext(Dispatchers.IO) {
                            AdbTool.exec("am force-stop $packageName")
                            delay(300)
                            AdbTool.exec("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
                        }
                        showTipDialog(
                            MsgContent.Resource(
                                Res.string.dialog_restart_success, listOf(
                                    packageName
                                )
                            )
                        )
                    }

                    AdbFunctionType.CLEAR_DATA -> execResult("pm clear $packageName")
                    AdbFunctionType.CLEAR_AND_RESTART -> {
                        withContext(Dispatchers.IO) {
                            AdbTool.exec("pm clear $packageName")
                            delay(300)
                            AdbTool.exec("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
                        }
                        showTipDialog(
                            MsgContent.Resource(
                                Res.string.dialog_clear_and_restart_success, listOf(
                                    packageName
                                )
                            )
                        )
                    }

                    AdbFunctionType.RESET_PERMISSIONS -> execResult("pm reset-permissions $packageName")
                    AdbFunctionType.RESET_PERMISSIONS_AND_RESTART -> {
                        withContext(Dispatchers.IO) {
                            AdbTool.exec("pm reset-permissions $packageName")
                            delay(300)
                            AdbTool.exec("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
                        }
                        showTipDialog(
                            MsgContent.Resource(
                                Res.string.dialog_reset_permissions_and_restart_success, listOf(
                                    packageName
                                )
                            )
                        )
                    }

                    AdbFunctionType.GRANT_ALL_PERMISSIONS -> {
                        /* withContext(Dispatchers.IO){

                         }*/
                    }

                    AdbFunctionType.GET_PATH -> execResult("pm path $packageName")
                    AdbFunctionType.EXPORT_APK -> {
                        val tip = withContext(Dispatchers.IO) {
                            val path =
                                AdbTool.exec("pm path $packageName").split(":").getOrNull(1)?.trim()
                            if (path != null) {
                                val folderPath =
                                    FileUtils.selectFolder() // 假设 FileUtils.selectFolder() 是一个同步操作
                                if (folderPath != null) {
                                    val savePath =
                                        "$folderPath/${selectedApp.value?.appName}_${System.currentTimeMillis()}.apk"
                                    val success = AdbTool.pullFile(path, savePath)
                                    if (success) MsgContent.Resource(Res.string.dialog_export_success) else MsgContent.Resource(
                                        Res.string.dialog_export_failed
                                    )
                                } else {
                                    MsgContent.Resource(Res.string.dialog_no_export_path)
                                }
                            } else {
                                MsgContent.Resource(Res.string.dialog_get_install_path_failed)
                            }
                        }
                        showTipDialog(tip)
                    }

                    AdbFunctionType.GET_APP_SIZE -> execResult("dumpsys package $packageName | grep 'codePath\\|dataDir'")
                    else -> null
                }

            } catch (e: Exception) {
                showTipDialog(MsgContent.Resource(Res.string.dialog_operation_failed))
            }
        }
    }
}
