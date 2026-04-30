package com.ludoven.adbtool.pages

import adbtool_desktop.composeapp.generated.resources.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.entity.LogEntry
import com.ludoven.adbtool.entity.LogFilter
import com.ludoven.adbtool.entity.LogLevel
import com.ludoven.adbtool.viewmodel.LogViewModel
import com.ludoven.adbtool.widget.GlassCard
import org.jetbrains.compose.resources.stringResource
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    viewModel: LogViewModel,
    selectedDevice: String?,
    onDeviceSelect: () -> Unit
) {
    val logs by viewModel.logs.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val listState = rememberLazyListState()
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    val filteredLogs = remember(logs, filter) {
        viewModel.getFilteredLogs()
    }

    var showFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Filter button
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.FilterList, "Filter")
                }

                // Clear button
                IconButton(onClick = { viewModel.clearLogs() }) {
                    Icon(Icons.Default.DeleteSweep, "Clear")
                }

                // Export button
                IconButton(
                    onClick = {
                        val dialog = FileDialog(null as Frame?, "Export Logs", FileDialog.SAVE)
                        dialog.isVisible = true
                        val dir = dialog.directory
                        val filename = dialog.file
                        if (dir != null && filename != null) {
                            viewModel.exportLogs(File(dir, filename))
                        }
                    }
                ) {
                    Icon(Icons.Default.FileDownload, "Export")
                }
            }
        }

        // Filter summary
        if (filter.level != null || filter.keyword.isNotEmpty() || filter.tag.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildString {
                        append("Filters: ")
                        val filters = mutableListOf<String>()
                        filter.level?.let { filters.add("Level: ${it.displayName}") }
                        if (filter.keyword.isNotEmpty()) filters.add("Keyword: ${filter.keyword}")
                        if (filter.tag.isNotEmpty()) filters.add("Tag: ${filter.tag}")
                        append(filters.joinToString(", "))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { viewModel.updateFilter(LogFilter()) }) {
                    Text("Clear Filters")
                }
            }
        }

        // Control bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device selection
            OutlinedCard(
                modifier = Modifier.weight(1f),
                onClick = onDeviceSelect
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedDevice ?: stringResource(Res.string.select_device),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Start/Stop capture
            Button(
                onClick = {
                    if (isCapturing) {
                        viewModel.stopCapture()
                    } else if (selectedDevice != null) {
                        viewModel.startCapture(selectedDevice)
                    }
                },
                enabled = selectedDevice != null || isCapturing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCapturing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isCapturing) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isCapturing) stringResource(Res.string.log_stop) else stringResource(Res.string.log_start))
            }
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(filteredLogs) { entry ->
                        LogEntryItem(entry, dateFormat)
                    }
                }
            }
        }

        // Error message
        errorMessage?.let { error ->
            Snackbar(
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(stringResource(Res.string.log_dismiss))
                    }
                }
            ) {
                Text(error)
            }
        }
    }

    // Filter dialog
    if (showFilterDialog) {
        FilterDialog(
            currentFilter = filter,
            onDismiss = { showFilterDialog = false },
            onApply = { newFilter ->
                viewModel.updateFilter(newFilter)
                showFilterDialog = false
            }
        )
    }
}

@Composable
private fun LogEntryItem(
    entry: LogEntry,
    dateFormat: SimpleDateFormat
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FilterDialog(
    currentFilter: LogFilter,
    onDismiss: () -> Unit,
    onApply: (LogFilter) -> Unit
) {
    var level by remember { mutableStateOf(currentFilter.level) }
    var keyword by remember { mutableStateOf(currentFilter.keyword) }
    var tag by remember { mutableStateOf(currentFilter.tag) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.log_filter_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Level filter
                Column {
                    Text(
                        text = stringResource(Res.string.log_filter_level),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LogLevel.values().forEach { logLevel ->
                            FilterChip(
                                selected = level == logLevel,
                                onClick = { level = if (level == logLevel) null else logLevel },
                                label = { Text(logLevel.displayName) }
                            )
                        }
                    }
                }

                // Keyword filter
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(stringResource(Res.string.log_filter_keyword)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Tag filter
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text(stringResource(Res.string.log_filter_tag)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(LogFilter(level = level, keyword = keyword, tag = tag))
            }) {
                Text(stringResource(Res.string.log_apply_filter))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
