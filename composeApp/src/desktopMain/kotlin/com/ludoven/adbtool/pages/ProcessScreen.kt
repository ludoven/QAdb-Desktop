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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Refresh
import com.ludoven.adbtool.ui.mac.CircularProgressIndicator
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.IconButton
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.OutlinedTextField
import com.ludoven.adbtool.ui.mac.Text
import com.ludoven.adbtool.widget.EmptyStatePanel
import com.ludoven.adbtool.widget.PageHeader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.util.AdbTool
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

internal data class ProcessItem(
    val name: String,
    val cpuPercent: String,
    val cpuTime: String,
    val memory: String,
    val pid: String,
    val user: String
)

private enum class SortColumn { NAME, CPU, CPU_TIME, MEMORY, PID, USER }

@Composable
fun ProcessScreen(selectedDevice: String?) {
    var keyword by remember { mutableStateOf("") }
    var processes by remember { mutableStateOf(emptyList<ProcessItem>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf(SortColumn.CPU) }
    var sortDesc by remember { mutableStateOf(true) }
    var loadedDevice by remember { mutableStateOf<String?>(null) }
    var loadJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun refresh(forceRefresh: Boolean = false) {
        val deviceId = selectedDevice?.trim()?.takeIf { it.isNotBlank() }
        if (deviceId == null) {
            loadJob?.cancel()
            loadJob = null
            loadedDevice = null
            processes = emptyList()
            errorText = null
            isLoading = false
            return
        }
        if (!forceRefresh && loadedDevice == deviceId && loadJob?.isActive != true) {
            isLoading = false
            return
        }
        loadJob?.cancel()
        loadJob = scope.launch {
            isLoading = true
            errorText = null
            val result = loadProcessList(deviceId)
            if (selectedDevice?.trim() == deviceId) {
                result.onSuccess {
                    processes = it
                    loadedDevice = deviceId
                }.onFailure {
                    errorText = it.message ?: "Failed to load process list."
                }
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedDevice) {
        refresh()
    }
    DisposableEffect(Unit) {
        onDispose {
            loadJob?.cancel()
            loadJob = null
            isLoading = false
        }
    }

    val filteredList = remember(processes, keyword, sortBy, sortDesc) {
        val base = if (keyword.isBlank()) processes
        else processes.filter {
            it.name.contains(keyword, ignoreCase = true) ||
                it.user.contains(keyword, ignoreCase = true) ||
                it.pid.contains(keyword) ||
                it.cpuPercent.contains(keyword) ||
                it.cpuTime.contains(keyword) ||
                it.memory.contains(keyword)
        }
        val comparator: Comparator<ProcessItem> = when (sortBy) {
            SortColumn.NAME -> compareBy { it.name.lowercase() }
            SortColumn.CPU -> compareBy { parseCpuForSort(it.cpuPercent) }
            SortColumn.CPU_TIME -> compareBy { it.cpuTime }
            SortColumn.MEMORY -> compareBy { parseMemoryForSort(it.memory) }
            SortColumn.PID -> compareBy { it.pid.toIntOrNull() ?: Int.MAX_VALUE }
            SortColumn.USER -> compareBy { it.user }
        }
        if (sortDesc) base.sortedWith(comparator.reversed()) else base.sortedWith(comparator)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PageHeader(
            title = stringResource(Res.string.process_title),
            subtitle = stringResource(Res.string.process_subtitle)
        ) {
            IconButton(onClick = { refresh(forceRefresh = true) }, enabled = !selectedDevice.isNullOrBlank() && !isLoading) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.refresh))
            }
        }

        if (selectedDevice.isNullOrBlank()) {
            EmptyStatePanel(
                title = stringResource(Res.string.process_no_device),
                description = "连接或选择设备后即可读取进程列表。",
                icon = Icons.Default.Refresh,
                modifier = Modifier.fillMaxSize()
            )
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
                    EmptyStatePanel(
                        title = "进程读取失败",
                        description = errorText.orEmpty(),
                        actionLabel = stringResource(Res.string.refresh),
                        onAction = { refresh(forceRefresh = true) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                filteredList.isEmpty() -> {
                    EmptyStatePanel(
                        title = stringResource(Res.string.process_empty),
                        description = if (keyword.isBlank()) "设备当前没有返回进程数据。" else "没有进程匹配当前搜索条件。",
                        modifier = Modifier.fillMaxSize()
                    )
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
                                    SortableColumnHeader(
                                        text = stringResource(Res.string.process_name),
                                        width = 240.dp,
                                        column = SortColumn.NAME,
                                        currentSort = sortBy,
                                        descending = sortDesc,
                                        onClick = {
                                            if (sortBy == SortColumn.NAME) sortDesc = !sortDesc
                                            else { sortBy = SortColumn.NAME; sortDesc = true }
                                        }
                                    )
                                    SortableColumnHeader(
                                        text = stringResource(Res.string.process_cpu),
                                        width = 70.dp,
                                        column = SortColumn.CPU,
                                        currentSort = sortBy,
                                        descending = sortDesc,
                                        onClick = {
                                            if (sortBy == SortColumn.CPU) sortDesc = !sortDesc
                                            else { sortBy = SortColumn.CPU; sortDesc = true }
                                        }
                                    )
                                    SortableColumnHeader(
                                        text = stringResource(Res.string.process_cpu_time),
                                        width = 100.dp,
                                        column = SortColumn.CPU_TIME,
                                        currentSort = sortBy,
                                        descending = sortDesc,
                                        onClick = {
                                            if (sortBy == SortColumn.CPU_TIME) sortDesc = !sortDesc
                                            else { sortBy = SortColumn.CPU_TIME; sortDesc = true }
                                        }
                                    )
                                    SortableColumnHeader(
                                        text = stringResource(Res.string.process_memory),
                                        width = 82.dp,
                                        column = SortColumn.MEMORY,
                                        currentSort = sortBy,
                                        descending = sortDesc,
                                        onClick = {
                                            if (sortBy == SortColumn.MEMORY) sortDesc = !sortDesc
                                            else { sortBy = SortColumn.MEMORY; sortDesc = true }
                                        }
                                    )
                                    SortableColumnHeader(
                                        text = stringResource(Res.string.process_pid),
                                        width = 70.dp,
                                        column = SortColumn.PID,
                                        currentSort = sortBy,
                                        descending = sortDesc,
                                        onClick = {
                                            if (sortBy == SortColumn.PID) sortDesc = !sortDesc
                                            else { sortBy = SortColumn.PID; sortDesc = false }
                                        }
                                    )
                                    SortableColumnHeader(
                                        text = stringResource(Res.string.process_user),
                                        width = null,
                                        column = SortColumn.USER,
                                        currentSort = sortBy,
                                        descending = sortDesc,
                                        onClick = {
                                            if (sortBy == SortColumn.USER) sortDesc = !sortDesc
                                            else { sortBy = SortColumn.USER; sortDesc = true }
                                        }
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
                                        text = item.user,
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

internal fun parseTopOutput(raw: String): List<ProcessItem> {
    val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList()

    val headerIndex = lines.indexOfFirst {
        val upper = it.uppercase()
        upper.contains("PID") && upper.contains("CPU") &&
            (upper.contains("NAME") || upper.contains("CMD") || upper.contains("COMMAND") || upper.contains("ARGS"))
    }
    if (headerIndex < 0 || headerIndex + 1 >= lines.size) return emptyList()

    val header = lines[headerIndex].split(Regex("\\s+"))
    val pidIndex = header.indexOfFirst { it.equals("PID", true) }
    val userIndex = header.indexOfFirst { it.equals("USER", true) || it.equals("UID", true) }
    val cpuIndex = header.indexOfFirst {
        it.equals("%CPU", true) || it.equals("CPU%", true) || it.equals("CPU", true) ||
            it.contains("CPU", ignoreCase = true)
    }
    val timeIndex = header.indexOfFirst {
        it.equals("TIME+", true) || it.equals("TIME", true) || it.equals("CPUTIME", true)
    }
    val memIndex = header.indexOfFirst {
        it.equals("RES", true) || it.equals("RSS", true) || it.equals("%MEM", true) || it.equals("MEM", true)
    }
    val nameIndex = header.indexOfFirst {
        it.equals("NAME", true) || it.equals("CMD", true) || it.equals("COMMAND", true)
            || it.equals("ARGS", true)
    }

    if (pidIndex < 0 || nameIndex < 0) return emptyList()
    val mergedStateCpuIndex = header.indexOfFirst {
        it.contains("CPU", ignoreCase = true) && it.contains("[") && it.contains("]")
    }

    fun dataIndex(headerIndex: Int): Int {
        if (headerIndex < 0) return headerIndex
        return if (mergedStateCpuIndex >= 0 && headerIndex >= mergedStateCpuIndex) {
            headerIndex + 1
        } else {
            headerIndex
        }
    }

    return lines.drop(headerIndex + 1).mapNotNull { line ->
        val tokens = line.split(Regex("\\s+"))
        val pidDataIndex = dataIndex(pidIndex)
        val userDataIndex = dataIndex(userIndex)
        val cpuDataIndex = dataIndex(cpuIndex)
        val timeDataIndex = dataIndex(timeIndex)
        val memDataIndex = dataIndex(memIndex)
        val nameDataIndex = dataIndex(nameIndex)
        if (tokens.size <= pidDataIndex || tokens.size <= nameDataIndex) return@mapNotNull null
        val name = tokens.drop(nameDataIndex).joinToString(" ").ifBlank { "-" }
        ProcessItem(
            name = name,
            cpuPercent = tokens.getOrNull(cpuDataIndex)?.let { normalizeCpuValue(it) } ?: "-",
            cpuTime = tokens.getOrNull(timeDataIndex) ?: "-",
            memory = tokens.getOrNull(memDataIndex) ?: "-",
            pid = tokens.getOrNull(pidDataIndex) ?: "-",
            user = tokens.getOrNull(userDataIndex) ?: "-"
        )
    }.sortedByDescending { parseCpuForSort(it.cpuPercent) }
}

internal fun parsePsOutput(raw: String): List<ProcessItem> {
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
            it.equals("CMDLINE", ignoreCase = true) ||
            it.equals("ARGS", ignoreCase = true)
    }.takeIf { it >= 0 } ?: (header.size - 1)
    val residentMemoryIndex = header.indexOfFirst {
        it.equals("RSS", ignoreCase = true) ||
            it.equals("RES", ignoreCase = true)
    }.takeIf { it >= 0 }
    val virtualMemoryIndex = header.indexOfFirst {
        it.equals("VSZ", ignoreCase = true) ||
            it.equals("VSIZE", ignoreCase = true)
    }.takeIf { it >= 0 }
    val memoryIndex = residentMemoryIndex ?: virtualMemoryIndex

    return lines.drop(1).mapNotNull { line ->
        val tokens = line.split(Regex("\\s+"))
        if (tokens.size <= pidIndex || tokens.size <= nameIndex) return@mapNotNull null
        val pid = tokens[pidIndex]
        val user = tokens.getOrNull(userIndex) ?: "-"
        val name = tokens.drop(nameIndex).joinToString(" ").ifBlank { tokens.lastOrNull().orEmpty() }
        val memory = memoryIndex?.let { tokens.getOrNull(it) } ?: "-"
        ProcessItem(
            name = name,
            cpuPercent = "-",
            cpuTime = "-",
            memory = memory,
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

private fun parseMemoryForSort(raw: String): Double {
    val cleaned = raw.trim().removeSuffix("K").removeSuffix("M").removeSuffix("G").removeSuffix("k").removeSuffix("m").removeSuffix("g")
    return cleaned.toDoubleOrNull() ?: -1.0
}

@Composable
private fun SortableColumnHeader(
    text: String,
    width: Dp?,
    column: SortColumn,
    currentSort: SortColumn,
    descending: Boolean,
    onClick: () -> Unit
) {
    val isActive = column == currentSort
    val modifier = if (width != null) Modifier.width(width) else Modifier
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isActive) {
            Icon(
                imageVector = if (descending) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropUp,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
