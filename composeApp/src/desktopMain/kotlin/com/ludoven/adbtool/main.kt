@file:OptIn(ExperimentalMaterial3Api::class)

package com.ludoven.adbtool

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.ic_logo
import adbtool_desktop.composeapp.generated.resources.menu_about_qadb
import adbtool_desktop.composeapp.generated.resources.menu_about_qadb_message
import adbtool_desktop.composeapp.generated.resources.menu_always_on_top
import adbtool_desktop.composeapp.generated.resources.menu_check_update
import adbtool_desktop.composeapp.generated.resources.menu_close
import adbtool_desktop.composeapp.generated.resources.menu_connect_device
import adbtool_desktop.composeapp.generated.resources.menu_connect_device_prompt
import adbtool_desktop.composeapp.generated.resources.menu_connect_device_title
import adbtool_desktop.composeapp.generated.resources.menu_device
import adbtool_desktop.composeapp.generated.resources.menu_device_info
import adbtool_desktop.composeapp.generated.resources.menu_docs
import adbtool_desktop.composeapp.generated.resources.menu_exit
import adbtool_desktop.composeapp.generated.resources.menu_export_logs
import adbtool_desktop.composeapp.generated.resources.menu_feedback
import adbtool_desktop.composeapp.generated.resources.menu_file
import adbtool_desktop.composeapp.generated.resources.menu_github
import adbtool_desktop.composeapp.generated.resources.menu_go_app
import adbtool_desktop.composeapp.generated.resources.menu_go_common
import adbtool_desktop.composeapp.generated.resources.menu_go_file
import adbtool_desktop.composeapp.generated.resources.menu_go_home
import adbtool_desktop.composeapp.generated.resources.menu_go_keyevent
import adbtool_desktop.composeapp.generated.resources.menu_go_log
import adbtool_desktop.composeapp.generated.resources.menu_go_process
import adbtool_desktop.composeapp.generated.resources.menu_go_setting
import adbtool_desktop.composeapp.generated.resources.menu_go_terminal
import adbtool_desktop.composeapp.generated.resources.menu_help
import adbtool_desktop.composeapp.generated.resources.menu_import_apk
import adbtool_desktop.composeapp.generated.resources.menu_install_apk
import adbtool_desktop.composeapp.generated.resources.menu_minimize
import adbtool_desktop.composeapp.generated.resources.menu_off
import adbtool_desktop.composeapp.generated.resources.menu_on
import adbtool_desktop.composeapp.generated.resources.menu_open_directory
import adbtool_desktop.composeapp.generated.resources.menu_page_navigation
import adbtool_desktop.composeapp.generated.resources.menu_qadb
import adbtool_desktop.composeapp.generated.resources.menu_reboot_device
import adbtool_desktop.composeapp.generated.resources.menu_refresh_devices
import adbtool_desktop.composeapp.generated.resources.menu_screen_record
import adbtool_desktop.composeapp.generated.resources.menu_screenshot
import adbtool_desktop.composeapp.generated.resources.menu_settings
import adbtool_desktop.composeapp.generated.resources.menu_shortcuts
import adbtool_desktop.composeapp.generated.resources.menu_shortcuts_message
import adbtool_desktop.composeapp.generated.resources.menu_terminal
import adbtool_desktop.composeapp.generated.resources.menu_toggle_dark_theme
import adbtool_desktop.composeapp.generated.resources.menu_tools
import adbtool_desktop.composeapp.generated.resources.menu_view
import adbtool_desktop.composeapp.generated.resources.menu_view_activity
import adbtool_desktop.composeapp.generated.resources.menu_window
import adbtool_desktop.composeapp.generated.resources.menu_zoom_in
import adbtool_desktop.composeapp.generated.resources.menu_zoom_out
import adbtool_desktop.composeapp.generated.resources.menu_zoom_reset
import com.ludoven.adbtool.ui.mac.*
import com.ludoven.adbtool.ui.mac.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuBarScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.swing.JOptionPane
import kotlinx.coroutines.launch
import com.ludoven.adbtool.util.ThemeManager
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val DOCS_URL = "https://ludoven.github.io/QADB/"
private const val GITHUB_URL = "https://github.com/ludoven/QADB"
private const val FEEDBACK_URL = "https://github.com/ludoven/QADB/issues"

fun main() = application {
    val isWindows = System.getProperty("os.name").contains("windows", ignoreCase = true)
    val windowState = rememberWindowState(
        width = 1200.dp,
        height = 800.dp,
        position = WindowPosition(Alignment.Center)
    )
    var alwaysOnTop by remember { mutableStateOf(false) }
    val currentThemeMode by ThemeManager.currentThemeMode.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "QADB",
        icon = painterResource(Res.drawable.ic_logo),
        alwaysOnTop = alwaysOnTop
    ) {
        if (!isWindows) {
            MenuBar {
                AppMenuBar(
                    alwaysOnTop = alwaysOnTop,
                    onAlwaysOnTopToggle = { alwaysOnTop = !alwaysOnTop },
                    onClose = ::exitApplication,
                    onMinimize = { windowState.isMinimized = true },
                    onZoomIn = { resizeWindow(windowState, 1.08f) },
                    onZoomOut = { resizeWindow(windowState, 0.92f) },
                    onZoomReset = { windowState.size = DpSize(1200.dp, 800.dp) },
                    onToggleTheme = {
                        val nextMode = if (currentThemeMode == ThemeManager.ThemeMode.DARK) {
                            ThemeManager.ThemeMode.LIGHT
                        } else {
                            ThemeManager.ThemeMode.DARK
                        }
                        ThemeManager.setThemeMode(nextMode)
                    },
                    onConnectDevice = { address ->
                        coroutineScope.launch {
                            AppMenuCommandBus.dispatch(AppMenuCommand.ConnectDevice(address))
                        }
                    }
                )
            }
        }
        App()
    }
}

@Composable
private fun MenuBarScope.AppMenuBar(
    alwaysOnTop: Boolean,
    onAlwaysOnTopToggle: () -> Unit,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    onToggleTheme: () -> Unit,
    onConnectDevice: (String) -> Unit
) {
    val menuQadb = stringResource(Res.string.menu_qadb)
    val menuFile = stringResource(Res.string.menu_file)
    val menuDevice = stringResource(Res.string.menu_device)
    val menuTools = stringResource(Res.string.menu_tools)
    val menuView = stringResource(Res.string.menu_view)
    val menuWindow = stringResource(Res.string.menu_window)
    val menuHelp = stringResource(Res.string.menu_help)
    val menuOn = stringResource(Res.string.menu_on)
    val menuOff = stringResource(Res.string.menu_off)
    val menuAboutQadb = stringResource(Res.string.menu_about_qadb)
    val menuAboutQadbMessage = stringResource(Res.string.menu_about_qadb_message)
    val menuConnectDevicePrompt = stringResource(Res.string.menu_connect_device_prompt)
    val menuConnectDeviceTitle = stringResource(Res.string.menu_connect_device_title)
    val menuShortcuts = stringResource(Res.string.menu_shortcuts)
    val menuShortcutsMessage = stringResource(Res.string.menu_shortcuts_message)

    Menu(menuQadb) {
        Item(menuAboutQadb) {
            showInfoDialog(
                title = menuAboutQadb,
                message = menuAboutQadbMessage
            )
        }
        Item(stringResource(Res.string.menu_settings)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("setting")) }
        Item(stringResource(Res.string.menu_check_update)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("setting")) }
        Separator()
        Item(stringResource(Res.string.menu_exit), onClick = onClose)
    }

    Menu(menuFile) {
        Item(stringResource(Res.string.menu_import_apk)) { AppMenuCommandBus.dispatch(AppMenuCommand.InstallApk) }
        Item(stringResource(Res.string.menu_export_logs)) { AppMenuCommandBus.dispatch(AppMenuCommand.ExportLogs) }
        Separator()
        Item(stringResource(Res.string.menu_open_directory)) { openDirectory(File(System.getProperty("user.home"))) }
    }

    Menu(menuDevice) {
        Item(stringResource(Res.string.menu_connect_device)) {
            val input = JOptionPane.showInputDialog(
                null,
                menuConnectDevicePrompt,
                menuConnectDeviceTitle,
                JOptionPane.PLAIN_MESSAGE
            )
            val address = input?.trim().orEmpty()
            if (address.isNotBlank()) onConnectDevice(address)
        }
        Item(stringResource(Res.string.menu_refresh_devices)) { AppMenuCommandBus.dispatch(AppMenuCommand.RefreshDevices) }
        Item(stringResource(Res.string.menu_reboot_device)) { AppMenuCommandBus.dispatch(AppMenuCommand.RebootDevice) }
        Separator()
        Item(stringResource(Res.string.menu_screenshot)) { AppMenuCommandBus.dispatch(AppMenuCommand.Screenshot) }
        Item(stringResource(Res.string.menu_screen_record)) { AppMenuCommandBus.dispatch(AppMenuCommand.ScreenRecord) }
    }

    Menu(menuTools) {
        Item(stringResource(Res.string.menu_terminal)) { AppMenuCommandBus.dispatch(AppMenuCommand.OpenTerminalTool) }
        Item(stringResource(Res.string.menu_install_apk)) { AppMenuCommandBus.dispatch(AppMenuCommand.InstallApk) }
        Item(stringResource(Res.string.menu_view_activity)) { AppMenuCommandBus.dispatch(AppMenuCommand.ViewCurrentActivity) }
        Item(stringResource(Res.string.menu_device_info)) { AppMenuCommandBus.dispatch(AppMenuCommand.ViewDeviceInfo) }
    }

    Menu(menuView) {
        Menu(stringResource(Res.string.menu_page_navigation)) {
            Item(stringResource(Res.string.menu_go_home)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("home")) }
            Item(stringResource(Res.string.menu_go_common)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("common")) }
            Item(stringResource(Res.string.menu_go_terminal)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("terminal")) }
            Item(stringResource(Res.string.menu_go_keyevent)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("keyevent")) }
            Item(stringResource(Res.string.menu_go_app)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("app")) }
            Item(stringResource(Res.string.menu_go_file)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("filebrowser")) }
            Item(stringResource(Res.string.menu_go_log)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("log")) }
            Item(stringResource(Res.string.menu_go_process)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("process")) }
            Item(stringResource(Res.string.menu_go_setting)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("setting")) }
        }
        Separator()
        Item(stringResource(Res.string.menu_toggle_dark_theme)) { onToggleTheme() }
        Separator()
        Item(stringResource(Res.string.menu_zoom_in)) { onZoomIn() }
        Item(stringResource(Res.string.menu_zoom_out)) { onZoomOut() }
        Item(stringResource(Res.string.menu_zoom_reset)) { onZoomReset() }
    }

    Menu(menuWindow) {
        Item(stringResource(Res.string.menu_minimize), onClick = onMinimize)
        Item(stringResource(Res.string.menu_close), onClick = onClose)
        Separator()
        Item(
            stringResource(
                Res.string.menu_always_on_top,
                if (alwaysOnTop) menuOn else menuOff
            ),
            onClick = onAlwaysOnTopToggle
        )
    }

    Menu(menuHelp) {
        Item(stringResource(Res.string.menu_docs)) { openUrl(DOCS_URL) }
        Item(stringResource(Res.string.menu_github)) { openUrl(GITHUB_URL) }
        Item(stringResource(Res.string.menu_feedback)) { openUrl(FEEDBACK_URL) }
        Separator()
        Item(menuShortcuts) {
            showInfoDialog(
                title = menuShortcuts,
                message = menuShortcutsMessage
            )
        }
    }
}

private fun resizeWindow(windowState: WindowState, scale: Float) {
    val width = (windowState.size.width.value * scale).coerceIn(960f, 2400f).dp
    val height = (windowState.size.height.value * scale).coerceIn(640f, 1600f).dp
    windowState.size = DpSize(width, height)
}

private fun openUrl(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}

private fun openDirectory(dir: File) {
    runCatching {
        if (!dir.exists()) dir.mkdirs()
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(dir)
        }
    }
}

private fun showInfoDialog(title: String, message: String) {
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE)
}
