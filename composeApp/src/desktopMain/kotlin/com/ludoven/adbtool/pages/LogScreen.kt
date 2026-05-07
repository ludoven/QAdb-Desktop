package com.ludoven.adbtool.pages

import adbtool_desktop.composeapp.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.ludoven.adbtool.ui.mac.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.entity.LogEntry
import com.ludoven.adbtool.entity.LogFilter
import com.ludoven.adbtool.entity.LogLevel
import com.ludoven.adbtool.viewmodel.LogViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    var autoWrap by remember { mutableStateOf(true) }
    var showLevelMenu by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, filter) {
        viewModel.getFilteredLogs()
    }

    LaunchedEffect(filteredLogs.size, isCapturing) {
        if (isCapturing && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
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
        }

        // Filter and actions in one row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(148.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .clickable { showLevelMenu = true }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = filter.level?.displayName ?: stringResource(Res.string.log_level_all),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(Res.string.log_filter_level)
                    )
                }

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

            InlineFilterInput(
                value = filter.packageName,
                onValueChange = { viewModel.updateFilter(filter.copy(packageName = it)) },
                placeholder = stringResource(Res.string.log_filter_package),
                modifier = Modifier.width(196.dp)
            )

            InlineFilterInput(
                value = filter.tag,
                onValueChange = { viewModel.updateFilter(filter.copy(tag = it)) },
                placeholder = stringResource(Res.string.log_filter_tag),
                modifier = Modifier.width(176.dp)
            )

            IconButton(
                onClick = { viewModel.updateFilter(LogFilter()) },
                enabled = filter.level != null || filter.packageName.isNotEmpty() || filter.tag.isNotEmpty(),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.FilterAltOff,
                    contentDescription = stringResource(Res.string.log_clear_filter)
                )
            }

            IconButton(
                onClick = {
                    val dialog = FileDialog(null as Frame?, "Export Logs", FileDialog.SAVE)
                    dialog.isVisible = true
                    val dir = dialog.directory
                    val filename = dialog.file
                    if (dir != null && filename != null) {
                        viewModel.exportLogs(File(dir, filename))
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = "Export")
            }

            IconButton(
                onClick = { autoWrap = !autoWrap },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    if (autoWrap) Icons.Default.WrapText else Icons.Default.DoNotDisturbAlt,
                    contentDescription = stringResource(Res.string.log_auto_wrap)
                )
            }

            IconButton(
                onClick = {
                    if (filteredLogs.isNotEmpty()) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(filteredLogs.size - 1)
                        }
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.VerticalAlignBottom,
                    contentDescription = stringResource(Res.string.log_scroll_bottom)
                )
            }

            IconButton(
                onClick = {
                    if (selectedDevice != null) {
                        viewModel.restartCapture(selectedDevice)
                    }
                },
                enabled = selectedDevice != null && !isLoading,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.log_restart))
            }

            IconButton(
                onClick = {
                    if (isCapturing) {
                        viewModel.stopCapture()
                    } else if (selectedDevice != null) {
                        viewModel.startCapture(selectedDevice)
                    }
                },
                enabled = (selectedDevice != null || isCapturing) && !isLoading,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isCapturing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCapturing) {
                        stringResource(Res.string.log_pause)
                    } else {
                        stringResource(Res.string.log_resume)
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { viewModel.clearLogs() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = stringResource(Res.string.log_clear),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // Active filter summary
        if (filter.level != null || filter.packageName.isNotEmpty() || filter.tag.isNotEmpty()) {
            Text(
                text = buildString {
                    append("${stringResource(Res.string.log_filter_summary_prefix)} ")
                    val filters = mutableListOf<String>()
                    filter.level?.let { filters.add("Level: ${it.displayName}") }
                    if (filter.packageName.isNotEmpty()) filters.add("Package: ${filter.packageName}")
                    if (filter.tag.isNotEmpty()) filters.add("Tag: ${filter.tag}")
                    append(filters.joinToString(", "))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Log count info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.log_entries_count, filteredLogs.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isCapturing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.log_capturing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Log list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (isCapturing) stringResource(Res.string.log_waiting) else stringResource(Res.string.log_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 12.dp),
                        state = listState,
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(filteredLogs) { entry ->
                            LogEntryItem(
                                entry = entry,
                                dateFormat = dateFormat,
                                autoWrap = autoWrap
                            )
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

        // Error message
        errorMessage?.let { error ->
            Snackbar(
                action = {
                    IconButton(onClick = { viewModel.clearError() }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(Res.string.log_dismiss)
                        )
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

@Composable
private fun InlineFilterInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun LogEntryItem(
    entry: LogEntry,
    dateFormat: SimpleDateFormat,
    autoWrap: Boolean
) {
    val backgroundColor = when (entry.level) {
        LogLevel.ERROR, LogLevel.FATAL -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        LogLevel.WARN -> Color(0xFFFFF3CD)
        LogLevel.DEBUG -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    val levelColor = when (entry.level) {
        LogLevel.VERBOSE -> Color.Gray
        LogLevel.DEBUG -> Color.Blue
        LogLevel.INFO -> Color(0xFF4CAF50)
        LogLevel.WARN -> Color(0xFFFFC107)
        LogLevel.ERROR -> Color(0xFFF44336)
        LogLevel.FATAL -> Color(0xFF9C27B0)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Timestamp
        Text(
            text = dateFormat.format(Date(entry.timestamp)),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(85.dp)
        )

        // Level badge
        Text(
            text = entry.level.displayName,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = Color.White,
            modifier = Modifier
                .background(levelColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .width(18.dp)
        )

        // Tag
        Text(
            text = entry.tag,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(100.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Message
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = if (autoWrap) Int.MAX_VALUE else 1,
            overflow = if (autoWrap) TextOverflow.Clip else TextOverflow.Ellipsis
        )
    }
}
