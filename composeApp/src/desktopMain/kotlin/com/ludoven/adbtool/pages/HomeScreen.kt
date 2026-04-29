package com.ludoven.adbtool.pages

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.battery_level
import adbtool_desktop.composeapp.generated.resources.connected
import adbtool_desktop.composeapp.generated.resources.connection_info
import adbtool_desktop.composeapp.generated.resources.connection_type
import adbtool_desktop.composeapp.generated.resources.cpu_usage
import adbtool_desktop.composeapp.generated.resources.device_info
import adbtool_desktop.composeapp.generated.resources.android_version
import adbtool_desktop.composeapp.generated.resources.build_fingerprint
import adbtool_desktop.composeapp.generated.resources.device_overview
import adbtool_desktop.composeapp.generated.resources.device_model
import adbtool_desktop.composeapp.generated.resources.device_status
import adbtool_desktop.composeapp.generated.resources.disconnected
import adbtool_desktop.composeapp.generated.resources.disconnect
import adbtool_desktop.composeapp.generated.resources.file_manager
import adbtool_desktop.composeapp.generated.resources.font_scale
import adbtool_desktop.composeapp.generated.resources.ip_address
import adbtool_desktop.composeapp.generated.resources.install_apk_short
import adbtool_desktop.composeapp.generated.resources.kernel_version
import adbtool_desktop.composeapp.generated.resources.last_refresh
import adbtool_desktop.composeapp.generated.resources.mac_address
import adbtool_desktop.composeapp.generated.resources.manufacturer
import adbtool_desktop.composeapp.generated.resources.memory_usage
import adbtool_desktop.composeapp.generated.resources.no_device
import adbtool_desktop.composeapp.generated.resources.no_device_available
import adbtool_desktop.composeapp.generated.resources.no_device_info
import adbtool_desktop.composeapp.generated.resources.offline
import adbtool_desktop.composeapp.generated.resources.online
import adbtool_desktop.composeapp.generated.resources.open_shell
import adbtool_desktop.composeapp.generated.resources.quick_actions
import adbtool_desktop.composeapp.generated.resources.refresh
import adbtool_desktop.composeapp.generated.resources.port
import adbtool_desktop.composeapp.generated.resources.rom_version
import adbtool_desktop.composeapp.generated.resources.screen_resolution
import adbtool_desktop.composeapp.generated.resources.select_device
import adbtool_desktop.composeapp.generated.resources.select_device_hint
import adbtool_desktop.composeapp.generated.resources.storage_usage
import adbtool_desktop.composeapp.generated.resources.usb_connection
import adbtool_desktop.composeapp.generated.resources.wireless_connection
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.entity.DeviceInfoData
import com.ludoven.adbtool.viewmodel.DevicesViewModel
import com.ludoven.adbtool.widget.DashboardMetricCard
import com.ludoven.adbtool.widget.DashboardPanel
import com.ludoven.adbtool.widget.DeviceInfoCell
import com.ludoven.adbtool.widget.GlassCard
import com.ludoven.adbtool.widget.LabeledValueRow
import com.ludoven.adbtool.widget.OutlineActionButton
import com.ludoven.adbtool.widget.QuickActionCard
import com.ludoven.adbtool.widget.StatusBadge
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: DevicesViewModel) {
    val devices by viewModel.devices.collectAsState()
    val deviceDisplayNames by viewModel.deviceDisplayNames.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val centerInfo by viewModel.centerInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val lastRefreshTime by viewModel.lastRefreshTime.collectAsState()

    var showDropdown by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        if (devices.isEmpty()) {
            viewModel.refreshDevices()
        }
    }

    val isConnected = selectedDevice != null
    val wirelessConnection = isWirelessConnection(selectedDevice, deviceInfo?.ipAddress)
    val connectionType = stringResource(
        if (wirelessConnection) Res.string.wireless_connection else Res.string.usb_connection
    )
    val connectionStatus = stringResource(
        if (isConnected) Res.string.online else Res.string.offline
    )

    val batterySupporting = centerInfo?.batteryStatus?.let { stringResource(it.stringResource) }.orEmpty()
    val metricItems = listOf(
        HomeMetricModel(
            titleKey = Res.string.cpu_usage,
            value = centerInfo?.cpuUsage.orDash(),
            icon = Icons.Default.Memory,
            accentColor = Color(0xFFFF5A5F),
            progress = parsePercentProgress(centerInfo?.cpuUsage)
        ),
        HomeMetricModel(
            titleKey = Res.string.memory_usage,
            value = centerInfo?.memoryUsage.orDash(),
            icon = Icons.Default.Storage,
            accentColor = Color(0xFF3B82F6),
            progress = parsePercentProgress(centerInfo?.memoryUsage)
        ),
        HomeMetricModel(
            titleKey = Res.string.storage_usage,
            value = centerInfo?.storageUsage.orDash("-- / --"),
            icon = Icons.Default.SdStorage,
            accentColor = Color(0xFF2DBE60),
            progress = parseStorageProgress(centerInfo?.storageUsage)
        ),
        HomeMetricModel(
            titleKey = Res.string.battery_level,
            value = centerInfo?.batteryLevel.orDash(),
            supporting = batterySupporting,
            icon = Icons.Default.BatteryFull,
            accentColor = Color(0xFFF59E0B),
            progress = parsePercentProgress(centerInfo?.batteryLevel)
        )
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compactLayout = maxWidth < 900.dp
        val spacing = if (compactLayout) 10.dp else 14.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 18.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                HomeHeader(
                    compactLayout = compactLayout,
                    isConnected = isConnected,
                    isLoading = isLoading,
                    onRefresh = { viewModel.refreshDevices() },
                    onDisconnect = { viewModel.disconnectSelectedDevice() }
                )

                DeviceSelectorCard(
                    devices = devices,
                    deviceDisplayNames = deviceDisplayNames,
                    selectedDevice = selectedDevice,
                    isConnected = isConnected,
                    expanded = showDropdown,
                    onExpandedChange = { showDropdown = it },
                    onDeviceSelected = { viewModel.selectDevice(it) }
                )

                MetricsSection(
                    items = metricItems,
                    compactLayout = compactLayout
                )

                if (compactLayout) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                        DeviceInfoPanel(
                            modifier = Modifier.fillMaxWidth(),
                            deviceInfo = deviceInfo
                        )
                        ConnectionInfoPanel(
                            connectionType = connectionType,
                            connectionStatus = connectionStatus,
                            ipAddress = deviceInfo?.ipAddress.orDash(),
                            port = extractPort(selectedDevice, deviceInfo?.ipAddress),
                            lastRefreshTime = lastRefreshTime,
                            isConnected = isConnected
                        )
                        QuickActionsPanel()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        DeviceInfoPanel(
                            modifier = Modifier.weight(1.45f),
                            deviceInfo = deviceInfo
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ConnectionInfoPanel(
                                connectionType = connectionType,
                                connectionStatus = connectionStatus,
                                ipAddress = deviceInfo?.ipAddress.orDash(),
                                port = extractPort(selectedDevice, deviceInfo?.ipAddress),
                                lastRefreshTime = lastRefreshTime,
                                isConnected = isConnected
                            )

                            QuickActionsPanel()
                        }
                    }
                }

                if (!isConnected) {
                    Text(
                        text = stringResource(Res.string.select_device_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun HomeHeader(
    compactLayout: Boolean,
    isConnected: Boolean,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit
) {
    if (compactLayout) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.device_overview),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(
                    text = stringResource(if (isConnected) Res.string.connected else Res.string.disconnected),
                    active = isConnected
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlineActionButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.refresh),
                    icon = Icons.Default.Refresh,
                    tint = MaterialTheme.colorScheme.primary,
                    enabled = !isLoading,
                    onClick = onRefresh
                )
                OutlineActionButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.disconnect),
                    icon = Icons.Default.Close,
                    tint = MaterialTheme.colorScheme.error,
                    enabled = isConnected && !isLoading,
                    onClick = onDisconnect
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.device_overview),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            StatusBadge(
                text = stringResource(if (isConnected) Res.string.connected else Res.string.disconnected),
                active = isConnected
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlineActionButton(
                text = stringResource(Res.string.refresh),
                icon = Icons.Default.Refresh,
                tint = MaterialTheme.colorScheme.primary,
                enabled = !isLoading,
                onClick = onRefresh
            )
            OutlineActionButton(
                text = stringResource(Res.string.disconnect),
                icon = Icons.Default.Close,
                tint = MaterialTheme.colorScheme.error,
                enabled = isConnected && !isLoading,
                onClick = onDisconnect
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSelectorCard(
    devices: List<String>,
    deviceDisplayNames: Map<String, String>,
    selectedDevice: String?,
    isConnected: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDeviceSelected: (String) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(Res.string.select_device),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedDevice
                            ?.let { formatDeviceDisplay(it, deviceDisplayNames[it]) }
                            ?: stringResource(Res.string.no_device),
                        onValueChange = {},
                        readOnly = true,
                        enabled = devices.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable(
                                enabled = devices.isNotEmpty(),
                                onClick = { onExpandedChange(!expanded) }
                            )
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { onExpandedChange(false) },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (devices.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.no_device_available)) },
                                onClick = { onExpandedChange(false) },
                                enabled = false
                            )
                        } else {
                            devices.forEach { deviceId ->
                                val displayName = formatDeviceDisplay(deviceId, deviceDisplayNames[deviceId])
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = displayName,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        onDeviceSelected(deviceId)
                                        onExpandedChange(false)
                                    }
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 48.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(9.dp)
                            .background(
                                if (isConnected) Color(0xFF2DBE60) else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(999.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoPanel(
    modifier: Modifier = Modifier,
    deviceInfo: DeviceInfoData?
) {
    DashboardPanel(
        modifier = modifier,
        title = stringResource(Res.string.device_info),
        icon = Icons.Default.Info
    ) {
        if (deviceInfo == null) {
            Text(
                text = stringResource(Res.string.no_device_info),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            return@DashboardPanel
        }

        val cellModifier = Modifier.weight(1f)

        DeviceInfoRow(
            left = DeviceInfoEntry(
                label = stringResource(Res.string.android_version),
                value = formatAndroidVersionWithApi(deviceInfo.androidVersion, deviceInfo.sdkVersion),
                icon = Icons.Default.Info,
                tint = Color(0xFF2DBE60)
            ),
            right = DeviceInfoEntry(
                label = stringResource(Res.string.kernel_version),
                value = deviceInfo.kernelVersion.orDash(),
                icon = Icons.Default.Code,
                tint = Color(0xFF3B82F6)
            ),
            cellModifier = cellModifier
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(16.dp))

        DeviceInfoRow(
            left = DeviceInfoEntry(
                label = stringResource(Res.string.device_model),
                value = deviceInfo.deviceModel.orDash(),
                icon = Icons.Default.Apps,
                tint = Color(0xFF8B5CF6)
            ),
            right = DeviceInfoEntry(
                label = stringResource(Res.string.manufacturer),
                value = deviceInfo.manufacturer.orDash(),
                icon = Icons.Default.Storage,
                tint = Color(0xFFF59E0B)
            ),
            cellModifier = cellModifier
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(16.dp))

        DeviceInfoRow(
            left = DeviceInfoEntry(
                label = stringResource(Res.string.rom_version),
                value = deviceInfo.romVersion.orDash(),
                icon = Icons.Default.DeveloperMode,
                tint = Color(0xFF42A5F5)
            ),
            right = DeviceInfoEntry(
                label = stringResource(Res.string.screen_resolution),
                value = deviceInfo.screenResolution.orDash(),
                icon = Icons.Default.Memory,
                tint = Color(0xFF34D399)
            ),
            cellModifier = cellModifier
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(16.dp))

        DeviceInfoRow(
            left = DeviceInfoEntry(
                label = stringResource(Res.string.font_scale),
                value = deviceInfo.fontScale.orDash(),
                icon = Icons.Default.Storage,
                tint = Color(0xFF0EA5E9)
            ),
            right = DeviceInfoEntry(
                label = stringResource(Res.string.mac_address),
                value = deviceInfo.macAddress.orDash(),
                icon = Icons.Default.Link,
                tint = Color(0xFF8B5CF6)
            ),
            cellModifier = cellModifier
        )

        Spacer(modifier = Modifier.height(16.dp))

        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(Res.string.build_fingerprint),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = deviceInfo.buildFingerprint.orDash(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ConnectionInfoPanel(
    connectionType: String,
    connectionStatus: String,
    ipAddress: String,
    port: String,
    lastRefreshTime: String,
    isConnected: Boolean
) {
    DashboardPanel(
        title = stringResource(Res.string.connection_info),
        icon = Icons.Default.Link
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledValueRow(
                label = stringResource(Res.string.connection_type),
                value = connectionType,
                pillColor = MaterialTheme.colorScheme.primary
            )
            LabeledValueRow(
                label = stringResource(Res.string.device_status),
                value = connectionStatus,
                pillColor = if (isConnected) Color(0xFF2DBE60) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            LabeledValueRow(label = stringResource(Res.string.ip_address), value = ipAddress)
            LabeledValueRow(label = stringResource(Res.string.port), value = port)
            LabeledValueRow(label = stringResource(Res.string.last_refresh), value = lastRefreshTime)
        }
    }
}

@Composable
private fun QuickActionsPanel() {
    DashboardPanel(
        title = stringResource(Res.string.quick_actions),
        icon = Icons.Default.CheckCircle
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.open_shell),
                icon = Icons.Default.DeveloperMode,
                accentColor = Color(0xFF3B82F6)
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.file_manager),
                icon = Icons.Default.Folder,
                accentColor = Color(0xFF22C55E)
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.install_apk_short),
                icon = Icons.Default.Download,
                accentColor = Color(0xFF8B5CF6)
            )
        }
    }
}

@Composable
private fun DeviceInfoRow(
    left: DeviceInfoEntry,
    right: DeviceInfoEntry,
    cellModifier: Modifier
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DeviceInfoCell(
            modifier = cellModifier,
            label = left.label,
            value = left.value,
            icon = left.icon,
            tint = left.tint
        )
        DeviceInfoCell(
            modifier = cellModifier,
            label = right.label,
            value = right.value,
            icon = right.icon,
            tint = right.tint
        )
    }
}

@Composable
private fun MetricsSection(
    items: List<HomeMetricModel>,
    compactLayout: Boolean
) {
    if (compactLayout) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        DashboardMetricCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(item.titleKey),
                            value = item.value,
                            supportingText = item.supporting.takeIf { it.isNotBlank() },
                            icon = item.icon,
                            accentColor = item.accentColor,
                            progress = item.progress
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEach { item ->
                DashboardMetricCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(item.titleKey),
                    value = item.value,
                    supportingText = item.supporting.takeIf { it.isNotBlank() },
                    icon = item.icon,
                    accentColor = item.accentColor,
                    progress = item.progress
                )
            }
        }
    }
}

private data class HomeMetricModel(
    val titleKey: org.jetbrains.compose.resources.StringResource,
    val value: String,
    val icon: ImageVector,
    val accentColor: Color,
    val supporting: String = "",
    val progress: Float
)

private data class DeviceInfoEntry(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val tint: Color
)

private fun parsePercentProgress(raw: String?): Float {
    val percent = raw?.trim()?.removeSuffix("%")?.toFloatOrNull() ?: return 0f
    return (percent / 100f).coerceIn(0f, 1f)
}

private fun parseStorageProgress(raw: String?): Float {
    if (raw.isNullOrBlank()) return 0f
    val match = Regex("""([\d.]+)\s*G?(?:B)?\s*/\s*([\d.]+)\s*G?(?:B)?""").find(raw) ?: return 0f
    val used = match.groupValues.getOrNull(1)?.toFloatOrNull() ?: return 0f
    val total = match.groupValues.getOrNull(2)?.toFloatOrNull() ?: return 0f
    if (total <= 0f) return 0f
    return (used / total).coerceIn(0f, 1f)
}

private fun formatAndroidVersionWithApi(androidVersion: String?, sdkVersion: String?): String {
    val version = androidVersion.orDash()
    val sdk = sdkVersion?.takeIf { it.isNotBlank() } ?: return version
    return if (androidVersion.isNullOrBlank()) "API $sdk" else "$version (API $sdk)"
}

private fun formatDeviceDisplay(deviceId: String, model: String?): String {
    val cleanModel = model?.trim().orEmpty()
    return if (cleanModel.isNotBlank()) "$cleanModel （$deviceId）" else deviceId
}

private fun isWirelessConnection(deviceId: String?, ipAddress: String?): Boolean {
    if (!ipAddress.isNullOrBlank()) return true
    if (deviceId.isNullOrBlank()) return false
    return deviceId.contains(':') || deviceId.contains("tls-connect", ignoreCase = true)
}

private fun extractPort(deviceId: String?, ipAddress: String?): String {
    val port = deviceId
        ?.substringAfterLast(':', "")
        ?.takeIf { it.all(Char::isDigit) && it.isNotBlank() }
    if (!port.isNullOrBlank()) return port
    return if (!ipAddress.isNullOrBlank()) "5555" else "--"
}

private fun String?.orDash(fallback: String = "--"): String {
    return if (this.isNullOrBlank()) fallback else this
}
