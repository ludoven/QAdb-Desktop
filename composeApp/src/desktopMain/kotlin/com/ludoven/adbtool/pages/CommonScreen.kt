package com.ludoven.adbtool.pages

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.activity_not_found
import adbtool_desktop.composeapp.generated.resources.apk_not_selected
import adbtool_desktop.composeapp.generated.resources.battery_status
import adbtool_desktop.composeapp.generated.resources.cancel
import adbtool_desktop.composeapp.generated.resources.common_functions
import adbtool_desktop.composeapp.generated.resources.confirm
import adbtool_desktop.composeapp.generated.resources.cpu
import adbtool_desktop.composeapp.generated.resources.cpu_info
import adbtool_desktop.composeapp.generated.resources.current_activity
import adbtool_desktop.composeapp.generated.resources.developer_options
import adbtool_desktop.composeapp.generated.resources.escape_spaces
import adbtool_desktop.composeapp.generated.resources.folder_not_selected
import adbtool_desktop.composeapp.generated.resources.input_hint
import adbtool_desktop.composeapp.generated.resources.input_text
import adbtool_desktop.composeapp.generated.resources.install_app
import adbtool_desktop.composeapp.generated.resources.install_failed
import adbtool_desktop.composeapp.generated.resources.install_success
import adbtool_desktop.composeapp.generated.resources.installing
import adbtool_desktop.composeapp.generated.resources.is_rooted
import adbtool_desktop.composeapp.generated.resources.network_status
import adbtool_desktop.composeapp.generated.resources.reboot_device
import adbtool_desktop.composeapp.generated.resources.screen_resolution
import adbtool_desktop.composeapp.generated.resources.screenshot
import adbtool_desktop.composeapp.generated.resources.screenshot_failed
import adbtool_desktop.composeapp.generated.resources.screenshot_success
import adbtool_desktop.composeapp.generated.resources.system_functions
import adbtool_desktop.composeapp.generated.resources.tip_title
import adbtool_desktop.composeapp.generated.resources.view_activity
import adbtool_desktop.composeapp.generated.resources.wifi_info
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
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.VerifiedUser
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
import com.ludoven.adbtool.iconColors
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.util.AdbTool.runAdbAndShowResult
import com.ludoven.adbtool.util.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@ExperimentalMaterial3Api
@Composable
@Preview
fun CommonScreen() {
    val installingText = stringResource(Res.string.installing)
    val installSuccessText = stringResource(Res.string.install_success)
    val installFailedText = stringResource(Res.string.install_failed)
    val apkNotSelectText = stringResource(Res.string.apk_not_selected)
    val folderNotSelectedText = stringResource(Res.string.folder_not_selected)
    val screenshotSuccessText = stringResource(Res.string.screenshot_success)
    val screenshotFailedText = stringResource(Res.string.screenshot_failed)
    val activityNotFoundText = stringResource(Res.string.activity_not_found)
    val currentActivityText = stringResource(Res.string.current_activity)


    var showDialog by remember { mutableStateOf(false) }
    var dialogText by remember { mutableStateOf(installingText) }
    // 使用 rememberCoroutineScope 来管理协程生命周期，避免泄露
    val coroutineScope = rememberCoroutineScope()
    // 新增的状态，用于控制文本输入弹窗的显示
    var showTextInputDialog by remember { mutableStateOf(false) }


    val commonItems = listOf(
        AdbFunction(stringResource(Res.string.install_app), Icons.Default.InstallMobile) {
            val apkPath = FileUtils.selectApkFile()
            if (apkPath != null) {
                // 1. 立即显示“正在安装...”的弹窗
                dialogText = installingText
                showDialog = true

                // 2. 启动后台安装任务
                coroutineScope.launch(Dispatchers.IO) { // 在 IO 线程执行耗时操作
                    val success = AdbTool.installApk(apkPath) // 执行安装
                    withContext(Dispatchers.Main) { // 切换回主线程更新 UI
                        // 3. 根据安装结果更新弹窗文本
                        dialogText = if (success) installSuccessText else installFailedText
                        // 4. 短暂显示结果，然后关闭弹窗
                        delay(2000) // 显示结果2秒
                        showDialog = false
                    }
                }
            } else {
                // 如果用户没有选择文件，可以给一个提示
                coroutineScope.launch {
                    dialogText = apkNotSelectText
                    showDialog = true
                    delay(2000)
                    showDialog = false
                }
            }
        },

        AdbFunction(stringResource(Res.string.input_text), Icons.Default.Edit) {
            showTextInputDialog = true
        },
        AdbFunction(stringResource(Res.string.screenshot), Icons.Default.PhotoCamera) {
            coroutineScope.launch {
                // 不要放 IO 线程，否则 Swing UI 不会弹窗！
                val folderPath = FileUtils.selectFolder()

                if (folderPath == null) {
                    dialogText = folderNotSelectedText
                    showDialog = true
                    delay(2000)
                    showDialog = false
                    return@launch
                }

                val savePath = "$folderPath/screen_${System.currentTimeMillis()}.png"

                // 截图可放 IO 线程
                val success = withContext(Dispatchers.IO) {
                    AdbTool.takeScreenshot(savePath)
                }

                dialogText = if (success) screenshotSuccessText else screenshotFailedText
                showDialog = true
                delay(2000)
                showDialog = false
            }
        },
        AdbFunction(stringResource(Res.string.view_activity), Icons.Default.Visibility) {
            coroutineScope.launch {
                val activity = withContext(Dispatchers.IO) {
                    AdbTool.getCurrentActivity()
                }
                dialogText = activity?.let { currentActivityText.format(it) } ?: activityNotFoundText
                showDialog = true
            }
        }
    )

    val sysItems = listOf(
        AdbFunction(stringResource(Res.string.reboot_device), Icons.Default.RestartAlt) {
            runAdbAndShowResult(
                coroutineScope,
                "reboot",
                setDialogText = { dialogText = it },
                setShowDialog = { showDialog = it }
            )
        },
        AdbFunction(stringResource(Res.string.is_rooted), Icons.Default.VerifiedUser) {
            runAdbAndShowResult(
                coroutineScope,
                "su -c id",
                setDialogText = { dialogText = it },
                setShowDialog = { showDialog = it }
            )
        },
        AdbFunction(stringResource(Res.string.wifi_info), Icons.Default.Wifi) {
            runAdbAndShowResult(
                coroutineScope,
                "dumpsys wifi",
                setDialogText = { dialogText = it },
                setShowDialog = { showDialog = it }
            )
        },
        AdbFunction(stringResource(Res.string.cpu_info), Icons.Default.Memory) {
            runAdbAndShowResult(
                coroutineScope,
                "top -n 1",
                setDialogText = { dialogText = it },
                setShowDialog = { showDialog = it }
            )
        },
        AdbFunction(stringResource(Res.string.network_status), Icons.Default.NetworkCheck) {
            runAdbAndShowResult(
                coroutineScope,
                "dumpsys connectivity",
                setDialogText = { dialogText = it },
                setShowDialog = { showDialog = it }
            )
        },
        AdbFunction(stringResource(Res.string.battery_status), Icons.Default.BatteryStd) {
            runAdbAndShowResult(
                coroutineScope,
                "dumpsys battery",
                setDialogText = { dialogText = it },
                setShowDialog = { showDialog = it }
            )
        },
        AdbFunction(stringResource(Res.string.screen_resolution), Icons.Default.ScreenSearchDesktop) {
            runAdbAndShowResult(
                coroutineScope,
                "wm size",
                setDialogText = { dialogText = it },
                setShowDialog = { showDialog = it }
            )
        },
        AdbFunction(stringResource(Res.string.developer_options), Icons.Default.DeveloperMode) {
            runAdbAndShowResult(
                coroutineScope,
                "am start -a android.settings.APPLICATION_DEVELOPMENT_SETTINGS",
                setDialogText = { dialogText = it },
                setShowDialog = { showDialog = it }
            )
        }
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
                        SectionTitle(stringResource(Res.string.common_functions), Color.Red, modifier = Modifier.padding(top = 12.dp, start = 15.dp))
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
                        SectionTitle(stringResource(Res.string.system_functions), Color.Blue, modifier = Modifier.padding(top = 12.dp, start = 15.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            maxItemsInEachRow = 5
                        ) {
                            sysItems.forEachIndexed { index, item ->
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

@Composable
fun SectionTitle(title: String, barColor: Color,modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(0.18f)
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

    val inputHint = stringResource(Res.string.input_hint)
    val escapeText = stringResource(Res.string.escape_spaces)
    val cancelText = stringResource(Res.string.cancel)
    val confirmText = stringResource(Res.string.confirm)

    AlertDialog(
        onDismissRequest = { onDismiss.invoke() },
        text = {
            Column {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text(inputHint) },
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = escapeSpaces,
                        onCheckedChange = { escapeSpaces = it }
                    )
                    Text(
                        text = escapeText,
                        modifier = Modifier.clickable { escapeSpaces = !escapeSpaces })
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onDismiss.invoke() }) {
                    Text(cancelText)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onDismiss.invoke()
                        coroutineScope.launch {
                            val success = AdbTool.inputText(text = inputText)
                            println(if (success) "输入成功" else "输入失败")
                        }
                    }
                ) {
                    Text(confirmText)
                }
            }
        }, containerColor = LightColorScheme.background
    )
}

@Composable
fun TipDialog(dialogText: String, onDismiss: () -> Unit) {
    val tipTitle = stringResource(Res.string.tip_title)
    val confirmText = stringResource(Res.string.confirm)

    AlertDialog(
        onDismissRequest = { onDismiss.invoke() },
        title = { Text(tipTitle) },
        text = { Text(dialogText) },
        confirmButton = {
            TextButton(onClick = { onDismiss.invoke() }) {
                Text(confirmText)
            }
        }, containerColor = LightColorScheme.background
    )
}

