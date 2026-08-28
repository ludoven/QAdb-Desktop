@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)

package com.ludoven.adbtool

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.ic_logo
import adbtool_desktop.composeapp.generated.resources.menu_about_qadb
import adbtool_desktop.composeapp.generated.resources.menu_about_qadb_message
import adbtool_desktop.composeapp.generated.resources.menu_always_on_top
import adbtool_desktop.composeapp.generated.resources.menu_check_update
import adbtool_desktop.composeapp.generated.resources.menu_close
import adbtool_desktop.composeapp.generated.resources.menu_connect_device
import adbtool_desktop.composeapp.generated.resources.menu_device
import adbtool_desktop.composeapp.generated.resources.menu_device_info
import adbtool_desktop.composeapp.generated.resources.menu_docs
import adbtool_desktop.composeapp.generated.resources.menu_exit
import adbtool_desktop.composeapp.generated.resources.menu_export_logs
import adbtool_desktop.composeapp.generated.resources.menu_feedback
import adbtool_desktop.composeapp.generated.resources.menu_file
import adbtool_desktop.composeapp.generated.resources.menu_github
import adbtool_desktop.composeapp.generated.resources.menu_go_app
import adbtool_desktop.composeapp.generated.resources.menu_go_device
import adbtool_desktop.composeapp.generated.resources.menu_go_file
import adbtool_desktop.composeapp.generated.resources.menu_go_home
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuBarScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberWindowState
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JOptionPane
import kotlin.system.exitProcess
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.AppBehaviorPreferences
import com.ludoven.adbtool.util.LanguageManager
import com.ludoven.adbtool.util.LocalAppLanguage
import com.ludoven.adbtool.util.ThemeManager
import com.ludoven.adbtool.util.l10n
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val DOCS_URL = "https://ludoven.github.io/QADB/"
private const val GITHUB_URL = "https://github.com/ludoven/QADB"
private const val FEEDBACK_URL = "https://github.com/ludoven/QADB/issues"

fun main() = application {
    val isMacOs = System.getProperty("os.name").contains("mac", ignoreCase = true)
    val isWindows = System.getProperty("os.name").contains("windows", ignoreCase = true)
    val baseAppIcon = painterResource(Res.drawable.ic_logo)
    val appIcon = remember(baseAppIcon, isMacOs) {
        if (isMacOs) RoundedMacDockIconPainter(baseAppIcon) else baseAppIcon
    }
    val windowState = rememberWindowState(
        width = 1200.dp,
        height = 800.dp,
        position = WindowPosition(Alignment.Center)
    )
    val behaviorPreferences = remember { AppBehaviorPreferences.store }
    val minimizeToTrayOnLaunch by behaviorPreferences.minimizeToTrayOnLaunch.collectAsState()
    val minimizeToTrayOnClose by behaviorPreferences.minimizeToTrayOnClose.collectAsState()
    var isWindowVisible by remember {
        mutableStateOf(!behaviorPreferences.minimizeToTrayOnLaunch.value || !isTraySupported)
    }
    var alwaysOnTop by remember { mutableStateOf(false) }
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val currentThemeMode by ThemeManager.currentThemeMode.collectAsState()
    val useDarkTheme = when (currentThemeMode) {
        ThemeManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeManager.ThemeMode.LIGHT -> false
        ThemeManager.ThemeMode.DARK -> true
    }
    val isClosing = remember { AtomicBoolean(false) }
    val shutdownAndExit: () -> Unit = shutdownAndExit@{
        if (!isClosing.compareAndSet(false, true)) {
            return@shutdownAndExit
        }

        try {
            AdbTool.shutdownRelatedProcesses()
        } finally {
            exitProcess(0)
        }
    }
    val showMainWindow: () -> Unit = {
        isWindowVisible = true
        windowState.isMinimized = false
    }
    val handleWindowClose: () -> Unit = {
        if (minimizeToTrayOnClose && isTraySupported) {
            isWindowVisible = false
        } else {
            shutdownAndExit()
        }
    }

    CompositionLocalProvider(LocalAppLanguage provides currentLanguage) {
        if (isTraySupported && (minimizeToTrayOnLaunch || minimizeToTrayOnClose || !isWindowVisible)) {
            Tray(
                icon = appIcon,
                tooltip = "QADB",
                onAction = showMainWindow
            ) {
                Item(l10n("显示 QADB", "Show QADB"), onClick = showMainWindow)
                Separator()
                Item(stringResource(Res.string.menu_exit), onClick = shutdownAndExit)
            }
        }
        Window(
            onCloseRequest = handleWindowClose,
            state = windowState,
            visible = isWindowVisible,
            title = "QADB",
            icon = appIcon,
            decoration = if (isWindows) {
                WindowDecoration.Undecorated()
            } else {
                WindowDecoration.SystemDefault
            },
            alwaysOnTop = alwaysOnTop
        ) {
            if (!isWindows) {
                MenuBar {
                    AppMenuBar(
                        alwaysOnTop = alwaysOnTop,
                        onAlwaysOnTopToggle = { alwaysOnTop = !alwaysOnTop },
                        onClose = shutdownAndExit,
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
                        }
                    )
                }
            }
            AppWindowFrame(
                isMacOs = isMacOs,
                isWindows = isWindows,
                useDarkTheme = useDarkTheme,
                windowState = windowState,
                onClose = handleWindowClose
            ) {
                App()
            }
        }
    }
}

private class RoundedMacDockIconPainter(
    private val delegate: Painter
) : Painter() {
    override val intrinsicSize = delegate.intrinsicSize

    override fun DrawScope.onDraw() {
        val radius = size.minDimension * 0.22f
        clipPath(Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, radius, radius))
        }) {
            with(delegate) { draw(size) }
        }
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
    onToggleTheme: () -> Unit
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
            AppMenuCommandBus.dispatch(AppMenuCommand.OpenWirelessConnection)
        }
        Item(stringResource(Res.string.menu_refresh_devices)) { AppMenuCommandBus.dispatch(AppMenuCommand.RefreshDevices) }
        Item(stringResource(Res.string.menu_reboot_device)) { AppMenuCommandBus.dispatch(AppMenuCommand.RebootDevice) }
        Separator()
        Item(stringResource(Res.string.menu_screenshot)) { AppMenuCommandBus.dispatch(AppMenuCommand.Screenshot) }
        Item(stringResource(Res.string.menu_screen_record)) { AppMenuCommandBus.dispatch(AppMenuCommand.ScreenRecord) }
    }

    Menu(menuTools) {
        Item(l10n("命令中心", "Command Center")) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("common")) }
        Item(stringResource(Res.string.menu_terminal)) { AppMenuCommandBus.dispatch(AppMenuCommand.OpenTerminalTool) }
        Item(stringResource(Res.string.menu_install_apk)) { AppMenuCommandBus.dispatch(AppMenuCommand.InstallApk) }
        Item(stringResource(Res.string.menu_view_activity)) { AppMenuCommandBus.dispatch(AppMenuCommand.ViewCurrentActivity) }
        Item(stringResource(Res.string.menu_device_info)) { AppMenuCommandBus.dispatch(AppMenuCommand.ViewDeviceInfo) }
    }

    Menu(menuView) {
        Menu(stringResource(Res.string.menu_page_navigation)) {
            Item(stringResource(Res.string.menu_go_home)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("home")) }
            Item(stringResource(Res.string.menu_go_device)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("home")) }
            Item(l10n("前往设备控制", "Go to Device Control")) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("device-control")) }
            Item(stringResource(Res.string.menu_go_app)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("app")) }
            Item(stringResource(Res.string.menu_go_file)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("filebrowser")) }
            Item(l10n("前往诊断", "Go to Diagnostics")) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("diagnostics")) }
            Item(stringResource(Res.string.menu_go_terminal)) { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("terminal")) }
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
