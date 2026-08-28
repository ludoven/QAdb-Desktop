package com.ludoven.adbtool.pages

import com.ludoven.adbtool.ui.mac.*

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.QadbColors
import com.ludoven.adbtool.QadbPalette
import com.ludoven.adbtool.QadbTokens
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.entity.DeviceMirrorSettings
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.ui.icons.IconParkIcons
import com.ludoven.adbtool.util.copyPlainTextToClipboard
import com.ludoven.adbtool.util.isWirelessAdbConnection
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.viewmodel.DeviceMirrorViewModel
import com.ludoven.adbtool.viewmodel.KeyEventViewModel
import com.ludoven.adbtool.widget.FramedStateSurface

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
    val showMirrorDialog by mirrorViewModel.showDialog.collectAsState()
    val mirrorDialogMessage by mirrorViewModel.dialogMessage.collectAsState()
    val mirrorErrorMessage by mirrorViewModel.mirrorErrorMessage.collectAsState()
    val mirrorErrorText = resolveMessageText(mirrorErrorMessage)
    val hasDevice = !selectedDevice.isNullOrBlank()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var copyToastVisible by remember { mutableStateOf(false) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ControlVisualTokens.Surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(pageScrollState)
                    .padding(horizontal = UiTokens.PagePadding, vertical = UiTokens.ItemSpacing),
                verticalArrangement = Arrangement.spacedBy(UiTokens.SectionSpacing)
            ) {
                if (hasDevice) {
                    DeviceControlHeroCard(
                        deviceDisplayName = selectedDevice?.let { deviceDisplayNames[it] },
                        deviceId = selectedDevice,
                        connectionType = resolveConnectionType(selectedDevice),
                        mirrorRunning = mirrorRunning,
                        settings = settings,
                        errorMessage = mirrorErrorText,
                        onOpenSettings = { showSettingsDialog = true },
                        onCopySerial = { serial ->
                            copyPlainTextToClipboard(serial)
                            copyToastVisible = true
                        },
                        onMirrorAction = {
                            if (mirrorRunning) {
                                mirrorViewModel.stopMirror()
                            } else {
                                mirrorViewModel.openMirror(selectedDevice)
                            }
                        }
                    )
                } else {
                    DeviceControlEmptyState(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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

        MirrorToast(visible = showMirrorDialog, message = mirrorDialogMessage)
        MirrorToast(
            visible = copyToastVisible,
            message = MsgContent.Text(l10n("设备序列号已复制到剪贴板", "Device serial copied to clipboard"))
        )
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

// ── Device Control Hero Card ──────────────────────────────────────────────────

@Composable
private fun DeviceControlHeroCard(
    deviceDisplayName: String?,
    deviceId: String?,
    connectionType: String,
    mirrorRunning: Boolean,
    settings: DeviceMirrorSettings,
    errorMessage: String?,
    onOpenSettings: () -> Unit,
    onCopySerial: (String) -> Unit,
    onMirrorAction: () -> Unit
) {
    val isWireless = connectionType.contains("Wi", ignoreCase = true)

    FramedStateSurface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UiTokens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
        ) {
            // Top Row: Identity + Badges + Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = UiTokens.SpaceMedium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
                ) {
                    // Avatar Box
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(UiTokens.RadiusMedium))
                            .background(ControlVisualTokens.Primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = IconParkIcons.Devices,
                            contentDescription = null,
                            tint = ControlVisualTokens.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = deviceDisplayName?.takeIf { it.isNotBlank() }
                                    ?: deviceId
                                    ?: l10n("未知设备", "Unknown Device"),
                                color = ControlVisualTokens.Text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Status Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(UiTokens.BadgeRadius))
                                    .background(ControlVisualTokens.Success.copy(alpha = 0.12f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(ControlVisualTokens.Success)
                                    )
                                    Text(
                                        text = l10n("就绪", "Ready"),
                                        color = ControlVisualTokens.Success,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Connection Type Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(UiTokens.BadgeRadius))
                                    .background(ControlVisualTokens.Soft.copy(alpha = 0.8f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isWireless) IconParkIcons.Wifi else IconParkIcons.Usb,
                                        contentDescription = null,
                                        tint = ControlVisualTokens.Muted,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = connectionType,
                                        color = ControlVisualTokens.Muted,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Serial number pill with copy
                        if (!deviceId.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(UiTokens.RadiusSmall))
                                    .clickable { onCopySerial(deviceId) }
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = deviceId,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = ControlVisualTokens.Tertiary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = l10n("复制序列号", "Copy Serial"),
                                    tint = ControlVisualTokens.Tertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                // Action button (Open/Stop mirror)
                Button(
                    onClick = onMirrorAction,
                    modifier = Modifier.height(34.dp).widthIn(min = 108.dp),
                    shape = RoundedCornerShape(UiTokens.RadiusMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mirrorRunning) ControlVisualTokens.Danger else ControlVisualTokens.Primary,
                        contentColor = QadbColors.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (mirrorRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = QadbColors.onPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (mirrorRunning) l10n("停止镜像", "Stop Mirror") else l10n("打开镜像", "Open Mirror"),
                        color = QadbColors.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
                    )
                }
            }

            // Error banner if any
            if (!errorMessage.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(UiTokens.RadiusMedium))
                        .background(ControlVisualTokens.Danger.copy(alpha = 0.10f))
                        .border(1.dp, ControlVisualTokens.Danger.copy(alpha = 0.25f), RoundedCornerShape(UiTokens.RadiusMedium))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = IconParkIcons.ShieldAlert,
                        contentDescription = null,
                        tint = ControlVisualTokens.Danger,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = errorMessage,
                        color = ControlVisualTokens.Danger,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Bottom Overview Metrics Row
            HorizontalDivider(color = ControlVisualTokens.Divider.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Metric 1: Mirror Status
                Row(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (mirrorRunning) ControlVisualTokens.Success else ControlVisualTokens.Tertiary)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = l10n("镜像状态", "Mirror Status"),
                            color = ControlVisualTokens.Muted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (mirrorRunning) l10n("运行中 (Scrcpy)", "Running (Scrcpy)") else l10n("未启动", "Stopped"),
                            color = if (mirrorRunning) ControlVisualTokens.Success else ControlVisualTokens.Text,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                OverviewDivider()

                // Metric 2: Connection Type
                Row(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isWireless) IconParkIcons.Wifi else IconParkIcons.Usb,
                        contentDescription = null,
                        tint = ControlVisualTokens.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = l10n("连接协议", "Connection"),
                            color = ControlVisualTokens.Muted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = connectionType,
                            color = ControlVisualTokens.Text,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                OverviewDivider()

                // Metric 3: Quick Settings Cell (Clickable)
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                Row(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(UiTokens.RadiusMedium))
                        .background(
                            if (isHovered) ControlVisualTokens.Soft.copy(alpha = 0.8f)
                            else ControlVisualTokens.Soft.copy(alpha = 0.4f)
                        )
                        .clickable(interactionSource = interactionSource, indication = null, onClick = onOpenSettings)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = IconParkIcons.Setting,
                            contentDescription = null,
                            tint = ControlVisualTokens.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = l10n("镜像画质参数", "Mirror Settings"),
                                color = ControlVisualTokens.Muted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = mirrorSettingsSummary(settings),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = ControlVisualTokens.Text,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = IconParkIcons.Right,
                        contentDescription = null,
                        tint = ControlVisualTokens.Muted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ── Empty State & Illustration ────────────────────────────────────────────────

@Composable
private fun EmptyDeviceControlIllustration() {
    val brand = QadbTokens.brand
    val muted = QadbTokens.textMuted
    val success = QadbTokens.success

    Canvas(modifier = Modifier.size(96.dp, 72.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2f, cap = StrokeCap.Round)

        // 1. Laptop / Desktop Screen on the Left
        val screenW = w * 0.44f
        val screenH = h * 0.54f
        val screenX = w * 0.08f
        val screenY = h * 0.16f

        drawRoundRect(
            color = brand.copy(alpha = 0.85f),
            topLeft = Offset(screenX, screenY),
            size = Size(screenW, screenH),
            cornerRadius = CornerRadius(6f),
            style = stroke
        )
        // Laptop base
        drawLine(
            color = brand.copy(alpha = 0.5f),
            start = Offset(screenX - 8f, screenY + screenH + 6f),
            end = Offset(screenX + screenW + 8f, screenY + screenH + 6f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        // Inner screen playback icon / wireframe
        drawRoundRect(
            color = brand.copy(alpha = 0.15f),
            topLeft = Offset(screenX + 5f, screenY + 5f),
            size = Size(screenW - 10f, screenH - 10f),
            cornerRadius = CornerRadius(3f)
        )

        // 2. Smartphone on the Right
        val phoneW = w * 0.28f
        val phoneH = h * 0.64f
        val phoneX = w * 0.64f
        val phoneY = h * 0.12f

        drawRoundRect(
            color = success.copy(alpha = 0.85f),
            topLeft = Offset(phoneX, phoneY),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(6f),
            style = stroke
        )
        // Phone speaker notch
        drawRoundRect(
            color = success.copy(alpha = 0.5f),
            topLeft = Offset(phoneX + phoneW * 0.32f, phoneY + 4f),
            size = Size(phoneW * 0.36f, 2f),
            cornerRadius = CornerRadius(1f)
        )
        // Phone home line
        drawLine(
            color = success.copy(alpha = 0.5f),
            start = Offset(phoneX + phoneW * 0.3f, phoneY + phoneH - 5f),
            end = Offset(phoneX + phoneW * 0.7f, phoneY + phoneH - 5f),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )

        // 3. Wireless transmission waves / Data packets between Laptop and Phone
        val midY = h * 0.42f
        val startX = screenX + screenW + 4f
        val endX = phoneX - 4f

        drawCircle(
            color = brand.copy(alpha = 0.7f),
            radius = 2.2f,
            center = Offset(startX + (endX - startX) * 0.25f, midY - 6f)
        )
        drawCircle(
            color = brand.copy(alpha = 0.9f),
            radius = 2.8f,
            center = Offset(startX + (endX - startX) * 0.5f, midY)
        )
        drawCircle(
            color = success.copy(alpha = 0.7f),
            radius = 2.2f,
            center = Offset(startX + (endX - startX) * 0.75f, midY + 6f)
        )
    }
}

@Composable
private fun DeviceControlEmptyState(
    modifier: Modifier = Modifier
) {
    FramedStateSurface(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UiTokens.SpaceXXLarge, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
        ) {
            EmptyDeviceControlIllustration()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = l10n("未连接 Android 设备", "No Android Device Connected"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ControlVisualTokens.Text,
                    fontSize = 15.sp
                )
                Text(
                    text = l10n(
                        "通过 USB 数据线或无线调试 (Wi‑Fi) 将设备连接至电脑，即可进行实时屏幕镜像与物理按键协同调试",
                        "Connect your Android device via USB or Wi‑Fi debugging to enable real-time mirror and hardware key controls"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ControlVisualTokens.Muted,
                    fontSize = 12.sp
                )
            }

            // Quick Guidance Checklist
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(UiTokens.RadiusMedium))
                    .background(ControlVisualTokens.Soft.copy(alpha = 0.6f))
                    .border(1.dp, ControlVisualTokens.Border, RoundedCornerShape(UiTokens.RadiusMedium))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GuidanceStepItem(step = "1", text = l10n("开启「USB 调试」", "Enable USB debugging"))
                GuidanceStepDivider()
                GuidanceStepItem(step = "2", text = l10n("授权电脑 ADB 密钥", "Authorize ADB key"))
                GuidanceStepDivider()
                GuidanceStepItem(step = "3", text = l10n("在顶部选择目标设备", "Select device in top bar"))
            }
        }
    }
}

@Composable
private fun GuidanceStepItem(step: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(ControlVisualTokens.Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = ControlVisualTokens.Primary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = text,
            color = ControlVisualTokens.Text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GuidanceStepDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(14.dp)
            .background(ControlVisualTokens.Divider.copy(alpha = 0.6f))
    )
}

// ── Metrics & Helpers ─────────────────────────────────────────────────────────

@Composable
private fun OverviewDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(ControlVisualTokens.Divider.copy(alpha = 0.5f))
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
