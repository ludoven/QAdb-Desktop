package com.ludoven.adbtool.pages

import com.ludoven.adbtool.ui.mac.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.QadbColors
import com.ludoven.adbtool.QadbPalette
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.entity.DeviceMirrorSettings
import com.ludoven.adbtool.ui.icons.IconParkIcons
import com.ludoven.adbtool.util.isWirelessAdbConnection
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.viewmodel.DeviceMirrorViewModel
import com.ludoven.adbtool.viewmodel.KeyEventViewModel

enum class DeviceControlTab {
    Mirror,
    Keys
}

@Composable
fun DeviceControlScreen(
    mirrorViewModel: DeviceMirrorViewModel,
    keyEventViewModel: KeyEventViewModel,
    selectedDevice: String?,
    deviceDisplayNames: Map<String, String>,
    @Suppress("UNUSED_PARAMETER")
    initialTab: DeviceControlTab = DeviceControlTab.Mirror
) {
    val pageScrollState = rememberScrollState()
    val mirrorRunning by mirrorViewModel.mirrorRunning.collectAsState()
    val settings by mirrorViewModel.settings.collectAsState()
    val hasDevice = !selectedDevice.isNullOrBlank()
    var showSettingsDialog by remember { mutableStateOf(false) }

    val maxSizeOptions = listOf(
        IntOption("720", 720),
        IntOption("1080", 1080),
        IntOption("1280", 1280),
        IntOption("1920", 1920),
        IntOption(l10n("不限制", "No limit"), null)
    )
    val maxFpsOptions = listOf(
        IntOption("30", 30),
        IntOption("45", 45),
        IntOption("60", 60),
        IntOption("90", 90),
        IntOption("120", 120)
    )
    val bitRateOptions = listOf(
        StringOption("4M", "4M"),
        StringOption("8M", "8M"),
        StringOption("12M", "12M"),
        StringOption("16M", "16M"),
        StringOption("24M", "24M")
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(pageScrollState)
                .padding(horizontal = UiTokens.PagePadding, vertical = UiTokens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SectionSpacing)
        ) {
            DeviceOverviewCard(
                deviceDisplayName = selectedDevice?.let { deviceDisplayNames[it] },
                deviceId = selectedDevice,
                connectionType = resolveConnectionType(selectedDevice),
                mirrorRunning = mirrorRunning,
                settings = settings,
                onOpenSettings = { showSettingsDialog = true },
                onMirrorAction = {
                    if (mirrorRunning) {
                        mirrorViewModel.stopMirror()
                    } else {
                        mirrorViewModel.openMirror(selectedDevice)
                    }
                }
            )

            KeyEventScreen(
                viewModel = keyEventViewModel,
                selectedDevice = selectedDevice,
                deviceDisplayNames = deviceDisplayNames,
                modifier = Modifier.fillMaxWidth(),
                embedded = true,
                showHeader = false,
                showConnectionWarning = false,
                useInternalScroll = false
            )
        }
    }

    if (showSettingsDialog) {
        MirrorSettingsDialog(
            settings = settings,
            maxSizeOptions = maxSizeOptions,
            maxFpsOptions = maxFpsOptions,
            bitRateOptions = bitRateOptions,
            onSelectProfile = mirrorViewModel::selectProfile,
            onUpdateSettings = mirrorViewModel::updateSettings,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun DeviceOverviewCard(
    deviceDisplayName: String?,
    deviceId: String?,
    connectionType: String,
    mirrorRunning: Boolean,
    settings: DeviceMirrorSettings,
    onOpenSettings: () -> Unit,
    onMirrorAction: () -> Unit
) {
    val hasDevice = !deviceId.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UiTokens.RadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = UiTokens.BorderWidth,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(UiTokens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = l10n("设备概览", "Device overview"),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = UiTokens.TextSection
                )
                Button(
                    onClick = onMirrorAction,
                    enabled = mirrorRunning || hasDevice,
                    modifier = Modifier.height(UiTokens.ControlHeight),
                    shape = RoundedCornerShape(UiTokens.RadiusMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                ) {
                    Icon(
                        imageVector = if (mirrorRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(UiTokens.IconSmall)
                    )
                    Spacer(Modifier.width(UiTokens.SpaceSmall))
                    Text(
                        text = if (mirrorRunning) {
                            l10n("停止镜像", "Stop Mirror")
                        } else {
                            l10n("打开镜像", "Open Mirror")
                        },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = UiTokens.TextBody
                    )
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth >= 720.dp) {
                    // Wide: single row with 4 groups
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DeviceIdentityCell(
                            modifier = Modifier.weight(1.4f),
                            deviceDisplayName = deviceDisplayName,
                            deviceId = deviceId,
                            hasDevice = hasDevice
                        )
                        OverviewDivider()
                        OverviewMetric(
                            modifier = Modifier.weight(0.75f),
                            icon = IconParkIcons.Wifi,
                            label = l10n("连接方式", "Connection"),
                            value = if (hasDevice) connectionType else "--"
                        )
                        OverviewDivider()
                        OverviewMetric(
                            modifier = Modifier.weight(0.75f),
                            icon = IconParkIcons.CastScreen,
                            label = l10n("镜像状态", "Mirror status"),
                            value = if (mirrorRunning) l10n("已启动", "Running") else l10n("未启动", "Not started")
                        )
                        OverviewDivider()
                        MirrorSettingsCell(
                            modifier = Modifier.weight(1.1f),
                            summary = mirrorSettingsSummary(settings),
                            onClick = onOpenSettings
                        )
                    }
                } else {
                    // Compact: 2x2 grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DeviceIdentityCell(
                                modifier = Modifier.weight(1f),
                                deviceDisplayName = deviceDisplayName,
                                deviceId = deviceId,
                                hasDevice = hasDevice
                            )
                            OverviewMetric(
                                modifier = Modifier.weight(1f),
                                icon = IconParkIcons.Wifi,
                                label = l10n("连接方式", "Connection"),
                                value = if (hasDevice) connectionType else "--"
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OverviewMetric(
                                modifier = Modifier.weight(1f),
                                icon = IconParkIcons.CastScreen,
                                label = l10n("镜像状态", "Mirror status"),
                                value = if (mirrorRunning) l10n("已启动", "Running") else l10n("未启动", "Not started")
                            )
                            MirrorSettingsCell(
                                modifier = Modifier.weight(1f),
                                summary = mirrorSettingsSummary(settings),
                                onClick = onOpenSettings
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceIdentityCell(
    deviceDisplayName: String?,
    deviceId: String?,
    hasDevice: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(UiTokens.RadiusMedium)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = IconParkIcons.Devices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(UiTokens.SpaceMedium))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = deviceDisplayName?.takeIf { it.isNotBlank() }
                        ?: deviceId
                        ?: l10n("未选择设备", "No device selected"),
                    modifier = Modifier.weight(1f, fill = false),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = UiTokens.TextBodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (hasDevice) {
                    Spacer(Modifier.width(UiTokens.SpaceSmall))
                    Text(
                        text = l10n("已连接", "Connected"),
                        modifier = Modifier
                            .background(
                                color = QadbPalette.Green.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(UiTokens.RadiusSmall)
                            )
                            .padding(horizontal = UiTokens.SpaceSmall, vertical = UiTokens.SpaceXSmall),
                        color = QadbPalette.Green,
                        fontSize = UiTokens.TextCaption,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Text(
                text = deviceId ?: "--",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = UiTokens.TextBody,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MirrorSettingsCell(
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background by animateColorAsState(
        targetValue = if (hovered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else Color.Transparent,
        animationSpec = tween(UiTokens.HoverDurationMillis),
        label = "settingsCellBg"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(UiTokens.RadiusMedium))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = UiTokens.SpaceMedium, vertical = UiTokens.SpaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = IconParkIcons.Setting,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(UiTokens.IconLarge)
        )
        Spacer(Modifier.width(UiTokens.SpaceMedium))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)
        ) {
            Text(
                text = l10n("镜像设置", "Mirror settings"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = UiTokens.TextBody
            )
            Text(
                text = summary,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = UiTokens.TextBody,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = IconParkIcons.Right,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(UiTokens.IconMedium)
        )
    }
}

@Composable
private fun OverviewMetric(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = UiTokens.SpaceMedium, vertical = UiTokens.SpaceSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(UiTokens.IconLarge)
        )
        Spacer(Modifier.width(UiTokens.SpaceMedium))
        Column(verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = UiTokens.TextBody,
                maxLines = 1
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = UiTokens.TextBody,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OverviewDivider() {
    Box(
        modifier = Modifier
            .width(UiTokens.BorderWidth)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    )
}

@Composable
private fun mirrorSettingsSummary(settings: DeviceMirrorSettings): String {
    val size = settings.maxSize?.toString() ?: l10n("原始", "Original")
    return "$size × ${settings.maxFps}fps · ${settings.videoBitRate}"
}

private fun resolveConnectionType(deviceId: String?): String {
    if (deviceId.isNullOrBlank()) return "--"
    return if (isWirelessAdbConnection(deviceId)) l10n("Wi‑Fi", "Wi‑Fi") else "USB"
}
