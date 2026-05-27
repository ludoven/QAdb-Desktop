package com.ludoven.adbtool.pages

import com.ludoven.adbtool.ui.mac.*
import com.ludoven.adbtool.entity.MsgContent

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.battery_level
import adbtool_desktop.composeapp.generated.resources.connected
import adbtool_desktop.composeapp.generated.resources.connected_for
import adbtool_desktop.composeapp.generated.resources.connection_info
import adbtool_desktop.composeapp.generated.resources.connection_type
import adbtool_desktop.composeapp.generated.resources.cpu_usage
import adbtool_desktop.composeapp.generated.resources.device_info
import adbtool_desktop.composeapp.generated.resources.android_version
import adbtool_desktop.composeapp.generated.resources.build_fingerprint
import adbtool_desktop.composeapp.generated.resources.device_overview
import adbtool_desktop.composeapp.generated.resources.device_model
import adbtool_desktop.composeapp.generated.resources.device_mirror
import adbtool_desktop.composeapp.generated.resources.device_status
import adbtool_desktop.composeapp.generated.resources.disconnected
import adbtool_desktop.composeapp.generated.resources.disconnect
import adbtool_desktop.composeapp.generated.resources.font_scale
import adbtool_desktop.composeapp.generated.resources.ip_address
import adbtool_desktop.composeapp.generated.resources.install_apk_short
import adbtool_desktop.composeapp.generated.resources.key_screenshot_short
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
import adbtool_desktop.composeapp.generated.resources.transfer_speed
import adbtool_desktop.composeapp.generated.resources.updated_ago
import adbtool_desktop.composeapp.generated.resources.latency
import adbtool_desktop.composeapp.generated.resources.usb_connection
import adbtool_desktop.composeapp.generated.resources.wireless_connection
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import com.ludoven.adbtool.ui.mac.DropdownMenu
import com.ludoven.adbtool.ui.mac.DropdownMenuItem
import com.ludoven.adbtool.ui.mac.ExperimentalMaterial3Api
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.Text
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
import com.ludoven.adbtool.entity.DeviceInfoData
import com.ludoven.adbtool.util.AdbPathManager
import com.ludoven.adbtool.util.isWirelessAdbConnection
import com.ludoven.adbtool.viewmodel.DevicesViewModel
import com.ludoven.adbtool.widget.DashboardMetricCard
import com.ludoven.adbtool.widget.DashboardPanel
import com.ludoven.adbtool.widget.DeviceInfoCell
import com.ludoven.adbtool.widget.GlassCard
import com.ludoven.adbtool.widget.LabeledValueRow
import com.ludoven.adbtool.widget.OutlineActionButton
import com.ludoven.adbtool.widget.QuickActionCard
import com.ludoven.adbtool.widget.StatusBadge
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DevicesViewModel,
    onScreenshot: () -> Unit = {},
    onInstallApk: () -> Unit = {},
    onMirrorDevice: () -> Unit = {},
    onOpenShell: () -> Unit = {}
) {
    val devices by viewModel.devices.collectAsState()
    val deviceDisplayNames by viewModel.deviceDisplayNames.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val centerInfo by viewModel.centerInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val lastRefreshTime by viewModel.lastRefreshTime.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val dialogMessage by viewModel.dialogMessage.collectAsState()

    var showDropdown by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (showDialog) {
        dialogMessage?.let { message ->
            TipDialog(
                dialogText = when (message) {
                    is MsgContent.Resource -> stringResource(message.stringResource, *message.args.toTypedArray())
                    is MsgContent.Text -> message.text
                }
            ) { viewModel.dismissTipDialog() }
        }
    }

    LaunchedEffect(Unit) {
        if (devices.isEmpty()) {
            viewModel.refreshDevices()
        }
    }

    val isConnected = selectedDevice != null
    var connectedSinceDeviceId by remember { mutableStateOf<String?>(null) }
    var connectedSince by remember { mutableStateOf<LocalDateTime?>(null) }
    LaunchedEffect(selectedDevice) {
        val deviceId = selectedDevice
        if (deviceId != connectedSinceDeviceId) {
            connectedSinceDeviceId = deviceId
            connectedSince = if (deviceId == null) null else LocalDateTime.now()
        }
    }
    val wirelessConnection = isWirelessAdbConnection(selectedDevice, deviceInfo?.ipAddress)
    val connectionType = stringResource(
        if (wirelessConnection) Res.string.wireless_connection else Res.string.usb_connection
    )
    val connectionStatus = stringResource(
        if (isConnected) Res.string.online else Res.string.offline
    )

    val batterySupporting = centerInfo?.batteryStatus?.let { stringResource(it.stringResource) }.orEmpty()
    val batteryValue = buildInlineMetricValue(
        primary = centerInfo?.batteryLevel.orDash(),
        suffix = batterySupporting
    )
    val selectedDeviceLabel = selectedDevice
        ?.let { formatPrimaryDeviceName(deviceInfo?.deviceModel, deviceDisplayNames[it], it) }
        ?: stringResource(Res.string.no_device)
    val androidHeadline = formatAndroidVersionWithApi(deviceInfo?.androidVersion, deviceInfo?.sdkVersion)
    val connectionHeadline = if (isConnected) "$connectionType · $connectionStatus" else connectionStatus
    val relativeUpdated = formatRelativeRefresh(lastRefreshTime)
    val connectedDuration = formatConnectedDuration(connectedSince)

    val metricItems = listOf(
        HomeMetricModel(
            titleKey = Res.string.cpu_usage,
            value = centerInfo?.cpuUsage.orDash(),
            icon = Icons.Default.Memory,
            accentColor = Color(0xFF5F7FA8),
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
            accentColor = Color(0xFF4F8E78),
            progress = parseStorageProgress(centerInfo?.storageUsage)
        ),
        HomeMetricModel(
            titleKey = Res.string.battery_level,
            value = batteryValue,
            icon = Icons.Default.BatteryFull,
            accentColor = Color(0xFFAD8A4A),
            progress = parsePercentProgress(centerInfo?.batteryLevel)
        )
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compactLayout = maxWidth < 900.dp
        val spacing = if (compactLayout) 16.dp else 24.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                DeviceControlPanel(
                    compactLayout = compactLayout,
                    deviceName = selectedDeviceLabel,
                    androidVersion = androidHeadline,
                    connectionInfo = connectionHeadline,
                    batteryInfo = batteryValue,
                    isConnected = isConnected,
                    isLoading = isLoading,
                    devices = devices,
                    deviceDisplayNames = deviceDisplayNames,
                    selectedDevice = selectedDevice,
                    selectedDeviceLabel = selectedDeviceLabel,
                    selectedDeviceAddress = selectedDevice.orDash(),
                    selectedDeviceStatus = if (isConnected) connectionHeadline else stringResource(Res.string.no_device),
                    selectorExpanded = showDropdown,
                    onSelectorExpandedChange = { showDropdown = it },
                    onDeviceSelected = { viewModel.selectDevice(it) },
                    onRefresh = { viewModel.refreshDevices() },
                    onDisconnect = { viewModel.disconnectSelectedDevice() }
                )

                MetricsSection(
                    items = metricItems,
                    compactLayout = compactLayout
                )

                if (!isConnected) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = stringResource(Res.string.no_device_available),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.select_device_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else if (compactLayout) {
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
                            latency = deviceInfo?.latency.orDash(),
                            connectionSpeed = deviceInfo?.connectionSpeed.orDash(),
                            relativeUpdated = relativeUpdated,
                            connectedDuration = connectedDuration,
                            isConnected = isConnected
                        )
                        QuickActionsPanel(
                            onScreenshot = onScreenshot,
                            onInstallApk = onInstallApk,
                            onMirrorDevice = onMirrorDevice,
                            onOpenShell = onOpenShell
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        DeviceInfoPanel(
                            modifier = Modifier.weight(1.45f),
                            deviceInfo = deviceInfo
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ConnectionInfoPanel(
                                connectionType = connectionType,
                                connectionStatus = connectionStatus,
                                ipAddress = deviceInfo?.ipAddress.orDash(),
                                port = extractPort(selectedDevice, deviceInfo?.ipAddress),
                                latency = deviceInfo?.latency.orDash(),
                                connectionSpeed = deviceInfo?.connectionSpeed.orDash(),
                                relativeUpdated = relativeUpdated,
                                connectedDuration = connectedDuration,
                                isConnected = isConnected
                            )

                            QuickActionsPanel(
                                onScreenshot = onScreenshot,
                                onInstallApk = onInstallApk,
                                onMirrorDevice = onMirrorDevice,
                                onOpenShell = onOpenShell
                            )
                        }
                    }
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
private fun DeviceControlPanel(
    compactLayout: Boolean,
    deviceName: String,
    androidVersion: String,
    connectionInfo: String,
    batteryInfo: String,
    isConnected: Boolean,
    isLoading: Boolean,
    devices: List<String>,
    deviceDisplayNames: Map<String, String>,
    selectedDevice: String?,
    selectedDeviceLabel: String,
    selectedDeviceAddress: String,
    selectedDeviceStatus: String,
    selectorExpanded: Boolean,
    onSelectorExpandedChange: (Boolean) -> Unit,
    onDeviceSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        borderStroke = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (compactLayout) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DeviceHeadline(
                        deviceName = deviceName,
                        androidVersion = androidVersion,
                        connectionInfo = connectionInfo,
                        batteryInfo = batteryInfo,
                        isConnected = isConnected
                    )
                    DeviceControlActions(
                        compactLayout = true,
                        isLoading = isLoading,
                        isConnected = isConnected,
                        onRefresh = onRefresh,
                        onDisconnect = onDisconnect
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    DeviceHeadline(
                        modifier = Modifier.weight(1f),
                        deviceName = deviceName,
                        androidVersion = androidVersion,
                        connectionInfo = connectionInfo,
                        batteryInfo = batteryInfo,
                        isConnected = isConnected
                    )
                    DeviceControlActions(
                        compactLayout = false,
                        isLoading = isLoading,
                        isConnected = isConnected,
                        onRefresh = onRefresh,
                        onDisconnect = onDisconnect
                    )
                }
            }

            DeviceSelectorCard(
                devices = devices,
                deviceDisplayNames = deviceDisplayNames,
                selectedDevice = selectedDevice,
                selectedDeviceLabel = selectedDeviceLabel,
                selectedDeviceAddress = selectedDeviceAddress,
                selectedDeviceStatus = selectedDeviceStatus,
                isConnected = isConnected,
                expanded = selectorExpanded,
                onExpandedChange = onSelectorExpandedChange,
                onDeviceSelected = onDeviceSelected
            )
        }
    }
}

@Composable
private fun DeviceControlActions(
    compactLayout: Boolean,
    isLoading: Boolean,
    isConnected: Boolean,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlineActionButton(
            modifier = if (compactLayout) Modifier.weight(1f) else Modifier,
            text = stringResource(Res.string.refresh),
            icon = Icons.Default.Refresh,
            tint = MaterialTheme.colorScheme.primary,
            enabled = !isLoading,
            onClick = onRefresh
        )
        OutlineActionButton(
            modifier = if (compactLayout) Modifier.weight(1f) else Modifier,
            text = stringResource(Res.string.disconnect),
            icon = Icons.Default.Close,
            tint = MaterialTheme.colorScheme.error,
            enabled = isConnected && !isLoading,
            onClick = onDisconnect
        )
    }
}

@Composable
private fun DeviceHeadline(
    deviceName: String,
    androidVersion: String,
    connectionInfo: String,
    batteryInfo: String,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = deviceName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            StatusBadge(
                text = stringResource(if (isConnected) Res.string.connected else Res.string.disconnected),
                active = isConnected
            )
        }
        Text(
            text = androidVersion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$connectionInfo · ${stringResource(Res.string.battery_level)} $batteryInfo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSelectorCard(
    devices: List<String>,
    deviceDisplayNames: Map<String, String>,
    selectedDevice: String?,
    selectedDeviceLabel: String,
    selectedDeviceAddress: String,
    selectedDeviceStatus: String,
    isConnected: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDeviceSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.select_device),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(
                        enabled = devices.isNotEmpty(),
                        onClick = { onExpandedChange(!expanded) }
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = selectedDeviceLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selectedDeviceAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selectedDeviceStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (isConnected) Color(0xFF2DBE60) else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(999.dp)
                            )
                    )
                }
            }

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
        val headlineModel = deviceInfo.deviceModel.orDash()
        val headlineSystem = listOf(
            formatAndroidVersionWithApi(deviceInfo.androidVersion, deviceInfo.sdkVersion),
            deviceInfo.romVersion.orDash(),
            deviceInfo.screenResolution.orDash()
        ).joinToString(" · ")

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = headlineModel,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = headlineSystem,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(18.dp))

        DeviceInfoRow(
            left = DeviceInfoEntry(
                label = stringResource(Res.string.manufacturer),
                value = deviceInfo.manufacturer.orDash()
            ),
            right = DeviceInfoEntry(
                label = stringResource(Res.string.kernel_version),
                value = deviceInfo.kernelVersion.orDash()
            ),
            cellModifier = cellModifier
        )
        Spacer(modifier = Modifier.height(16.dp))

        DeviceInfoRow(
            left = DeviceInfoEntry(
                label = stringResource(Res.string.font_scale),
                value = deviceInfo.fontScale.orDash()
            ),
            right = DeviceInfoEntry(
                label = stringResource(Res.string.mac_address),
                value = deviceInfo.macAddress.orDash()
            ),
            cellModifier = cellModifier
        )
        Spacer(modifier = Modifier.height(16.dp))

        DeviceInfoRow(
            left = DeviceInfoEntry(
                label = stringResource(Res.string.android_version),
                value = formatAndroidVersionWithApi(deviceInfo.androidVersion, deviceInfo.sdkVersion)
            ),
            right = DeviceInfoEntry(
                label = stringResource(Res.string.device_model),
                value = deviceInfo.deviceModel.orDash()
            ),
            cellModifier = cellModifier
        )
        Spacer(modifier = Modifier.height(16.dp))

        DeviceInfoRow(
            left = DeviceInfoEntry(
                label = stringResource(Res.string.rom_version),
                value = deviceInfo.romVersion.orDash()
            ),
            right = DeviceInfoEntry(
                label = stringResource(Res.string.screen_resolution),
                value = deviceInfo.screenResolution.orDash()
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
    latency: String,
    connectionSpeed: String,
    relativeUpdated: String,
    connectedDuration: String,
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
            LabeledValueRow(
                label = stringResource(Res.string.latency),
                value = latency
            )
            LabeledValueRow(
                label = stringResource(Res.string.transfer_speed),
                value = connectionSpeed
            )
            LabeledValueRow(
                label = stringResource(Res.string.connected_for),
                value = connectedDuration
            )
            LabeledValueRow(
                label = stringResource(Res.string.last_refresh),
                value = stringResource(Res.string.updated_ago, relativeUpdated)
            )
        }
    }
}

@Composable
private fun QuickActionsPanel(
    onScreenshot: () -> Unit = {},
    onInstallApk: () -> Unit = {},
    onMirrorDevice: () -> Unit = {},
    onOpenShell: () -> Unit = {}
) {
    DashboardPanel(
        title = stringResource(Res.string.quick_actions),
        icon = Icons.Default.CheckCircle
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.key_screenshot_short),
                icon = Icons.Default.PhotoCamera,
                accentColor = Color(0xFF3B82F6),
                onClick = onScreenshot
            )
            QuickActionCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.install_apk_short),
                icon = Icons.Default.Download,
                accentColor = Color(0xFF4E74C9),
                onClick = onInstallApk
            )
            QuickActionCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.device_mirror),
                icon = Icons.AutoMirrored.Filled.ScreenShare,
                accentColor = Color(0xFF6E7B8B),
                onClick = onMirrorDevice
            )
            QuickActionCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.open_shell),
                icon = Icons.Default.DeveloperMode,
                accentColor = Color(0xFF3E7C8F),
                onClick = onOpenShell
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
            value = left.value
        )
        DeviceInfoCell(
            modifier = cellModifier,
            label = right.label,
            value = right.value
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
    val value: String
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

private fun buildInlineMetricValue(primary: String, suffix: String): String {
    if (suffix.isBlank()) return primary
    if (primary.isBlank() || primary == "--") return suffix
    return "$primary $suffix"
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

private fun formatPrimaryDeviceName(
    modelFromInfo: String?,
    modelFromMap: String?,
    deviceId: String
): String {
    val model = modelFromInfo?.takeIf { it.isNotBlank() }
        ?: modelFromMap?.takeIf { it.isNotBlank() }
    return model ?: deviceId
}

private fun formatConnectedDuration(connectedSince: LocalDateTime?): String {
    if (connectedSince == null) return "--"
    val duration = Duration.between(connectedSince, LocalDateTime.now()).coerceAtLeast(Duration.ZERO)
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatRelativeRefresh(rawTime: String): String {
    val parsed = runCatching {
        LocalDateTime.parse(rawTime, REFRESH_TIME_FORMATTER)
    }.getOrNull() ?: return "--"
    val duration = Duration.between(parsed, LocalDateTime.now()).coerceAtLeast(Duration.ZERO)
    val seconds = duration.seconds
    return when {
        seconds < 10 -> "0s"
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m"
        else -> "${seconds / 3600}h"
    }
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

private val REFRESH_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
