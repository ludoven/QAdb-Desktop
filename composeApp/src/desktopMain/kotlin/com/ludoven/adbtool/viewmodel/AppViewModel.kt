package com.ludoven.adbtool.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.pages.AppInfo
import com.ludoven.adbtool.pages.getInstalledApps
import com.ludoven.adbtool.util.AdbTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class AppViewModel : ViewModel() {

    private val _appInfo = MutableStateFlow<Map<String, String>>(emptyMap())
    val appInfo: StateFlow<Map<String, String>> = _appInfo.asStateFlow()

    private val _selectedApp = MutableStateFlow<AppInfo?>(null)
    val selectedApp: StateFlow<AppInfo?> = _selectedApp

    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appList: StateFlow<List<AppInfo>> = _appList


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

}
