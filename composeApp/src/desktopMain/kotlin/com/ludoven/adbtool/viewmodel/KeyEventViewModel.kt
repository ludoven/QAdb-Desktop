package com.ludoven.adbtool.viewmodel

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.no_device_available
import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.util.AdbTool
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

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private val _recentKeyEvents = MutableStateFlow<List<KeyEventRecord>>(emptyList())
    val recentKeyEvents: StateFlow<List<KeyEventRecord>> = _recentKeyEvents.asStateFlow()

    fun clearRecentKeyEvents() {
        _recentKeyEvents.value = emptyList()
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
                showToastMessage("已发送: $keyName")
            } catch (e: Exception) {
                showToastMessage("发送失败: ${e.message}")
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
            showToastMessage("长按时长必须大于 0")
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    AdbTool.execShell("input keyevent --longpress $keyCode")
                }
                recordKeyEvent(keyCode, "$keyName (长按${durationMs}ms)")
                showToastMessage("已发送: $keyName (长按${durationMs}ms)")
            } catch (e: Exception) {
                showToastMessage("发送失败: ${e.message}")
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
            showToastMessage("请输入有效的数字 KeyCode")
            return
        }
        sendKeyEvent(keyCode, "KeyCode($keyCode)")
    }

    private fun ensureDeviceSelected(): Boolean {
        if (AdbTool.selectDeviceId != null) return true
        showTipDialog(MsgContent.Resource(Res.string.no_device_available))
        return false
    }

    private fun recordKeyEvent(keyCode: Int, keyName: String) {
        _recentKeyEvents.value = (listOf(KeyEventRecord(keyCode, keyName, LocalTime.now().format(timeFormatter))) + _recentKeyEvents.value).take(10)
    }
}
