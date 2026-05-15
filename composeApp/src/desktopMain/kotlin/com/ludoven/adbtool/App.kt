package com.ludoven.adbtool

import com.ludoven.adbtool.ui.mac.*

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.app
import adbtool_desktop.composeapp.generated.resources.common
import adbtool_desktop.composeapp.generated.resources.file_browser
import adbtool_desktop.composeapp.generated.resources.home
import adbtool_desktop.composeapp.generated.resources.key_event_page
import adbtool_desktop.composeapp.generated.resources.log
import adbtool_desktop.composeapp.generated.resources.process
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideogameAsset
import com.ludoven.adbtool.ui.mac.ExperimentalMaterial3Api
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ludoven.adbtool.pages.AppScreen
import com.ludoven.adbtool.pages.CommonScreen
import com.ludoven.adbtool.pages.FileBrowserScreen
import com.ludoven.adbtool.pages.HomeScreen
import com.ludoven.adbtool.pages.KeyEventScreen
import com.ludoven.adbtool.pages.LogScreen
import com.ludoven.adbtool.pages.ProcessScreen
import com.ludoven.adbtool.pages.SettingScreen
import com.ludoven.adbtool.pages.TerminalScreen
import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.util.LanguageManager
import com.ludoven.adbtool.util.ThemeManager
import com.ludoven.adbtool.entity.AdbFunctionType
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.viewmodel.AppViewModel
import com.ludoven.adbtool.viewmodel.CommonModel
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
    val keyEventViewModel: KeyEventViewModel = viewModel()
    val logViewModel: LogViewModel = viewModel()
    val terminalViewModel: TerminalViewModel = viewModel()
    val fileBrowserViewModel: FileBrowserViewModel = viewModel()
    val devices by devicesViewModel.devices.collectAsState()
    val selectedDevice by devicesViewModel.selectedDevice.collectAsState()
    val deviceDisplayNames by devicesViewModel.deviceDisplayNames.collectAsState()

    LaunchedEffect(Unit) {
        LanguageManager.initialize()
        ThemeManager.initialize()
        AdbPathManager.getAdbPath()
    }

    val tabs = listOf(
        TabItem(stringResource(Res.string.home), Icons.Default.Home, "home"),
        TabItem(stringResource(Res.string.common), Icons.Default.Info, "common"),
        TabItem(stringResource(Res.string.terminal), Icons.Default.Code, "terminal"),
        TabItem(stringResource(Res.string.key_event_page), Icons.Default.VideogameAsset, "keyevent"),
        TabItem(stringResource(Res.string.app), Icons.Default.Apps, "app"),
        TabItem(stringResource(Res.string.file_browser), Icons.Default.Folder, "filebrowser"),
        TabItem(stringResource(Res.string.log), Icons.Default.List, "log"),
        TabItem(stringResource(Res.string.process), Icons.Default.Memory, "process"),
        TabItem(stringResource(Res.string.set), Icons.Default.Settings, "setting")
    )

    val navController = rememberNavController()
    val stateHolder = rememberSaveableStateHolder()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "home"
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

    LaunchedEffect(Unit) {
        AppMenuCommandBus.commands.collect { command ->
            when (command) {
                is AppMenuCommand.Navigate -> navigateToRoute(command.route)
                is AppMenuCommand.ConnectDevice -> {
                    val address = command.address.trim()
                    if (address.isNotEmpty()) {
                        val output = withContext(Dispatchers.IO) {
                            AdbTool.executeAdbCommand("connect", address)
                        }
                        commonModel.showTipDialog(MsgContent.Text(output), autoDismiss = true)
                        devicesViewModel.refreshDevices()
                    }
                }
                AppMenuCommand.RefreshDevices -> devicesViewModel.refreshDevices()
                AppMenuCommand.RebootDevice -> commonModel.executeAdbAction(AdbFunctionType.REBOOT_DEVICE)
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
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Sidebar(
                    items = tabs,
                    selectedRoute = currentRoute,
                    connectedDeviceCount = devices.size,
                    devices = devices,
                    selectedDevice = selectedDevice,
                    deviceDisplayNames = deviceDisplayNames,
                    onItemClick = { route ->
                        navigateToRoute(route)
                    },
                    onDeviceSelected = { deviceId ->
                        devicesViewModel.selectDevice(deviceId)
                    }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(14.dp),
                        borderStroke = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(8.dp)
                        ) {
                            NavHost(navController, startDestination = "home") {
                                composable("home") {
                                    stateHolder.SaveableStateProvider("home") {
                                        HomeScreen(
                                            viewModel = devicesViewModel,
                                            onScreenshot = { commonModel.executeAdbAction(AdbFunctionType.SCREENSHOT) },
                                            onInstallApk = { commonModel.executeAdbAction(AdbFunctionType.INSTALL_APK) },
                                            onOpenShell = {
                                                navigateToRoute("terminal")
                                            }
                                        )
                                    }
                                }
                                composable("common") {
                                    stateHolder.SaveableStateProvider("common") {
                                        CommonScreen(commonModel)
                                    }
                                }
                                composable("keyevent") {
                                    stateHolder.SaveableStateProvider("keyevent") {
                                        KeyEventScreen(
                                            viewModel = keyEventViewModel,
                                            selectedDevice = selectedDevice,
                                            deviceDisplayNames = deviceDisplayNames
                                        )
                                    }
                                }
                                composable("terminal") {
                                    stateHolder.SaveableStateProvider("terminal") {
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
                                        AppScreen(appViewModel)
                                    }
                                }
                                composable("filebrowser") {
                                    stateHolder.SaveableStateProvider("filebrowser") {
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
                                        LogScreen(
                                            viewModel = logViewModel,
                                            selectedDevice = selectedDevice
                                        )
                                    }
                                }
                                composable("process") {
                                    stateHolder.SaveableStateProvider("process") {
                                        ProcessScreen(
                                            selectedDevice = selectedDevice
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class TabItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)
