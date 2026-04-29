package com.ludoven.adbtool

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.app
import adbtool_desktop.composeapp.generated.resources.common
import adbtool_desktop.composeapp.generated.resources.home
import adbtool_desktop.composeapp.generated.resources.key_event_page
import adbtool_desktop.composeapp.generated.resources.set
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ludoven.adbtool.pages.AppScreen
import com.ludoven.adbtool.pages.CommonScreen
import com.ludoven.adbtool.pages.HomeScreen
import com.ludoven.adbtool.pages.KeyEventScreen
import com.ludoven.adbtool.pages.SettingScreen
import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.util.LanguageManager
import com.ludoven.adbtool.viewmodel.AppViewModel
import com.ludoven.adbtool.viewmodel.CommonModel
import com.ludoven.adbtool.viewmodel.DevicesViewModel
import com.ludoven.adbtool.viewmodel.KeyEventViewModel
import com.ludoven.adbtool.widget.GlassCard
import com.ludoven.adbtool.widget.Sidebar
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
    val devices by devicesViewModel.devices.collectAsState()
    val selectedDevice by devicesViewModel.selectedDevice.collectAsState()
    val deviceDisplayNames by devicesViewModel.deviceDisplayNames.collectAsState()

    LaunchedEffect(Unit) {
        LanguageManager.initialize()
        AdbPathManager.getAdbPath()
    }

    val tabs = listOf(
        TabItem(stringResource(Res.string.home), Icons.Default.Home, "home"),
        TabItem(stringResource(Res.string.common), Icons.Default.Info, "common"),
        TabItem(stringResource(Res.string.key_event_page), Icons.Default.VideogameAsset, "keyevent"),
        TabItem(stringResource(Res.string.app), Icons.Default.Apps, "app"),
        TabItem(stringResource(Res.string.set), Icons.Default.Settings, "setting")
    )

    val navController = rememberNavController()
    val stateHolder = rememberSaveableStateHolder()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "home"

    AdbToolTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Sidebar(
                    items = tabs,
                    selectedRoute = currentRoute,
                    connectedDeviceCount = devices.size,
                    onItemClick = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                            }
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(30.dp)
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
                                        HomeScreen(devicesViewModel)
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
                                composable("app") {
                                    stateHolder.SaveableStateProvider("app") {
                                        AppScreen(appViewModel)
                                    }
                                }
                                composable("setting") {
                                    stateHolder.SaveableStateProvider("setting") {
                                        SettingScreen()
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
