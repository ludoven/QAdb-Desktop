package com.ludoven.adbtool.viewmodel

import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.l10n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class KeyEventRecord(
    val code: Int,
    val name: String,
    val sentAt: String = ""
) {
    val adbCommand: String = "adb shell input keyevent $code"
    val displayText: String = "$name ($code)"
}

class KeyEventViewModel : BaseViewModel() {
    companion object {
        internal fun keyEventDeviceActionsEnabled(deviceId: String?): Boolean =
            !deviceId.isNullOrBlank()
    }

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private val _recentKeyEvents = MutableStateFlow<List<KeyEventRecord>>(emptyList())
    val recentKeyEvents: StateFlow<List<KeyEventRecord>> = _recentKeyEvents.asStateFlow()

    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput.asStateFlow()

    private val _isSendingText = MutableStateFlow(false)
    val isSendingText: StateFlow<Boolean> = _isSendingText.asStateFlow()

    private val _textSendMessage = MutableStateFlow<String?>(null)
    val textSendMessage: StateFlow<String?> = _textSendMessage.asStateFlow()

    fun clearRecentKeyEvents() {
        _recentKeyEvents.value = emptyList()
    }

    fun updateTextInput(text: String) {
        _textInput.value = text
        _textSendMessage.value = null
    }

    fun sendText(deviceId: String?) {
        val text = _textInput.value.trim()
        when {
            deviceId.isNullOrBlank() -> _textSendMessage.value = l10n("没有连接设备", "No device connected")
            text.isBlank() -> _textSendMessage.value = l10n("输入文本不能为空", "Text cannot be empty")
            else -> viewModelScope.launch {
                _isSendingText.value = true
                val result = withContext(Dispatchers.IO) {
                    AdbTool.inputTextAsync(text, deviceId)
                }
                _textSendMessage.value = if (result.success) {
                    _textInput.value = ""
                    l10n("文本已发送到设备", "Text sent to device")
                } else {
                    result.errorMessage ?: result.output.ifBlank { l10n("发送失败", "Send failed") }
                }
                _isSendingText.value = false
            }
        }
    }

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _showToast = MutableStateFlow(false)
    val showToast: StateFlow<Boolean> = _showToast.asStateFlow()

    private fun showToastMessage(message: String) {
        viewModelScope.launch {
            _toastMessage.value = message
            _showToast.value = true
            delay(2000)
            _showToast.value = false
            _toastMessage.value = null
        }
    }

    /**
     * 发送按键事件
     * @param keyCode Android KeyEvent KeyCode 值
     * @param keyName 用于显示的按键名称
     */
    fun sendKeyEvent(keyCode: Int, keyName: String = keyCode.toString()) {
        if (!ensureDeviceSelected()) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    AdbTool.execShell("input keyevent $keyCode")
                }
                recordKeyEvent(keyCode, keyName)
                showToastMessage(l10n("已发送: $keyName", "Sent: $keyName"))
            } catch (e: Exception) {
                showToastMessage(l10n("发送失败: ${e.message}", "Send failed: ${e.message}"))
            }
        }
    }

    /**
     * 长按按键事件
     * @param keyCode Android KeyEvent KeyCode 值
     * @param durationMs 长按时长（毫秒）
     * @param keyName 用于显示的按键名称
     */
    fun sendLongPressEvent(keyCode: Int, durationMs: Long, keyName: String = keyCode.toString()) {
        if (!ensureDeviceSelected()) return
        if (durationMs <= 0) {
            showToastMessage(l10n("长按时长必须大于 0", "Long-press duration must be greater than 0"))
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    AdbTool.execShell("input keyevent --longpress $keyCode")
                }
                recordKeyEvent(keyCode, l10n("$keyName (长按${durationMs}ms)", "$keyName (long press ${durationMs}ms)"))
                showToastMessage(l10n("已发送: $keyName (长按${durationMs}ms)", "Sent: $keyName (long press ${durationMs}ms)"))
            } catch (e: Exception) {
                showToastMessage(l10n("发送失败: ${e.message}", "Send failed: ${e.message}"))
            }
        }
    }

    /**
     * 通过自定义 KeyCode 发送按键
     * @param keyCodeStr 用户输入的 KeyCode 字符串
     */
    fun sendCustomKeyEvent(keyCodeStr: String) {
        val keyCode = keyCodeStr.trim().toIntOrNull()
        if (keyCode == null) {
            showToastMessage(l10n("请输入有效的数字 KeyCode", "Enter a valid numeric KeyCode"))
            return
        }
        sendKeyEvent(keyCode, "KeyCode($keyCode)")
    }

    private fun ensureDeviceSelected(): Boolean {
        if (keyEventDeviceActionsEnabled(AdbTool.selectDeviceId)) return true
        showToastMessage(l10n("没有连接设备", "No device connected"))
        return false
    }

    private fun recordKeyEvent(keyCode: Int, keyName: String) {
        _recentKeyEvents.value = (listOf(KeyEventRecord(keyCode, keyName, LocalTime.now().format(timeFormatter))) + _recentKeyEvents.value).take(10)
    }
}
