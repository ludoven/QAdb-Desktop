package com.ludoven.adbtool.viewmodel

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.activity_not_found
import adbtool_desktop.composeapp.generated.resources.apk_not_selected
import adbtool_desktop.composeapp.generated.resources.capture_logs
import adbtool_desktop.composeapp.generated.resources.current_activity
import adbtool_desktop.composeapp.generated.resources.device_mirror_failed
import adbtool_desktop.composeapp.generated.resources.device_mirror_scrcpy_missing
import adbtool_desktop.composeapp.generated.resources.device_mirror_started
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
import adbtool_desktop.composeapp.generated.resources.recording_saved
import adbtool_desktop.composeapp.generated.resources.screenshot_failed
import adbtool_desktop.composeapp.generated.resources.screenshot_success
import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.entity.AdbFunctionType
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.ChildProcessRegistry
import com.ludoven.adbtool.util.FileUtils
import com.ludoven.adbtool.util.ScrcpyPathManager
import com.ludoven.adbtool.util.l10n
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScreenRecordUiState(
    val isRecording: Boolean = false,
    val isStopping: Boolean = false,
    val localPath: String = "",
    val durationSeconds: Int? = null,
    val startedAtMillis: Long = 0L
)

private data class ScreenRecordSession(
    val process: Process,
    val outputFuture: CompletableFuture<String>,
    val deviceId: String,
    val remotePath: String,
    val localPath: String,
    val durationSeconds: Int?,
    val startedAtMillis: Long
)

class CommonModel : BaseViewModel() {
    companion object {
        private const val SCREEN_RECORD_MAX_SECONDS = 180
        private const val SCREEN_RECORD_STOP_TIMEOUT_MS = 2_000L

        internal fun deviceActionsEnabled(deviceId: String?): Boolean =
            !deviceId.isNullOrBlank()
    }

    private val _showInputDialog = MutableStateFlow(false)
    val showInputDialog: StateFlow<Boolean> = _showInputDialog.asStateFlow()

    private val _screenRecordConfigRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val screenRecordConfigRequests: SharedFlow<Unit> = _screenRecordConfigRequests.asSharedFlow()

    private val _screenRecordState = MutableStateFlow(ScreenRecordUiState())
    val screenRecordState: StateFlow<ScreenRecordUiState> = _screenRecordState.asStateFlow()

    private var activeScreenRecordSession: ScreenRecordSession? = null

    fun executeAdbAction(
        type: AdbFunctionType
    ) {
        if (type == AdbFunctionType.DEVICE_MIRROR) {
            startDeviceMirror()
            return
        }

        if (type == AdbFunctionType.SCREEN_RECORD) {
            requestScreenRecordConfig()
            return
        }

        if (!ensureDeviceSelected()) {
            return
        }

        viewModelScope.launch {
            try {
                when (type) {
                    AdbFunctionType.INSTALL_APK -> installApp()
                    AdbFunctionType.INPUT_TEXT -> showInputDialog(true)
                    AdbFunctionType.DEVICE_MIRROR -> Unit
                    AdbFunctionType.SCREENSHOT -> screenShoot()
                    AdbFunctionType.SCREEN_RECORD -> Unit
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

    fun requestScreenRecordConfig() {
        if (!ensureDeviceSelected(autoDismiss = true)) return
        _screenRecordConfigRequests.tryEmit(Unit)
    }

    fun startScreenRecord(durationSeconds: Int?) {
        if (!ensureDeviceSelected(autoDismiss = true)) return
        if (_screenRecordState.value.isRecording || activeScreenRecordSession != null) {
            showTipDialog(MsgContent.Text(l10n("已有录屏任务进行中", "A screen recording is already running")), true)
            return
        }

        val normalizedDuration = durationSeconds?.coerceIn(1, SCREEN_RECORD_MAX_SECONDS)
        viewModelScope.launch {
            val deviceId = AdbTool.selectDeviceId.orEmpty()
            val folderPath = withContext(Dispatchers.IO) { FileUtils.selectFolder() }
            if (folderPath == null) {
                showTipDialog(MsgContent.Resource(Res.string.folder_not_selected), true)
                return@launch
            }

            val timestamp = System.currentTimeMillis()
            val remotePath = "/sdcard/record_$timestamp.mp4"
            val localPath = "$folderPath/record_$timestamp.mp4"
            val session = withContext(Dispatchers.IO) {
                createScreenRecordSession(
                    deviceId = deviceId,
                    remotePath = remotePath,
                    localPath = localPath,
                    durationSeconds = normalizedDuration
                )
            }

            if (session == null) {
                showTipDialog(MsgContent.Resource(Res.string.recording_failed), true)
                return@launch
            }

            activeScreenRecordSession = session
            _screenRecordState.value = ScreenRecordUiState(
                isRecording = true,
                localPath = localPath,
                durationSeconds = normalizedDuration,
                startedAtMillis = session.startedAtMillis
            )

            if (normalizedDuration == null) {
                showTipDialog(MsgContent.Text(l10n("录屏已开始，停止后自动保存。", "Recording started. Stop it to save automatically.")), true)
            } else {
                showTipDialog(MsgContent.Text(l10n("开始录屏（${normalizedDuration}秒）...", "Recording for ${normalizedDuration}s...")))
                val completed = withContext(Dispatchers.IO) {
                    waitForFixedScreenRecordProcess(session, normalizedDuration)
                }
                if (activeScreenRecordSession !== session) return@launch
                val success = withContext(Dispatchers.IO) {
                    finishScreenRecordSession(session, stopProcess = !completed)
                }
                activeScreenRecordSession = null
                _screenRecordState.value = ScreenRecordUiState()
                showScreenRecordResult(success, localPath)
            }
        }
    }

    fun stopScreenRecord() {
        val session = activeScreenRecordSession ?: run {
            _screenRecordState.value = ScreenRecordUiState()
            return
        }

        viewModelScope.launch {
            _screenRecordState.value = _screenRecordState.value.copy(isStopping = true)
            val success = withContext(Dispatchers.IO) {
                finishScreenRecordSession(session, stopProcess = true)
            }
            activeScreenRecordSession = null
            _screenRecordState.value = ScreenRecordUiState()
            showScreenRecordResult(success, session.localPath)
        }
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

    private suspend fun createScreenRecordSession(
        deviceId: String,
        remotePath: String,
        localPath: String,
        durationSeconds: Int?
    ): ScreenRecordSession? {
        val adbPath = AdbPathManager.getAdbPath() ?: return null
        return runCatching {
            val command = buildList {
                add(adbPath)
                if (deviceId.isNotBlank()) {
                    add("-s")
                    add(deviceId)
                }
                add("shell")
                add("screenrecord")
                if (durationSeconds != null) {
                    add("--time-limit")
                    add(durationSeconds.toString())
                }
                add(remotePath)
            }
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            ChildProcessRegistry.register(process)
            val outputFuture = CompletableFuture.supplyAsync {
                process.inputStream.bufferedReader().use { it.readText() }
            }
            ScreenRecordSession(
                process = process,
                outputFuture = outputFuture,
                deviceId = deviceId,
                remotePath = remotePath,
                localPath = localPath,
                durationSeconds = durationSeconds,
                startedAtMillis = System.currentTimeMillis()
            )
        }.getOrNull()
    }

    private fun waitForFixedScreenRecordProcess(session: ScreenRecordSession, durationSeconds: Int): Boolean {
        return runCatching {
            session.process.waitFor((durationSeconds + 10L) * 1000L, TimeUnit.MILLISECONDS)
        }.getOrDefault(false)
    }

    private fun finishScreenRecordSession(session: ScreenRecordSession, stopProcess: Boolean): Boolean {
        return runCatching {
            if (stopProcess) {
                requestRemoteScreenRecordStop(session.deviceId)
                stopScreenRecordProcess(session.process)
            }
            drainScreenRecordOutput(session)
            val pulled = AdbTool.pullFile(session.remotePath, session.localPath, session.deviceId)
            AdbTool.execShell(AdbTool.buildShellCommand("rm", "-f", session.remotePath), session.deviceId)
            pulled
        }.getOrDefault(false).also {
            runCatching { session.process.inputStream.close() }
            runCatching { session.process.outputStream.close() }
            if (session.process.isAlive) {
                runCatching { session.process.destroyForcibly() }
            }
            ChildProcessRegistry.unregister(session.process)
        }
    }

    private fun requestRemoteScreenRecordStop(deviceId: String) {
        runCatching {
            AdbTool.execShell("pidof screenrecord >/dev/null 2>&1 && kill -2 \$(pidof screenrecord)", deviceId)
        }
    }

    private fun stopScreenRecordProcess(process: Process) {
        if (!process.isAlive) return
        process.destroy()
        val stopped = runCatching {
            process.waitFor(SCREEN_RECORD_STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        }.getOrDefault(false)
        if (!stopped && process.isAlive) {
            process.destroyForcibly()
            runCatching { process.waitFor(SCREEN_RECORD_STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS) }
        }
    }

    private fun drainScreenRecordOutput(session: ScreenRecordSession): String {
        return runCatching {
            session.outputFuture.get(500L, TimeUnit.MILLISECONDS)
        }.getOrDefault("")
    }

    private fun showScreenRecordResult(success: Boolean, localPath: String) {
        val message = if (success) {
            MsgContent.Resource(Res.string.recording_saved, listOf(localPath))
        } else {
            MsgContent.Resource(Res.string.recording_failed)
        }
        showTipDialog(message, true)
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

    private fun startDeviceMirror() {
        if (!ensureDeviceSelected(autoDismiss = true)) return

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                AdbTool.startDeviceMirrorAsync()
            }
            val message = when {
                result.success -> MsgContent.Resource(Res.string.device_mirror_started)
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

    private fun ensureDeviceSelected(autoDismiss: Boolean = false): Boolean {
        if (deviceActionsEnabled(AdbTool.selectDeviceId)) return true
        showTipDialog(MsgContent.Resource(Res.string.no_device_available), autoDismiss = autoDismiss)
        return false
    }
}
