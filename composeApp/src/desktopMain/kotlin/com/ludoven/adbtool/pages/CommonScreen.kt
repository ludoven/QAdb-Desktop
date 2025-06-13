package com.ludoven.adbtool.pages

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.LightColorScheme
import com.ludoven.adbtool.entity.AdbFunction
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.ui.tooling.preview.Preview

@ExperimentalMaterial3Api
@Composable
@Preview
fun CommonScreen() {
    var showDialog by remember { mutableStateOf(false) }
    var dialogText by remember { mutableStateOf("正在安装...") }
    // 使用 rememberCoroutineScope 来管理协程生命周期，避免泄露
    val coroutineScope = rememberCoroutineScope()
    // 新增的状态，用于控制文本输入弹窗的显示
    var showTextInputDialog by remember { mutableStateOf(false) }


    val commonItems = listOf(
        AdbFunction("安装应用", Icons.Default.InstallMobile) {
            val apkPath = FileUtils.selectApkFile()
            if (apkPath != null) {
                // 1. 立即显示“正在安装...”的弹窗
                dialogText = "正在安装..."
                showDialog = true

                // 2. 启动后台安装任务
                coroutineScope.launch(Dispatchers.IO) { // 在 IO 线程执行耗时操作
                    val success = AdbTool.installApk(apkPath) // 执行安装

                    withContext(Dispatchers.Main) { // 切换回主线程更新 UI
                        // 3. 根据安装结果更新弹窗文本
                        dialogText = if (success) "安装成功" else "安装失败"
                        // 4. 短暂显示结果，然后关闭弹窗
                        delay(2000) // 显示结果2秒
                        showDialog = false
                    }
                }
            } else {
                // 如果用户没有选择文件，可以给一个提示
                coroutineScope.launch {
                    dialogText = "未选择 APK 文件或选择有误。"
                    showDialog = true
                    delay(2000)
                    showDialog = false
                }
            }
        },

        AdbFunction("输入文本", Icons.Default.Edit) {
            showTextInputDialog = true
        },
        AdbFunction("截图保存", Icons.Default.PhotoCamera) {

        },
        AdbFunction("查看Activity", Icons.Default.Visibility) {

        }
    )

    val items = listOf(
        AdbFunction("重启设备", Icons.Default.RestartAlt) {

        },
        AdbFunction("日志查看", Icons.Default.Notes) {

        },
        AdbFunction("卸载应用", Icons.Default.Delete) {

        },
        AdbFunction("录屏保存", Icons.Default.Videocam) {

        },
        AdbFunction("无线连接", Icons.Default.Wifi) {

        },
        AdbFunction("无线连接", Icons.Default.Wifi) {

        }
    )

    val iconColors = listOf(
        Color(0xFFEF5350), // Red
        Color(0xFF42A5F5), // Blue
        Color(0xFF66BB6A), // Green
        Color(0xFFFFA726), // Orange
        Color(0xFFAB47BC), // Purple
        Color(0xFFFF7043), // Deep Orange
        Color(0xFF26C6DA), // Cyan
    )

    Scaffold(containerColor = Color.White) { paddingValues ->
        val scrollState = rememberLazyListState()

        Box {
            LazyColumn(
                state = scrollState, // 2. 将 state 传递给 LazyColumn
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                item {
                    // 常用功能模块
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightColorScheme.background, RoundedCornerShape(12.dp))
                    ) {
                        SectionTitle("常用功能", Color.Red)
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            maxItemsInEachRow = 5
                        ) {
                            commonItems.forEachIndexed { index, item ->
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

                item {
                    // 系统功能模块
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 15.dp)
                            .background(LightColorScheme.background, RoundedCornerShape(12.dp))
                    ) {
                        SectionTitle("系统功能", Color.Blue)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 20.dp),
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

                item {
                    // 系统功能模块
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 15.dp)
                            .background(LightColorScheme.background, RoundedCornerShape(12.dp))
                    ) {
                        SectionTitle("系统功能", Color.Blue)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 20.dp),
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

                item {
                    // 系统功能模块
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 15.dp)
                            .background(LightColorScheme.background, RoundedCornerShape(12.dp))
                    ) {
                        SectionTitle("系统功能", Color.Blue)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 20.dp),
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

    // --- 文本输入弹窗 ---
    if (showTextInputDialog) {
        textInputDialog(coroutineScope) { showTextInputDialog = false }
    }
}

@Composable
fun SectionTitle(title: String, barColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 12.dp, start = 15.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 16.dp)
                .background(barColor, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}


@Composable
fun GridItemCard(
    title: String,
    icon: ImageVector,
    iconColor: Color = LightColorScheme.primary,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(110.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = iconColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun textInputDialog(coroutineScope: CoroutineScope, onDismiss: () -> Unit) {
    var inputText by remember { mutableStateOf("") }
    var escapeSpaces by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { onDismiss.invoke() },
        text = {
            Column {
                OutlinedTextField( // 使用 OutlinedTextField 更好看
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("请输入文本") },
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = escapeSpaces,
                        onCheckedChange = { escapeSpaces = it }
                    )
                    Text(
                        "转义空格 (%s)",
                        modifier = Modifier.clickable { escapeSpaces = !escapeSpaces })
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.End // 按钮靠右对齐
            ) {
                TextButton(onClick = { onDismiss.invoke() }) {
                    Text("取消")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onDismiss.invoke()
                        coroutineScope.launch {
                            val success =
                                AdbTool.inputText(text = inputText, escapeSpaces = escapeSpaces)
                            if (success) {
                                println("输入成功")
                            } else {
                                println("输入失败")
                            }
                        }
                    }
                ) {
                    Text("确定")
                }
            }
        }, containerColor = LightColorScheme.background
    )
}

@Composable
fun TipDialog(dialogText: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss.invoke() },
        title = { Text("提示") },
        text = { Text(dialogText) },
        confirmButton = {
            TextButton(onClick = { onDismiss.invoke() }) {
                Text("确定")
            }
        }, containerColor = LightColorScheme.background
    )
}

