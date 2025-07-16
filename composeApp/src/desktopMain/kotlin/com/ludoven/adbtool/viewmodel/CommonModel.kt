package com.ludoven.adbtool.viewmodel

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.activity_not_found
import adbtool_desktop.composeapp.generated.resources.apk_not_selected
import adbtool_desktop.composeapp.generated.resources.current_activity
import adbtool_desktop.composeapp.generated.resources.dialog_operation_failed
import adbtool_desktop.composeapp.generated.resources.folder_not_selected
import adbtool_desktop.composeapp.generated.resources.install_failed
import adbtool_desktop.composeapp.generated.resources.install_success
import adbtool_desktop.composeapp.generated.resources.installing
import adbtool_desktop.composeapp.generated.resources.no_device_available
import adbtool_desktop.composeapp.generated.resources.screenshot_failed
import adbtool_desktop.composeapp.generated.resources.screenshot_success
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.entity.AdbFunctionType
import com.ludoven.adbtool.entity.LocalizedText
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CommonModel : ViewModel() {

    private val _dialogMessage = MutableStateFlow<LocalizedText?>(null)
    val dialogMessage: StateFlow<LocalizedText?> = _dialogMessage.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()


    private val _showInputDialog = MutableStateFlow(false)
    val showInputDialog: StateFlow<Boolean> = _showInputDialog.asStateFlow()


    fun showTipDialog(tag: LocalizedText, autoDismiss: Boolean = false, delayMillis: Long = 2000L) {
        viewModelScope.launch {
            _dialogMessage.value = tag
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
        type: AdbFunctionType
    ) {
        if (AdbTool.selectDeviceId == null) {
            showTipDialog(LocalizedText(Res.string.no_device_available))
            return
        }

        viewModelScope.launch {
            try {
                when (type) {
                    AdbFunctionType.INSTALL_APK -> {
                        installApp()
                    }

                    AdbFunctionType.INPUT_TEXT -> {
                        showInputDialog(true)
                    }

                    AdbFunctionType.SCREENSHOT -> {
                        screenShoot()
                    }

                    AdbFunctionType.VIEW_CURRENT_ACTIVITY -> {
                        viewActivity()
                    }

                    AdbFunctionType.REBOOT_DEVICE -> {

                    }

                    else -> {}
                }

            } catch (e: Exception) {
                showTipDialog(
                    LocalizedText(
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
            showTipDialog(LocalizedText(Res.string.installing))
            withContext(Dispatchers.IO) {
                val success = AdbTool.installApk(apkPath) // 执行安装
                withContext(Dispatchers.Main) { // 切换回主线程更新 UI
                    val localizedText =
                        LocalizedText(if (success) Res.string.install_success else Res.string.install_failed)
                    showTipDialog(localizedText, true)
                }
            }

        } else {
            showTipDialog(LocalizedText(Res.string.apk_not_selected), true)
        }
    }

    fun showInputDialog(show: Boolean) {
        _showInputDialog.value = show
    }

    private suspend fun screenShoot() {
        val folderPath = withContext(Dispatchers.IO) {
            FileUtils.selectFolder()
        }

        if (folderPath == null) {
            showTipDialog(LocalizedText(Res.string.folder_not_selected), true)
            return
        }

        val savePath = "$folderPath/screen_${System.currentTimeMillis()}.png"

        val success = withContext(Dispatchers.IO) {
            AdbTool.takeScreenshot(savePath)
        }

        val localizedText =
            LocalizedText(if (success) Res.string.screenshot_success else Res.string.screenshot_failed)
        showTipDialog(localizedText, true)
    }


    private suspend fun viewActivity() {
        val result = withContext(Dispatchers.IO) {
            AdbTool.getCurrentActivity()
        }
        val localizedText =
            result?.let { LocalizedText(Res.string.current_activity, listOf(it)) } ?: LocalizedText(
                Res.string.activity_not_found
            )
        showTipDialog(localizedText)
    }
}
