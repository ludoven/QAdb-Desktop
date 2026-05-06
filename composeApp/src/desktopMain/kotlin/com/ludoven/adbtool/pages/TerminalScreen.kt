package com.ludoven.adbtool.pages

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.execute
import adbtool_desktop.composeapp.generated.resources.no_device
import adbtool_desktop.composeapp.generated.resources.terminal
import adbtool_desktop.composeapp.generated.resources.terminal_clear_output
import adbtool_desktop.composeapp.generated.resources.terminal_command_preview
import adbtool_desktop.composeapp.generated.resources.terminal_copy_output
import adbtool_desktop.composeapp.generated.resources.terminal_input_hint
import adbtool_desktop.composeapp.generated.resources.terminal_no_output
import adbtool_desktop.composeapp.generated.resources.terminal_quick_commands
import adbtool_desktop.composeapp.generated.resources.terminal_recent_commands
import adbtool_desktop.composeapp.generated.resources.terminal_tip_text
import adbtool_desktop.composeapp.generated.resources.terminal_tips
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.viewmodel.TerminalLogEntry
import com.ludoven.adbtool.viewmodel.TerminalViewModel
import com.ludoven.adbtool.widget.GlassCard
import org.jetbrains.compose.resources.stringResource

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    selectedDevice: String?,
    deviceDisplayNames: Map<String, String>
) {
    val clipboard = LocalClipboardManager.current
    val commandInput by viewModel.commandInput.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val history by viewModel.history.collectAsState()
    val outputState = rememberLazyListState()
    val sideScrollState = rememberScrollState()

    val quickCommands = remember {
        listOf(
            "adb devices",
            "adb shell",
            "logcat -v time",
            "screencap -p /sdcard/",
            "pm list packages",
            "reboot"
        )
    }

    val selectedDeviceText = selectedDevice?.let { deviceId ->
        val model = deviceDisplayNames[deviceId].orEmpty().trim()
        if (model.isBlank()) deviceId else "$model ($deviceId)"
    } ?: stringResource(Res.string.no_device)

    val mergedOutput = remember(logs) { buildOutputSnapshot(logs) }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) outputState.animateScrollToItem(logs.lastIndex)
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TerminalOutputPane(
                logs = logs,
                listState = outputState,
                modifier = Modifier.weight(1f)
            )

            TerminalInputBar(
                selectedDeviceText = selectedDeviceText,
                commandInput = commandInput,
                onCommandChange = viewModel::updateCommandInput,
                onExecute = { viewModel.executeCommand(selectedDevice) },
                enabled = !isExecuting && !selectedDevice.isNullOrBlank()
            )

            Text(
                text = stringResource(Res.string.terminal_tip_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(320.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 14.dp)
                    .verticalScroll(sideScrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TerminalActionsCard(
                    onClear = viewModel::clearLogs,
                    onCopy = { clipboard.setText(AnnotatedString(mergedOutput)) }
                )

                TerminalQuickCommandsCard(
                    commands = quickCommands,
                    onApply = viewModel::updateCommandInput
                )

                TerminalRecentCommandsCard(
                    history = history,
                    onApply = viewModel::applyHistoryCommand
                )

                TerminalPreviewCard(commandInput = commandInput)
                TerminalTipsCard()
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(sideScrollState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun TerminalOutputPane(
    logs: List<TerminalLogEntry>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0B1220), Color(0xFF132A4D), Color(0xFF0A111D))
                )
            )
            .border(
                width = 1.dp,
                color = Color(0xFF1F2A44),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        if (logs.isEmpty()) {
            Text(
                text = stringResource(Res.string.terminal_no_output),
                color = Color(0xFF9FB5D6),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            SelectionContainer {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(logs) { entry ->
                        TerminalLogBlock(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalLogBlock(entry: TerminalLogEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = "[${entry.timestamp}] qadb@device ~$ ${entry.command}",
            color = Color(0xFF47D7FF),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = entry.output,
            color = if (entry.isError) Color(0xFFFFB4B4) else Color(0xFFE7EEF9),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TerminalInputBar(
    selectedDeviceText: String,
    commandInput: String,
    onCommandChange: (String) -> Unit,
    onExecute: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF9EB8FF), RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "qadb@${selectedDeviceText.take(24)} ~$",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF16A34A),
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(170.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = commandInput,
                onValueChange = onCommandChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(stringResource(Res.string.terminal_input_hint)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }

        Button(
            onClick = onExecute,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            modifier = Modifier.height(52.dp).width(110.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(Res.string.execute))
        }
    }
}

@Composable
private fun TerminalActionsCard(
    onClear: () -> Unit,
    onCopy: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TerminalActionButton(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.terminal_clear_output),
                icon = Icons.Default.DeleteSweep,
                onClick = onClear
            )
            TerminalActionButton(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.terminal_copy_output),
                icon = Icons.Default.ContentCopy,
                onClick = onCopy
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onCopy)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TerminalActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TerminalQuickCommandsCard(
    commands: List<String>,
    onApply: (String) -> Unit
) {
    TerminalInfoCard(title = stringResource(Res.string.terminal_quick_commands)) {
        commands.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { command ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onApply(command) }
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = command,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TerminalRecentCommandsCard(
    history: List<String>,
    onApply: (String) -> Unit
) {
    TerminalInfoCard(title = stringResource(Res.string.terminal_recent_commands)) {
        if (history.isEmpty()) {
            Text(
                text = stringResource(Res.string.terminal_no_output),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@TerminalInfoCard
        }

        history.take(5).forEachIndexed { index, command ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "13:${45 - index}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp)
                )
                Text(
                    text = command,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onApply(command) }
                )
            }
            if (index < history.take(5).lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun TerminalPreviewCard(commandInput: String) {
    TerminalInfoCard(title = stringResource(Res.string.terminal_command_preview)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                .padding(10.dp)
        ) {
            Text(
                text = if (commandInput.isBlank()) "adb shell" else commandInput,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun TerminalTipsCard() {
    TerminalInfoCard(title = stringResource(Res.string.terminal_tips)) {
        Text(
            text = stringResource(Res.string.terminal_tip_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TerminalInfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            content()
        }
    }
}

private fun buildOutputSnapshot(logs: List<TerminalLogEntry>): String {
    if (logs.isEmpty()) return ""
    return logs.joinToString("\n\n") { entry ->
        "[${entry.timestamp}] $ ${entry.command}\n${entry.output}"
    }
}
