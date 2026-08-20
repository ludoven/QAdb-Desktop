package com.ludoven.adbtool

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface AppMenuCommand {
    data class Navigate(val route: String) : AppMenuCommand
    data object OpenWirelessConnection : AppMenuCommand

    data object RefreshDevices : AppMenuCommand
    data object RebootDevice : AppMenuCommand
    data object Screenshot : AppMenuCommand
    data object ScreenRecord : AppMenuCommand

    data object OpenTerminalTool : AppMenuCommand
    data object InstallApk : AppMenuCommand
    data object ViewCurrentActivity : AppMenuCommand
    data object ViewDeviceInfo : AppMenuCommand

    data object ExportLogs : AppMenuCommand
}

object AppMenuCommandBus {
    private val _commands = MutableSharedFlow<AppMenuCommand>(extraBufferCapacity = 64)
    val commands: SharedFlow<AppMenuCommand> = _commands.asSharedFlow()

    fun dispatch(command: AppMenuCommand) {
        _commands.tryEmit(command)
    }
}
