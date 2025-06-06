package com.ludoven.adbtool

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.viewmodel.DevicesViewModel

// 假设 AdbTool 对象已定义，包含 executeAdbCommand, getConnectedDevices 等方法
// 确保 AdbTool 能够正确执行 adb 命令并解析输出

@ExperimentalMaterial3Api
@Composable
fun DevicesScreen(viewModel: DevicesViewModel) {

    val devices by viewModel.devices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshDevices()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        Text(text = "设备管理页面", style = MaterialTheme.typography.headlineMedium)
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
                        Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { showDropdown = !showDropdown })
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false }
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "设备信息",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (selectedDevice == null) {
                    Text("请选择一个设备以查看其信息。")
                } else if (deviceInfo.isEmpty() && !isLoading) {
                    Text("未能获取到设备详细信息。", color = Color.Gray)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(deviceInfo.entries.toList()) { (key, value) ->
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
//                                    Divider(modifier = Modifier.padding(horizontal = 12.dp))
                                }
                            }
                        }
                    }

                }
            }
        }
    }
}
