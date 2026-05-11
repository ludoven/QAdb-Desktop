@file:OptIn(ExperimentalMaterial3Api::class)

package com.ludoven.adbtool

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.ic_logo
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
    Menu("QADB") {
        Item("关于 QADB") {
            showInfoDialog(
                title = "关于 QADB",
                message = "QADB\n开源、跨平台、现代化的 ADB 桌面调试工具。"
            )
        }
        Item("设置") { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("setting")) }
        Item("检查更新") { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("setting")) }
        Separator()
        Item("退出", onClick = onClose)
    }

    Menu("文件") {
        Item("导入 APK") { AppMenuCommandBus.dispatch(AppMenuCommand.InstallApk) }
        Item("导出日志") { AppMenuCommandBus.dispatch(AppMenuCommand.ExportLogs) }
        Separator()
        Item("打开目录") { openDirectory(File(System.getProperty("user.home"))) }
    }

    Menu("设备") {
        Item("连接设备...") {
            val input = JOptionPane.showInputDialog(
                null,
                "请输入设备地址（例如 192.168.1.100:5555）",
                "连接设备",
                JOptionPane.PLAIN_MESSAGE
            )
            val address = input?.trim().orEmpty()
            if (address.isNotBlank()) onConnectDevice(address)
        }
        Item("刷新设备") { AppMenuCommandBus.dispatch(AppMenuCommand.RefreshDevices) }
        Item("重启设备") { AppMenuCommandBus.dispatch(AppMenuCommand.RebootDevice) }
        Separator()
        Item("截图") { AppMenuCommandBus.dispatch(AppMenuCommand.Screenshot) }
        Item("录屏") { AppMenuCommandBus.dispatch(AppMenuCommand.ScreenRecord) }
    }

    Menu("工具") {
        Item("终端") { AppMenuCommandBus.dispatch(AppMenuCommand.OpenTerminalTool) }
        Item("安装 APK") { AppMenuCommandBus.dispatch(AppMenuCommand.InstallApk) }
        Item("查看 Activity") { AppMenuCommandBus.dispatch(AppMenuCommand.ViewCurrentActivity) }
        Item("设备信息") { AppMenuCommandBus.dispatch(AppMenuCommand.ViewDeviceInfo) }
    }

    Menu("视图") {
        Menu("页面跳转") {
            Item("首页") { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("home")) }
            Item("常用") { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("common")) }
            Item("终端") { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("terminal")) }
            Item("按键") { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("keyevent")) }
            Item("应用") { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("app")) }
            Item("文件") { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("filebrowser")) }
            Item("日志") { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("log")) }
            Item("进程") { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("process")) }
            Item("设置") { AppMenuCommandBus.dispatch(AppMenuCommand.Navigate("setting")) }
        }
        Separator()
        Item("切换深色主题") { onToggleTheme() }
        Separator()
        Item("放大") { onZoomIn() }
        Item("缩小") { onZoomOut() }
        Item("重置缩放") { onZoomReset() }
    }

    Menu("窗口") {
        Item("最小化", onClick = onMinimize)
        Item("关闭", onClick = onClose)
        Separator()
        Item("置顶: ${if (alwaysOnTop) "开" else "关"}", onClick = onAlwaysOnTopToggle)
    }

    Menu("帮助") {
        Item("文档") { openUrl(DOCS_URL) }
        Item("GitHub") { openUrl(GITHUB_URL) }
        Item("反馈") { openUrl(FEEDBACK_URL) }
        Separator()
        Item("快捷键") {
            showInfoDialog(
                title = "快捷键",
                message = """
                常用快捷方式建议：
                - 刷新设备：菜单「设备 > 刷新设备」
                - 终端：菜单「工具 > 终端」
                - 截图/录屏：菜单「设备 > 截图/录屏」
                - 页面跳转：菜单「视图 > 页面跳转」
                """.trimIndent()
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
