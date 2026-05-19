package com.ludoven.adbtool.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.domain.terminal.TerminalMode
import com.ludoven.adbtool.ui.mac.*
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.viewmodel.TerminalViewModel
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                text = l10n("到底部", "Bottom"),
                icon = Icons.Default.ArrowDownward,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(session.lines.size)
                    }
                }
            )
        }

        TerminalOutput(
            lines = session.lines,
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
