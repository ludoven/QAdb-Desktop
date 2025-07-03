package com.ludoven.adbtool.pages

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.battery
import adbtool_desktop.composeapp.generated.resources.cpu
import adbtool_desktop.composeapp.generated.resources.device_info
import adbtool_desktop.composeapp.generated.resources.disconnect
import adbtool_desktop.composeapp.generated.resources.home
import adbtool_desktop.composeapp.generated.resources.memory_usage
import adbtool_desktop.composeapp.generated.resources.no_device
import adbtool_desktop.composeapp.generated.resources.no_device_available
import adbtool_desktop.composeapp.generated.resources.no_device_info
import adbtool_desktop.composeapp.generated.resources.refresh
import adbtool_desktop.composeapp.generated.resources.select_device
import adbtool_desktop.composeapp.generated.resources.select_device_hint
import adbtool_desktop.composeapp.generated.resources.storage_usage
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
import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.viewmodel.DevicesViewModel
import com.ludoven.adbtool.widget.InfoCard
import org.jetbrains.compose.resources.Resource
import org.jetbrains.compose.resources.stringResource


// 确保你导入了 @ExperimentalMaterial3Api
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: DevicesViewModel) {

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
                        value = selectedDevice ?: stringResource(Res.string.no_device),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.select_device)) },
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
                                text = { Text(stringResource(Res.string.no_device_available)) },
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
                        Text(stringResource(Res.string.refresh))
                    }
                    Button(
                        onClick = { viewModel.disconnectSelectedDevice() },
                        enabled = selectedDevice != null && !isLoading
                    ) {
                        Text(stringResource(Res.string.disconnect))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (selectedDevice != null){
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .padding(bottom = 30.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(Res.string.cpu),
                        value = "${centerInfo["CPU"]}",
                        icon = Icons.Default.Memory,
                        iconColor = Color(0xFFEF5350) // 红色
                    )
                    InfoCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(Res.string.memory_usage),
                        value = "${centerInfo["内存"]}",
                        icon = Icons.Default.Storage,
                        iconColor = Color(0xFF42A5F5) // 蓝色
                    )
                    InfoCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(Res.string.storage_usage),
                        value = "${centerInfo["存储"]}",
                        icon = Icons.Default.SdStorage,
                        iconColor = Color(0xFF66BB6A) // 绿色
                    )
                    InfoCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(Res.string.battery),
                        value = "${centerInfo["电量"]}",
                        icon = Icons.Default.BatteryFull,
                        iconColor = Color(0xFFFFA726) // 橙色
                    )
                }
            }


            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = LightColorScheme.background
                )
            ) {
                Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    SectionTitle(stringResource(Res.string.device_info), Color.Blue, modifier = Modifier.padding(bottom = 10.dp))
                    if (selectedDevice == null) {
                        Text(stringResource(Res.string.select_device_hint))
                    } else if (deviceInfo.isEmpty() && !isLoading) {
                        Text(stringResource(Res.string.no_device_info), color = Color.Gray)
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            deviceInfo.entries.toList().forEachIndexed { index, (key, value) ->
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
                                                style = MaterialTheme.typography.titleMedium.copy(color = Color.Black),
                                                maxLines = 1
                                            )
                                            Text(
                                                text = value,
                                                modifier = Modifier.weight(0.6f),
                                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
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
fun InfoCard2(
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
                Text(text = value, style = MaterialTheme.typography.bodyMedium)
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = Color.Gray)
            }
        }
    }
}
