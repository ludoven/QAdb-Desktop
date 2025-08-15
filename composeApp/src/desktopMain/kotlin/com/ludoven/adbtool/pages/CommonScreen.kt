package com.ludoven.adbtool.pages

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.activity_not_found
import adbtool_desktop.composeapp.generated.resources.apk_not_selected
import adbtool_desktop.composeapp.generated.resources.battery_status
import adbtool_desktop.composeapp.generated.resources.cancel
import adbtool_desktop.composeapp.generated.resources.common_functions
import adbtool_desktop.composeapp.generated.resources.confirm
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.LightColorScheme
import com.ludoven.adbtool.entity.AdbFunction
import com.ludoven.adbtool.entity.AdbFunctionType
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.iconColors
import com.ludoven.adbtool.util.AdbTool
import com.ludoven.adbtool.viewmodel.CommonModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@ExperimentalMaterial3Api
@Composable
@Preview
fun CommonScreen(viewModel: CommonModel) {
    val coroutineScope = rememberCoroutineScope()
    val showDialog by viewModel.showDialog.collectAsState()
    val showTextInputDialog by viewModel.showInputDialog.collectAsState()
    val dialogMsg by viewModel.dialogMessage.collectAsState()


    val commonItems = listOf(
        AdbFunction(
            stringResource(Res.string.install_app),
            Icons.Default.InstallMobile,
            AdbFunctionType.INSTALL_APK
        ),
        AdbFunction(
            stringResource(Res.string.input_text),
            Icons.Default.Edit,
            AdbFunctionType.INPUT_TEXT
        ),
        AdbFunction(
            stringResource(Res.string.screenshot),
            Icons.Default.PhotoCamera,
            AdbFunctionType.SCREENSHOT
        ),
        AdbFunction(
            stringResource(Res.string.view_activity),
            Icons.Default.Visibility,
            AdbFunctionType.VIEW_CURRENT_ACTIVITY
        ),
    )

    val sysItems = listOf(
        AdbFunction(
            stringResource(Res.string.reboot_device),
            Icons.Default.RestartAlt,
            AdbFunctionType.REBOOT_DEVICE
        ),
        AdbFunction(
            stringResource(Res.string.is_rooted),
            Icons.Default.VerifiedUser,
            AdbFunctionType.IS_ROOTED
        ),
        AdbFunction(
            stringResource(Res.string.wifi_info),
            Icons.Default.Wifi,
            AdbFunctionType.WIFI_INFO
        ),
        AdbFunction(
            stringResource(Res.string.cpu_info),
            Icons.Default.Memory,
            AdbFunctionType.CPU_INFO
        ),
        AdbFunction(
            stringResource(Res.string.network_status),
            Icons.Default.NetworkCheck,
            AdbFunctionType.NETWORK_STATUS
        ),
        AdbFunction(
            stringResource(Res.string.battery_status),
            Icons.Default.BatteryStd,
            AdbFunctionType.BATTERY_STATUS
        ),
        AdbFunction(
            stringResource(Res.string.screen_resolution),
            Icons.Default.ScreenSearchDesktop,
            AdbFunctionType.SCREEN_RESOLUTION
        ),
        AdbFunction(
            stringResource(Res.string.developer_options),
            Icons.Default.DeveloperMode,
            AdbFunctionType.DEVELOPER_OPTIONS
        ),

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
                        SectionTitle(
                            stringResource(Res.string.common_functions),
                            Color.Red,
                            modifier = Modifier.padding(top = 12.dp, start = 15.dp)
                        )
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
                                    onClick = { viewModel.executeAdbAction(item.type) },
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
                        SectionTitle(
                            stringResource(Res.string.system_functions),
                            Color.Blue,
                            modifier = Modifier.padding(top = 12.dp, start = 15.dp)
                        )
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


    if (showDialog) {
        dialogMsg?.let {
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

    if (showTextInputDialog) {
        textInputDialog(coroutineScope) {
            viewModel.showInputDialog(false)
        }
    }
}

@Composable
fun SectionTitle(title: String, barColor: Color, modifier: Modifier = Modifier) {
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
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center // 添加这一行，让 Text 内部的多行文本居中
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

