package com.ludoven.adbtool.pages

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.log_auto_scroll
import adbtool_desktop.composeapp.generated.resources.log_capture_paused
import adbtool_desktop.composeapp.generated.resources.log_capture_running
import adbtool_desktop.composeapp.generated.resources.log_clear
import adbtool_desktop.composeapp.generated.resources.log_clear_filter
import adbtool_desktop.composeapp.generated.resources.log_dismiss
import adbtool_desktop.composeapp.generated.resources.log_empty
import adbtool_desktop.composeapp.generated.resources.log_export
import adbtool_desktop.composeapp.generated.resources.log_filter_current_app
import adbtool_desktop.composeapp.generated.resources.log_filter_error
import adbtool_desktop.composeapp.generated.resources.log_filter_keyword
import adbtool_desktop.composeapp.generated.resources.log_filter_package
import adbtool_desktop.composeapp.generated.resources.log_filter_pid
import adbtool_desktop.composeapp.generated.resources.log_header_level
import adbtool_desktop.composeapp.generated.resources.log_header_message
import adbtool_desktop.composeapp.generated.resources.log_header_pid
import adbtool_desktop.composeapp.generated.resources.log_header_tid
import adbtool_desktop.composeapp.generated.resources.log_header_time
import adbtool_desktop.composeapp.generated.resources.log_keyword_chip_anr
import adbtool_desktop.composeapp.generated.resources.log_keyword_chip_crash
import adbtool_desktop.composeapp.generated.resources.log_keyword_chip_warning
import adbtool_desktop.composeapp.generated.resources.log_level_all
import adbtool_desktop.composeapp.generated.resources.log_more
import adbtool_desktop.composeapp.generated.resources.log_more_restart
import adbtool_desktop.composeapp.generated.resources.log_no_device
import adbtool_desktop.composeapp.generated.resources.log_pause
import adbtool_desktop.composeapp.generated.resources.log_resume
import adbtool_desktop.composeapp.generated.resources.log_subtitle
import adbtool_desktop.composeapp.generated.resources.log_title
import adbtool_desktop.composeapp.generated.resources.log_waiting
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.entity.LogEntry
import com.ludoven.adbtool.entity.LogFilter
import com.ludoven.adbtool.entity.LogLevel
import com.ludoven.adbtool.ui.mac.Button
import com.ludoven.adbtool.ui.mac.ButtonDefaults
import com.ludoven.adbtool.ui.mac.Card
import com.ludoven.adbtool.ui.mac.DropdownMenu
import com.ludoven.adbtool.ui.mac.DropdownMenuItem
import com.ludoven.adbtool.ui.mac.HorizontalDivider
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.IconButton
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.OutlinedButton
import com.ludoven.adbtool.ui.mac.Text
import com.ludoven.adbtool.ui.mac.bodyMedium
import com.ludoven.adbtool.ui.mac.bodySmall
import com.ludoven.adbtool.ui.mac.headlineMedium
import com.ludoven.adbtool.ui.mac.labelLarge
import com.ludoven.adbtool.ui.mac.labelSmall
import com.ludoven.adbtool.viewmodel.LogViewModel
import org.jetbrains.compose.resources.stringResource
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class QuickKeywordChip {
    CURRENT_APP,
    ERROR,
    WARNING,
    CRASH,
    ANR
}

@Composable
fun LogScreen(
    viewModel: LogViewModel,
    selectedDevice: String?
) {
    val logs by viewModel.logs.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val listState = rememberLazyListState()
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    var showLevelMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(true) }
    var selectedQuickChip by remember { mutableStateOf<QuickKeywordChip?>(null) }

    val filteredLogs = remember(logs, filter) { viewModel.getFilteredLogs() }
    val likelyCurrentPackage = remember(logs) { detectLikelyPackage(logs) }

    LaunchedEffect(selectedDevice) {
        viewModel.setSelectedDevice(selectedDevice)
    }

    LaunchedEffect(filteredLogs.size, isCapturing, autoScroll) {
        if (isCapturing && autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(Res.string.log_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(Res.string.log_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CaptureStatusBadge(isCapturing = isCapturing)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            if (isCapturing) {
                                viewModel.stopCapture()
                            } else if (selectedDevice != null) {
                                viewModel.startCapture(selectedDevice)
                            }
                        },
                        enabled = (selectedDevice != null || isCapturing) && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (isCapturing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.width(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCapturing) stringResource(Res.string.log_pause) else stringResource(Res.string.log_resume),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }

                    ToolbarButton(
                        text = stringResource(Res.string.log_clear),
                        icon = Icons.Default.DeleteSweep,
                        onClick = { viewModel.clearLogs() }
                    )

                    ToolbarButton(
                        text = stringResource(Res.string.log_export),
                        icon = Icons.Default.FileDownload,
                        onClick = {
                            val dialog = FileDialog(null as Frame?, "Export Logs", FileDialog.SAVE)
                            dialog.isVisible = true
                            val dir = dialog.directory
                            val filename = dialog.file
                            if (dir != null && filename != null) {
                                viewModel.exportLogs(File(dir, filename))
                            }
                        }
                    )

                    ToolbarButton(
                        text = stringResource(Res.string.log_auto_scroll),
                        icon = Icons.Default.AutoAwesomeMotion,
                        isActive = autoScroll,
                        onClick = { autoScroll = !autoScroll }
                    )

                    Box {
                        ToolbarButton(
                            text = stringResource(Res.string.log_more),
                            icon = Icons.Default.MoreHoriz,
                            onClick = { showMoreMenu = true }
                        )
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.log_more_restart)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                },
                                enabled = selectedDevice != null && !isLoading,
                                onClick = {
                                    if (selectedDevice != null) viewModel.restartCapture(selectedDevice)
                                    showMoreMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterInput(
                        value = filter.keyword,
                        onValueChange = {
                            viewModel.updateFilter(filter.copy(keyword = it))
                            selectedQuickChip = null
                        },
                        placeholder = stringResource(Res.string.log_filter_keyword),
                        leadingIcon = Icons.Default.Search,
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        FilterSelect(
                            text = filter.level?.displayName ?: stringResource(Res.string.log_level_all),
                            onClick = { showLevelMenu = true },
                            modifier = Modifier.width(118.dp)
                        )
                        DropdownMenu(
                            expanded = showLevelMenu,
                            onDismissRequest = { showLevelMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.log_level_all)) },
                                onClick = {
                                    viewModel.updateFilter(filter.copy(level = null))
                                    showLevelMenu = false
                                }
                            )
                            LogLevel.values().forEach { logLevel ->
                                DropdownMenuItem(
                                    text = { Text(logLevel.displayName) },
                                    onClick = {
                                        viewModel.updateFilter(filter.copy(level = logLevel))
                                        showLevelMenu = false
                                    }
                                )
                            }
                        }
                    }

                    FilterInput(
                        value = filter.packageName,
                        onValueChange = {
                            viewModel.updateFilter(filter.copy(packageName = it))
                            selectedQuickChip = null
                        },
                        placeholder = stringResource(Res.string.log_filter_package),
                        modifier = Modifier.width(190.dp)
                    )

                    FilterInput(
                        value = filter.pid,
                        onValueChange = {
                            viewModel.updateFilter(filter.copy(pid = it))
                            selectedQuickChip = null
                        },
                        placeholder = stringResource(Res.string.log_filter_pid),
                        modifier = Modifier.width(86.dp)
                    )

                    IconButton(
                        onClick = {
                            selectedQuickChip = null
                            viewModel.updateFilter(LogFilter())
                        },
                        enabled = filter != LogFilter(),
                        modifier = Modifier.width(34.dp)
                    ) {
                        Icon(
                            Icons.Default.FilterAltOff,
                            contentDescription = stringResource(Res.string.log_clear_filter),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickFilterChip(
                        text = stringResource(Res.string.log_filter_current_app),
                        active = selectedQuickChip == QuickKeywordChip.CURRENT_APP,
                        enabled = likelyCurrentPackage != null,
                        onClick = {
                            selectedQuickChip = QuickKeywordChip.CURRENT_APP
                            viewModel.updateFilter(filter.copy(packageName = likelyCurrentPackage ?: ""))
                        }
                    )
                    QuickFilterChip(
                        text = stringResource(Res.string.log_filter_error),
                        active = selectedQuickChip == QuickKeywordChip.ERROR,
                        onClick = {
                            selectedQuickChip = QuickKeywordChip.ERROR
                            viewModel.updateFilter(filter.copy(level = null, onlyErrors = true))
                        }
                    )
                    QuickFilterChip(
                        text = stringResource(Res.string.log_keyword_chip_warning),
                        active = selectedQuickChip == QuickKeywordChip.WARNING,
                        onClick = {
                            selectedQuickChip = QuickKeywordChip.WARNING
                            viewModel.updateFilter(filter.copy(level = LogLevel.WARN, onlyErrors = false))
                        }
                    )
                    QuickFilterChip(
                        text = stringResource(Res.string.log_keyword_chip_crash),
                        active = selectedQuickChip == QuickKeywordChip.CRASH,
                        onClick = {
                            selectedQuickChip = QuickKeywordChip.CRASH
                            viewModel.updateFilter(
                                filter.copy(
                                    keyword = "FATAL EXCEPTION|AndroidRuntime",
                                    isRegex = true,
                                    onlyErrors = true
                                )
                            )
                        }
                    )
                    QuickFilterChip(
                        text = stringResource(Res.string.log_keyword_chip_anr),
                        active = selectedQuickChip == QuickKeywordChip.ANR,
                        onClick = {
                            selectedQuickChip = QuickKeywordChip.ANR
                            viewModel.updateFilter(
                                filter.copy(
                                    keyword = "ANR|Application Not Responding",
                                    isRegex = true,
                                    onlyErrors = false
                                )
                            )
                        }
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LogTableHeader()
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))

                if (filteredLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isCapturing) stringResource(Res.string.log_waiting) else stringResource(Res.string.log_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SelectionContainer {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 10.dp),
                                state = listState
                            ) {
                                itemsIndexed(filteredLogs, key = { index, item -> "${item.timestamp}_${item.pid}_${index}" }) { _, entry ->
                                    LogTableRow(
                                        entry = entry,
                                        dateFormat = dateFormat
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(listState),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(end = 2.dp)
                                .padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        errorMessage?.let { error ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    IconButton(onClick = { viewModel.clearError() }, modifier = Modifier.width(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.log_dismiss))
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureStatusBadge(isCapturing: Boolean) {
    val background = if (isCapturing) Color(0xFFEAF4FF) else Color(0xFFF2F4F7)
    val textColor = if (isCapturing) Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, Color(0xFFE5E7EB), androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = if (isCapturing) stringResource(Res.string.log_capture_running) else stringResource(Res.string.log_capture_paused),
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ToolbarButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isActive: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
            contentColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.width(14.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FilterSelect(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
    }
}

@Composable
private fun FilterInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun QuickFilterChip(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
            .background(if (active) Color(0xFFEAF2FF) else Color(0xFFF8F9FB))
            .border(
                1.dp,
                if (active) Color(0xFFC8DBFF) else Color(0xFFE5E7EB),
                androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) {
                if (active) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }
        )
    }
}

@Composable
private fun LogTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FB))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell(text = stringResource(Res.string.log_header_time), width = 108.dp)
        HeaderCell(text = stringResource(Res.string.log_header_level), width = 48.dp)
        HeaderCell(text = stringResource(Res.string.log_header_pid), width = 58.dp)
        HeaderCell(text = stringResource(Res.string.log_header_tid), width = 58.dp)
        HeaderCell(text = stringResource(Res.string.log_header_message), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp? = null, modifier: Modifier = Modifier) {
    val resolved = if (width != null) modifier.width(width) else modifier
    Text(
        text = text,
        modifier = resolved,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun LogTableRow(
    entry: LogEntry,
    dateFormat: SimpleDateFormat
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val baseColor = when {
        entry.level == LogLevel.ERROR || entry.level == LogLevel.FATAL -> Color(0xFFFFF5F5)
        entry.level == LogLevel.WARN -> Color(0xFFFFF9F1)
        hovered -> Color(0xFFF8FAFC)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(baseColor)
            .hoverable(interactionSource = interactionSource)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        TableCell(
            text = dateFormat.format(Date(entry.timestamp)),
            width = 108.dp,
            mono = true
        )
        LevelBadge(level = entry.level, modifier = Modifier.width(48.dp))
        TableCell(text = if (entry.pid > 0) entry.pid.toString() else "-", width = 58.dp, mono = true)
        TableCell(text = if (entry.tid > 0) entry.tid.toString() else "-", width = 58.dp, mono = true)
        TableCell(
            text = entry.message,
            modifier = Modifier.weight(1f),
            mono = true,
            maxLines = Int.MAX_VALUE
        )
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp? = null,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    maxLines: Int = 1
) {
    val resolved = if (width != null) modifier.width(width) else modifier
    Text(
        text = text,
        modifier = resolved.padding(end = 8.dp),
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.6.sp),
        fontFamily = if (mono) FontFamily.Monospace else null,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = maxLines,
        overflow = if (maxLines == 1) TextOverflow.Ellipsis else TextOverflow.Clip
    )
}

@Composable
private fun LevelBadge(level: LogLevel, modifier: Modifier = Modifier) {
    val colors = when (level) {
        LogLevel.VERBOSE -> Color(0xFFF1F5F9) to Color(0xFF475569)
        LogLevel.DEBUG -> Color(0xFFEAF2FF) to Color(0xFF2563EB)
        LogLevel.INFO -> Color(0xFFEAF9F1) to Color(0xFF15803D)
        LogLevel.WARN -> Color(0xFFFFF4E8) to Color(0xFFB45309)
        LogLevel.ERROR -> Color(0xFFFFEBEB) to Color(0xFFDC2626)
        LogLevel.FATAL -> Color(0xFFFFE9F0) to Color(0xFFBE185D)
    }
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                .background(colors.first)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = level.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = colors.second,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun detectLikelyPackage(logs: List<LogEntry>): String? {
    if (logs.isEmpty()) return null
    val regex = Regex("""([a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z0-9_]+){2,})""")
    val score = linkedMapOf<String, Int>()

    logs.takeLast(1200).forEach { entry ->
        val source = "${entry.tag} ${entry.message}"
        regex.findAll(source).forEach { match ->
            val candidate = match.value
            if (candidate.startsWith("android.") || candidate.startsWith("java.")) return@forEach
            score[candidate] = (score[candidate] ?: 0) + 1
        }
    }

    return score.maxByOrNull { it.value }?.key
}
