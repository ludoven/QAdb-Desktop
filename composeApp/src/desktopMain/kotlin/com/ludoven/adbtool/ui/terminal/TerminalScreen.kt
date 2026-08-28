package com.ludoven.adbtool.ui.terminal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.QadbPalette
import com.ludoven.adbtool.QadbTokens
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.domain.terminal.TerminalMode
import com.ludoven.adbtool.ui.icons.IconParkIcons
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.Surface
import com.ludoven.adbtool.ui.mac.Text
import com.ludoven.adbtool.util.copyPlainTextToClipboard
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.viewmodel.TerminalViewModel
import com.ludoven.adbtool.widget.FramedStateSurface
import com.ludoven.adbtool.widget.InlineStatusBanner
import com.ludoven.adbtool.widget.InlineStatusTone
import kotlinx.coroutines.launch

private object TerminalUiTokens {
    val Surface: Color @Composable get() = QadbTokens.bg1
    val SurfaceSoft: Color @Composable get() = QadbTokens.bg3
    val Border: Color @Composable get() = QadbTokens.border
    val Text: Color @Composable get() = QadbTokens.textPrimary
    val TextMuted: Color @Composable get() = QadbTokens.textMuted
    val Primary: Color @Composable get() = QadbTokens.brand
    val Success: Color @Composable get() = QadbTokens.success
    val Warning: Color @Composable get() = QadbTokens.warning
    val Danger: Color @Composable get() = QadbTokens.danger
}

/**
 * Modern Developer-Grade Terminal Screen.
 *
 * Provides a unified workbench experience with:
 * 1. Control Console Header (Active device pill, terminal mode badge, search filter & action tools)
 * 2. Quick Command Snippets Bar (Frequent ADB commands for fast debugging)
 * 3. macOS Window Console with Syntax Highlighting and Vector Empty State
 */
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
    var showCopyToast by remember { mutableStateOf(false) }

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

    val deviceLabel = formatDeviceLabel(session.deviceId, session.deviceDisplayNames)
    val prompt = when (session.mode) {
        TerminalMode.QADB -> "qadb:$deviceLabel ❯"
        TerminalMode.ADB_SHELL -> "device:$deviceLabel $"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = UiTokens.PagePadding, vertical = UiTokens.ItemSpacing),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Console Workbench Header
        ConsoleWorkbenchHeader(
            deviceLabel = deviceLabel,
            deviceMissing = session.deviceId.isNullOrBlank(),
            mode = session.mode,
            isRunning = session.isRunning,
            searchQuery = session.searchQuery,
            matchCount = if (session.searchQuery.isNotBlank()) filteredLines.size else null,
            canCopy = filteredLines.isNotEmpty(),
            canClear = filteredLines.isNotEmpty(),
            isFollowing = session.followOutput,
            onSearchChange = viewModel::updateSearchQuery,
            onClear = viewModel::clearLogs,
            onInterrupt = viewModel::interruptCommand,
            onToggleFollow = {
                viewModel.setFollowOutput(!session.followOutput)
                coroutineScope.launch {
                    if (!session.followOutput) listState.scrollToItem(filteredLines.size)
                }
            },
            onCopy = {
                copyPlainTextToClipboard(filteredLines.joinToString("\n") { it.text })
                showCopyToast = true
            }
        )

        // 2. Quick Command Snippets Bar
        TerminalQuickSnippetsBar(
            onSnippetClick = { snippetCmd ->
                viewModel.updateCommandInput(snippetCmd)
            }
        )

        // 3. Modern Terminal Output Console
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
            sessionTitle = when (session.mode) {
                TerminalMode.QADB -> "qadb@terminal: ~$deviceLabel"
                TerminalMode.ADB_SHELL -> "shell@android: $deviceLabel"
            },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        // 4. Missing Device Warning
        if (session.deviceId.isNullOrBlank()) {
            InlineStatusBanner(
                text = l10n(
                    "当前未连接或未选中 Android 设备。执行设备相关命令需要先连接并在顶部选择目标设备。",
                    "No Android device is selected. Device-scoped commands require a connected and selected device."
                ),
                tone = InlineStatusTone.Warning
            )
        }
    }
}

// ── Console Workbench Header ──────────────────────────────────────────────────

@Composable
private fun ConsoleWorkbenchHeader(
    deviceLabel: String,
    deviceMissing: Boolean,
    mode: TerminalMode,
    isRunning: Boolean,
    searchQuery: String,
    matchCount: Int?,
    canCopy: Boolean,
    canClear: Boolean,
    isFollowing: Boolean,
    onSearchChange: (String) -> Unit,
    onClear: () -> Unit,
    onInterrupt: () -> Unit,
    onToggleFollow: () -> Unit,
    onCopy: () -> Unit
) {
    FramedStateSurface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Icon + Title + Device Pill + Mode Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(UiTokens.RadiusSmall))
                        .background(TerminalUiTokens.Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconParkIcons.Terminal,
                        contentDescription = null,
                        tint = TerminalUiTokens.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = l10n("ADB 终端", "ADB Terminal"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = TerminalUiTokens.Text
                        )

                        // Mode Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(UiTokens.BadgeRadius))
                                .background(
                                    if (mode == TerminalMode.ADB_SHELL) TerminalUiTokens.Warning.copy(alpha = 0.12f)
                                    else TerminalUiTokens.Primary.copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 6.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = if (mode == TerminalMode.ADB_SHELL) "ADB Shell" else "QADB Native",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (mode == TerminalMode.ADB_SHELL) TerminalUiTokens.Warning else TerminalUiTokens.Primary
                            )
                        }
                    }

                    // Device Status Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (deviceMissing) TerminalUiTokens.Warning else TerminalUiTokens.Success)
                        )
                        Text(
                            text = if (deviceMissing) l10n("未选择设备", "No Device Selected") else deviceLabel,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            color = if (deviceMissing) TerminalUiTokens.Warning else TerminalUiTokens.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right: Search Input + Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input Field
                TerminalSearchBox(
                    query = searchQuery,
                    matchCount = matchCount,
                    onQueryChange = onSearchChange
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(TerminalUiTokens.Border)
                )

                // Toolbar Actions
                TerminalToolButton(
                    tooltip = l10n("复制全部 (⌘+Shift+C)", "Copy all (⌘+Shift+C)"),
                    icon = Icons.Default.ContentCopy,
                    enabled = canCopy,
                    onClick = onCopy
                )
                TerminalToolButton(
                    tooltip = l10n("清空控制台 (⌘L)", "Clear console (⌘L)"),
                    icon = Icons.Default.DeleteSweep,
                    enabled = canClear,
                    onClick = onClear
                )
                TerminalToolButton(
                    tooltip = if (isFollowing) l10n("跟随输出中 · 点击暂停", "Following output · click to pause")
                    else l10n("滚动跟随输出", "Follow output"),
                    icon = Icons.Default.ArrowDownward,
                    enabled = true,
                    selected = isFollowing,
                    onClick = onToggleFollow
                )
                TerminalToolButton(
                    tooltip = l10n("终止命令 (⌘C)", "Interrupt command (⌘C)"),
                    icon = Icons.Default.StopCircle,
                    enabled = isRunning,
                    danger = true,
                    onClick = onInterrupt
                )
            }
        }
    }
}

// ── Search Box ────────────────────────────────────────────────────────────────

@Composable
private fun TerminalSearchBox(
    query: String,
    matchCount: Int?,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .widthIn(min = 160.dp, max = 220.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(UiTokens.RadiusSmall))
            .background(TerminalUiTokens.SurfaceSoft)
            .border(1.dp, TerminalUiTokens.Border, RoundedCornerShape(UiTokens.RadiusSmall))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = TerminalUiTokens.TextMuted,
            modifier = Modifier.size(13.dp)
        )

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            cursorBrush = SolidColor(TerminalUiTokens.Primary),
            textStyle = TextStyle(
                color = TerminalUiTokens.Text,
                fontSize = 11.5.sp
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = l10n("过滤输出…", "Filter…"),
                        fontSize = 11.5.sp,
                        color = TerminalUiTokens.TextMuted.copy(alpha = 0.6f)
                    )
                }
                inner()
            }
        )

        if (query.isNotEmpty()) {
            if (matchCount != null) {
                Text(
                    text = "${matchCount}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalUiTokens.Primary
                )
            }
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = l10n("清空", "Clear"),
                tint = TerminalUiTokens.TextMuted,
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .clickable { onQueryChange("") }
            )
        }
    }
}

// ── Quick Command Snippets Bar ────────────────────────────────────────────────

private data class SnippetItem(val label: String, val command: String, val desc: String)

private val DefaultSnippets = listOf(
    SnippetItem("devices", "adb devices -l", "列出在线设备"),
    SnippetItem("getprop", "adb shell getprop ro.build.version.release", "系统版本号"),
    SnippetItem("packages", "adb shell pm list packages -3", "第三方包名"),
    SnippetItem("battery", "adb shell dumpsys battery", "电池电量状态"),
    SnippetItem("top", "adb shell top -m 5 -n 1", "CPU/内存排行"),
    SnippetItem("ip", "adb shell ip -f inet addr show wlan0", "WLAN IP"),
    SnippetItem("shell", "shell", "进入交互 Shell"),
    SnippetItem("help", "help", "终端帮助手册")
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TerminalQuickSnippetsBar(
    onSnippetClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = l10n("快捷命令:", "Snippets:"),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TerminalUiTokens.TextMuted,
            modifier = Modifier.padding(end = 2.dp)
        )

        DefaultSnippets.forEach { snippet ->
            val interactionSource = remember { MutableInteractionSource() }
            val hovered by interactionSource.collectIsHoveredAsState()

            val bg by animateColorAsState(
                targetValue = if (hovered) TerminalUiTokens.Primary.copy(alpha = 0.12f) else TerminalUiTokens.SurfaceSoft.copy(alpha = 0.8f),
                animationSpec = tween(150),
                label = "snippetBg"
            )
            val borderCol by animateColorAsState(
                targetValue = if (hovered) TerminalUiTokens.Primary.copy(alpha = 0.35f) else TerminalUiTokens.Border,
                animationSpec = tween(150),
                label = "snippetBorder"
            )

            TooltipArea(
                tooltip = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, TerminalUiTokens.Border)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Text(
                                text = snippet.desc,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = QadbPalette.TerminalText
                            )
                            Text(
                                text = snippet.command,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                color = QadbPalette.TerminalBlue
                            )
                        }
                    }
                },
                delayMillis = 250,
                tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(UiTokens.RadiusSmall))
                        .background(bg)
                        .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(UiTokens.RadiusSmall))
                        .clickable(interactionSource = interactionSource, indication = null) {
                            onSnippetClick(snippet.command)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = snippet.label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hovered) TerminalUiTokens.Primary else TerminalUiTokens.Text
                    )
                }
            }
        }
    }
}

// ── Action Toolbar Button ─────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TerminalToolButton(
    tooltip: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    selected: Boolean = false,
    danger: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val tint = when {
        !enabled -> TerminalUiTokens.TextMuted.copy(alpha = 0.35f)
        danger -> TerminalUiTokens.Danger
        selected -> TerminalUiTokens.Primary
        else -> TerminalUiTokens.Text
    }

    val background by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            selected -> TerminalUiTokens.Primary.copy(alpha = 0.16f)
            danger -> TerminalUiTokens.Danger.copy(alpha = 0.12f)
            hovered -> TerminalUiTokens.SurfaceSoft
            else -> Color.Transparent
        },
        animationSpec = tween(120),
        label = "toolBg"
    )

    val borderCol by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            selected -> TerminalUiTokens.Primary.copy(alpha = 0.4f)
            danger -> TerminalUiTokens.Danger.copy(alpha = 0.3f)
            hovered -> TerminalUiTokens.Border
            else -> Color.Transparent
        },
        animationSpec = tween(120),
        label = "toolBorder"
    )

    TooltipArea(
        tooltip = {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, TerminalUiTokens.Border)
            ) {
                Text(
                    text = tooltip,
                    fontSize = 11.sp,
                    color = QadbPalette.TerminalText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        },
        delayMillis = 300,
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 10.dp))
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(UiTokens.RadiusSmall))
                .background(background)
                .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(UiTokens.RadiusSmall))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

private fun formatDeviceLabel(deviceId: String?, names: Map<String, String>): String {
    if (deviceId.isNullOrBlank()) return "no-device"
    val name = names[deviceId]
    return if (name.isNullOrBlank()) deviceId else name
}
