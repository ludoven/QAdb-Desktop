package com.ludoven.adbtool

import com.ludoven.adbtool.ui.mac.*

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.app
import adbtool_desktop.composeapp.generated.resources.common
import adbtool_desktop.composeapp.generated.resources.file_browser
import adbtool_desktop.composeapp.generated.resources.home
import adbtool_desktop.composeapp.generated.resources.key_event_page
import adbtool_desktop.composeapp.generated.resources.log
import adbtool_desktop.composeapp.generated.resources.set
import adbtool_desktop.composeapp.generated.resources.terminal
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.ludoven.adbtool.ui.icons.IconParkIcons
import com.ludoven.adbtool.ui.mac.ExperimentalMaterial3Api
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Alignment
import com.ludoven.adbtool.pages.AppScreen
import com.ludoven.adbtool.pages.CommonScreen
import com.ludoven.adbtool.pages.FileBrowserScreen
import com.ludoven.adbtool.pages.HomeScreen
import com.ludoven.adbtool.pages.KeyEventScreen
import com.ludoven.adbtool.pages.LogScreen
import com.ludoven.adbtool.pages.DeviceMirrorScreen
import com.ludoven.adbtool.pages.ProcessScreen
import com.ludoven.adbtool.widget.SidebarGroup
import com.ludoven.adbtool.pages.SettingScreen
import com.ludoven.adbtool.pages.TerminalScreen
import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.util.LanguageManager
import com.ludoven.adbtool.util.ThemeManager
import com.ludoven.adbtool.entity.AdbFunctionType
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.viewmodel.AppViewModel
import com.ludoven.adbtool.viewmodel.CommonModel
import com.ludoven.adbtool.viewmodel.DeviceMirrorViewModel
import com.ludoven.adbtool.viewmodel.DevicesViewModel
import com.ludoven.adbtool.viewmodel.FileBrowserViewModel
import com.ludoven.adbtool.viewmodel.KeyEventViewModel
import com.ludoven.adbtool.viewmodel.LogViewModel
import com.ludoven.adbtool.viewmodel.TerminalViewModel
import com.ludoven.adbtool.widget.GlassCard
import com.ludoven.adbtool.widget.Sidebar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val devicesViewModel: DevicesViewModel = viewModel()
    val appViewModel: AppViewModel = viewModel()
    val commonModel: CommonModel = viewModel()
    val deviceMirrorViewModel: DeviceMirrorViewModel = viewModel()
    val keyEventViewModel: KeyEventViewModel = viewModel()
    val logViewModel: LogViewModel = viewModel()
    val terminalViewModel: TerminalViewModel = viewModel()
    val fileBrowserViewModel: FileBrowserViewModel = viewModel()
    LaunchedEffect(Unit) {
        LanguageManager.initialize()
        ThemeManager.initialize()
        AdbPathManager.getAdbPath()
    }

    val tabGroups = listOf(
        SidebarGroup(
            id = "home",
            label = stringResource(Res.string.home),
            icon = IconParkIcons.Home,
            defaultRoute = "home",
            items = listOf(TabItem(stringResource(Res.string.home), IconParkIcons.Home, "home")),
            collapsible = false
        ),
        SidebarGroup(
            id = "common",
            label = stringResource(Res.string.common),
            icon = IconParkIcons.Info,
            defaultRoute = "common",
            items = listOf(TabItem(stringResource(Res.string.common), IconParkIcons.Info, "common")),
            collapsible = false
        ),
        SidebarGroup(
            id = "app",
            label = stringResource(Res.string.app),
            icon = IconParkIcons.Application,
            defaultRoute = "app",
            items = listOf(TabItem(stringResource(Res.string.app), IconParkIcons.Application, "app")),
            collapsible = false
        ),
        SidebarGroup(
            id = "terminal",
            label = stringResource(Res.string.terminal),
            icon = IconParkIcons.Terminal,
            defaultRoute = "terminal",
            items = listOf(TabItem(stringResource(Res.string.terminal), IconParkIcons.Terminal, "terminal")),
            collapsible = false
        ),
        SidebarGroup(
            id = "device",
            label = l10n("设备操作", "Device Ops"),
            icon = IconParkIcons.Phone,
            defaultRoute = "mirror",
            items = listOf(
                TabItem(l10n("镜像", "Mirror"), IconParkIcons.CastScreen, "mirror"),
                TabItem(stringResource(Res.string.key_event_page), IconParkIcons.GameHandle, "keyevent"),
                TabItem(stringResource(Res.string.file_browser), IconParkIcons.Folder, "filebrowser"),
            )
        ),
        SidebarGroup(
            id = "diagnostics",
            label = l10n("诊断分析", "Diagnostics"),
            icon = IconParkIcons.ChartLine,
            defaultRoute = "log",
            items = listOf(
                TabItem(stringResource(Res.string.log), IconParkIcons.List, "log"),
                TabItem(l10n("进程", "Process"), IconParkIcons.Phone, "process"),
            )
        ),
        SidebarGroup(
            id = "settings",
            label = stringResource(Res.string.set),
            icon = IconParkIcons.Setting,
            defaultRoute = "setting",
            items = listOf(TabItem(stringResource(Res.string.set), IconParkIcons.Setting, "setting")),
            collapsible = false
        )
    )

    val navController = rememberNavController()
    val stateHolder = rememberSaveableStateHolder()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "home"
    val usePlainContentContainer = currentRoute == "setting"
    var showRebootConfirm by remember { mutableStateOf(false) }
    val currentThemeMode by ThemeManager.currentThemeMode.collectAsState()
    val resolvedDarkTheme = when (currentThemeMode) {
        ThemeManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeManager.ThemeMode.LIGHT -> false
        ThemeManager.ThemeMode.DARK -> true
    }

    fun navigateToRoute(route: String) {
        if (currentRoute == route) return
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
        }
    }

    fun handleGlobalShortcut(key: Key): Boolean {
        return when (key) {
            Key.One -> { navigateToRoute("home"); true }
            Key.Two -> { navigateToRoute("common"); true }
            Key.Three -> { navigateToRoute("app"); true }
            Key.Four -> { navigateToRoute("terminal"); true }
            Key.Five -> { navigateToRoute("mirror"); true }
            Key.Six -> { navigateToRoute("keyevent"); true }
            Key.Seven -> { navigateToRoute("filebrowser"); true }
            Key.Eight -> { navigateToRoute("log"); true }
            Key.Nine -> { navigateToRoute("process"); true }
            Key.Zero -> { navigateToRoute("setting"); true }
            Key.R -> { devicesViewModel.refreshDevices(); true }
            Key.T -> { navigateToRoute("terminal"); true }
            else -> false
        }
    }

    LaunchedEffect(Unit) {
        AppMenuCommandBus.commands.collect { command ->
            when (command) {
                is AppMenuCommand.Navigate -> navigateToRoute(command.route)
                is AppMenuCommand.ConnectDevice -> {
                    val address = command.address.trim()
                    if (address.isNotEmpty()) {
                        val output = withContext(Dispatchers.IO) {
                            AdbTool.execAdbOutputAsync("connect", address)
                        }
                        commonModel.showTipDialog(MsgContent.Text(output), autoDismiss = true)
                        devicesViewModel.refreshDevices()
                    }
                }
                AppMenuCommand.RefreshDevices -> devicesViewModel.refreshDevices()
                AppMenuCommand.RebootDevice -> showRebootConfirm = true
                AppMenuCommand.Screenshot -> commonModel.executeAdbAction(AdbFunctionType.SCREENSHOT)
                AppMenuCommand.ScreenRecord -> commonModel.executeAdbAction(AdbFunctionType.SCREEN_RECORD)
                AppMenuCommand.OpenTerminalTool -> navigateToRoute("terminal")
                AppMenuCommand.InstallApk -> commonModel.executeAdbAction(AdbFunctionType.INSTALL_APK)
                AppMenuCommand.ViewCurrentActivity -> commonModel.executeAdbAction(AdbFunctionType.VIEW_CURRENT_ACTIVITY)
                AppMenuCommand.ViewDeviceInfo -> {
                    navigateToRoute("home")
                    devicesViewModel.refreshDevices()
                }
                AppMenuCommand.ExportLogs -> commonModel.executeAdbAction(AdbFunctionType.CAPTURE_LOGS)
            }
        }
    }

    AdbToolTheme(useDarkTheme = resolvedDarkTheme) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && (event.isMetaPressed || event.isCtrlPressed)) {
                        handleGlobalShortcut(event.key)
                    } else {
                        false
                    }
                },
            containerColor = if (usePlainContentContainer) Color.White else MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SidebarContainer(
                    groups = tabGroups,
                    selectedRoute = currentRoute,
                    devicesViewModel = devicesViewModel,
                    onItemClick = { route ->
                        navigateToRoute(route)
                    }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(if (usePlainContentContainer) 0.dp else 14.dp),
                        borderStroke = if (usePlainContentContainer) {
                            null
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(if (usePlainContentContainer) 0.dp else 8.dp)
                        ) {
                            NavHost(navController, startDestination = "home") {
                                composable("home") {
                                    stateHolder.SaveableStateProvider("home") {
                                        val commonDialogMessage by commonModel.dialogMessage.collectAsState()
                                        val commonShowDialog by commonModel.showDialog.collectAsState()
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            HomeScreen(
                                                viewModel = devicesViewModel,
                                                onScreenshot = { commonModel.executeAdbAction(AdbFunctionType.SCREENSHOT) },
                                                onScreenRecord = { commonModel.executeAdbAction(AdbFunctionType.SCREEN_RECORD) },
                                                onInstallApk = { commonModel.executeAdbAction(AdbFunctionType.INSTALL_APK) },
                                                onMirrorDevice = { navigateToRoute("mirror") },
                                                onOpenShell = {
                                                    navigateToRoute("terminal")
                                                },
                                                onOpenLogcat = { navigateToRoute("log") },
                                                onOpenFileManager = { navigateToRoute("filebrowser") },
                                                onOpenAppManager = { navigateToRoute("app") },
                                                onOpenDiagnostics = { navigateToRoute("process") },
                                                onMoreActions = { navigateToRoute("common") }
                                            )
                                            HomeActionToast(
                                                visible = commonShowDialog,
                                                message = commonDialogMessage
                                            )
                                        }
                                    }
                                }
                                composable("mirror") {
                                    stateHolder.SaveableStateProvider("mirror") {
                                        val selectedDevice by devicesViewModel.selectedDevice.collectAsState()
                                        DeviceMirrorScreen(
                                            viewModel = deviceMirrorViewModel,
                                            selectedDevice = selectedDevice
                                        )
                                    }
                                }
                                composable("common") {
                                    stateHolder.SaveableStateProvider("common") {
                                        val selectedDevice by devicesViewModel.selectedDevice.collectAsState()
                                        CommonScreen(
                                            viewModel = commonModel,
                                            selectedDevice = selectedDevice
                                        )
                                    }
                                }
                                composable("keyevent") {
                                    stateHolder.SaveableStateProvider("keyevent") {
                                        val selectedDevice by devicesViewModel.selectedDevice.collectAsState()
                                        val deviceDisplayNames by devicesViewModel.deviceDisplayNames.collectAsState()
                                        KeyEventScreen(
                                            viewModel = keyEventViewModel,
                                            selectedDevice = selectedDevice,
                                            deviceDisplayNames = deviceDisplayNames
                                        )
                                    }
                                }
                                composable("terminal") {
                                    stateHolder.SaveableStateProvider("terminal") {
                                        val devices by devicesViewModel.devices.collectAsState()
                                        val selectedDevice by devicesViewModel.selectedDevice.collectAsState()
                                        val deviceDisplayNames by devicesViewModel.deviceDisplayNames.collectAsState()
                                        TerminalScreen(
                                            viewModel = terminalViewModel,
                                            selectedDevice = selectedDevice,
                                            devices = devices,
                                            deviceDisplayNames = deviceDisplayNames,
                                            onSelectDevice = { deviceId ->
                                                devicesViewModel.selectDevice(deviceId)
                                            },
                                            onRefreshDevices = {
                                                devicesViewModel.refreshDevices()
                                            }
                                        )
                                    }
                                }
                                composable("app") {
                                    stateHolder.SaveableStateProvider("app") {
                                        val selectedDevice by devicesViewModel.selectedDevice.collectAsState()
                                        AppScreen(
                                            viewModel = appViewModel,
                                            selectedDevice = selectedDevice
                                        )
                                    }
                                }
                                composable("filebrowser") {
                                    stateHolder.SaveableStateProvider("filebrowser") {
                                        val selectedDevice by devicesViewModel.selectedDevice.collectAsState()
                                        FileBrowserScreen(
                                            viewModel = fileBrowserViewModel,
                                            selectedDevice = selectedDevice
                                        )
                                    }
                                }
                                composable("setting") {
                                    stateHolder.SaveableStateProvider("setting") {
                                        SettingScreen()
                                    }
                                }
                                composable("log") {
                                    stateHolder.SaveableStateProvider("log") {
                                        val selectedDevice by devicesViewModel.selectedDevice.collectAsState()
                                        LogScreen(
                                            viewModel = logViewModel,
                                            selectedDevice = selectedDevice
                                        )
                                    }
                                }
                                composable("process") {
                                    stateHolder.SaveableStateProvider("process") {
                                        val selectedDevice by devicesViewModel.selectedDevice.collectAsState()
                                        ProcessScreen(selectedDevice = selectedDevice)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRebootConfirm) {
        AlertDialog(
            onDismissRequest = { showRebootConfirm = false },
            title = { Text(l10n("确认重启设备", "Confirm Device Reboot")) },
            text = { Text(l10n("即将重启连接的设备，设备上的未保存数据可能丢失。是否继续？", "The connected device will reboot. Unsaved data may be lost. Continue?")) },
            confirmButton = {
                TextButton(onClick = {
                    showRebootConfirm = false
                    commonModel.executeAdbAction(AdbFunctionType.REBOOT_DEVICE)
                }) { Text(l10n("确认重启", "Reboot")) }
            },
            dismissButton = {
                TextButton(onClick = { showRebootConfirm = false }) {
                    Text(l10n("取消", "Cancel"))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun SidebarContainer(
    groups: List<SidebarGroup>,
    selectedRoute: String,
    devicesViewModel: DevicesViewModel,
    onItemClick: (String) -> Unit
) {
    val devices by devicesViewModel.devices.collectAsState()
    val selectedDevice by devicesViewModel.selectedDevice.collectAsState()
    val deviceDisplayNames by devicesViewModel.deviceDisplayNames.collectAsState()

    Sidebar(
        groups = groups,
        selectedRoute = selectedRoute,
        connectedDeviceCount = devices.size,
        devices = devices,
        selectedDevice = selectedDevice,
        deviceDisplayNames = deviceDisplayNames,
        onItemClick = onItemClick,
        onDeviceSelected = { deviceId ->
            devicesViewModel.selectDevice(deviceId)
        }
    )
}

@Composable
private fun HomeActionToast(
    visible: Boolean,
    message: MsgContent?
) {
    if (!visible || message == null) return

    val text = when (message) {
        is MsgContent.Resource -> stringResource(message.stringResource, *message.args.toTypedArray())
        is MsgContent.Text -> message.text
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            shadowElevation = 6.dp,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

data class TabItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)
