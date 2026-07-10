package com.ludoven.adbtool.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StopCircle
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.domain.terminal.TerminalMode
import com.ludoven.adbtool.ui.mac.*
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.util.copyPlainTextToClipboard
import com.ludoven.adbtool.viewmodel.TerminalViewModel
import com.ludoven.adbtool.widget.DesktopToolbar
import com.ludoven.adbtool.widget.InlineStatusBanner
import com.ludoven.adbtool.widget.InlineStatusTone
import com.ludoven.adbtool.widget.PageHeader
import com.ludoven.adbtool.widget.StatusBadge
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    selectedDevice: String?,
    devices: List<String>,
    deviceDisplayNames: Map<String, String>,
    onSelectDevice: (String) -> Unit,
    onRefreshDevices: () -> Unit
) {
    val session by viewModel.session.collectAsState()
    val commandInput by viewModel.commandInput.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var deviceMenuExpanded by remember { mutableStateOf(false) }
    val filteredLines = remember(session.lines, session.searchQuery) {
        val query = session.searchQuery.trim()
        if (query.isEmpty()) session.lines else session.lines.filter { it.text.contains(query, ignoreCase = true) }
    }

    LaunchedEffect(devices, selectedDevice, deviceDisplayNames) {
        viewModel.syncDeviceState(
            devices = devices,
            selectedDevice = selectedDevice,
            deviceDisplayNames = deviceDisplayNames
        )
    }

    LaunchedEffect(session.deviceId, selectedDevice, session.devices) {
        val device = session.deviceId
        if (!device.isNullOrBlank() && device != selectedDevice && device in session.devices) {
            onSelectDevice(device)
        }
    }

    LaunchedEffect(filteredLines.size, session.followOutput) {
        if (session.followOutput && filteredLines.isNotEmpty()) {
            listState.scrollToItem(filteredLines.size)
        }
    }

    val prompt = when (session.mode) {
        TerminalMode.QADB -> "qadb:${formatDeviceLabel(session.deviceId, session.deviceDisplayNames)}>"
        TerminalMode.ADB_SHELL -> "device:${formatDeviceLabel(session.deviceId, session.deviceDisplayNames)}\$"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PageHeader(
            title = l10n("终端", "Terminal"),
            subtitle = l10n("单设备 ADB 会话 · Enter 执行 · ↑↓ 历史 · Cmd/Ctrl+L 清屏", "Single-device ADB session · Enter run · ↑↓ history · Cmd/Ctrl+L clear")
        )

        DesktopToolbar {
            Box {
                Surface(
                    onClick = { deviceMenuExpanded = true },
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.height(32.dp).padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatDeviceLabel(session.deviceId, session.deviceDisplayNames),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
                DropdownMenu(
                    expanded = deviceMenuExpanded,
                    onDismissRequest = { deviceMenuExpanded = false }
                ) {
                    session.devices.forEach { deviceId ->
                        DropdownMenuItem(
                            text = { Text(formatDeviceLabel(deviceId, session.deviceDisplayNames)) },
                            onClick = {
                                onSelectDevice(deviceId)
                                deviceMenuExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(l10n("刷新设备", "Refresh devices")) },
                        onClick = {
                            onRefreshDevices()
                            deviceMenuExpanded = false
                        }
                    )
                }
            }

            StatusBadge(
                text = if (session.deviceId.isNullOrBlank()) l10n("未选择设备", "No device") else l10n("已连接", "Connected"),
                tone = if (session.deviceId.isNullOrBlank()) InlineStatusTone.Warning else InlineStatusTone.Success
            )

            OutlinedTextField(
                value = session.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                singleLine = true,
                modifier = Modifier.width(240.dp).height(34.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                placeholder = { Text(l10n("搜索输出", "Search output"), style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )

            Spacer(modifier = Modifier.weight(1f))

            ToolbarActionButton(
                text = l10n("清屏", "Clear"),
                icon = Icons.Default.DeleteSweep,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = viewModel::clearLogs
            )
            ToolbarActionButton(
                text = l10n("终止输出", "Interrupt"),
                icon = Icons.Default.StopCircle,
                enabled = session.isRunning,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                onClick = viewModel::interruptCommand
            )
            ToolbarActionButton(
                text = if (session.followOutput) l10n("跟随中", "Following") else l10n("跟随输出", "Follow"),
                icon = Icons.Default.ArrowDownward,
                containerColor = if (session.followOutput) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (session.followOutput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                onClick = {
                    viewModel.setFollowOutput(!session.followOutput)
                    coroutineScope.launch {
                        if (!session.followOutput) listState.scrollToItem(filteredLines.size)
                    }
                }
            )

            ToolbarActionButton(
                text = l10n("复制全部", "Copy all"),
                icon = Icons.Default.ContentCopy,
                enabled = filteredLines.isNotEmpty(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = { copyPlainTextToClipboard(filteredLines.joinToString("\n") { it.text }) }
            )
        }

        if (session.deviceId.isNullOrBlank()) {
            InlineStatusBanner(
                text = l10n("当前没有选择设备。ADB shell 和设备相关命令需要先连接并选择设备。", "No device is selected. ADB shell and device commands need a connected selected device."),
                tone = InlineStatusTone.Warning
            )
        }

        TerminalOutput(
            lines = filteredLines,
            prompt = prompt,
            input = commandInput,
            isRunning = session.isRunning,
            onInputChange = viewModel::updateCommandInput,
            onSubmit = viewModel::executeCommand,
            onHistoryPrev = viewModel::applyPreviousHistoryCommand,
            onHistoryNext = viewModel::applyNextHistoryCommand,
            onClearOutput = viewModel::clearLogs,
            onInterrupt = viewModel::interruptCommand,
            listState = listState,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatDeviceLabel(deviceId: String?, names: Map<String, String>): String {
    if (deviceId.isNullOrBlank()) return "no-device"
    val name = names[deviceId]
    return if (name.isNullOrBlank()) deviceId else name
}

@Composable
private fun ToolbarActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            contentColor = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier
            .size(width = 108.dp, height = 34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}
