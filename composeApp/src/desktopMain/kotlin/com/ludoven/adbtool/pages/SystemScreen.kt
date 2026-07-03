package com.ludoven.adbtool.pages

import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.ui.mac.*
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.viewmodel.SystemViewModel
import com.ludoven.adbtool.widget.DashboardPanel
import com.ludoven.adbtool.widget.EmptyStatePanel
import com.ludoven.adbtool.widget.LabeledValueRow
import com.ludoven.adbtool.widget.OutlineActionButton
import com.ludoven.adbtool.widget.PageHeader

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.battery_status
import adbtool_desktop.composeapp.generated.resources.cpu_info
import adbtool_desktop.composeapp.generated.resources.no_device_available
import adbtool_desktop.composeapp.generated.resources.refresh
import adbtool_desktop.composeapp.generated.resources.screen_resolution
import adbtool_desktop.composeapp.generated.resources.system_page

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

@Composable
fun SystemScreen(
    viewModel: SystemViewModel,
    selectedDevice: String?
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val filteredProps by viewModel.filteredProps.collectAsState()
    val systemProps by viewModel.systemProps.collectAsState()
    val batteryInfo by viewModel.batteryInfo.collectAsState()
    val cpuInfo by viewModel.cpuInfo.collectAsState()
    val screenInfo by viewModel.screenInfo.collectAsState()
    val propSearchText by viewModel.propSearchText.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val dialogMessage by viewModel.dialogMessage.collectAsState()

    var showRebootConfirm by remember { mutableStateOf(false) }
    var rebootMode by remember { mutableStateOf(RebootMode.NORMAL) }
    var showScreenChangeConfirm by remember { mutableStateOf(false) }
    var pendingScreenAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingScreenDescription by remember { mutableStateOf("") }

    var customResolution by remember { mutableStateOf("") }
    var customDensity by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    LaunchedEffect(selectedDevice) {
        viewModel.setDevice(selectedDevice)
        val device = selectedDevice?.takeIf { it.isNotBlank() }
        if (device != null) {
            viewModel.loadSystemInfo(device)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.cancelActiveLoad()
        }
    }

    // TipDialog
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

    // Reboot confirmation dialog
    if (showRebootConfirm) {
        val title = when (rebootMode) {
            RebootMode.NORMAL -> l10n("确认重启", "Confirm Restart")
            RebootMode.RECOVERY -> l10n("确认重启到Recovery", "Confirm Reboot to Recovery")
            RebootMode.FASTBOOT -> l10n("确认重启到Fastboot", "Confirm Reboot to Fastboot")
        }
        val description = when (rebootMode) {
            RebootMode.NORMAL -> l10n(
                "即将重启连接的设备，未保存的数据可能丢失。是否继续？",
                "The connected device will restart. Unsaved data may be lost. Continue?"
            )
            RebootMode.RECOVERY -> l10n(
                "即将重启设备进入Recovery模式，未保存的数据可能丢失。是否继续？",
                "The device will reboot into Recovery mode. Unsaved data may be lost. Continue?"
            )
            RebootMode.FASTBOOT -> l10n(
                "即将重启设备进入Fastboot模式，未保存的数据可能丢失。是否继续？",
                "The device will reboot into Fastboot mode. Unsaved data may be lost. Continue?"
            )
        }
        AlertDialog(
            onDismissRequest = { showRebootConfirm = false },
            title = { Text(title) },
            text = { Text(description) },
            confirmButton = {
                TextButton(onClick = {
                    val device = selectedDevice ?: return@TextButton
                    showRebootConfirm = false
                    when (rebootMode) {
                        RebootMode.NORMAL -> viewModel.rebootNormal(device)
                        RebootMode.RECOVERY -> viewModel.rebootRecovery(device)
                        RebootMode.FASTBOOT -> viewModel.rebootBootloader(device)
                    }
                }) {
                    Text(l10n("确认", "Confirm"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootConfirm = false }) {
                    Text(l10n("取消", "Cancel"))
                }
            }
        )
    }

    // Screen change confirmation dialog
    if (showScreenChangeConfirm) {
        AlertDialog(
            onDismissRequest = { showScreenChangeConfirm = false },
            title = { Text(l10n("确认修改屏幕参数", "Confirm Screen Parameter Change")) },
            text = { Text(pendingScreenDescription) },
            confirmButton = {
                TextButton(onClick = {
                    pendingScreenAction?.invoke()
                    showScreenChangeConfirm = false
                    pendingScreenAction = null
                }) {
                    Text(l10n("确认", "Confirm"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showScreenChangeConfirm = false
                    pendingScreenAction = null
                }) {
                    Text(l10n("取消", "Cancel"))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PageHeader(
            title = stringResource(Res.string.system_page),
            subtitle = l10n(
                "查看设备系统信息、管理重启与屏幕参数",
                "View device system info, manage reboot and screen parameters"
            )
        ) {
            IconButton(
                onClick = {
                    if (!selectedDevice.isNullOrBlank()) {
                        viewModel.loadSystemInfo(selectedDevice, forceRefresh = true)
                    }
                },
                enabled = !selectedDevice.isNullOrBlank() && !isLoading
            ) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.refresh))
            }
        }

        // No device state
        if (selectedDevice.isNullOrBlank()) {
            EmptyStatePanel(
                title = stringResource(Res.string.no_device_available),
                description = l10n("选择设备后即可读取系统属性、电池、CPU 和屏幕信息。", "Select a device to read properties, battery, CPU, and screen info."),
                icon = Icons.Default.PhoneAndroid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
            return@Column
        }

        // Loading state
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        // Section 1: Reboot Modes
        RebootSection(
            onRebootClick = { mode ->
                rebootMode = mode
                showRebootConfirm = true
            }
        )

        // Section 2: System Properties
        SystemPropsSection(
            propSearchText = propSearchText,
            onSearchChange = { viewModel.updatePropSearch(it) },
            filteredProps = filteredProps,
            totalPropsCount = systemProps.size
        )

        // Section 3: Battery Details
        BatterySection(batteryInfo = batteryInfo)

        // Section 4: CPU Info
        CpuSection(cpuInfo = cpuInfo)

        // Section 5: Screen Parameters
        ScreenParametersSection(
            screenInfo = screenInfo,
            customResolution = customResolution,
            onCustomResolutionChange = { customResolution = it },
            customDensity = customDensity,
            onCustomDensityChange = { customDensity = it },
            onApplyResolution = {
                val device = selectedDevice ?: return@ScreenParametersSection
                pendingScreenAction = {
                    viewModel.setScreenSize(device, customResolution)
                    customResolution = ""
                }
                pendingScreenDescription = l10n(
                    "即将设置屏幕分辨率为 $customResolution，是否继续？",
                    "Screen resolution will be set to $customResolution. Continue?"
                )
                showScreenChangeConfirm = true
            },
            onApplyDensity = {
                val device = selectedDevice ?: return@ScreenParametersSection
                pendingScreenAction = {
                    viewModel.setScreenDensity(device, customDensity)
                    customDensity = ""
                }
                pendingScreenDescription = l10n(
                    "即将设置屏幕密度为 ${customDensity}dpi，是否继续？",
                    "Screen density will be set to ${customDensity}dpi. Continue?"
                )
                showScreenChangeConfirm = true
            },
            onResetSize = {
                val device = selectedDevice ?: return@ScreenParametersSection
                pendingScreenAction = { viewModel.resetScreenSize(device) }
                pendingScreenDescription = l10n(
                    "即将重置屏幕分辨率为默认值，是否继续？",
                    "Screen resolution will be reset to default. Continue?"
                )
                showScreenChangeConfirm = true
            },
            onResetDensity = {
                val device = selectedDevice ?: return@ScreenParametersSection
                pendingScreenAction = { viewModel.resetScreenDensity(device) }
                pendingScreenDescription = l10n(
                    "即将重置屏幕密度为默认值，是否继续？",
                    "Screen density will be reset to default. Continue?"
                )
                showScreenChangeConfirm = true
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ============================================================
// Section composables
// ============================================================

@Composable
private fun RebootSection(
    onRebootClick: (RebootMode) -> Unit
) {
    DashboardPanel(
        title = l10n("设备重启", "Device Reboot"),
        icon = Icons.Default.PhoneAndroid
    ) {
        Text(
            text = l10n(
                "选择重启模式，操作前将弹出确认对话框",
                "Select reboot mode. A confirmation dialog will appear before execution."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlineActionButton(
                modifier = Modifier.weight(1f),
                text = l10n("重启", "Restart"),
                icon = Icons.Default.Refresh,
                tint = MaterialTheme.colorScheme.primary,
                onClick = { onRebootClick(RebootMode.NORMAL) }
            )
            OutlineActionButton(
                modifier = Modifier.weight(1f),
                text = l10n("Recovery", "Recovery"),
                icon = Icons.Default.RestartAlt,
                tint = MaterialTheme.colorScheme.secondary,
                onClick = { onRebootClick(RebootMode.RECOVERY) }
            )
            OutlineActionButton(
                modifier = Modifier.weight(1f),
                text = l10n("Fastboot", "Fastboot"),
                icon = Icons.Default.SystemUpdateAlt,
                tint = MaterialTheme.colorScheme.tertiary,
                onClick = { onRebootClick(RebootMode.FASTBOOT) }
            )
        }
    }
}

@Composable
private fun SystemPropsSection(
    propSearchText: String,
    onSearchChange: (String) -> Unit,
    filteredProps: Map<String, String>,
    totalPropsCount: Int
) {
    val propsListState = rememberLazyListState()

    DashboardPanel(
        title = l10n("系统属性", "System Properties"),
        icon = Icons.Default.Memory
    ) {
        OutlinedTextField(
            value = propSearchText,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(l10n("搜索属性...", "Search properties...")) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredProps.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "/ $totalPropsCount ${l10n("条属性", "properties")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SelectionContainer {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (filteredProps.isEmpty()) {
                    EmptyStatePanel(
                        title = l10n("暂无属性数据", "No property data available"),
                        description = l10n("刷新设备信息后仍为空时，请确认 ADB 授权和设备状态。", "If refresh still returns nothing, check ADB authorization and device state."),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 12.dp),
                        state = propsListState,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filteredProps.entries.toList(), key = { it.key }) { (key, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = key,
                                    modifier = Modifier.width(280.dp),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(propsListState),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(end = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BatterySection(batteryInfo: Map<String, String>) {
    DashboardPanel(
        title = stringResource(Res.string.battery_status),
        icon = Icons.Default.BatteryChargingFull
    ) {
        if (batteryInfo.isEmpty()) {
            Text(
                text = l10n("暂无电池数据", "No battery data available"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@DashboardPanel
        }

        val statusMap = mapOf(
            "Unknown" to l10n("未知", "Unknown"),
            "Charging" to l10n("充电中", "Charging"),
            "Discharging" to l10n("放电中", "Discharging"),
            "Not charging" to l10n("未充电", "Not charging"),
            "Full" to l10n("已充满", "Full")
        )

        val healthMap = mapOf(
            "Unknown" to l10n("未知", "Unknown"),
            "Good" to l10n("良好", "Good"),
            "Overheat" to l10n("过热", "Overheat"),
            "Dead" to l10n("损坏", "Dead"),
            "Over voltage" to l10n("过压", "Over voltage"),
            "Cold" to l10n("低温", "Cold")
        )

        fun formatTemperature(raw: String): String {
            val intValue = raw.toIntOrNull()
            return if (intValue != null) "${intValue / 10.0}°C" else raw
        }

        fun formatVoltage(raw: String): String {
            val intValue = raw.toIntOrNull()
            return if (intValue != null) "${intValue}mV" else raw
        }

        fun localizedStatus(raw: String): String = statusMap[raw] ?: raw

        fun localizedHealth(raw: String): String = healthMap[raw] ?: raw

        fun formatPowered(batteryProps: Map<String, String>): String {
            val ac = batteryProps["AC powered"]?.toBooleanStrictOrNull() ?: false
            val usb = batteryProps["USB powered"]?.toBooleanStrictOrNull() ?: false
            val wireless = batteryProps["Wireless powered"]?.toBooleanStrictOrNull() ?: false
            val parts = mutableListOf<String>()
            if (ac) parts.add("AC")
            if (usb) parts.add("USB")
            if (wireless) parts.add(l10n("无线", "Wireless"))
            return if (parts.isEmpty()) l10n("否", "No") else parts.joinToString(", ")
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LabeledValueRow(
                        label = l10n("状态", "Status"),
                        value = localizedStatus(batteryInfo["status"].orEmpty())
                    )
                    LabeledValueRow(
                        label = l10n("健康度", "Health"),
                        value = localizedHealth(batteryInfo["health"].orEmpty())
                    )
                    LabeledValueRow(
                        label = l10n("电量", "Level"),
                        value = batteryInfo["level"]?.let { "${it}%" }.orEmpty()
                    )
                    LabeledValueRow(
                        label = l10n("温度", "Temperature"),
                        value = formatTemperature(batteryInfo["temperature"].orEmpty())
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LabeledValueRow(
                        label = l10n("电压", "Voltage"),
                        value = formatVoltage(batteryInfo["voltage"].orEmpty())
                    )
                    LabeledValueRow(
                        label = l10n("电池技术", "Technology"),
                        value = batteryInfo["technology"].orEmpty()
                    )
                    LabeledValueRow(
                        label = l10n("充电方式", "Powered"),
                        value = formatPowered(batteryInfo)
                    )
                    LabeledValueRow(
                        label = l10n("最大充电电流", "Max Charging Current"),
                        value = batteryInfo["Max charging current"]?.let { "${it}mA" }.orEmpty()
                    )
                }
            }
        }
    }
}

@Composable
private fun CpuSection(cpuInfo: List<Pair<String, String>>) {
    DashboardPanel(
        title = stringResource(Res.string.cpu_info),
        icon = Icons.Default.Memory
    ) {
        if (cpuInfo.isEmpty()) {
            Text(
                text = l10n("暂无CPU数据", "No CPU data available"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@DashboardPanel
        }

        val processorCount = cpuInfo.count { it.first.equals("processor", ignoreCase = true) }
        val interestingKeys = setOf(
            "processor", "model name", "architecture",
            "cpu implementer", "cpu part", "features"
        )

        Text(
            text = l10n("处理器数量: $processorCount", "Processor count: $processorCount"),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for ((key, value) in cpuInfo) {
                    if (key.isBlank() && value.isBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        continue
                    }
                    if (key.isNotBlank() && key.lowercase() !in interestingKeys) {
                        continue
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = key,
                            modifier = Modifier.width(160.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (key.equals("features", ignoreCase = true)) {
                                value.lines().firstOrNull()?.take(80)?.let {
                                    if (value.length > 80) "$it..." else it
                                } ?: value
                            } else {
                                value
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenParametersSection(
    screenInfo: Map<String, String>,
    customResolution: String,
    onCustomResolutionChange: (String) -> Unit,
    customDensity: String,
    onCustomDensityChange: (String) -> Unit,
    onApplyResolution: () -> Unit,
    onApplyDensity: () -> Unit,
    onResetSize: () -> Unit,
    onResetDensity: () -> Unit
) {
    DashboardPanel(
        title = stringResource(Res.string.screen_resolution),
        icon = Icons.Default.PhoneAndroid
    ) {
        if (screenInfo.isEmpty()) {
            Text(
                text = l10n("暂无屏幕数据", "No screen data available"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@DashboardPanel
        }

        // Current screen info
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            screenInfo["Physical size"]?.takeIf { it.isNotBlank() }?.let { size ->
                LabeledValueRow(
                    label = l10n("物理分辨率", "Physical Resolution"),
                    value = size
                )
            }
            screenInfo["Override size"]?.takeIf { it.isNotBlank() }?.let { size ->
                LabeledValueRow(
                    label = l10n("当前分辨率", "Current Resolution"),
                    value = size,
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
            screenInfo["Physical density"]?.takeIf { it.isNotBlank() }?.let { density ->
                LabeledValueRow(
                    label = l10n("物理密度", "Physical Density"),
                    value = density
                )
            }
            screenInfo["Override density"]?.takeIf { it.isNotBlank() }?.let { density ->
                LabeledValueRow(
                    label = l10n("当前密度", "Current Density"),
                    value = density,
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom resolution
        Text(
            text = l10n("自定义分辨率", "Custom Resolution"),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customResolution,
                onValueChange = onCustomResolutionChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(l10n("例如: 1080x1920", "e.g.: 1080x1920")) }
            )
            OutlineActionButton(
                text = l10n("应用", "Apply"),
                icon = Icons.Default.Refresh,
                tint = MaterialTheme.colorScheme.primary,
                onClick = onApplyResolution,
                enabled = customResolution.isNotBlank()
            )
            OutlineActionButton(
                text = l10n("重置", "Reset"),
                icon = Icons.Default.Refresh,
                tint = MaterialTheme.colorScheme.error,
                onClick = onResetSize
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom density
        Text(
            text = l10n("自定义密度", "Custom Density"),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customDensity,
                onValueChange = onCustomDensityChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(l10n("例如: 480", "e.g.: 480")) }
            )
            OutlineActionButton(
                text = l10n("应用", "Apply"),
                icon = Icons.Default.Refresh,
                tint = MaterialTheme.colorScheme.primary,
                onClick = onApplyDensity,
                enabled = customDensity.isNotBlank()
            )
            OutlineActionButton(
                text = l10n("重置", "Reset"),
                icon = Icons.Default.Refresh,
                tint = MaterialTheme.colorScheme.error,
                onClick = onResetDensity
            )
        }
    }
}

// ============================================================
// Internal enums
// ============================================================

private enum class RebootMode { NORMAL, RECOVERY, FASTBOOT }
