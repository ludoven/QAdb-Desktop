package com.ludoven.adbtool.viewmodel

import androidx.lifecycle.ViewModel
import com.ludoven.adbtool.domain.terminal.TerminalController
import com.ludoven.adbtool.domain.terminal.TerminalSession
import kotlinx.coroutines.flow.StateFlow

class TerminalViewModel : ViewModel() {
    private val controller = TerminalController()

    val session: StateFlow<TerminalSession> = controller.session
    val commandInput: StateFlow<String> = controller.input

    fun syncDeviceState(
        devices: List<String>,
        selectedDevice: String?,
        deviceDisplayNames: Map<String, String>
    ) {
        controller.bindDeviceState(
            devices = devices,
            displayNames = deviceDisplayNames,
            selectedDevice = selectedDevice
        )
    }

    fun updateCommandInput(value: String) {
        controller.updateInput(value)
    }

    fun executeCommand() {
        controller.executeCurrentInput()
    }

    fun executeCommand(command: String) {
        controller.executeInput(command)
    }

    fun clearLogs() {
        controller.clearOutput()
    }

    fun clearInput() {
        controller.clearInput()
    }

    fun applyPreviousHistoryCommand() {
        controller.previousHistory()
    }

    fun applyNextHistoryCommand() {
        controller.nextHistory()
    }

    fun interruptCommand() {
        controller.interruptRunningCommand()
    }

    override fun onCleared() {
        controller.dispose()
        super.onCleared()
    }
}
