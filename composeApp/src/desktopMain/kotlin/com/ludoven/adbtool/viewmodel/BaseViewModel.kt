package com.ludoven.adbtool.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.util.AdbTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class BaseViewModel: ViewModel() {

    private val _dialogMessage = MutableStateFlow<MsgContent?>(null)
    val dialogMessage: StateFlow<MsgContent?> = _dialogMessage.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _toastMessage = MutableStateFlow<MsgContent?>(null)
    val feedbackToastMessage: StateFlow<MsgContent?> = _toastMessage.asStateFlow()

    private var dialogDismissJob: Job? = null
    private var toastDismissJob: Job? = null

    fun showTipDialog(tag: MsgContent, autoDismiss: Boolean = false, delayMillis: Long = 2000L) {
        dialogDismissJob?.cancel()
        dialogDismissJob = viewModelScope.launch {
            _dialogMessage.value = tag
            _showDialog.value = true
            if (autoDismiss) {
                delay(delayMillis)
                _showDialog.value = false
                _dialogMessage.value = null
            }
        }
    }

    fun showToast(tag: MsgContent, delayMillis: Long = 2400L) {
        toastDismissJob?.cancel()
        toastDismissJob = viewModelScope.launch {
            _toastMessage.value = tag
            delay(delayMillis)
            _toastMessage.value = null
        }
    }

    fun dismissTipDialog() {
        dialogDismissJob?.cancel()
        _showDialog.value = false
        _dialogMessage.value = null
    }

    fun dismissToast() {
        toastDismissJob?.cancel()
        _toastMessage.value = null
    }

     suspend fun execResult(command: String) {
        val result = withContext(Dispatchers.IO) {
            AdbTool.exec(command)
        }
        if (result.isBlank()) {
            showToast(MsgContent.Text(com.ludoven.adbtool.util.l10n("操作已完成", "Operation completed")))
        } else {
            showTipDialog(MsgContent.Text(result))
        }
    }
}
