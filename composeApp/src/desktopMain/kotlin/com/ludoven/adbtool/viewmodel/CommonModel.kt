package com.ludoven.adbtool.viewmodel

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.activity_not_found
import adbtool_desktop.composeapp.generated.resources.apk_not_selected
import adbtool_desktop.composeapp.generated.resources.capture_logs
import adbtool_desktop.composeapp.generated.resources.current_activity
import adbtool_desktop.composeapp.generated.resources.dialog_operation_failed
import adbtool_desktop.composeapp.generated.resources.folder_not_selected
import adbtool_desktop.composeapp.generated.resources.install_failed
import adbtool_desktop.composeapp.generated.resources.install_success_launch_failed
import adbtool_desktop.composeapp.generated.resources.install_success
import adbtool_desktop.composeapp.generated.resources.installing
import adbtool_desktop.composeapp.generated.resources.logs_failed
import adbtool_desktop.composeapp.generated.resources.logs_saved
import adbtool_desktop.composeapp.generated.resources.no_device_available
import adbtool_desktop.composeapp.generated.resources.recording_failed
import adbtool_desktop.composeapp.generated.resources.recording_in_progress
import adbtool_desktop.composeapp.generated.resources.recording_saved
import adbtool_desktop.composeapp.generated.resources.screenshot_failed
import adbtool_desktop.composeapp.generated.resources.screenshot_success
import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.entity.AdbFunctionType
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.FileUtils
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CommonModel : BaseViewModel() {
    private val _showInputDialog = MutableStateFlow(false)
    val showInputDialog: StateFlow<Boolean> = _showInputDialog.asStateFlow()

    fun executeAdbAction(
        type: AdbFunctionType
    ) {
        if (!ensureDeviceSelected()) {
            return
        }

        viewModelScope.launch {
            try {
                when (type) {
                    AdbFunctionType.INSTALL_APK -> installApp()
                    AdbFunctionType.INPUT_TEXT -> showInputDialog(true)
                    AdbFunctionType.SCREENSHOT -> screenShoot()
                    AdbFunctionType.SCREEN_RECORD -> screenRecord()
                    AdbFunctionType.CAPTURE_LOGS -> captureLogs()
                    AdbFunctionType.OPEN_FILE_MANAGER -> execResult("am start -a android.intent.action.VIEW -d file:///sdcard")
                    AdbFunctionType.KEY_BACK -> execResult("input keyevent 4")
                    AdbFunctionType.KEY_HOME -> execResult("input keyevent 3")
                    AdbFunctionType.VIEW_CURRENT_ACTIVITY -> viewActivity()
                    AdbFunctionType.REBOOT_DEVICE -> execResult("reboot")
                    AdbFunctionType.IS_ROOTED -> execResult("su -c id")
                    AdbFunctionType.WIFI_INFO -> execResult("dumpsys wifi")
                    AdbFunctionType.CPU_INFO -> execResult("top -n 1")
                    AdbFunctionType.NETWORK_STATUS -> execResult("dumpsys connectivity")
                    AdbFunctionType.BATTERY_STATUS -> execResult("dumpsys battery")
                    AdbFunctionType.SCREEN_RESOLUTION -> execResult("wm size")
                    AdbFunctionType.DEVELOPER_OPTIONS -> execResult("am start -a android.settings.APPLICATION_DEVELOPMENT_SETTINGS")
                    else -> {}
                }
            } catch (e: Exception) {
                showTipDialog(
                    MsgContent.Resource(
                        Res.string.dialog_operation_failed,
                        listOf("${e.message}")
                    )
                )
            }
        }
    }

    fun executeShellCommand(command: String) {
        if (!ensureDeviceSelected()) return
        val normalized = command.trim()
        if (normalized.isEmpty()) return
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    AdbTool.execShell(normalized)
                }
                showTipDialog(MsgContent.Text(result))
            } catch (e: Exception) {
                showTipDialog(
                    MsgContent.Resource(
                        Res.string.dialog_operation_failed,
                        listOf("${e.message}")
                    )
                )
            }
        }
    }

    fun executePackageAction(type: AdbFunctionType, packageName: String) {
        if (!ensureDeviceSelected()) return
        val normalized = packageName.trim()
        if (normalized.isEmpty()) return

        viewModelScope.launch {
            try {
                when (type) {
                    AdbFunctionType.LAUNCH_APP_BY_PACKAGE -> {
                        val success = withContext(Dispatchers.IO) { AdbTool.startApp(normalized) }
                        showTipDialog(MsgContent.Text(if (success) "启动成功：$normalized" else "启动失败：$normalized"), true)
                    }

                    AdbFunctionType.STOP_APP_BY_PACKAGE -> {
                        val success = withContext(Dispatchers.IO) { AdbTool.stopApp(normalized) }
                        showTipDialog(MsgContent.Text(if (success) "已停止：$normalized" else "停止失败：$normalized"), true)
                    }

                    AdbFunctionType.CLEAR_CACHE_AND_RESTART -> {
                        val clearResult = withContext(Dispatchers.IO) { AdbTool.clearAppData(normalized) }
                        val restartResult = if (clearResult) {
                            withContext(Dispatchers.IO) { AdbTool.startApp(normalized) }
                        } else {
                            false
                        }
                        showTipDialog(
                            MsgContent.Text(
                                if (clearResult && restartResult) {
                                    "清缓存并重启成功：$normalized"
                                } else {
                                    "清缓存或重启失败：$normalized"
                                }
                            ),
                            true
                        )
                    }

                    AdbFunctionType.INSTALL_AND_LAUNCH -> {
                        installAndLaunch(normalized)
                    }

                    else -> Unit
                }
            } catch (e: Exception) {
                showTipDialog(
                    MsgContent.Resource(
                        Res.string.dialog_operation_failed,
                        listOf("${e.message}")
                    )
                )
            }
        }
    }

    private suspend fun installApp() {
        val apkPath = FileUtils.selectApkFile()
        if (apkPath != null) {
            showTipDialog(MsgContent.Resource(Res.string.installing))
            withContext(Dispatchers.IO) {
                val success = AdbTool.installApk(apkPath) // 执行安装
                withContext(Dispatchers.Main) { // 切换回主线程更新 UI
                    val localizedText =
                        MsgContent.Resource(if (success) Res.string.install_success else Res.string.install_failed)
                    showTipDialog(localizedText, true)
                }
            }

        } else {
            showTipDialog(MsgContent.Resource(Res.string.apk_not_selected), true)
        }
    }

    private suspend fun installAndLaunch(packageName: String) {
        val apkPath = FileUtils.selectApkFile()
        if (apkPath == null) {
            showTipDialog(MsgContent.Resource(Res.string.apk_not_selected), true)
            return
        }

        showTipDialog(MsgContent.Resource(Res.string.installing))
        val installSuccess = withContext(Dispatchers.IO) {
            AdbTool.installApk(apkPath)
        }
        if (!installSuccess) {
            showTipDialog(MsgContent.Resource(Res.string.install_failed), true)
            return
        }

        val launchSuccess = withContext(Dispatchers.IO) {
            AdbTool.startApp(packageName)
        }
        showTipDialog(
            MsgContent.Resource(
                if (launchSuccess) Res.string.install_success else Res.string.install_success_launch_failed
            ),
            true
        )
    }

    fun showInputDialog(show: Boolean) {
        _showInputDialog.value = show
    }

    private suspend fun screenShoot() {
        val folderPath = withContext(Dispatchers.IO) {
            FileUtils.selectFolder()
        }

        if (folderPath == null) {
            showTipDialog(MsgContent.Resource(Res.string.folder_not_selected), true)
            return
        }

        val savePath = "$folderPath/screen_${System.currentTimeMillis()}.png"

        val success = withContext(Dispatchers.IO) {
            AdbTool.takeScreenshot(savePath)
        }

        val localizedText =
            MsgContent.Resource(if (success) Res.string.screenshot_success else Res.string.screenshot_failed)
        showTipDialog(localizedText, true)
    }

    private suspend fun screenRecord() {
        val folderPath = withContext(Dispatchers.IO) { FileUtils.selectFolder() }
        if (folderPath == null) {
            showTipDialog(MsgContent.Resource(Res.string.folder_not_selected), true)
            return
        }

        showTipDialog(MsgContent.Resource(Res.string.recording_in_progress))

        val timestamp = System.currentTimeMillis()
        val remotePath = "/sdcard/record_$timestamp.mp4"
        val localPath = "$folderPath/record_$timestamp.mp4"

        val success = withContext(Dispatchers.IO) {
            runCatching {
                AdbTool.execShell("screenrecord --time-limit 15 $remotePath")
                val pulled = AdbTool.pullFile(remotePath, localPath)
                AdbTool.execShell("rm $remotePath")
                pulled
            }.getOrDefault(false)
        }

        val localized = if (success) {
            MsgContent.Resource(Res.string.recording_saved, listOf(localPath))
        } else {
            MsgContent.Resource(Res.string.recording_failed)
        }
        showTipDialog(localized, true)
    }

    private suspend fun captureLogs() {
        val folderPath = withContext(Dispatchers.IO) { FileUtils.selectFolder() }
        if (folderPath == null) {
            showTipDialog(MsgContent.Resource(Res.string.folder_not_selected), true)
            return
        }

        val savePath = "$folderPath/logcat_${System.currentTimeMillis()}.txt"
        val success = withContext(Dispatchers.IO) {
            runCatching {
                val output = AdbTool.execShell("logcat -d -v time")
                File(savePath).writeText(output)
                output.isNotBlank()
            }.getOrDefault(false)
        }
        val localized = if (success) {
            MsgContent.Resource(Res.string.logs_saved, listOf(savePath))
        } else {
            MsgContent.Resource(Res.string.logs_failed)
        }
        showTipDialog(localized, true)
    }

    private suspend fun viewActivity() {
        val result = withContext(Dispatchers.IO) {
            AdbTool.getCurrentActivity()
        }
        val localizedText =
            result?.let { MsgContent.Resource(Res.string.current_activity, listOf(it)) }
                ?: MsgContent.Resource(
                    Res.string.activity_not_found
                )
        showTipDialog(localizedText)
    }

    private fun ensureDeviceSelected(): Boolean {
        if (AdbTool.selectDeviceId != null) return true
        showTipDialog(MsgContent.Resource(Res.string.no_device_available))
        return false
    }
}
