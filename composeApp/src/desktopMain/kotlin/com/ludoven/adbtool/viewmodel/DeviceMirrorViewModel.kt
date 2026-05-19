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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val _mirrorErrorMessage = MutableStateFlow<MsgContent?>(null)
    val mirrorErrorMessage: StateFlow<MsgContent?> = _mirrorErrorMessage.asStateFlow()
    private var mirrorStateWatcherJob: Job? = null
    private val _deviceConnectionState = MutableStateFlow("disconnected")
    val deviceConnectionState: StateFlow<String> = _deviceConnectionState.asStateFlow()
    private var deviceConnectionWatcherJob: Job? = null
    private val _inputInjectionBlocked = MutableStateFlow(false)
    val inputInjectionBlocked: StateFlow<Boolean> = _inputInjectionBlocked.asStateFlow()

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
                    _mirrorErrorMessage.value = null
                    startMirrorStateWatcher()
                    MsgContent.Resource(Res.string.device_mirror_started)
                }
                result.errorMessage == ScrcpyPathManager.ERROR_BUNDLED_SCRCPY_NOT_FOUND -> {
                    MsgContent.Resource(Res.string.device_mirror_scrcpy_missing).also {
                        _mirrorErrorMessage.value = it
                    }
                }
                else -> MsgContent.Resource(
                    Res.string.device_mirror_failed,
                    listOf(result.errorMessage ?: result.output)
                ).also {
                    _mirrorErrorMessage.value = it
                }
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
            stopMirrorStateWatcher()
            val message = if (result.success) {
                _mirrorErrorMessage.value = null
                MsgContent.Text("已停止设备镜像窗口")
            } else {
                MsgContent.Resource(
                    Res.string.device_mirror_failed,
                    listOf(result.errorMessage ?: result.output)
                ).also {
                    _mirrorErrorMessage.value = it
                }
            }
            showTipDialog(message, autoDismiss = true)
        }
    }

    fun sendKeyEvent(deviceId: String?, keyCode: Int) {
        val targetDeviceId = resolveTargetDeviceId(deviceId)
        if (targetDeviceId.isNullOrBlank()) {
            showTipDialog(MsgContent.Resource(Res.string.no_device_available), autoDismiss = true)
            return
        }
        viewModelScope.launch {
            val precheck = withContext(Dispatchers.IO) {
                AdbTool.execAdbAsync("-s", targetDeviceId, "get-state")
            }
            val deviceState = precheck.output.trim()
            if (!precheck.success || deviceState != "device") {
                showTipDialog(
                    MsgContent.Text(
                        "设备不可操作（$targetDeviceId）：${formatAdbError(precheck, if (deviceState.isNotBlank()) deviceState else "unknown")}"
                    ),
                    autoDismiss = true
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                AdbTool.execShellAsync("input keyevent $keyCode", targetDeviceId)
            }
            if (!result.success) {
                if (isInputInjectionSecurityError(result)) {
                    _inputInjectionBlocked.value = true
                    showTipDialog(
                        MsgContent.Text("系统拒绝输入注入：请在开发者选项开启 USB 调试（安全设置）后重试。"),
                        autoDismiss = true
                    )
                    return@launch
                }
                showTipDialog(
                    MsgContent.Text("快捷控制失败（$targetDeviceId）：${formatAdbError(result, "执行失败")}"),
                    autoDismiss = true
                )
            } else {
                _inputInjectionBlocked.value = false
            }
        }
    }

    fun rotateScreen(deviceId: String?) {
        val targetDeviceId = resolveTargetDeviceId(deviceId)
        if (targetDeviceId.isNullOrBlank()) {
            showTipDialog(MsgContent.Resource(Res.string.no_device_available), autoDismiss = true)
            return
        }
        viewModelScope.launch {
            val precheck = withContext(Dispatchers.IO) {
                AdbTool.execAdbAsync("-s", targetDeviceId, "get-state")
            }
            val deviceState = precheck.output.trim()
            if (!precheck.success || deviceState != "device") {
                showTipDialog(
                    MsgContent.Text(
                        "设备不可操作（$targetDeviceId）：${formatAdbError(precheck, if (deviceState.isNotBlank()) deviceState else "unknown")}"
                    ),
                    autoDismiss = true
                )
                return@launch
            }
            val disableResult = withContext(Dispatchers.IO) {
                AdbTool.execShellAsync("settings put system accelerometer_rotation 0", targetDeviceId)
            }
            if (!disableResult.success) {
                if (isInputInjectionSecurityError(disableResult)) {
                    _inputInjectionBlocked.value = true
                    showTipDialog(
                        MsgContent.Text("系统拒绝输入注入：请在开发者选项开启 USB 调试（安全设置）后重试。"),
                        autoDismiss = true
                    )
                    return@launch
                }
                showTipDialog(
                    MsgContent.Text("旋转屏幕失败（$targetDeviceId）：${formatAdbError(disableResult, "执行失败")}"),
                    autoDismiss = true
                )
                return@launch
            }
            val currentRotationResult = withContext(Dispatchers.IO) {
                AdbTool.execShellAsync("settings get system user_rotation", targetDeviceId)
            }
            if (!currentRotationResult.success) {
                showTipDialog(
                    MsgContent.Text("读取屏幕方向失败（$targetDeviceId）：${formatAdbError(currentRotationResult, "执行失败")}"),
                    autoDismiss = true
                )
                return@launch
            }
            val currentRotation = currentRotationResult.output.trim()
            val nextRotation = if (currentRotation == "1") 0 else 1
            val rotateResult = withContext(Dispatchers.IO) {
                AdbTool.execShellAsync("settings put system user_rotation $nextRotation", targetDeviceId)
            }
            if (!rotateResult.success) {
                if (isInputInjectionSecurityError(rotateResult)) {
                    _inputInjectionBlocked.value = true
                    showTipDialog(
                        MsgContent.Text("系统拒绝输入注入：请在开发者选项开启 USB 调试（安全设置）后重试。"),
                        autoDismiss = true
                    )
                    return@launch
                }
                showTipDialog(
                    MsgContent.Text("旋转屏幕失败（$targetDeviceId）：${formatAdbError(rotateResult, "执行失败")}"),
                    autoDismiss = true
                )
            } else {
                _inputInjectionBlocked.value = false
            }
        }
    }

    fun takeScreenSnapshot(deviceId: String?) {
        val targetDeviceId = resolveTargetDeviceId(deviceId)
        if (targetDeviceId.isNullOrBlank()) {
            showTipDialog(MsgContent.Resource(Res.string.no_device_available), autoDismiss = true)
            return
        }
        viewModelScope.launch {
            val precheck = withContext(Dispatchers.IO) {
                AdbTool.execAdbAsync("-s", targetDeviceId, "get-state")
            }
            val deviceState = precheck.output.trim()
            if (!precheck.success || deviceState != "device") {
                showTipDialog(
                    MsgContent.Text(
                        "设备不可操作（$targetDeviceId）：${formatAdbError(precheck, if (deviceState.isNotBlank()) deviceState else "unknown")}"
                    ),
                    autoDismiss = true
                )
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                AdbTool.execShellAsync("input keyevent 120", targetDeviceId)
            }
            if (!result.success) {
                if (isInputInjectionSecurityError(result)) {
                    _inputInjectionBlocked.value = true
                    showTipDialog(
                        MsgContent.Text("系统拒绝输入注入：请在开发者选项开启 USB 调试（安全设置）后重试。"),
                        autoDismiss = true
                    )
                    return@launch
                }
                showTipDialog(
                    MsgContent.Text("截图操作失败（$targetDeviceId）：${formatAdbError(result, "执行失败")}"),
                    autoDismiss = true
                )
            } else {
                _inputInjectionBlocked.value = false
            }
        }
    }

    private fun startMirrorStateWatcher() {
        mirrorStateWatcherJob?.cancel()
        mirrorStateWatcherJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_mirrorRunning.value) continue
                val alive = withContext(Dispatchers.IO) { AdbTool.isDeviceMirrorRunning() }
                if (!alive) {
                    _mirrorRunning.value = false
                    _activeDeviceId.value = null
                    _mirrorStartedAt.value = null
                    _mirrorErrorMessage.value = null
                    showTipDialog(MsgContent.Text("镜像窗口已关闭"), autoDismiss = true)
                    break
                }
            }
        }
    }

    private fun stopMirrorStateWatcher() {
        mirrorStateWatcherJob?.cancel()
        mirrorStateWatcherJob = null
    }

    fun watchDeviceConnectionState(deviceId: String?) {
        deviceConnectionWatcherJob?.cancel()
        val targetDeviceId = resolveTargetDeviceId(deviceId)
        if (targetDeviceId.isNullOrBlank()) {
            _deviceConnectionState.value = "disconnected"
            return
        }
        deviceConnectionWatcherJob = viewModelScope.launch {
            while (true) {
                val state = withContext(Dispatchers.IO) {
                    val result = AdbTool.execAdbAsync("-s", targetDeviceId, "get-state")
                    val output = result.output.trim()
                    when {
                        result.success && output == "device" -> "device"
                        output.contains("offline", ignoreCase = true) -> "offline"
                        output.contains("unauthorized", ignoreCase = true) -> "unauthorized"
                        output.contains("not found", ignoreCase = true) -> "not_found"
                        output.isNotBlank() -> output
                        result.errorMessage?.contains("not found", ignoreCase = true) == true -> "not_found"
                        else -> "unknown"
                    }
                }
                _deviceConnectionState.value = state
                delay(2000)
            }
        }
    }

    private fun resolveTargetDeviceId(deviceId: String?): String? {
        return deviceId?.takeIf { it.isNotBlank() }
            ?: _activeDeviceId.value?.takeIf { it.isNotBlank() }
            ?: AdbTool.selectDeviceId?.takeIf { it.isNotBlank() }
    }

    private fun formatAdbError(result: AdbTool.AdbResult, fallback: String): String {
        return result.output.ifBlank { result.errorMessage.orEmpty() }.ifBlank { fallback }.trim()
    }

    private fun isInputInjectionSecurityError(result: AdbTool.AdbResult): Boolean {
        val text = (result.output + "\n" + result.errorMessage.orEmpty()).lowercase()
        return text.contains("securityexception")
            && (text.contains("injectinputevent") || text.contains("inject_events"))
    }
}
