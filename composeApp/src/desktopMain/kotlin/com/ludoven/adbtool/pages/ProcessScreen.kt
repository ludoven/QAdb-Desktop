package com.ludoven.adbtool.pages

import com.ludoven.adbtool.ui.mac.*

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.process_empty
import adbtool_desktop.composeapp.generated.resources.process_name
import adbtool_desktop.composeapp.generated.resources.process_cpu
import adbtool_desktop.composeapp.generated.resources.process_cpu_time
import adbtool_desktop.composeapp.generated.resources.process_memory
import adbtool_desktop.composeapp.generated.resources.process_no_device
import adbtool_desktop.composeapp.generated.resources.process_pid
import adbtool_desktop.composeapp.generated.resources.process_search_hint
import adbtool_desktop.composeapp.generated.resources.process_subtitle
import adbtool_desktop.composeapp.generated.resources.process_title
import adbtool_desktop.composeapp.generated.resources.process_user
import adbtool_desktop.composeapp.generated.resources.refresh
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import com.ludoven.adbtool.ui.mac.CircularProgressIndicator
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.IconButton
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.OutlinedTextField
import com.ludoven.adbtool.ui.mac.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.util.AdbTool
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private data class ProcessItem(
    val name: String,
    val cpuPercent: String,
    val cpuTime: String,
    val memory: String,
    val pid: String,
    val user: String
)

@Composable
fun ProcessScreen(selectedDevice: String?) {
    var keyword by remember { mutableStateOf("") }
    var processes by remember { mutableStateOf(emptyList<ProcessItem>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun refresh() {
        if (selectedDevice.isNullOrBlank()) {
            processes = emptyList()
            errorText = null
            return
        }
        scope.launch {
            isLoading = true
            errorText = null
            val result = loadProcessList(selectedDevice)
            result.onSuccess { processes = it }
                .onFailure { errorText = it.message ?: "Failed to load process list." }
            isLoading = false
        }
    }

    LaunchedEffect(selectedDevice) {
        refresh()
    }

    val filteredList = remember(processes, keyword) {
        if (keyword.isBlank()) return@remember processes
        processes.filter {
            it.name.contains(keyword, ignoreCase = true) ||
                it.user.contains(keyword, ignoreCase = true) ||
                it.pid.contains(keyword) ||
                it.cpuPercent.contains(keyword) ||
                it.cpuTime.contains(keyword) ||
                it.memory.contains(keyword)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.process_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(Res.string.process_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { refresh() }, enabled = !selectedDevice.isNullOrBlank() && !isLoading) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.refresh))
            }
        }

        if (selectedDevice.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.process_no_device),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(Res.string.process_search_hint)) }
        )

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${filteredList.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(Res.string.process_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(8.dp)
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                errorText != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = errorText.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                filteredList.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.process_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 12.dp),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = stringResource(Res.string.process_name),
                                        modifier = Modifier.width(240.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(Res.string.process_cpu),
                                        modifier = Modifier.width(70.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(Res.string.process_cpu_time),
                                        modifier = Modifier.width(100.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(Res.string.process_memory),
                                        modifier = Modifier.width(82.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(Res.string.process_pid),
                                        modifier = Modifier.width(70.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(Res.string.process_user),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(filteredList) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = item.name,
                                        modifier = Modifier.width(240.dp),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.cpuPercent,
                                        modifier = Modifier.width(70.dp),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.cpuTime,
                                        modifier = Modifier.width(100.dp),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.memory,
                                        modifier = Modifier.width(82.dp),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.pid,
                                        modifier = Modifier.width(70.dp),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
                        )
                    }
                }
            }
        }
    }
}

private suspend fun loadProcessList(deviceId: String): Result<List<ProcessItem>> {
    val topResult = AdbTool.execShellAsync("top -b -n 1", deviceId)
    if (topResult.success && topResult.output.isNotBlank()) {
        val parsedTop = runCatching { parseTopOutput(topResult.output) }.getOrNull().orEmpty()
        if (parsedTop.isNotEmpty()) {
            return Result.success(parsedTop)
        }
    }

    val processResult = AdbTool.execShellAsync("ps -A", deviceId)
    val output = if (processResult.success && processResult.output.isNotBlank()) {
        processResult.output
    } else {
        val fallback = AdbTool.execShellAsync("ps", deviceId)
        if (!fallback.success) {
            return Result.failure(IllegalStateException(fallback.errorMessage ?: "Load process failed"))
        }
        fallback.output
    }

    return runCatching { parsePsOutput(output) }
}

private fun parseTopOutput(raw: String): List<ProcessItem> {
    val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList()

    val headerIndex = lines.indexOfFirst {
        val upper = it.uppercase()
        upper.contains("PID") && upper.contains("CPU") &&
            (upper.contains("NAME") || upper.contains("CMD") || upper.contains("COMMAND"))
    }
    if (headerIndex < 0 || headerIndex + 1 >= lines.size) return emptyList()

    val header = lines[headerIndex].split(Regex("\\s+"))
    val pidIndex = header.indexOfFirst { it.equals("PID", true) }
    val userIndex = header.indexOfFirst { it.equals("USER", true) || it.equals("UID", true) }
    val cpuIndex = header.indexOfFirst {
        it.equals("%CPU", true) || it.equals("CPU%", true) || it.equals("CPU", true)
    }
    val timeIndex = header.indexOfFirst {
        it.equals("TIME+", true) || it.equals("TIME", true) || it.equals("CPUTIME", true)
    }
    val memIndex = header.indexOfFirst {
        it.equals("RES", true) || it.equals("RSS", true) || it.equals("%MEM", true) || it.equals("MEM", true)
    }
    val nameIndex = header.indexOfFirst {
        it.equals("NAME", true) || it.equals("CMD", true) || it.equals("COMMAND", true)
    }

    if (pidIndex < 0 || nameIndex < 0) return emptyList()

    return lines.drop(headerIndex + 1).mapNotNull { line ->
        val tokens = line.split(Regex("\\s+"))
        if (tokens.size <= pidIndex || tokens.size <= nameIndex) return@mapNotNull null
        val name = tokens.drop(nameIndex).joinToString(" ").ifBlank { "-" }
        ProcessItem(
            name = name,
            cpuPercent = tokens.getOrNull(cpuIndex)?.let { normalizeCpuValue(it) } ?: "-",
            cpuTime = tokens.getOrNull(timeIndex) ?: "-",
            memory = tokens.getOrNull(memIndex) ?: "-",
            pid = tokens.getOrNull(pidIndex) ?: "-",
            user = tokens.getOrNull(userIndex) ?: "-"
        )
    }.sortedByDescending { parseCpuForSort(it.cpuPercent) }
}

private fun parsePsOutput(raw: String): List<ProcessItem> {
    val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList()

    val header = lines.first().split(Regex("\\s+"))
    val pidIndex = header.indexOfFirst { it.equals("PID", ignoreCase = true) }.takeIf { it >= 0 } ?: 1
    val userIndex = header.indexOfFirst { it.equals("USER", ignoreCase = true) || it.equals("UID", ignoreCase = true) }
        .takeIf { it >= 0 } ?: 0
    val nameIndex = header.indexOfFirst {
        it.equals("NAME", ignoreCase = true) ||
            it.equals("CMD", ignoreCase = true) ||
            it.equals("COMMAND", ignoreCase = true) ||
            it.equals("ARGS", ignoreCase = true)
    }.takeIf { it >= 0 } ?: (header.size - 1)

    return lines.drop(1).mapNotNull { line ->
        val tokens = line.split(Regex("\\s+"))
        if (tokens.size <= pidIndex || tokens.size <= nameIndex) return@mapNotNull null
        val pid = tokens[pidIndex]
        val user = tokens.getOrNull(userIndex) ?: "-"
        val name = tokens.drop(nameIndex).joinToString(" ").ifBlank { tokens.lastOrNull().orEmpty() }
        ProcessItem(
            name = name,
            cpuPercent = "-",
            cpuTime = "-",
            memory = "-",
            pid = pid,
            user = user,
        )
    }.sortedBy { it.pid.toIntOrNull() ?: Int.MAX_VALUE }
}

private fun normalizeCpuValue(raw: String): String {
    val cleaned = raw.trim()
    return if (cleaned.endsWith("%")) cleaned else "$cleaned%"
}

private fun parseCpuForSort(raw: String): Double {
    return raw.removeSuffix("%").toDoubleOrNull() ?: -1.0
}
