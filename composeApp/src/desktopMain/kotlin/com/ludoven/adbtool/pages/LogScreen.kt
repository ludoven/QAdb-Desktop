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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.TooltipArea
import com.ludoven.adbtool.entity.LogEntry
import com.ludoven.adbtool.entity.LogFilter
import com.ludoven.adbtool.entity.LogLevel
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.QadbColors
import com.ludoven.adbtool.QadbTokens
import com.ludoven.adbtool.ui.icons.IconParkIcons
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
import com.ludoven.adbtool.ui.mac.Surface
import com.ludoven.adbtool.ui.mac.Text
import com.ludoven.adbtool.ui.mac.bodyMedium
import com.ludoven.adbtool.ui.mac.bodySmall
import com.ludoven.adbtool.ui.mac.labelLarge
import com.ludoven.adbtool.ui.mac.labelSmall
import com.ludoven.adbtool.ui.mac.titleMedium
import com.ludoven.adbtool.viewmodel.LogViewModel
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.widget.DiagnosticsEmptyKind
import com.ludoven.adbtool.widget.DiagnosticsEmptyState
import com.ludoven.adbtool.widget.FramedStateSurface
import com.ludoven.adbtool.widget.DeviceRequiredState
import com.ludoven.adbtool.widget.InlineStatusBanner
import com.ludoven.adbtool.widget.InlineStatusTone
import org.jetbrains.compose.resources.stringResource
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collect

private enum class QuickKeywordChip {
    CURRENT_APP,
    ERROR,
    WARNING,
    CRASH,
    ANR
}

internal fun logCaptureActionsEnabled(selectedDevice: String?): Boolean =
    !selectedDevice.isNullOrBlank()

@Composable
fun LogScreen(
    viewModel: LogViewModel,
    selectedDevice: String?,
    onRefreshDevices: () -> Unit,
    onOpenWirelessConnection: () -> Unit,
    onOpenTroubleshooting: () -> Unit
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
    var userPinnedToBottom by remember { mutableStateOf(true) }
    var selectedQuickChip by remember { mutableStateOf<QuickKeywordChip?>(null) }
    var selectedLogKey by remember { mutableStateOf<String?>(null) }

    val filteredLogs by viewModel.filteredLogs.collectAsState()
    val likelyCurrentPackage by viewModel.likelyCurrentPackage.collectAsState()
    val captureDevice = selectedDevice?.takeIf { logCaptureActionsEnabled(it) }

    LaunchedEffect(selectedDevice) {
        viewModel.setSelectedDevice(selectedDevice)
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.stopCapture()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            Triple(listState.isScrollInProgress, lastVisibleIndex, layoutInfo.totalItemsCount)
        }.collect { (isScrolling, lastVisibleIndex, totalItemsCount) ->
            if (isScrolling) {
                userPinnedToBottom = totalItemsCount == 0 || lastVisibleIndex >= totalItemsCount - 2
            }
        }
    }

    LaunchedEffect(filteredLogs.size, isCapturing, autoScroll, userPinnedToBottom) {
        if (isCapturing && autoScroll && userPinnedToBottom && filteredLogs.isNotEmpty()) {
            listState.scrollToItem(filteredLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = UiTokens.PagePadding, vertical = UiTokens.SectionSpacingCompact),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
    ) {
        // ── Header: title+subtitle on left, status dot + toolbar on right ────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = UiTokens.SpaceMedium),
                verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)
            ) {
                Text(
                    text = stringResource(Res.string.log_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = UiTokens.TextSection
                )
                Text(
                    text = stringResource(Res.string.log_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status dot + main action
                CaptureStatusDot(isCapturing = isCapturing)
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    onClick = {
                        if (isCapturing) {
                            viewModel.stopCapture()
                        } else if (captureDevice != null) {
                            viewModel.startCapture(captureDevice)
                        }
                    },
                    enabled = (captureDevice != null || isCapturing) && !isLoading,
                    modifier = Modifier.height(34.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = QadbColors.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isCapturing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = QadbColors.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(UiTokens.SpaceXSmall))
                    Text(
                        text = if (isCapturing) stringResource(Res.string.log_pause) else stringResource(Res.string.log_resume),
                        style = MaterialTheme.typography.labelLarge,
                        color = QadbColors.onPrimary
                    )
                }

                // Vertical separator
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Icon-only toolbar buttons
                LogIconToolbarButton(
                    icon = Icons.Default.DeleteSweep,
                    tooltip = stringResource(Res.string.log_clear),
                    onClick = { viewModel.clearLogs() }
                )
                LogIconToolbarButton(
                    icon = IconParkIcons.Download,
                    tooltip = stringResource(Res.string.log_export),
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
                LogIconToolbarButton(
                    icon = Icons.Default.AutoAwesomeMotion,
                    tooltip = stringResource(Res.string.log_auto_scroll),
                    isActive = autoScroll,
                    onClick = {
                        autoScroll = !autoScroll
                        if (autoScroll) userPinnedToBottom = true
                    }
                )

                Box {
                    LogIconToolbarButton(
                        icon = IconParkIcons.More,
                        tooltip = stringResource(Res.string.log_more),
                        onClick = { showMoreMenu = true }
                    )
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.log_more_restart)) },
                            leadingIcon = {
                                Icon(IconParkIcons.Refresh, contentDescription = null)
                            },
                            enabled = captureDevice != null && !isLoading,
                            onClick = {
                                if (captureDevice != null) viewModel.restartCapture(captureDevice)
                                showMoreMenu = false
                            }
                        )
                    }
                }
            }
        }

        // ── Filter zone: open layout, no Card wrapping ───────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterInput(
                    value = filter.keyword,
                    onValueChange = {
                        viewModel.updateFilter(filter.copy(keyword = it))
                        selectedQuickChip = null
                    },
                    placeholder = stringResource(Res.string.log_filter_keyword),
                    leadingIcon = IconParkIcons.Search,
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

                // Clear-filter: round icon button
                val filterActive = filter != LogFilter()
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(UiTokens.BadgeRadius))
                        .background(
                            if (filterActive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(UiTokens.BadgeRadius))
                        .clickable(enabled = filterActive) {
                            selectedQuickChip = null
                            viewModel.updateFilter(LogFilter())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FilterAltOff,
                        contentDescription = stringResource(Res.string.log_clear_filter),
                        tint = if (filterActive) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Quick-filter chips row
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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

        // ── No-device warning banner ─────────────────────────────────────────────
        if (captureDevice == null && !isCapturing) {
            InlineStatusBanner(
                text = stringResource(Res.string.log_no_device),
                tone = InlineStatusTone.Warning,
                icon = IconParkIcons.Refresh
            )
        }

        // ── Log table (FramedStateSurface = transparent bg + thin border) ────────
        FramedStateSurface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LogTableHeader()
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f))

                val emptyKind = when {
                    captureDevice == null && !isCapturing -> DiagnosticsEmptyKind.NoDevice
                    isCapturing && filteredLogs.isEmpty() -> DiagnosticsEmptyKind.WaitingForLogs
                    filter != LogFilter() && filteredLogs.isEmpty() -> DiagnosticsEmptyKind.NoMatchingLogs
                    else -> null
                }

                if (emptyKind != null || filteredLogs.isEmpty()) {
                    val kind = emptyKind ?: DiagnosticsEmptyKind.NoDevice
                    if (kind == DiagnosticsEmptyKind.NoDevice) {
                        DeviceRequiredState(
                            title = stringResource(Res.string.log_empty),
                            description = l10n(
                                "连接并选择设备后即可开始捕获日志。",
                                "Connect and select a device to start capturing logs."
                            ),
                            onRefreshDevices = onRefreshDevices,
                            onOpenWirelessConnection = onOpenWirelessConnection,
                            onOpenTroubleshooting = onOpenTroubleshooting,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val titleRes = when (kind) {
                            DiagnosticsEmptyKind.WaitingForLogs -> stringResource(Res.string.log_waiting)
                            else -> stringResource(Res.string.log_empty)
                        }
                        val desc = when (kind) {
                            DiagnosticsEmptyKind.NoDevice -> "选择设备并启动捕获后，日志会显示在这里。"
                            DiagnosticsEmptyKind.WaitingForLogs -> "日志捕获已启动，等待设备输出新日志。"
                            DiagnosticsEmptyKind.NoMatchingLogs -> "当前筛选条件没有匹配日志，清除筛选后可查看完整输出。"
                            DiagnosticsEmptyKind.EmptyProcessList -> ""
                        }
                        DiagnosticsEmptyState(
                            kind = kind,
                            title = titleRes,
                            description = desc,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SelectionContainer {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = UiTokens.SpaceSmall),
                                state = listState
                            ) {
                                itemsIndexed(filteredLogs, key = { index, item -> "${item.timestamp}_${item.pid}_${index}" }) { index, entry ->
                                    val rowKey = "${entry.timestamp}_${entry.pid}_$index"
                                    LogTableRow(
                                        entry = entry,
                                        dateFormat = dateFormat,
                                        isSelected = selectedLogKey == rowKey,
                                        onClick = { selectedLogKey = rowKey }
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(listState),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(end = UiTokens.SpaceXSmall)
                                .padding(vertical = UiTokens.SpaceSmall)
                        )
                    }
                }
            }
        }

        // ── Error toast ──────────────────────────────────────────────────────────
        errorMessage?.let { error ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = UiTokens.SpaceMedium, vertical = UiTokens.SpaceSmall),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    IconButton(onClick = { viewModel.clearError() }, modifier = Modifier.width(24.dp)) {
                        Icon(IconParkIcons.Close, contentDescription = stringResource(Res.string.log_dismiss))
                    }
                }
            }
        }
    }
}

// ── Capture status dot ────────────────────────────────────────────────────────
@Composable
private fun CaptureStatusDot(isCapturing: Boolean) {
    val dotColor = if (isCapturing) QadbTokens.brand else QadbTokens.textMuted
    val transition = rememberInfiniteTransition(label = "dot")
    val pulseAlpha by if (isCapturing) {
        transition.animateFloat(
            initialValue = 0.35f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "alpha"
        )
    } else {
        remember { mutableStateOf(0.55f) }
    }

    Canvas(modifier = Modifier.size(8.dp)) {
        drawCircle(color = dotColor.copy(alpha = pulseAlpha))
    }
}

// ── Icon-only toolbar button with tooltip ────────────────────────────────────
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun LogIconToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tooltip: String,
    onClick: () -> Unit,
    isActive: Boolean = false
) {
    TooltipArea(
        tooltip = {
            Surface(
                shape = RoundedCornerShape(UiTokens.RadiusSmall),
                color = MaterialTheme.colorScheme.inverseSurface
            ) {
                Text(
                    text = tooltip,
                    modifier = Modifier.padding(horizontal = UiTokens.SpaceSmall, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        },
        delayMillis = 350,
        tooltipPlacement = androidx.compose.foundation.TooltipPlacement.CursorPoint(
            offset = androidx.compose.ui.unit.DpOffset(0.dp, 10.dp)
        )
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(UiTokens.RadiusMedium))
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else Color.Transparent
                )
                .border(
                    1.dp,
                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    else Color.Transparent,
                    RoundedCornerShape(UiTokens.RadiusMedium)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                tint = if (isActive) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

// ── Filter controls ───────────────────────────────────────────────────────────
@Composable
private fun FilterSelect(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(UiTokens.RadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(UiTokens.RadiusMedium))
            .clickable { onClick() }
            .padding(horizontal = UiTokens.SpaceSmall),
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
        Icon(IconParkIcons.ArrowDown, contentDescription = null, modifier = Modifier.size(13.dp))
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
            .clip(RoundedCornerShape(UiTokens.RadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(UiTokens.RadiusMedium))
            .padding(horizontal = UiTokens.SpaceSmall),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(UiTokens.SpaceSmall))
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
    val shape = RoundedCornerShape(UiTokens.BadgeRadius)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
            )
            .border(
                1.dp,
                if (active) QadbColors.selectedBorder else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f),
                shape
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = UiTokens.SpaceSmall, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)
                active -> QadbColors.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

// ── Log table ─────────────────────────────────────────────────────────────────
@Composable
private fun LogTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
            .padding(horizontal = UiTokens.SpaceSmall, vertical = 6.dp),
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
        text = text.uppercase(),
        modifier = resolved,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            letterSpacing = 0.6.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun LogTableRow(
    entry: LogEntry,
    dateFormat: SimpleDateFormat,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val rowBackground = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
        entry.level == LogLevel.ERROR || entry.level == LogLevel.FATAL -> QadbColors.errorSurface
        entry.level == LogLevel.WARN -> QadbColors.warningSurface
        hovered -> QadbColors.surfaceHover
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .hoverable(interactionSource = interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = UiTokens.SpaceSmall, vertical = UiTokens.SpaceSmall),
        verticalAlignment = Alignment.Top
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(UiTokens.IndicatorWidth)
                    .fillMaxHeight()
                    .padding(end = UiTokens.SpaceSmall)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(UiTokens.RadiusSmall)
                    )
            )
        }
        TableCell(text = dateFormat.format(Date(entry.timestamp)), width = 108.dp, mono = true)
        LevelBadge(level = entry.level, modifier = Modifier.width(48.dp))
        TableCell(text = if (entry.pid > 0) entry.pid.toString() else "-", width = 58.dp, mono = true)
        TableCell(text = if (entry.tid > 0) entry.tid.toString() else "-", width = 58.dp, mono = true)
        TableCell(text = entry.message, modifier = Modifier.weight(1f), mono = true, maxLines = Int.MAX_VALUE)
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
        modifier = resolved.padding(end = UiTokens.SpaceSmall),
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.6.sp),
        fontFamily = if (mono) FontFamily.Monospace else null,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = maxLines,
        overflow = if (maxLines == 1) TextOverflow.Ellipsis else TextOverflow.Clip
    )
}

// ── Level badge — semantic colour mapping via QadbTokens ──────────────────────
@Composable
private fun LevelBadge(level: LogLevel, modifier: Modifier = Modifier) {
    // Resolve all @Composable color properties up front (inside @Composable context)
    val brandColor      = QadbTokens.brand
    val mutedColor      = QadbTokens.textMuted
    val successBg       = QadbTokens.successContainer
    val successFg       = QadbTokens.successText
    val warningBg       = QadbTokens.warningContainer
    val warningFg       = QadbTokens.warningText
    val dangerBg        = QadbTokens.dangerContainer
    val dangerFg        = QadbTokens.dangerText
    val dangerSolid     = QadbTokens.danger

    val bg = when (level) {
        LogLevel.VERBOSE -> mutedColor.copy(alpha = 0.12f)
        LogLevel.DEBUG   -> brandColor.copy(alpha = 0.14f)
        LogLevel.INFO    -> successBg
        LogLevel.WARN    -> warningBg
        LogLevel.ERROR   -> dangerBg
        LogLevel.FATAL   -> dangerSolid
    }
    val fg = when (level) {
        LogLevel.VERBOSE -> mutedColor
        LogLevel.DEBUG   -> brandColor
        LogLevel.INFO    -> successFg
        LogLevel.WARN    -> warningFg
        LogLevel.ERROR   -> dangerFg
        LogLevel.FATAL   -> Color.White
    }
    val label = when (level) {
        LogLevel.VERBOSE -> "V"
        LogLevel.DEBUG   -> "D"
        LogLevel.INFO    -> "I"
        LogLevel.WARN    -> "W"
        LogLevel.ERROR   -> "E"
        LogLevel.FATAL   -> "F"
    }

    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(UiTokens.RadiusSmall))
                .background(bg)
                .padding(horizontal = 6.dp, vertical = UiTokens.SpaceXSmall)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}
