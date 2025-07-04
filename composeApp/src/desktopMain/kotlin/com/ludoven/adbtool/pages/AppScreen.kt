@file:OptIn(ExperimentalMaterial3Api::class)

package com.ludoven.adbtool.pages

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.LightColorScheme
import com.ludoven.adbtool.entity.AdbFunction
import com.ludoven.adbtool.iconColors
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.FileUtils
import com.ludoven.adbtool.viewmodel.AppViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppScreen(viewModel: AppViewModel) {
    val appInfo by viewModel.appInfo.collectAsState()
    val selectedApp by viewModel.selectedApp.collectAsState()
    val appList by viewModel.appList.collectAsState()


    var showDialog by remember { mutableStateOf(false) }
    var dialogText by remember { mutableStateOf("") }
    var showTextInputDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.getAppList()
    }

    val items = listOf(
        AdbFunction("卸载应用", Icons.Default.Delete) {
            selectedApp?.let { app ->
                performAdbAction(
                    coroutineScope,
                    "pm uninstall ${app.packageName}",
                    { dialogText = it },
                    { showDialog = it },
                    "卸载成功：${app.packageName}",
                    "卸载失败"
                ) { success ->
                    if (success) {
                        viewModel.getAppList()
                        viewModel.selectApp(null)
                    }
                }
            } ?: run {
                coroutineScope.launch {
                    dialogText = "请先选择一个应用"
                    showDialog = true
                    delay(2000)
                    showDialog = false
                }
            }
        },
        AdbFunction("启动应用", Icons.Default.PlayArrow) {
            selectedApp?.let {
                performAdbAction(
                    coroutineScope,
                    "monkey -p ${it.packageName} -c android.intent.category.LAUNCHER 1",
                    { dialogText = it },
                    { showDialog = it },
                    "启动成功：${it.packageName}",
                    "启动失败"
                )
            }
        },
        AdbFunction("停止运行", Icons.Default.Stop) {
            selectedApp?.let {
                performAdbAction(
                    coroutineScope,
                    "am force-stop ${it.packageName}",
                    { dialogText = it },
                    { showDialog = it },
                    "已停止运行：${it.packageName}",
                    "停止失败"
                )
            }
        },
        AdbFunction("重启应用", Icons.Default.Refresh) {
            selectedApp?.let {
                coroutineScope.launch {
                    AdbTool.exec("am force-stop ${it.packageName}")
                    delay(300)
                    AdbTool.exec("monkey -p ${it.packageName} -c android.intent.category.LAUNCHER 1")
                    dialogText = "已重启应用：${it.packageName}"
                    showDialog = true
                    delay(2000)
                    showDialog = false
                }
            }
        },
        AdbFunction("清除数据", Icons.Default.DeleteSweep) {
            selectedApp?.let {
                performAdbAction(
                    coroutineScope,
                    "pm clear ${it.packageName}",
                    { dialogText = it },
                    { showDialog = it },
                    "清除成功：${it.packageName}",
                    "清除失败"
                )
            }
        },
        AdbFunction("清除并重启", Icons.Default.RestartAlt) {
            selectedApp?.let {
                coroutineScope.launch {
                    AdbTool.exec("pm clear ${it.packageName}")
                    delay(300)
                    AdbTool.exec("monkey -p ${it.packageName} -c android.intent.category.LAUNCHER 1")
                    dialogText = "清除并重启成功：${it.packageName}"
                    showDialog = true
                    delay(2000)
                    showDialog = false
                }
            }
        },
        AdbFunction("重置权限", Icons.Default.Security) {
            selectedApp?.let {
                performAdbAction(
                    coroutineScope,
                    "pm reset-permissions ${it.packageName}",
                    { dialogText = it },
                    { showDialog = it },
                    "已重置权限：${it.packageName}",
                    "重置失败"
                )
            }
        },
        AdbFunction("重置权限并重启", Icons.Default.RestartAlt) {
            selectedApp?.let {
                coroutineScope.launch {
                    AdbTool.exec("pm reset-permissions ${it.packageName}")
                    delay(300)
                    AdbTool.exec("monkey -p ${it.packageName} -c android.intent.category.LAUNCHER 1")
                    dialogText = "已重置权限并重启：${it.packageName}"
                    showDialog = true
                    delay(2000)
                    showDialog = false
                }
            }
        },
        AdbFunction("授权所有权限", Icons.Default.CheckCircle) {
            selectedApp?.let {
                performAdbAction(
                    coroutineScope,
                    "pm grant ${it.packageName} android.permission.READ_EXTERNAL_STORAGE && " +
                            "pm grant ${it.packageName} android.permission.WRITE_EXTERNAL_STORAGE",
                    { dialogText = it },
                    { showDialog = it },
                    "授权完成：${it.packageName}",
                    "授权失败"
                )
            }
        },
        AdbFunction("查看安装路径", Icons.Default.Folder) {
            selectedApp?.let {
                coroutineScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        AdbTool.exec("pm path ${it.packageName}")
                    }
                    dialogText = result ?: "获取路径失败"
                    showDialog = true
                }
            }
        },
        AdbFunction("导出Apk", Icons.Default.Download) {
            selectedApp?.let {
                coroutineScope.launch {
                    val path =
                        AdbTool.exec("pm path ${it.packageName}")?.split(":")?.getOrNull(1)?.trim()
                    if (path != null) {
                        val folderPath = FileUtils.selectFolder()
                        if (folderPath != null) {
                            val savePath =
                                "$folderPath/${it.appName}_${System.currentTimeMillis()}.apk"
                            val success = AdbTool.pullFile(path, savePath)
                            dialogText = if (success) "导出成功：$savePath" else "导出失败"
                        } else {
                            dialogText = "未选择导出路径"
                        }
                    } else {
                        dialogText = "获取安装路径失败"
                    }
                    showDialog = true
                    delay(2000)
                    showDialog = false
                }
            }
        },
        AdbFunction("应用大小", Icons.Default.Storage) {
            selectedApp?.let {
                coroutineScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        AdbTool.exec("dumpsys package ${it.packageName} | grep 'codePath\\|dataDir'")
                    }
                    dialogText = result ?: "查询失败"
                    showDialog = true
                }
            }
        }
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
                                    "请选择一个应用"
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("选择应用") },
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
                                "设备信息",
                                Color.Blue,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            if (selectedApp == null) {
                                Text("请选择一个应用以查看其信息。")
                            } else if (appInfo.isEmpty()) {
                                Text("未能获取到应用详细信息。", color = Color.Gray)
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
                            "应用功能",
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
                                    onClick = item.onClick,
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

    if (showDialog) {
        TipDialog(dialogText) {
            showDialog = false
        }
    }

    if (showTextInputDialog) {
        textInputDialog(coroutineScope) { showTextInputDialog = false }
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


fun performAdbAction(
    scope: CoroutineScope,
    command: String,
    setDialogText: (String) -> Unit,
    setShowDialog: (Boolean) -> Unit,
    successMsg: String,
    failMsg: String,
    onComplete: ((Boolean) -> Unit)? = null,
) {
    scope.launch {
        setShowDialog(true)
        setDialogText("执行中...")
        val result = withContext(Dispatchers.IO) {
            AdbTool.exec(command)
        }
        val success = result?.contains("Success") == true
        setDialogText(if (success) successMsg else failMsg)
        delay(2000)
        setShowDialog(false)
        onComplete?.invoke(success)
    }
}



