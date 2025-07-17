package com.ludoven.adbtool.viewmodel

import androidx.lifecycle.ViewModel
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

open class BaseViewModel: ViewModel() {

    private val _dialogMessage = MutableStateFlow<MsgContent?>(null)
    val dialogMessage: StateFlow<MsgContent?> = _dialogMessage.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    fun showTipDialog(tag: MsgContent, autoDismiss: Boolean = false, delayMillis: Long = 2000L) {
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

     suspend fun execResult(command: String) {
        val result = withContext(Dispatchers.IO) {
            AdbTool.exec(command)
        }
        showTipDialog(MsgContent.Text(result))
    }
}