package com.ludoven.adbtool

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.compose_multiplatform
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ludoven.adbtool.pages.AppScreen
import com.ludoven.adbtool.pages.CommonScreen
import com.ludoven.adbtool.pages.DevicesScreen
import com.ludoven.adbtool.pages.KeyEventScreen
import com.ludoven.adbtool.pages.SettingScreen
import com.ludoven.adbtool.pages.SystemScreen
import com.ludoven.adbtool.viewmodel.DevicesViewModel

@ExperimentalMaterial3Api
@Composable
@Preview
fun App() {

    val devicesViewModel: DevicesViewModel = viewModel() // 仅初始化一次

    MaterialTheme(colorScheme = LightColorScheme) {
        // 定义Tab数据，通常是数据类或枚举
        val tabs = remember {
            listOf(
                TabItem("首页", Icons.Default.Home),
                TabItem("常用", Icons.Default.Info),
                TabItem("应用", Icons.Default.Settings),
                TabItem("系统", Icons.Default.Settings),
                TabItem("按键", Icons.Default.Settings),
                TabItem("设置", Icons.Default.Settings)
            )
        }

        var selectedTabIndex by remember { mutableStateOf(0) } // 默认选中第一个Tab

        // 使用Scaffold来构建基本布局
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = LightColorScheme.background // 设置背景颜色
        ) { paddingValues -> // paddingValues 会自动处理系统栏（如Android上的状态栏、桌面窗口的标题栏）
            Row(
                modifier = Modifier
                    .fillMaxSize()
            ) {

                NavigationRail(
                    modifier = Modifier.fillMaxHeight().padding(top = 30.dp),
                    containerColor = LightColorScheme.background,
                ) {
                    tabs.forEachIndexed { index, item ->
                        NavigationRailItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            modifier = Modifier.padding(bottom = 15.dp),
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor =LightColorScheme.primary,      // 选中时图标颜色
                                unselectedIconColor = LightColorScheme.onSurface,  // 未选中图标颜色
                                selectedTextColor = LightColorScheme.primary,      // 选中时文字颜色
                                unselectedTextColor = LightColorScheme.onSurface   // 未选中文字颜色
                            )
                        )
                    }
                }

                // 右侧内容区域
                Box(
                    modifier = Modifier
                        .weight(1f) // 右侧占据剩余空间
                        .fillMaxHeight()
                        .background(LightColorScheme.surface) // 内容区域背景色
//                        .padding(16.dp) // 内容区域内边距
                ) {
                    // 根据选中的Tab索引显示不同的页面
                    when (selectedTabIndex) {
                        0 -> DevicesScreen(devicesViewModel) // 你的设备管理页面
                        1 -> CommonScreen()     // 你的常用页面
                        2 -> AppScreen()    // 你的应用管理页面
                        3 -> SystemScreen()        // 你的系统页面
                        4 -> KeyEventScreen()         // 你的按键输入页面
                        5 -> SettingScreen()         // 你的设置页面
                    }
                }
            }
        }
    }
}

data class TabItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)


// 这是你原来的 App() 函数，我把它改名为 OriginalAppExample，以防冲突
// 你可以根据需要将其内容整合到 AdbToolApp() 中，或者作为独立示例保留
@Composable
@Preview
fun OriginalAppExample() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}