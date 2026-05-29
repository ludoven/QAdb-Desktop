package com.ludoven.adbtool.pages

import com.ludoven.adbtool.ui.mac.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.viewmodel.CpuProcess
import com.ludoven.adbtool.viewmodel.MemProcess
import com.ludoven.adbtool.viewmodel.PrimaryStorageSummary
import com.ludoven.adbtool.viewmodel.ResourceViewModel
import com.ludoven.adbtool.viewmodel.StorageApp
import com.ludoven.adbtool.viewmodel.StoragePartition
import com.ludoven.adbtool.widget.BarChartItem
import com.ludoven.adbtool.widget.DashboardPanel
import com.ludoven.adbtool.widget.GlassCard
import com.ludoven.adbtool.widget.HorizontalBarChart
import com.ludoven.adbtool.widget.LineChartSeries
import com.ludoven.adbtool.widget.MultiRealtimeLineChart
import com.ludoven.adbtool.widget.RealtimeLineChart

@Composable
fun PerformanceScreen(
    viewModel: ResourceViewModel,
    selectedDevice: String?
) {
    val cpuHistory by viewModel.cpuHistory.collectAsState()
    val currentCpuPercent by viewModel.currentCpuPercent.collectAsState()
    val cpuProcesses by viewModel.cpuProcesses.collectAsState()
    val memHistory by viewModel.memHistory.collectAsState()
    val memTotalMb by viewModel.memTotalMb.collectAsState()
    val memAvailableMb by viewModel.memAvailableMb.collectAsState()
    val memUsedMb by viewModel.memUsedMb.collectAsState()
    val memUsedPercent by viewModel.memUsedPercent.collectAsState()
    val memProcesses by viewModel.memProcesses.collectAsState()
    val storagePartitions by viewModel.storagePartitions.collectAsState()
    val primaryStorageSummary by viewModel.primaryStorageSummary.collectAsState()
    val storageHistory by viewModel.storageHistory.collectAsState()
    val storageApps by viewModel.storageApps.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val lastUpdatedAtMillis by viewModel.lastUpdatedAtMillis.collectAsState()

    LaunchedEffect(selectedDevice) {
        viewModel.setDevice(selectedDevice)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopMonitoring()
        }
    }

    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compactLayout = maxWidth < 1080.dp
        val overviewSeries = buildOverviewSeries(
            cpuHistory = cpuHistory,
            memHistory = memHistory,
            storageHistory = storageHistory
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(if (compactLayout) 18.dp else 14.dp)
        ) {
            PerformanceHeader(
                compactLayout = compactLayout,
                selectedDevice = selectedDevice,
                isMonitoring = isMonitoring,
                hasDevice = !selectedDevice.isNullOrBlank(),
                lastUpdatedAtMillis = lastUpdatedAtMillis,
                onRefresh = {
                    if (!selectedDevice.isNullOrBlank()) {
                        viewModel.refreshOnce(selectedDevice)
                    }
                },
                onToggleMonitoring = {
                    if (!selectedDevice.isNullOrBlank()) {
                        if (isMonitoring) {
                            viewModel.stopMonitoring()
                        } else {
                            viewModel.startMonitoring(selectedDevice)
                        }
                    }
                }
            )

            if (selectedDevice.isNullOrBlank()) {
                EmptyPerformanceState()
                return@Column
            }

            OverviewTrendPanel(overviewSeries = overviewSeries)

            OverviewCards(
                compactLayout = compactLayout,
                currentCpuPercent = currentCpuPercent,
                cpuHistory = cpuHistory,
                cpuProcesses = cpuProcesses,
                memUsedMb = memUsedMb,
                memTotalMb = memTotalMb,
                memAvailableMb = memAvailableMb,
                memHistory = memHistory,
                storageSummary = primaryStorageSummary,
                storageHistory = storageHistory,
                storageApps = storageApps
            )

            if (compactLayout) {
                CpuSection(
                    compactLayout = true,
                    cpuHistory = cpuHistory,
                    currentCpuPercent = currentCpuPercent,
                    cpuProcesses = cpuProcesses
                )

                MemorySection(
                    compactLayout = true,
                    memHistory = memHistory,
                    memUsedMb = memUsedMb,
                    memTotalMb = memTotalMb,
                    memAvailableMb = memAvailableMb,
                    memUsedPercent = memUsedPercent,
                    memProcesses = memProcesses
                )

                StorageSection(
                    compactLayout = true,
                    storageSummary = primaryStorageSummary,
                    storageHistory = storageHistory,
                    partitions = storagePartitions,
                    apps = storageApps
                )
            } else {
                DesktopDetailBoard(
                    cpuHistory = cpuHistory,
                    currentCpuPercent = currentCpuPercent,
                    cpuProcesses = cpuProcesses,
                    memHistory = memHistory,
                    memUsedMb = memUsedMb,
                    memTotalMb = memTotalMb,
                    memAvailableMb = memAvailableMb,
                    memUsedPercent = memUsedPercent,
                    memProcesses = memProcesses,
                    storageSummary = primaryStorageSummary,
                    storageHistory = storageHistory,
                    partitions = storagePartitions,
                    apps = storageApps
                )
            }
        }
    }
}

@Composable
private fun DesktopDetailBoard(
    cpuHistory: List<Float>,
    currentCpuPercent: Float,
    cpuProcesses: List<CpuProcess>,
    memHistory: List<Float>,
    memUsedMb: Long,
    memTotalMb: Long,
    memAvailableMb: Long,
    memUsedPercent: Float,
    memProcesses: List<MemProcess>,
    storageSummary: PrimaryStorageSummary?,
    storageHistory: List<Float>,
    partitions: List<StoragePartition>,
    apps: List<StorageApp>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            DashboardPanel(
                title = l10n("CPU 详情", "CPU Details"),
                icon = Icons.Default.Speed
            ) {
                CpuSectionContent(
                    cpuHistory = cpuHistory,
                    currentCpuPercent = currentCpuPercent,
                    cpuProcesses = cpuProcesses,
                    stacked = false
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            DashboardPanel(
                title = l10n("内存详情", "Memory Details"),
                icon = Icons.Default.Memory
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1.1f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            MemoryTrendChart(memHistory, memUsedMb, memTotalMb, memUsedPercent)
                            MemoryStats(memUsedMb, memTotalMb, memAvailableMb)
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MemoryTopProcesses(memProcesses, maxItems = 8)
                    }
                }
            }
        }
    }

    DashboardPanel(
        title = l10n("存储详情", "Storage Details"),
        icon = Icons.Default.Storage
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (storageSummary != null || storageHistory.isNotEmpty()) {
                RealtimeLineChart(
                    data = storageHistory,
                    modifier = Modifier.fillMaxWidth(),
                    lineColor = MaterialTheme.colorScheme.tertiary,
                    fillColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    chartHeight = 140.dp,
                    label = storageSummary?.let {
                        l10n(
                            "${it.mount} ${formatGb(it.usedGb)} / ${formatGb(it.totalGb)} (${formatPercent(it.usedPercent)}%)",
                            "${it.mount} ${formatGb(it.usedGb)} / ${formatGb(it.totalGb)} (${formatPercent(it.usedPercent)}%)"
                        )
                    } ?: l10n("主分区加载中", "Primary partition loading")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    StoragePartitionsList(partitions.take(6))
                }
                Box(modifier = Modifier.weight(1f)) {
                    StorageTopApps(apps, maxItems = 10)
                }
            }
        }
    }
}

@Composable
private fun PerformanceHeader(
    compactLayout: Boolean,
    selectedDevice: String?,
    isMonitoring: Boolean,
    hasDevice: Boolean,
    lastUpdatedAtMillis: Long?,
    onRefresh: () -> Unit,
    onToggleMonitoring: () -> Unit
) {
    if (compactLayout) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HeaderCopy()
            HeaderStatusCard(selectedDevice, isMonitoring, lastUpdatedAtMillis)
            HeaderActions(hasDevice, isMonitoring, onRefresh, onToggleMonitoring)
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HeaderCopy()
        }
        HeaderStatusCard(selectedDevice, isMonitoring, lastUpdatedAtMillis)
        HeaderActions(hasDevice, isMonitoring, onRefresh, onToggleMonitoring)
    }
}

@Composable
private fun HeaderCopy() {
    Text(
        text = l10n("性能监控", "Performance Monitor"),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = l10n(
            "CPU、内存、存储统一总览，滚动查看同页详细分析。",
            "Unified CPU, memory and storage monitor with same-page detail sections."
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun HeaderStatusCard(
    selectedDevice: String?,
    isMonitoring: Boolean,
    lastUpdatedAtMillis: Long?
) {
    GlassCard(
        modifier = Modifier.width(250.dp),
        borderStroke = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = l10n("监控状态", "Monitor Status"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isMonitoring) l10n("运行中", "Running") else l10n("已停止", "Stopped"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            StatusLine(l10n("设备", "Device"), selectedDevice ?: l10n("未连接", "Disconnected"))
            StatusLine(l10n("采样", "Sampling"), l10n("CPU/内存 1s · 存储 5s", "CPU/Memory 1s · Storage 5s"))
            StatusLine(l10n("刷新", "Updated"), formatRelativeUpdate(lastUpdatedAtMillis))
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HeaderActions(
    hasDevice: Boolean,
    isMonitoring: Boolean,
    onRefresh: () -> Unit,
    onToggleMonitoring: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = onRefresh, enabled = hasDevice) {
            Icon(Icons.Default.Refresh, contentDescription = l10n("刷新", "Refresh"))
        }
        IconButton(onClick = onToggleMonitoring, enabled = hasDevice) {
            Icon(
                imageVector = if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isMonitoring) l10n("停止", "Stop") else l10n("开始", "Start")
            )
        }
    }
}

@Composable
private fun EmptyPerformanceState() {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        borderStroke = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = l10n("请先连接设备后再查看性能监控。", "Connect a device to start performance monitoring."),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OverviewTrendPanel(overviewSeries: List<LineChartSeries>) {
    DashboardPanel(
        title = l10n("60 秒综合趋势", "60s Overview Trend"),
        icon = Icons.Default.Speed
    ) {
        MultiRealtimeLineChart(series = overviewSeries, chartHeight = 110.dp)
    }
}

@Composable
private fun OverviewCards(
    compactLayout: Boolean,
    currentCpuPercent: Float,
    cpuHistory: List<Float>,
    cpuProcesses: List<CpuProcess>,
    memUsedMb: Long,
    memTotalMb: Long,
    memAvailableMb: Long,
    memHistory: List<Float>,
    storageSummary: PrimaryStorageSummary?,
    storageHistory: List<Float>,
    storageApps: List<StorageApp>
) {
    if (compactLayout) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CpuOverviewCard(currentCpuPercent, cpuHistory, cpuProcesses)
            MemoryOverviewCard(memUsedMb, memTotalMb, memAvailableMb, memHistory)
            StorageOverviewCard(storageSummary, storageHistory, storageApps)
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            CpuOverviewCard(currentCpuPercent, cpuHistory, cpuProcesses)
        }
        Box(modifier = Modifier.weight(1f)) {
            MemoryOverviewCard(memUsedMb, memTotalMb, memAvailableMb, memHistory)
        }
        Box(modifier = Modifier.weight(1f)) {
            StorageOverviewCard(storageSummary, storageHistory, storageApps)
        }
    }
}

@Composable
private fun CpuOverviewCard(
    currentCpuPercent: Float,
    cpuHistory: List<Float>,
    cpuProcesses: List<CpuProcess>
) {
    OverviewMetricCard(
        title = "CPU",
        icon = Icons.Default.Speed,
        value = "${formatPercent(currentCpuPercent)}%",
        subtitle = l10n("当前总占用", "Current usage")
    ) {
        RealtimeLineChart(
            data = cpuHistory,
            chartHeight = 72.dp,
            showGrid = false,
            showAxisLabels = false
        )
        OverviewCaption(
            cpuProcesses.take(3).joinToString(" · ") { "${shortProcessName(it.name)} ${formatPercent(it.cpuPercent)}%" }
                .ifBlank { l10n("等待 CPU 进程数据...", "Waiting for CPU process data...") }
        )
    }
}

@Composable
private fun MemoryOverviewCard(
    memUsedMb: Long,
    memTotalMb: Long,
    memAvailableMb: Long,
    memHistory: List<Float>
) {
    OverviewMetricCard(
        title = l10n("内存", "Memory"),
        icon = Icons.Default.Memory,
        value = "${formatBytesMb(memUsedMb)} / ${formatBytesMb(memTotalMb)}",
        subtitle = l10n("已用 / 总量", "Used / Total")
    ) {
        RealtimeLineChart(
            data = memHistory,
            lineColor = MaterialTheme.colorScheme.secondary,
            fillColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
            chartHeight = 72.dp,
            showGrid = false,
            showAxisLabels = false
        )
        OverviewCaption(l10n("可用内存: ${formatBytesMb(memAvailableMb)}", "Available: ${formatBytesMb(memAvailableMb)}"))
    }
}

@Composable
private fun StorageOverviewCard(
    storageSummary: PrimaryStorageSummary?,
    storageHistory: List<Float>,
    storageApps: List<StorageApp>
) {
    OverviewMetricCard(
        title = l10n("存储", "Storage"),
        icon = Icons.Default.SdStorage,
        value = storageSummary?.let { "${formatPercent(it.usedPercent)}%" } ?: "--",
        subtitle = storageSummary?.let { "${formatGb(it.usedGb)} / ${formatGb(it.totalGb)}" }
            ?: l10n("主分区加载中", "Primary partition loading")
    ) {
        RealtimeLineChart(
            data = storageHistory,
            lineColor = MaterialTheme.colorScheme.tertiary,
            fillColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
            chartHeight = 72.dp,
            showGrid = false,
            showAxisLabels = false
        )
        val topApp = storageApps.firstOrNull()
        OverviewCaption(
            when {
                topApp != null -> l10n(
                    "最大应用: ${shortProcessName(topApp.name)} ${formatAppSize(topApp.sizeMb)}",
                    "Largest app: ${shortProcessName(topApp.name)} ${formatAppSize(topApp.sizeMb)}"
                )
                storageSummary != null -> l10n("分区: ${storageSummary.mount}", "Partition: ${storageSummary.mount}")
                else -> l10n("等待存储数据...", "Waiting for storage data...")
            }
        )
    }
}

@Composable
private fun OverviewMetricCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderStroke = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
private fun OverviewCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun CpuSection(
    compactLayout: Boolean,
    cpuHistory: List<Float>,
    currentCpuPercent: Float,
    cpuProcesses: List<CpuProcess>
) {
    DashboardPanel(
        title = l10n("CPU 详情", "CPU Details"),
        icon = Icons.Default.Speed
    ) {
        if (compactLayout) {
            CpuSectionContent(
                cpuHistory = cpuHistory,
                currentCpuPercent = currentCpuPercent,
                cpuProcesses = cpuProcesses,
                stacked = true
            )
        } else {
            CpuSectionContent(
                cpuHistory = cpuHistory,
                currentCpuPercent = currentCpuPercent,
                cpuProcesses = cpuProcesses,
                stacked = false
            )
        }
    }
}

@Composable
private fun CpuSectionContent(
    cpuHistory: List<Float>,
    currentCpuPercent: Float,
    cpuProcesses: List<CpuProcess>,
    stacked: Boolean
) {
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CpuTrendChart(cpuHistory, currentCpuPercent)
            CpuTopProcesses(cpuProcesses)
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.weight(1.2f)) {
            CpuTrendChart(cpuHistory, currentCpuPercent)
        }
        Box(modifier = Modifier.weight(1f)) {
            CpuTopProcesses(cpuProcesses)
        }
    }
}

@Composable
private fun CpuTrendChart(cpuHistory: List<Float>, currentCpuPercent: Float) {
    RealtimeLineChart(
        data = cpuHistory,
        modifier = Modifier.fillMaxWidth(),
        label = l10n(
            "当前 CPU: ${formatPercent(currentCpuPercent)}%",
            "Current CPU: ${formatPercent(currentCpuPercent)}%"
        )
    )
}

@Composable
private fun CpuTopProcesses(cpuProcesses: List<CpuProcess>) {
    if (cpuProcesses.isEmpty()) {
        LoadingHint(l10n("正在加载 CPU 进程数据...", "Loading CPU process data..."))
        return
    }
    HorizontalBarChart(
        items = cpuProcesses.map { proc ->
            BarChartItem(
                label = shortProcessName(proc.name),
                value = proc.cpuPercent,
                displayValue = "${formatPercent(proc.cpuPercent)}%"
            )
        },
        barColor = MaterialTheme.colorScheme.secondary
    )
}

@Composable
private fun MemorySection(
    compactLayout: Boolean,
    memHistory: List<Float>,
    memUsedMb: Long,
    memTotalMb: Long,
    memAvailableMb: Long,
    memUsedPercent: Float,
    memProcesses: List<MemProcess>
) {
    DashboardPanel(
        title = l10n("内存详情", "Memory Details"),
        icon = Icons.Default.Memory
    ) {
        if (compactLayout) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MemoryTrendChart(memHistory, memUsedMb, memTotalMb, memUsedPercent)
                MemoryStats(memUsedMb, memTotalMb, memAvailableMb)
                MemoryTopProcesses(memProcesses)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1.2f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MemoryTrendChart(memHistory, memUsedMb, memTotalMb, memUsedPercent)
                        MemoryStats(memUsedMb, memTotalMb, memAvailableMb)
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    MemoryTopProcesses(memProcesses)
                }
            }
        }
    }
}

@Composable
private fun MemoryTrendChart(
    memHistory: List<Float>,
    memUsedMb: Long,
    memTotalMb: Long,
    memUsedPercent: Float
) {
    RealtimeLineChart(
        data = memHistory,
        modifier = Modifier.fillMaxWidth(),
        lineColor = MaterialTheme.colorScheme.secondary,
        fillColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
        label = l10n(
            "已用 ${formatBytesMb(memUsedMb)} / ${formatBytesMb(memTotalMb)} (${formatPercent(memUsedPercent)}%)",
            "Used ${formatBytesMb(memUsedMb)} / ${formatBytesMb(memTotalMb)} (${formatPercent(memUsedPercent)}%)"
        )
    )
}

@Composable
private fun MemoryStats(
    memUsedMb: Long,
    memTotalMb: Long,
    memAvailableMb: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MemoryStat(l10n("已用", "Used"), formatBytesMb(memUsedMb))
        MemoryStat(l10n("总量", "Total"), formatBytesMb(memTotalMb))
        MemoryStat(l10n("可用", "Available"), formatBytesMb(memAvailableMb))
    }
}

@Composable
private fun MemoryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MemoryTopProcesses(
    memProcesses: List<MemProcess>,
    maxItems: Int = 10
) {
    if (memProcesses.isEmpty()) {
        LoadingHint(l10n("正在加载内存进程数据...", "Loading memory process data..."))
        return
    }
    HorizontalBarChart(
        items = memProcesses.map { proc ->
            BarChartItem(
                label = shortProcessName(proc.name),
                value = proc.memMb.toFloat(),
                displayValue = formatBytesMb(proc.memMb)
            )
        },
        maxItems = maxItems,
        barColor = MaterialTheme.colorScheme.tertiary
    )
}

@Composable
private fun StorageSection(
    compactLayout: Boolean,
    storageSummary: PrimaryStorageSummary?,
    storageHistory: List<Float>,
    partitions: List<StoragePartition>,
    apps: List<StorageApp>
) {
    DashboardPanel(
        title = l10n("存储详情", "Storage Details"),
        icon = Icons.Default.Storage
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (storageSummary != null || storageHistory.isNotEmpty()) {
                RealtimeLineChart(
                    data = storageHistory,
                    modifier = Modifier.fillMaxWidth(),
                    lineColor = MaterialTheme.colorScheme.tertiary,
                    fillColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    label = storageSummary?.let {
                        l10n(
                            "${it.mount} ${formatGb(it.usedGb)} / ${formatGb(it.totalGb)} (${formatPercent(it.usedPercent)}%)",
                            "${it.mount} ${formatGb(it.usedGb)} / ${formatGb(it.totalGb)} (${formatPercent(it.usedPercent)}%)"
                        )
                    } ?: l10n("主分区加载中", "Primary partition loading")
                )
            }

            if (compactLayout) {
                StoragePartitionsList(partitions)
                StorageTopApps(apps)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        StoragePartitionsList(partitions)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StorageTopApps(apps)
                    }
                }
            }
        }
    }
}

@Composable
private fun StoragePartitionsList(partitions: List<StoragePartition>) {
    if (partitions.isEmpty()) {
        LoadingHint(l10n("正在加载分区数据...", "Loading partition data..."))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        partitions.forEach { partition ->
            StoragePartitionRow(partition)
        }
    }
}

@Composable
private fun StorageTopApps(
    apps: List<StorageApp>,
    maxItems: Int = 15
) {
    if (apps.isEmpty()) {
        LoadingHint(l10n("正在加载应用体积数据...", "Loading app storage data..."))
        return
    }
    HorizontalBarChart(
        items = apps.map { app ->
            BarChartItem(
                label = shortProcessName(app.name),
                value = app.sizeMb.toFloat(),
                displayValue = formatAppSize(app.sizeMb)
            )
        },
        maxItems = maxItems,
        barColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    )
}

@Composable
private fun StoragePartitionRow(partition: StoragePartition) {
    val usedRatio = if (partition.totalGb > 0) {
        (partition.usedGb / partition.totalGb).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = partition.mount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${formatGb(partition.usedGb)} / ${formatGb(partition.totalGb)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    RoundedCornerShape(999.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(usedRatio)
                    .height(10.dp)
                    .background(
                        when {
                            usedRatio > 0.9f -> MaterialTheme.colorScheme.error
                            usedRatio > 0.75f -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        RoundedCornerShape(999.dp)
                    )
            )
        }

        Text(
            text = l10n(
                "可用 ${formatGb(partition.availGb)}",
                "Available ${formatGb(partition.availGb)}"
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadingHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildOverviewSeries(
    cpuHistory: List<Float>,
    memHistory: List<Float>,
    storageHistory: List<Float>
): List<LineChartSeries> {
    val targetSize = maxOf(cpuHistory.size, memHistory.size, storageHistory.size)
    if (targetSize == 0) return emptyList()

    return listOf(
        LineChartSeries(l10n("CPU", "CPU"), normalizeHistory(cpuHistory, targetSize), Color(0xFF4F8E78)),
        LineChartSeries(l10n("内存", "Memory"), normalizeHistory(memHistory, targetSize), Color(0xFF3B82F6)),
        LineChartSeries(l10n("存储", "Storage"), normalizeHistory(storageHistory, targetSize), Color(0xFFB7791F))
    )
}

private fun normalizeHistory(history: List<Float>, targetSize: Int): List<Float> {
    if (targetSize <= 0 || history.isEmpty()) return history
    if (history.size >= targetSize) return history.takeLast(targetSize)

    val step = targetSize.toFloat() / history.size
    return List(targetSize) { index ->
        val sourceIndex = (index / step).toInt().coerceIn(0, history.lastIndex)
        history[sourceIndex]
    }
}

private fun formatRelativeUpdate(lastUpdatedAtMillis: Long?): String {
    if (lastUpdatedAtMillis == null) return l10n("未刷新", "Not updated")
    val deltaSeconds = ((System.currentTimeMillis() - lastUpdatedAtMillis) / 1000).coerceAtLeast(0)
    return when {
        deltaSeconds < 5 -> l10n("刚刚", "Just now")
        deltaSeconds < 60 -> l10n("${deltaSeconds} 秒前", "${deltaSeconds}s ago")
        deltaSeconds < 3600 -> {
            val minutes = deltaSeconds / 60
            l10n("${minutes} 分钟前", "${minutes}m ago")
        }
        else -> {
            val hours = deltaSeconds / 3600
            l10n("${hours} 小时前", "${hours}h ago")
        }
    }
}

private fun shortProcessName(raw: String): String {
    return raw.substringAfterLast('/').substringAfterLast('.').take(18)
}

private fun formatPercent(value: Float): String = "%.1f".format(value)

private fun formatGb(value: Double): String = "%.1f GB".format(value)

private fun formatBytesMb(value: Long): String {
    return if (value >= 1024L) {
        "%.1f GB".format(value / 1024.0)
    } else {
        "$value MB"
    }
}

private fun formatAppSize(valueMb: Long): String = formatBytesMb(valueMb)
