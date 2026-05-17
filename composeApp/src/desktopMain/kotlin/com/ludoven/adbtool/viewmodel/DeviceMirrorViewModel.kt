package com.ludoven.adbtool.viewmodel

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.device_mirror_failed
import adbtool_desktop.composeapp.generated.resources.device_mirror_scrcpy_missing
import adbtool_desktop.composeapp.generated.resources.device_mirror_started
import adbtool_desktop.composeapp.generated.resources.no_device_available
import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.entity.DeviceMirrorSettings
import com.ludoven.adbtool.entity.MirrorLaunchProfile
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.ScrcpyPathManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceMirrorViewModel : BaseViewModel() {
    private val _settings = MutableStateFlow(DeviceMirrorSettings())
    val settings: StateFlow<DeviceMirrorSettings> = _settings.asStateFlow()

    private val _mirrorRunning = MutableStateFlow(false)
    val mirrorRunning: StateFlow<Boolean> = _mirrorRunning.asStateFlow()

    private val _activeDeviceId = MutableStateFlow<String?>(null)
    val activeDeviceId: StateFlow<String?> = _activeDeviceId.asStateFlow()

    private val _mirrorStartedAt = MutableStateFlow<Long?>(null)
    val mirrorStartedAt: StateFlow<Long?> = _mirrorStartedAt.asStateFlow()

    fun updateSettings(settings: DeviceMirrorSettings) {
        _settings.value = settings
    }

    fun selectProfile(profile: MirrorLaunchProfile) {
        _settings.value = _settings.value.applyProfile(profile)
    }

    fun notifyCommandCopied() {
        showTipDialog(MsgContent.Text("命令已复制到剪贴板"), autoDismiss = true)
    }

    fun buildCommandPreview(deviceId: String?): String {
        val resolvedDevice = deviceId?.takeIf { it.isNotBlank() } ?: "<device-serial>"
        val scrcpyPath = ScrcpyPathManager.getScrcpyPath() ?: "scrcpy"
        val command = AdbTool.buildScrcpyCommand(
            scrcpyPath = scrcpyPath,
            deviceId = resolvedDevice,
            windowTitle = "QADB Device Mirror",
            settings = _settings.value
        )
        return command.joinToString(" ") { arg ->
            if (arg.any(Char::isWhitespace)) "\"$arg\"" else arg
        }
    }

    fun openMirror(deviceId: String?) {
        if (deviceId.isNullOrBlank()) {
            showTipDialog(MsgContent.Resource(Res.string.no_device_available), autoDismiss = true)
            return
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                AdbTool.startDeviceMirrorAsync(
                    deviceId = deviceId,
                    settings = _settings.value,
                    forceRestart = true
                )
            }
            val message = when {
                result.success -> {
                    _mirrorRunning.value = true
                    _activeDeviceId.value = deviceId
                    _mirrorStartedAt.value = System.currentTimeMillis()
                    MsgContent.Resource(Res.string.device_mirror_started)
                }
                result.errorMessage == ScrcpyPathManager.ERROR_BUNDLED_SCRCPY_NOT_FOUND -> {
                    MsgContent.Resource(Res.string.device_mirror_scrcpy_missing)
                }
                else -> MsgContent.Resource(
                    Res.string.device_mirror_failed,
                    listOf(result.errorMessage ?: result.output)
                )
            }
            showTipDialog(message, autoDismiss = true)
        }
    }

    fun stopMirror() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                AdbTool.stopDeviceMirrorAsync()
            }
            _mirrorRunning.value = false
            _activeDeviceId.value = null
            _mirrorStartedAt.value = null
            val message = if (result.success) {
                MsgContent.Text("已停止设备镜像窗口")
            } else {
                MsgContent.Resource(
                    Res.string.device_mirror_failed,
                    listOf(result.errorMessage ?: result.output)
                )
            }
            showTipDialog(message, autoDismiss = true)
        }
    }

    fun sendKeyEvent(deviceId: String?, keyCode: Int) {
        if (deviceId.isNullOrBlank()) {
            showTipDialog(MsgContent.Resource(Res.string.no_device_available), autoDismiss = true)
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AdbTool.execShell("input keyevent $keyCode", deviceId)
            }
        }
    }

    fun rotateScreen(deviceId: String?) {
        if (deviceId.isNullOrBlank()) {
            showTipDialog(MsgContent.Resource(Res.string.no_device_available), autoDismiss = true)
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AdbTool.execShell("settings put system accelerometer_rotation 0", deviceId)
                val currentRotation = AdbTool.execShell("settings get system user_rotation", deviceId).trim()
                val nextRotation = if (currentRotation == "1") 0 else 1
                AdbTool.execShell("settings put system user_rotation $nextRotation", deviceId)
            }
        }
    }

    fun takeScreenSnapshot(deviceId: String?) {
        if (deviceId.isNullOrBlank()) {
            showTipDialog(MsgContent.Resource(Res.string.no_device_available), autoDismiss = true)
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AdbTool.execShell("input keyevent 120", deviceId)
            }
        }
    }
}
