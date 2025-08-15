@file:OptIn(ExperimentalMaterial3Api::class)

package com.ludoven.adbtool.pages

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.app_functions_section_title
import adbtool_desktop.composeapp.generated.resources.app_info_no_detail
import adbtool_desktop.composeapp.generated.resources.app_info_placeholder
import adbtool_desktop.composeapp.generated.resources.app_info_section_title
import adbtool_desktop.composeapp.generated.resources.app_size
import adbtool_desktop.composeapp.generated.resources.clear_and_restart
import adbtool_desktop.composeapp.generated.resources.clear_data
import adbtool_desktop.composeapp.generated.resources.dialog_unknown_error
import adbtool_desktop.composeapp.generated.resources.export_apk
import adbtool_desktop.composeapp.generated.resources.grant_all_permissions
import adbtool_desktop.composeapp.generated.resources.launch_app
import adbtool_desktop.composeapp.generated.resources.reset_permissions
import adbtool_desktop.composeapp.generated.resources.reset_permissions_and_restart
import adbtool_desktop.composeapp.generated.resources.restart_app
import adbtool_desktop.composeapp.generated.resources.select_a_app
import adbtool_desktop.composeapp.generated.resources.select_app
import adbtool_desktop.composeapp.generated.resources.stop_app
import adbtool_desktop.composeapp.generated.resources.uninstall_app
import adbtool_desktop.composeapp.generated.resources.view_install_path
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.LightColorScheme
import com.ludoven.adbtool.entity.AdbFunction
import com.ludoven.adbtool.entity.AdbFunctionType
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.iconColors
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.viewmodel.AppViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppScreen(viewModel: AppViewModel) {
    val appInfo by viewModel.appInfo.collectAsState()
    val selectedApp by viewModel.selectedApp.collectAsState()
    val appList by viewModel.appList.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getAppList()
    }

    val items = listOf(
        AdbFunction(stringResource(Res.string.uninstall_app), Icons.Default.Delete, AdbFunctionType.UNINSTALL),
        AdbFunction(stringResource(Res.string.launch_app), Icons.Default.PlayArrow, AdbFunctionType.LAUNCH),
        AdbFunction(stringResource(Res.string.stop_app), Icons.Default.Stop, AdbFunctionType.FORCE_STOP),
        AdbFunction(stringResource(Res.string.restart_app), Icons.Default.Refresh, AdbFunctionType.RESTART_APP),
        AdbFunction(stringResource(Res.string.clear_data), Icons.Default.DeleteSweep, AdbFunctionType.CLEAR_DATA),
        AdbFunction(stringResource(Res.string.clear_and_restart), Icons.Default.RestartAlt, AdbFunctionType.CLEAR_AND_RESTART),
        AdbFunction(stringResource(Res.string.reset_permissions), Icons.Default.Security, AdbFunctionType.RESET_PERMISSIONS),
        AdbFunction(stringResource(Res.string.reset_permissions_and_restart), Icons.Default.RestartAlt, AdbFunctionType.RESET_PERMISSIONS_AND_RESTART),
        AdbFunction(stringResource(Res.string.grant_all_permissions), Icons.Default.CheckCircle, AdbFunctionType.GRANT_ALL_PERMISSIONS),
        AdbFunction(stringResource(Res.string.view_install_path), Icons.Default.Folder, AdbFunctionType.GET_PATH),
        AdbFunction(stringResource(Res.string.export_apk), Icons.Default.Download, AdbFunctionType.EXPORT_APK),
        AdbFunction(stringResource(Res.string.app_size), Icons.Default.Storage, AdbFunctionType.GET_APP_SIZE)
    )

    Scaffold { paddingValues ->
        // 使用 Row 来创建左右分栏布局
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(LightColorScheme.background)
        ) {
            // 左侧：应用列表
            AppListSection(
                modifier = Modifier.weight(0.3f), // 左侧占据 30% 的宽度
                appList = appList,
                selectedApp = selectedApp,
                onAppSelected = { app ->
                    viewModel.selectApp(app)
                    viewModel.loadAppInfo(app.packageName)
                }
            )

            // 右侧：应用信息和功能
            AppInfoAndFunctionsSection(
                modifier = Modifier.weight(0.7f), // 右侧占据 70% 的宽度
                selectedApp = selectedApp,
                appInfo = appInfo,
                adbFunctions = items,
                onAdbAction = { actionType ->
                    viewModel.executeAdbAction(actionType)
                }
            )
        }
    }

    // 处理对话框显示
    val dialogMessage by viewModel.dialogMessage.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()

    if (showDialog) {
        dialogMessage?.let {
            TipDialog(
                dialogText = when (it) {
                    is MsgContent.Resource -> {
                        stringResource(it.stringResource, *it.args.toTypedArray())
                    }

                    is MsgContent.Text -> {
                        it.text
                    }
                }
            ) {
                viewModel.dismissTipDialog()
            }
        }
    }

}


// 提取左侧应用列表为一个单独的 Composable
@Composable
fun AppListSection(
    modifier: Modifier = Modifier,
    appList: List<AppInfo>,
    selectedApp: AppInfo?,
    onAppSelected: (AppInfo) -> Unit
) {
    val listState = rememberLazyListState()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(12.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
    ) {
        if (appList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
        /*        Text(
                    text = if (isAppListLoading) "加载中..." else "未找到应用",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )*/
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(appList.size) { index ->
                    val app = appList[index]
                    AppListItem(
                        app = app,
                        isSelected = app == selectedApp,
                        onClick = { onAppSelected(app) }
                    )
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}

// 列表项，用于展示应用的图标、名称和包名
@Composable
fun AppListItem(
    app: AppInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
    val textColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black
    val packageNameColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TODO: 在这里添加应用图标，需要根据包名加载
        // Icon(
        //     painter = ...,
        //     contentDescription = null,
        //     modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
        // )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                maxLines = 1
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = packageNameColor,
                maxLines = 1
            )
        }
    }
}


@Composable
fun AppInfoAndFunctionsSection(
    modifier: Modifier = Modifier,
    selectedApp: AppInfo?,
    appInfo: Map<String, String>,
    adbFunctions: List<AdbFunction>,
    onAdbAction: (AdbFunctionType) -> Unit
) {
    // 为右侧内容创建一个可滚动的状态
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(12.dp)
            .background(LightColorScheme.background, RoundedCornerShape(12.dp))
    ) {
        if (selectedApp == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "请选择一个应用",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Gray
                )
            }
        } else {
            // 将整个 Column 变为可滚动
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState) // <-- 关键改动：添加了垂直滚动修饰符
            ) {
                // 应用信息卡片
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle(
                            stringResource(Res.string.app_info_section_title),
                            Color.Blue,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        if (appInfo.isEmpty()) {
                            Text(stringResource(Res.string.app_info_no_detail), color = Color.Gray)
                        } else {
                            // 应用信息详情
                            Column(modifier = Modifier.fillMaxWidth()) {
                                appInfo.entries.toList().forEach { (key, value) ->
                                    SelectionContainer {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "$key:",
                                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                                            )
                                            Text(
                                                text = value,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 功能区
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    SectionTitle(
                        stringResource(Res.string.app_functions_section_title),
                        Color.Red,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        adbFunctions.forEachIndexed { index, item ->
                            val color = iconColors[index % iconColors.size]
                            GridItemCard(
                                title = item.title,
                                icon = item.icon,
                                iconColor = color,
                                onClick = { onAdbAction(item.type) }
                            )
                        }
                    }
                }
            }

            // <-- 关键改动：为可滚动的 Column 添加滚动条
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}


data class AppInfo(val appName: String, val packageName: String)

fun getInstalledApps(): List<AppInfo> {
    val output = AdbTool.exec("pm list packages -f -3") ?: return emptyList()
    return output.lines().mapNotNull { line ->
        val regex = Regex("package:(.+)=([\\w\\.]+)")
        val match = regex.find(line)
        val (apkPath, packageName) = match?.destructured ?: return@mapNotNull null
        val name = packageName.substringAfterLast(".")
        AppInfo(appName = name, packageName = packageName)
    }
}




