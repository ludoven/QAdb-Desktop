package com.ludoven.adbtool.pages

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.LightColorScheme
import com.ludoven.adbtool.viewmodel.DevicesViewModel


// 确保你导入了 @ExperimentalMaterial3Api
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(viewModel: DevicesViewModel) {

    val devices by viewModel.devices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val centerInfo by viewModel.centerInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showDropdown by remember { mutableStateOf(false) }

    // 获取 ScrollState
    val scrollState = rememberScrollState() // <-- 1. 创建滚动状态

    LaunchedEffect(Unit) {
        viewModel.refreshDevices()
    }

    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ExposedDropdownMenuBox(
                    expanded = showDropdown,
                    onExpandedChange = { showDropdown = !it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedDevice ?: "无设备",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("选择设备") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                null,
                                Modifier.clickable { showDropdown = !showDropdown })
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },
                        modifier = Modifier.background(LightColorScheme.surface) // 确保这里使用 Material3 的 Colors

                    ) {
                        if (devices.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("没有可用的设备") },
                                onClick = { showDropdown = false },
                                enabled = false
                            )
                        } else {
                            devices.forEach { deviceId ->
                                DropdownMenuItem(
                                    text = { Text(deviceId) },
                                    onClick = {
                                        viewModel.selectDevice(deviceId)
                                        showDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.refreshDevices() }, enabled = !isLoading) {
                        Text("刷新")
                    }
                    Button(
                        onClick = { viewModel.disconnectSelectedDevice() },
                        enabled = selectedDevice != null && !isLoading
                    ) {
                        Text("断开连接")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(bottom = 30.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoCard(
                    modifier = Modifier.weight(1f),
                    title = "CPU",
                    value = "${centerInfo["CPU"]}",
                    icon = Icons.Default.Memory,
                    iconColor = Color(0xFFEF5350) // 红色
                )
                InfoCard(
                    modifier = Modifier.weight(1f),
                    title = "内存占用",
                    value = "${centerInfo["内存"]}",
                    icon = Icons.Default.Storage,
                    iconColor = Color(0xFF42A5F5) // 蓝色
                )
                InfoCard(
                    modifier = Modifier.weight(1f),
                    title = "存储占用",
                    value = "${centerInfo["存储"]}",
                    icon = Icons.Default.SdStorage,
                    iconColor = Color(0xFF66BB6A) // 绿色
                )
                InfoCard(
                    modifier = Modifier.weight(1f),
                    title = "电池",
                    value = "${centerInfo["电量"]}",
                    icon = Icons.Default.BatteryFull,
                    iconColor = Color(0xFFFFA726) // 橙色
                )
            }


            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = LightColorScheme.background
                )
            ) {
                Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
                    SectionTitle("设备信息", Color.Blue, modifier = Modifier.padding(bottom = 10.dp))
                    if (selectedDevice == null) {
                        Text("请选择一个设备以查看其信息。")
                    } else if (deviceInfo.isEmpty() && !isLoading) {
                        Text("未能获取到设备详细信息。", color = Color.Gray)
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            deviceInfo.entries.toList()
                                .forEach { (key, value) -> // 使用 forEach 代替 items
                                    SelectionContainer {
                                        Column {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp, horizontal = 12.dp)
                                            ) {
                                                Text(
                                                    text = "$key:",
                                                    modifier = Modifier.weight(0.4f),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = value,
                                                    modifier = Modifier.weight(0.6f),
                                                    style = MaterialTheme.typography.bodyMedium
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
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}

@Composable
fun InfoCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary // 默认颜色
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = LightColorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = value, style = MaterialTheme.typography.titleMedium)
                Text(text = title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
