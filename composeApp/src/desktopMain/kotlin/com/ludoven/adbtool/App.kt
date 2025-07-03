package com.ludoven.adbtool

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.app
import adbtool_desktop.composeapp.generated.resources.common
import adbtool_desktop.composeapp.generated.resources.home
import adbtool_desktop.composeapp.generated.resources.ic_logo
import adbtool_desktop.composeapp.generated.resources.set
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ludoven.adbtool.pages.AppScreen
import com.ludoven.adbtool.pages.CommonScreen
import com.ludoven.adbtool.pages.HomeScreen
import com.ludoven.adbtool.pages.SettingScreen
import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.viewmodel.AppViewModel
import com.ludoven.adbtool.viewmodel.DevicesViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@ExperimentalMaterial3Api
@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun App() {
    val devicesViewModel: DevicesViewModel = viewModel()
    val appViewModel: AppViewModel = viewModel()

    LaunchedEffect(Unit) {
        val adbPath = AdbPathManager.getAdbPath()
        if (adbPath != null) {
            println("ADB 路径: $adbPath")
        } else {
            println("未找到有效的 ADB，请手动设置")
        }
    }

    val tabs = listOf(
        TabItem(stringResource(Res.string.home), Icons.Default.Home, "home"),
        TabItem(stringResource(Res.string.common), Icons.Default.Info, "common"),
        TabItem(stringResource(Res.string.app), Icons.Default.Apps, "app"),
        TabItem(stringResource(Res.string.set), Icons.Default.Settings, "setting")
    )

    val navController = rememberNavController()
    val stateHolder = rememberSaveableStateHolder()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "home"

    MaterialTheme(colorScheme = LightColorScheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = LightColorScheme.background
        ) { paddingValues ->
            Row(modifier = Modifier.fillMaxSize()) {

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(Res.drawable.ic_logo),
                        modifier = Modifier.size(60.dp).padding(top = 30.dp),
                        contentDescription = "图标"
                    )
                    Text("QADB", fontSize = 13.sp)

                    NavigationRail(
                        modifier = Modifier.fillMaxHeight().padding(top = 30.dp),
                        containerColor = LightColorScheme.background,
                    ) {
                        tabs.forEach { item ->
                            NavigationRailItem(
                                icon = { Icon(item.icon, contentDescription = item.title) },
                                label = { Text(item.title) },
                                selected = currentRoute == item.route,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            launchSingleTop = true
                                            restoreState = true
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.padding(bottom = 15.dp),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = LightColorScheme.primary,
                                    unselectedIconColor = LightColorScheme.onSurface,
                                    selectedTextColor = LightColorScheme.primary,
                                    unselectedTextColor = LightColorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(LightColorScheme.surface)
                        .padding(paddingValues)
                ) {
                    NavHost(navController, startDestination = "home") {
                        composable("home") {
                            stateHolder.SaveableStateProvider("home") {
                                HomeScreen(devicesViewModel)
                            }
                        }
                        composable("common") {
                            stateHolder.SaveableStateProvider("common") {
                                CommonScreen()
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


data class TabItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector,val route: String)


// 这是你原来的 App() 函数，我把它改名为 OriginalAppExample，以防冲突
// 你可以根据需要将其内容整合到 AdbToolApp() 中，或者作为独立示例保留
@Composable
@Preview
fun OriginalAppExample() {
    val devicesViewModel: DevicesViewModel = viewModel() // 仅初始化一次
    val appViewModel: AppViewModel = viewModel() // 仅初始化一次


    val navController = rememberNavController()
        val tabs = listOf("home", "settings", "profile")

        Scaffold(
            bottomBar = {
                BottomNavigation {
                    tabs.forEach { route ->
                        BottomNavigationItem(
                            selected = navController.currentDestination?.route == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text(route) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            val stateHolder = rememberSaveableStateHolder()

            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") {
                    stateHolder.SaveableStateProvider("home") {
                        HomeScreen(devicesViewModel)
                    }
                }
                composable("settings") {
                    stateHolder.SaveableStateProvider("settings") {
                        SettingScreen()
                    }
                }
                composable("profile") {
                    stateHolder.SaveableStateProvider("profile") {
                        AppScreen(appViewModel)
                    }
                }
            }
        }
}