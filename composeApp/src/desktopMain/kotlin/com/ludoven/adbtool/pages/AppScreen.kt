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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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


    Scaffold(containerColor = Color.White) { paddingValues ->
        val scrollState = rememberLazyListState()
        Box {
            LazyColumn(
                state = scrollState,
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.fillMaxSize().background(Color.White)
            ) {
                item {
                    var expanded by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightColorScheme.background, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = if (selectedApp != null) {
                                    "${selectedApp?.appName} (${selectedApp?.packageName})"
                                } else {
                                    stringResource(Res.string.select_a_app)
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(Res.string.select_app)) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(LightColorScheme.surface) // 确保这里使用 Material3 的 Colors
                            ) {
                                appList.forEach { app ->
                                    DropdownMenuItem(
                                        text = { Text("${app.appName} (${app.packageName})") },
                                        onClick = {
                                            viewModel.selectApp(app)
                                            expanded = false
                                            viewModel.loadAppInfo(app.packageName)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = LightColorScheme.background
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                top = 16.dp,
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp
                            )
                        ) {
                            SectionTitle(
                                stringResource(Res.string.app_info_section_title),
                                Color.Blue,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            if (selectedApp == null) {
                                Text(stringResource(Res.string.app_info_placeholder))
                            } else if (appInfo.isEmpty()) {
                                Text(stringResource(Res.string.app_info_no_detail), color = Color.Gray)
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    appInfo.entries.toList().forEachIndexed { index, (key, value) ->
                                        SelectionContainer {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 3.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "$key:",
                                                        modifier = Modifier.weight(0.4f),
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = Color.Gray
                                                        ),
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = value,
                                                        modifier = Modifier.weight(0.6f),
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = Color.Black
                                                        )
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


                item {
                    // 功能区
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 15.dp)
                            .background(LightColorScheme.background, RoundedCornerShape(12.dp))
                    ) {
                        SectionTitle(
                            stringResource(Res.string.app_functions_section_title),
                            Color.Red,
                            modifier = Modifier.padding(top = 12.dp, start = 15.dp)
                        )
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            maxItemsInEachRow = 5
                        ) {
                            items.forEachIndexed { index, item ->
                                val color = iconColors[index % iconColors.size]
                                GridItemCard(
                                    title = item.title,
                                    icon = item.icon,
                                    iconColor = color,
                                    onClick = { viewModel.executeAdbAction(item.type) }
                                )
                            }
                        }
                    }
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }

    // 处理对话框显示
    val dialogMessage by viewModel.dialogMessage.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()

    if (showDialog) {
        TipDialog(dialogMessage ?: stringResource(Res.string.dialog_unknown_error)) {
            viewModel.dismissTipDialog()
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




