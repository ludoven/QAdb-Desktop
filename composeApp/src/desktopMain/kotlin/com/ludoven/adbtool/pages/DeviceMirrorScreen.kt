package com.ludoven.adbtool.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontFamily
import com.ludoven.adbtool.QadbTokens
import com.ludoven.adbtool.widget.FramedStateSurface
import com.ludoven.adbtool.entity.DeviceMirrorSettings
import com.ludoven.adbtool.QadbColors
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.ui.icons.IconParkIcons
import com.ludoven.adbtool.entity.MirrorLaunchProfile
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.ui.icons.CompatIconVectors
import com.ludoven.adbtool.ui.mac.Button
import com.ludoven.adbtool.ui.mac.ButtonDefaults
import com.ludoven.adbtool.ui.mac.DropdownMenu
import com.ludoven.adbtool.ui.mac.DropdownMenuItem
import com.ludoven.adbtool.ui.mac.HorizontalDivider
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.IconButton
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.OutlinedButton
import com.ludoven.adbtool.ui.mac.Surface
import com.ludoven.adbtool.ui.mac.Switch
import com.ludoven.adbtool.ui.mac.Text
import com.ludoven.adbtool.ui.mac.TextButton
import com.ludoven.adbtool.ui.mac.bodyMedium
import com.ludoven.adbtool.ui.mac.bodySmall
import com.ludoven.adbtool.ui.mac.headlineSmall
import com.ludoven.adbtool.ui.mac.titleMedium
import com.ludoven.adbtool.util.isWirelessAdbConnection
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.viewmodel.DeviceMirrorViewModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private object MirrorKeyCode {
    const val BACK = 4
    const val HOME = 3
    const val RECENT = 187
    const val POWER = 26
    const val VOLUME_UP = 24
    const val VOLUME_DOWN = 25
}

internal object ControlVisualTokens {
    val Primary: Color @Composable get() = QadbTokens.brand
    val BrandAction: Color @Composable get() = QadbTokens.brandAction
    val BrandSoft: Color @Composable get() = QadbTokens.brandSoft
    val Text: Color @Composable get() = QadbTokens.textPrimary
    val Muted: Color @Composable get() = QadbTokens.textSecondary
    val Tertiary: Color @Composable get() = QadbTokens.textTertiary
    val Border: Color @Composable get() = QadbTokens.border
    val BorderStrong: Color @Composable get() = QadbTokens.borderStrong
    val Divider: Color @Composable get() = QadbTokens.divider
    val Surface: Color @Composable get() = QadbTokens.bg1
    val Soft: Color @Composable get() = QadbTokens.bg2
    val Inset: Color @Composable get() = QadbTokens.bg3
    val Selected: Color @Composable get() = QadbTokens.brandSoft
    val Success: Color @Composable get() = QadbTokens.success
    val Warning: Color @Composable get() = QadbTokens.warning
    val Danger: Color @Composable get() = QadbTokens.danger
}

private data class MirrorColorsData(
    val PageBackground: Color,
    val ContentBackground: Color,
    val PrimaryText: Color,
    val SecondaryText: Color,
    val Divider: Color,
    val Primary: Color,
    val SelectedBackground: Color,
    val DisabledText: Color,
    val DisabledBackground: Color,
    val Danger: Color,
    val Success: Color
)

@Composable
private fun mirrorColors() = MirrorColorsData(
    PageBackground = ControlVisualTokens.Surface,
    ContentBackground = ControlVisualTokens.Surface,
    PrimaryText = ControlVisualTokens.Text,
    SecondaryText = ControlVisualTokens.Muted,
    Divider = ControlVisualTokens.Divider,
    Primary = ControlVisualTokens.Primary,
    SelectedBackground = ControlVisualTokens.Selected,
    DisabledText = ControlVisualTokens.Tertiary,
    DisabledBackground = ControlVisualTokens.Soft,
    Danger = ControlVisualTokens.Danger,
    Success = ControlVisualTokens.Success
)

internal data class IntOption(
    val label: String,
    val value: Int?
)

internal data class StringOption(
    val label: String,
    val value: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceMirrorScreen(
    viewModel: DeviceMirrorViewModel,
    selectedDevice: String?,
    deviceDisplayName: String? = null,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
    showHeader: Boolean = true,
    collapseSettings: Boolean = false,
    showRuntimeControls: Boolean = true,
    useInternalScroll: Boolean = true
) {
    val settings by viewModel.settings.collectAsState()
    val mirrorRunning by viewModel.mirrorRunning.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val dialogMessage by viewModel.dialogMessage.collectAsState()
    val activeDeviceId by viewModel.activeDeviceId.collectAsState()
    val mirrorStartedAt by viewModel.mirrorStartedAt.collectAsState()
    val mirrorErrorMessage by viewModel.mirrorErrorMessage.collectAsState()
    val deviceConnectionState by viewModel.deviceConnectionState.collectAsState()
    val inputInjectionBlocked by viewModel.inputInjectionBlocked.collectAsState()

    var MirrorColors = mirrorColors()
    var showSettingsDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val statusDeviceId = selectedDevice?.takeIf { it.isNotBlank() }
    val runningDeviceId = activeDeviceId?.takeIf { it.isNotBlank() } ?: statusDeviceId
    val hasDevice = statusDeviceId != null
    val runtimeText = rememberRuntimeText(mirrorRunning, mirrorStartedAt)
    val mirrorErrorText = resolveMessageText(mirrorErrorMessage)
    val connectionStateLabel = resolveConnectionStateLabel(deviceConnectionState)
    val controlsEnabled = mirrorRunning &&
        runningDeviceId != null &&
        deviceConnectionState == "device" &&
        !inputInjectionBlocked

    LaunchedEffect(runningDeviceId) {
        viewModel.watchDeviceConnectionState(runningDeviceId)
    }

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

    val rootModifier = if (useInternalScroll) {
        modifier.fillMaxSize()
    } else {
        modifier.fillMaxWidth()
    }
    val contentModifier = if (useInternalScroll) {
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    } else {
        Modifier.fillMaxWidth()
    }

    Box(
        modifier = rootModifier.background(MirrorColors.PageBackground)
    ) {
        Column(
            modifier = contentModifier
                .padding(
                    horizontal = if (embedded && collapseSettings) 0.dp else if (embedded) 16.dp else 32.dp,
                    vertical = if (embedded && collapseSettings) 0.dp else if (embedded) 16.dp else 28.dp
                ),
            verticalArrangement = Arrangement.spacedBy(if (embedded) 14.dp else 18.dp)
        ) {
            if (showHeader) {
                HeaderSection(
                    connected = hasDevice,
                    connectionType = resolveConnectionType(statusDeviceId)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (embedded && collapseSettings) 0.dp else UiTokens.SpaceXSmall),
                verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXXLarge)
            ) {
                if (collapseSettings) {
                    MirrorStatusSettingsPanel(
                        hasDevice = hasDevice,
                        currentDevice = runningDeviceId,
                        deviceDisplayName = deviceDisplayName,
                        connectionType = resolveConnectionType(runningDeviceId),
                        mirrorRunning = mirrorRunning,
                        settings = settings,
                        onOpenSettings = { showSettingsDialog = true }
                    )
                } else {
                    MirrorStatusPanel(
                        hasDevice = hasDevice,
                        currentDevice = runningDeviceId,
                        connectionType = resolveConnectionType(runningDeviceId),
                        deviceConnectionStateLabel = connectionStateLabel,
                        mirrorRunning = mirrorRunning,
                        runtimeText = runtimeText,
                        onPrimaryAction = {
                            if (mirrorRunning) {
                                viewModel.stopMirror()
                            } else {
                                viewModel.openMirror(statusDeviceId)
                            }
                        }
                    )
                }

                if (mirrorErrorText != null) {
                    Text(
                        text = mirrorErrorText,
                        color = MirrorColors.Danger,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = UiTokens.TextBody
                    )
                } else if (!hasDevice && !collapseSettings) {
                    Text(
                        text = l10n(
                            "连接 Android 设备后即可启动镜像",
                            "Connect an Android device to start mirroring."
                        ),
                        color = MirrorColors.SecondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = UiTokens.TextBody
                    )
                }

                if (!collapseSettings) {
                    MirrorSettingsSections(
                        settings = settings,
                        maxSizeOptions = maxSizeOptions,
                        maxFpsOptions = maxFpsOptions,
                        bitRateOptions = bitRateOptions,
                        onSelectProfile = viewModel::selectProfile,
                        onUpdateSettings = viewModel::updateSettings
                    )
                }

                if (showRuntimeControls) {
                    Section(
                        title = l10n("快捷控制", "Quick Controls")
                    ) {
                        if (inputInjectionBlocked) {
                            Text(
                                text = l10n(
                                    "当前设备拒绝 ADB 输入注入，请在开发者选项开启“USB 调试（安全设置）”。",
                                    "Input injection is blocked. Enable USB debugging (security settings) in developer options."
                                ),
                                color = MirrorColors.Danger,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = UiTokens.TextBody
                            )
                        } else {
                            RuntimeControls(
                                selectedDevice = runningDeviceId,
                                enabled = controlsEnabled,
                                viewModel = viewModel
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(UiTokens.SpaceXSmall))
                }

            }
        }

        if (showSettingsDialog) {
            MirrorSettingsDialog(
                settings = settings,
                maxSizeOptions = maxSizeOptions,
                maxFpsOptions = maxFpsOptions,
                bitRateOptions = bitRateOptions,
                onSelectProfile = viewModel::selectProfile,
                onUpdateSettings = viewModel::updateSettings,
                onDismiss = { showSettingsDialog = false }
            )
        }

        MirrorToast(visible = showDialog, message = dialogMessage)
    }
}

@Composable
internal fun MirrorSettingsSections(
    settings: DeviceMirrorSettings,
    maxSizeOptions: List<IntOption>,
    maxFpsOptions: List<IntOption>,
    bitRateOptions: List<StringOption>,
    onSelectProfile: (MirrorLaunchProfile) -> Unit,
    onUpdateSettings: (DeviceMirrorSettings) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceLarge)
    ) {
        // Preset Profile Cards Section
        MirrorSettingsGroupCard(
            title = l10n("镜像画质模式", "Mirror Quality Mode"),
            subtitle = l10n("选择预设的画质与性能策略，或手动自定义核心参数", "Select a preset quality strategy or configure manually"),
            icon = IconParkIcons.CastScreen
        ) {
            MirrorProfileCards(
                selectedProfile = settings.launchProfile,
                onSelectProfile = onSelectProfile,
                modifier = Modifier.fillMaxWidth()
            )

            if (settings.launchProfile == MirrorLaunchProfile.CUSTOM) {
                CustomAdvancedParameters(
                    maxSize = settings.maxSize,
                    maxFps = settings.maxFps,
                    bitRate = settings.videoBitRate,
                    maxSizeOptions = maxSizeOptions,
                    maxFpsOptions = maxFpsOptions,
                    bitRateOptions = bitRateOptions,
                    onMaxSizeSelected = {
                        onUpdateSettings(settings.copy(maxSize = it, launchProfile = MirrorLaunchProfile.CUSTOM))
                    },
                    onMaxFpsSelected = {
                        onUpdateSettings(settings.copy(maxFps = it, launchProfile = MirrorLaunchProfile.CUSTOM))
                    },
                    onBitRateSelected = {
                        onUpdateSettings(settings.copy(videoBitRate = it, launchProfile = MirrorLaunchProfile.CUSTOM))
                    }
                )
            }
        }

        // Basic experience options
        MirrorSettingsGroupCard(
            title = l10n("基础与交互选项", "Basic Experience"),
            subtitle = l10n("桌面窗口与 Android 设备基础交互协同行为", "Basic desktop and device interaction settings"),
            icon = IconParkIcons.Setting
        ) {
            MirrorSwitchPairRow(
                title = l10n("始终置顶窗口", "Always on top"),
                description = l10n("镜像窗口始终保持在其他桌面窗口上方", "Keep mirror window above all other windows."),
                checked = settings.alwaysOnTop,
                onCheckedChange = { onUpdateSettings(settings.copy(alwaysOnTop = it)) },
                second = MirrorSwitchItemData(
                    title = l10n("同步设备音频", "Sync device audio"),
                    description = l10n("镜像时将设备声音同步至电脑端播放", "Stream device audio to desktop speakers."),
                    checked = settings.audioEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(audioEnabled = it)) }
                )
            )
            MirrorSwitchPairRow(
                title = l10n("保持设备亮屏", "Keep device awake"),
                description = l10n("镜像运行期间防止设备自动休眠息屏", "Prevent screen timeout while mirroring."),
                checked = settings.stayAwake,
                onCheckedChange = { onUpdateSettings(settings.copy(stayAwake = it)) },
                second = MirrorSwitchItemData(
                    title = l10n("显示触摸轨迹", "Show touch feedback"),
                    description = l10n("在设备屏幕上显示手指触摸圆点轨迹", "Show touch position dots on device."),
                    checked = settings.showTouches,
                    onCheckedChange = { onUpdateSettings(settings.copy(showTouches = it)) }
                )
            )
        }

        // Window behavior options
        MirrorSettingsGroupCard(
            title = l10n("窗口显示选项", "Window Behavior"),
            subtitle = l10n("配置镜像窗口启动时的展现形式与边框风格", "Window appearance and fullscreen behavior"),
            icon = IconParkIcons.CastScreen
        ) {
            MirrorSwitchPairRow(
                title = l10n("启动后全屏显示", "Start in fullscreen"),
                description = l10n("打开镜像窗口后自动进入全屏显示模式", "Automatically expand mirror to fullscreen."),
                checked = settings.fullscreen,
                onCheckedChange = { onUpdateSettings(settings.copy(fullscreen = it)) },
                second = MirrorSwitchItemData(
                    title = l10n("隐藏窗口边框", "Hide window borders"),
                    description = l10n("移除窗口原生标题栏与系统外边框", "Borderless clean window presentation."),
                    checked = settings.borderless,
                    onCheckedChange = { onUpdateSettings(settings.copy(borderless = it)) }
                )
            )
        }

        // Power and screen options
        MirrorSettingsGroupCard(
            title = l10n("设备屏幕与电源", "Device Screen & Power"),
            subtitle = l10n("镜像期间设备端屏幕的电源与熄屏策略", "Control device screen power state during mirroring"),
            icon = Icons.Default.PowerSettingsNew
        ) {
            MirrorSwitchPairRow(
                title = l10n("镜像时关闭设备屏幕", "Turn off screen while mirroring"),
                description = l10n("镜像开启后自动关闭设备屏幕以省电降温", "Turn off device display to save battery."),
                checked = settings.turnScreenOffOnStart,
                onCheckedChange = { onUpdateSettings(settings.copy(turnScreenOffOnStart = it)) },
                second = MirrorSwitchItemData(
                    title = l10n("结束后关闭设备屏幕", "Power off screen when mirror ends"),
                    description = l10n("关闭镜像窗口时自动将设备锁屏", "Automatically lock device on mirror close."),
                    checked = settings.powerOffOnClose,
                    onCheckedChange = { onUpdateSettings(settings.copy(powerOffOnClose = it)) }
                )
            )
        }
    }
}

@Composable
internal fun MirrorProfileCards(
    selectedProfile: MirrorLaunchProfile,
    onSelectProfile: (MirrorLaunchProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val profiles = listOf(
        Triple(
            MirrorLaunchProfile.SMOOTH,
            Pair(l10n("流畅优先", "Smooth"), l10n("推荐日常", "Recommended")),
            Pair("1280 × 720 · 60 fps · 8M", IconParkIcons.Speed)
        ),
        Triple(
            MirrorLaunchProfile.CLEAR,
            Pair(l10n("清晰优先", "Clear"), l10n("高清演示", "High Def")),
            Pair("1920 × 1080 · 60 fps · 16M", IconParkIcons.Eye)
        ),
        Triple(
            MirrorLaunchProfile.LOW_LATENCY,
            Pair(l10n("低延迟", "Low latency"), l10n("Wi‑Fi 优选", "Wi‑Fi")),
            Pair("1280 × 720 · 60 fps · 4M", IconParkIcons.Wifi)
        ),
        Triple(
            MirrorLaunchProfile.CUSTOM,
            Pair(l10n("自定义参数", "Custom"), l10n("高级配置", "Advanced")),
            Pair(l10n("手动调整分辨率、帧率与码率", "Manual resolution, FPS, bitrate"), IconParkIcons.Setting)
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
    ) {
        val rows = profiles.chunked(2)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
            ) {
                row.forEach { (profile, labels, meta) ->
                    val (title, tag) = labels
                    val (subtitle, icon) = meta
                    val selected = selectedProfile == profile
                    val shape = RoundedCornerShape(UiTokens.RadiusMedium)
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(
                                when {
                                    selected -> ControlVisualTokens.BrandSoft
                                    isHovered -> ControlVisualTokens.Soft.copy(alpha = 0.7f)
                                    else -> ControlVisualTokens.Soft.copy(alpha = 0.4f)
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    selected -> ControlVisualTokens.Primary.copy(alpha = 0.55f)
                                    isHovered -> ControlVisualTokens.Primary.copy(alpha = 0.3f)
                                    else -> ControlVisualTokens.Border
                                },
                                shape
                            )
                            .clickable(interactionSource = interactionSource, indication = null) {
                                onSelectProfile(profile)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(UiTokens.RadiusSmall))
                                            .background(
                                                if (selected) ControlVisualTokens.Primary.copy(alpha = 0.15f)
                                                else ControlVisualTokens.Soft
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (selected) ControlVisualTokens.Primary else ControlVisualTokens.Muted,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (selected) ControlVisualTokens.Primary else ControlVisualTokens.Text,
                                        fontSize = 13.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(UiTokens.BadgeRadius))
                                        .background(
                                            if (selected) ControlVisualTokens.Primary.copy(alpha = 0.12f)
                                            else ControlVisualTokens.Soft.copy(alpha = 0.8f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selected) ControlVisualTokens.Primary else ControlVisualTokens.Muted
                                    )
                                }
                            }

                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                fontSize = 11.5.sp,
                                color = if (selected) ControlVisualTokens.Text.copy(alpha = 0.85f) else ControlVisualTokens.Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MirrorSettingsGroupCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    FramedStateSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(UiTokens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(UiTokens.RadiusSmall))
                            .background(ControlVisualTokens.Primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ControlVisualTokens.Primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ControlVisualTokens.Text,
                        fontSize = 13.5.sp
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = ControlVisualTokens.Muted,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
            HorizontalDivider(color = ControlVisualTokens.Divider.copy(alpha = 0.4f))
            content()
        }
    }
}

@Composable
private fun MirrorStatusSettingsPanel(
    hasDevice: Boolean,
    currentDevice: String?,
    deviceDisplayName: String?,
    connectionType: String,
    mirrorRunning: Boolean,
    settings: DeviceMirrorSettings,
    onOpenSettings: () -> Unit
) {
    FramedStateSurface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = UiTokens.SpaceLarge, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = l10n("设备概览", "Device overview"),
                    style = MaterialTheme.typography.titleMedium,
                    color = ControlVisualTokens.Text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (hasDevice) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(UiTokens.BadgeRadius))
                            .background(ControlVisualTokens.Success.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = l10n("已连接 · $connectionType", "Connected · $connectionType"),
                            color = ControlVisualTokens.Success,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1.5f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(UiTokens.RadiusMedium))
                            .background(ControlVisualTokens.Primary.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = IconParkIcons.Devices,
                            contentDescription = null,
                            tint = ControlVisualTokens.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = deviceDisplayName?.takeIf { it.isNotBlank() }
                                ?: currentDevice
                                ?: l10n("未选择设备", "No device selected"),
                            color = ControlVisualTokens.Text,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentDevice ?: "--",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = ControlVisualTokens.Tertiary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OverviewDivider()
                OverviewMetric(
                    modifier = Modifier.weight(0.75f),
                    icon = IconParkIcons.Wifi,
                    label = l10n("连接方式", "Connection"),
                    value = if (hasDevice) connectionType else "--",
                    cardText = ControlVisualTokens.Text,
                    secondaryText = ControlVisualTokens.Muted
                )

                OverviewDivider()
                OverviewMetric(
                    modifier = Modifier.weight(0.75f),
                    icon = Icons.Default.ViewAgenda,
                    label = l10n("镜像状态", "Mirror status"),
                    value = if (mirrorRunning) l10n("已启动", "Running") else l10n("未启动", "Not started"),
                    cardText = if (mirrorRunning) ControlVisualTokens.Success else ControlVisualTokens.Text,
                    secondaryText = ControlVisualTokens.Muted
                )

                OverviewDivider()
                Row(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(UiTokens.RadiusMedium))
                        .background(ControlVisualTokens.Soft.copy(alpha = 0.5f))
                        .clickable(onClick = onOpenSettings)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = IconParkIcons.Setting,
                        contentDescription = null,
                        tint = ControlVisualTokens.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = l10n("镜像设置", "Mirror settings"),
                            color = ControlVisualTokens.Muted,
                            fontSize = 11.5.sp
                        )
                        Text(
                            text = mirrorSettingsSummary(settings),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = ControlVisualTokens.Text,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = IconParkIcons.Right,
                        contentDescription = null,
                        tint = ControlVisualTokens.Muted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewMetric(
    label: String,
    value: String,
    icon: ImageVector,
    cardText: Color,
    secondaryText: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ControlVisualTokens.Primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                color = secondaryText,
                fontSize = 11.5.sp,
                maxLines = 1
            )
            Text(
                text = value,
                color = cardText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
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
            .width(1.dp)
            .height(36.dp)
            .background(ControlVisualTokens.Divider.copy(alpha = 0.5f))
    )
}

@Composable
private fun mirrorSettingsSummary(settings: DeviceMirrorSettings): String {
    val size = settings.maxSize?.toString() ?: l10n("原始", "Original")
    return "$size × ${settings.maxFps}fps · ${settings.videoBitRate}"
}

@Composable
internal fun MirrorSettingsDialog(
    settings: DeviceMirrorSettings,
    maxSizeOptions: List<IntOption>,
    maxFpsOptions: List<IntOption>,
    bitRateOptions: List<StringOption>,
    onSelectProfile: (MirrorLaunchProfile) -> Unit,
    onUpdateSettings: (DeviceMirrorSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val dialogScrollState = rememberScrollState()

    LaunchedEffect(settings.launchProfile) {
        dialogScrollState.scrollTo(0)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(680.dp)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(UiTokens.RadiusXLarge),
            color = ControlVisualTokens.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ControlVisualTokens.Border),
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header: title + subtitle + close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = UiTokens.SpaceXXLarge, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(UiTokens.RadiusMedium))
                                .background(ControlVisualTokens.Primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = IconParkIcons.CastScreen,
                                contentDescription = null,
                                tint = ControlVisualTokens.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = l10n("镜像与控制配置", "Mirror & Control Settings"),
                                style = MaterialTheme.typography.titleMedium,
                                color = ControlVisualTokens.Text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            )
                            Text(
                                text = l10n("调整画质预设、分辨率码率与窗口设备协同行为", "Adjust quality presets, resolution, bitrate, and window behavior"),
                                style = MaterialTheme.typography.bodySmall,
                                color = ControlVisualTokens.Muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(UiTokens.RadiusSmall))
                            .background(ControlVisualTokens.Soft.copy(alpha = 0.6f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = l10n("关闭", "Close"),
                            tint = ControlVisualTokens.Muted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                HorizontalDivider(color = ControlVisualTokens.Divider.copy(alpha = 0.6f))

                // Scrollable Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = UiTokens.SpaceXXLarge)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(dialogScrollState)
                            .padding(vertical = UiTokens.SpaceLarge),
                        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceLarge)
                    ) {
                        MirrorSettingsSections(
                            settings = settings,
                            maxSizeOptions = maxSizeOptions,
                            maxFpsOptions = maxFpsOptions,
                            bitRateOptions = bitRateOptions,
                            onSelectProfile = onSelectProfile,
                            onUpdateSettings = onUpdateSettings
                        )
                    }
                }

                // Footer action bar
                HorizontalDivider(color = ControlVisualTokens.Divider.copy(alpha = 0.6f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = UiTokens.SpaceXXLarge, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onSelectProfile(MirrorLaunchProfile.SMOOTH) }
                    ) {
                        Text(
                            text = l10n("恢复默认", "Reset Default"),
                            color = ControlVisualTokens.Muted,
                            fontSize = 12.5.sp
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.height(34.dp).widthIn(min = 90.dp),
                        shape = RoundedCornerShape(UiTokens.RadiusMedium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ControlVisualTokens.Primary,
                            contentColor = QadbColors.onPrimary
                        )
                    ) {
                        Text(
                            text = l10n("完成", "Done"),
                            fontWeight = FontWeight.SemiBold,
                            color = QadbColors.onPrimary,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }
    }
}

private fun profileLabel(profile: MirrorLaunchProfile): String {
    return when (profile) {
        MirrorLaunchProfile.SMOOTH -> l10n("流畅优先", "Smooth")
        MirrorLaunchProfile.CLEAR -> l10n("清晰优先", "Clear")
        MirrorLaunchProfile.LOW_LATENCY -> l10n("低延迟", "Low latency")
        MirrorLaunchProfile.CUSTOM -> l10n("自定义", "Custom")
    }
}

@Composable
private fun HeaderSection(
    connected: Boolean,
    connectionType: String
) {
    var MirrorColors = mirrorColors()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)) {
            Text(
                text = l10n("设备镜像", "Device Mirror"),
                style = MaterialTheme.typography.headlineSmall,
                color = MirrorColors.PrimaryText,
                fontWeight = FontWeight.SemiBold,
                fontSize = UiTokens.TextPageTitle
            )
            Text(
                text = l10n(
                    "通过 scrcpy 在独立窗口中查看并控制 Android 设备",
                    "Use scrcpy to view and control Android devices in a standalone window"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MirrorColors.SecondaryText,
                fontSize = UiTokens.TextBody
            )
        }

        Surface(
            shape = RoundedCornerShape(UiTokens.BadgeRadius),
            color = if (connected) MirrorColors.SelectedBackground else QadbColors.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, MirrorColors.Divider)
        ) {
            Text(
                text = if (connected) l10n("已连接 · $connectionType", "Connected · $connectionType")
                else l10n("未连接", "Disconnected"),
                modifier = Modifier.padding(horizontal = UiTokens.SpaceMedium, vertical = UiTokens.SpaceSmall),
                style = MaterialTheme.typography.bodyMedium,
                color = if (connected) MirrorColors.Primary else MirrorColors.SecondaryText,
                fontSize = UiTokens.TextBody
            )
        }
    }
}

@Composable
private fun MirrorStatusPanel(
    hasDevice: Boolean,
    currentDevice: String?,
    connectionType: String,
    deviceConnectionStateLabel: String,
    mirrorRunning: Boolean,
    runtimeText: String,
    onPrimaryAction: () -> Unit
) {
    var MirrorColors = mirrorColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .background(MirrorColors.ContentBackground, RoundedCornerShape(UiTokens.RadiusMedium))
            .border(1.dp, MirrorColors.Divider, RoundedCornerShape(UiTokens.RadiusMedium))
            .padding(horizontal = UiTokens.SpaceLarge, vertical = UiTokens.SpaceMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceLarge)
    ) {
        Column(
            modifier = Modifier.weight(1.35f),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
        ) {
            StatusLine(
                label = l10n("当前设备", "Current device"),
                value = currentDevice ?: l10n("未连接", "Disconnected"),
                ellipsize = true
            )
            StatusLine(
                label = l10n("连接方式", "Connection"),
                value = if (hasDevice) connectionType else "--"
            )
            StatusLine(
                label = l10n("连接状态", "Connection state"),
                value = if (hasDevice) deviceConnectionStateLabel else "--",
                statusColor = when {
                    !hasDevice -> MirrorColors.SecondaryText
                    deviceConnectionStateLabel.contains("device", ignoreCase = true) ||
                        deviceConnectionStateLabel.contains("可用") -> MirrorColors.Success
                    else -> MirrorColors.Danger
                }
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
        ) {
            StatusLine(
                label = l10n("镜像窗口状态", "Mirror status"),
                value = if (mirrorRunning) l10n("运行中", "Running") else l10n("未启动", "Not started"),
                statusColor = if (mirrorRunning) MirrorColors.Success else MirrorColors.SecondaryText,
                withStatusDot = mirrorRunning
            )
            StatusLine(
                label = l10n("运行时长", "Runtime"),
                value = if (mirrorRunning) runtimeText else "--"
            )
        }

        val buttonEnabled = mirrorRunning || hasDevice
        Button(
            onClick = onPrimaryAction,
            enabled = buttonEnabled,
            shape = RoundedCornerShape(UiTokens.RadiusMedium),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (mirrorRunning) MirrorColors.Danger else MirrorColors.Primary,
                contentColor = QadbColors.onPrimary,
                disabledContainerColor = MirrorColors.DisabledBackground,
                disabledContentColor = MirrorColors.DisabledText
            )
        ) {
            Icon(
                imageVector = if (mirrorRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (buttonEnabled) QadbColors.onPrimary else MirrorColors.DisabledText,
                modifier = Modifier.size(UiTokens.IconSmall)
            )
            Spacer(Modifier.width(UiTokens.SpaceSmall))
            Text(
                text = when {
                    mirrorRunning -> l10n("停止镜像", "Stop Mirror")
                    hasDevice -> l10n("打开镜像", "Open Mirror")
                    else -> l10n("请选择设备", "Select device")
                },
                color = if (buttonEnabled) QadbColors.onPrimary else MirrorColors.DisabledText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = UiTokens.TextBody
            )
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
    statusColor: Color = Color.Unspecified,
    withStatusDot: Boolean = false,
    ellipsize: Boolean = false
) {
    var MirrorColors = mirrorColors()
    val resolvedStatusColor = if (statusColor == Color.Unspecified) MirrorColors.PrimaryText else statusColor
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MirrorColors.SecondaryText,
            fontSize = UiTokens.TextCaption
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
        ) {
            if (withStatusDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(resolvedStatusColor, RoundedCornerShape(UiTokens.BadgeRadius))
                )
            }
            if (ellipsize) {
                DeviceValueText(value = value, color = resolvedStatusColor)
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = resolvedStatusColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = UiTokens.TextBody
                )
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    var MirrorColors = mirrorColors()
    Column(verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MirrorColors.PrimaryText,
            fontWeight = FontWeight.SemiBold,
            fontSize = UiTokens.TextAction
        )
        content()
    }
}

@Composable
internal fun MirrorProfileSegmentedControl(
    selectedProfile: MirrorLaunchProfile,
    onSelectProfile: (MirrorLaunchProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val profiles = listOf(
        MirrorLaunchProfile.SMOOTH to l10n("流畅优先", "Smooth"),
        MirrorLaunchProfile.CLEAR to l10n("清晰优先", "Clear"),
        MirrorLaunchProfile.LOW_LATENCY to l10n("低延迟", "Low latency"),
        MirrorLaunchProfile.CUSTOM to l10n("自定义", "Custom")
    )
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(UiTokens.RadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
    ) {
        profiles.forEach { (profile, label) ->
            MirrorSegmentItem(
                modifier = Modifier.weight(1f),
                label = label,
                selected = selectedProfile == profile,
                onClick = { onSelectProfile(profile) }
            )
        }
    }
}

@Composable
private fun MirrorSegmentItem(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var MirrorColors = mirrorColors()
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.surface
            hovered -> MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
            else -> Color.Transparent
        },
        animationSpec = tween(UiTokens.HoverDurationMillis),
        label = "segmentBg"
    )
    val contentColor = if (selected) MirrorColors.Primary else MirrorColors.SecondaryText
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(UiTokens.RadiusSmall))
            .then(if (selected) Modifier.shadow(1.dp, RoundedCornerShape(UiTokens.RadiusSmall)) else Modifier)
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = UiTokens.TextBody,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
internal fun CustomAdvancedParameters(
    maxSize: Int?,
    maxFps: Int?,
    bitRate: String,
    maxSizeOptions: List<IntOption>,
    maxFpsOptions: List<IntOption>,
    bitRateOptions: List<StringOption>,
    onMaxSizeSelected: (Int?) -> Unit,
    onMaxFpsSelected: (Int?) -> Unit,
    onBitRateSelected: (String) -> Unit
) {
    val shape = RoundedCornerShape(UiTokens.RadiusMedium)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ControlVisualTokens.Soft.copy(alpha = 0.4f))
            .border(1.dp, ControlVisualTokens.Border, shape)
            .padding(UiTokens.SpaceMedium),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = IconParkIcons.Setting,
                contentDescription = null,
                tint = ControlVisualTokens.Primary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = l10n("高级分辨率与码率微调", "Advanced Resolution & Bitrate Tuning"),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = ControlVisualTokens.Text
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
        ) {
            IntSelectField(
                modifier = Modifier.weight(1f),
                label = l10n("最大分辨率", "Max resolution"),
                value = maxSize,
                options = maxSizeOptions,
                onSelected = onMaxSizeSelected
            )
            IntSelectField(
                modifier = Modifier.weight(1f),
                label = l10n("最大帧率", "Max FPS"),
                value = maxFps,
                options = maxFpsOptions,
                onSelected = onMaxFpsSelected
            )
            StringSelectField(
                modifier = Modifier.weight(1f),
                label = l10n("视频码率", "Video bitrate"),
                value = bitRate,
                options = bitRateOptions,
                onSelected = onBitRateSelected
            )
        }

        Text(
            text = l10n(
                "💡 推荐使用 1280 分辨率、60 帧、8M 码率，可在流畅度与清晰度之间取得最佳平衡。",
                "💡 Recommended 1280 / 60fps / 8M provides the ideal balance between low latency and sharp text."
            ),
            color = ControlVisualTokens.Muted,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RuntimeControls(
    selectedDevice: String?,
    enabled: Boolean,
    viewModel: DeviceMirrorViewModel
) {
    if (!enabled) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .background(ControlVisualTokens.Soft.copy(alpha = 0.5f), RoundedCornerShape(UiTokens.RadiusMedium))
                .border(1.dp, ControlVisualTokens.Border, RoundedCornerShape(UiTokens.RadiusMedium))
                .padding(horizontal = UiTokens.SpaceMedium, vertical = UiTokens.SpaceSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = ControlVisualTokens.Tertiary,
                modifier = Modifier.size(UiTokens.IconSmall)
            )
            Text(
                text = l10n(
                    "连接设备并启动镜像后，可使用返回、主页、截图等快捷控制。",
                    "Connect a device and start mirroring to use back, home, screenshot, and other controls."
                ),
                color = ControlVisualTokens.Muted,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
        ) {
            MirrorControlButton(modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Filled.ArrowBack, label = l10n("返回", "Back"), enabled = enabled) {
                viewModel.sendKeyEvent(selectedDevice, MirrorKeyCode.BACK)
            }
            MirrorControlButton(modifier = Modifier.weight(1f), icon = IconParkIcons.Home, label = l10n("主页", "Home"), enabled = enabled) {
                viewModel.sendKeyEvent(selectedDevice, MirrorKeyCode.HOME)
            }
            MirrorControlButton(modifier = Modifier.weight(1f), icon = Icons.Default.ViewAgenda, label = l10n("多任务", "Recent"), enabled = enabled) {
                viewModel.sendKeyEvent(selectedDevice, MirrorKeyCode.RECENT)
            }
            MirrorControlButton(modifier = Modifier.weight(1f), icon = Icons.Default.PowerSettingsNew, label = l10n("电源", "Power"), enabled = enabled) {
                viewModel.sendKeyEvent(selectedDevice, MirrorKeyCode.POWER)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
        ) {
            MirrorControlButton(modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Filled.VolumeDown, label = l10n("音量减", "Vol -"), enabled = enabled) {
                viewModel.sendKeyEvent(selectedDevice, MirrorKeyCode.VOLUME_DOWN)
            }
            MirrorControlButton(modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Filled.VolumeUp, label = l10n("音量加", "Vol +"), enabled = enabled) {
                viewModel.sendKeyEvent(selectedDevice, MirrorKeyCode.VOLUME_UP)
            }
            MirrorControlButton(modifier = Modifier.weight(1f), icon = IconParkIcons.Camera, label = l10n("截图", "Screenshot"), enabled = enabled) {
                viewModel.takeScreenSnapshot(selectedDevice)
            }
            MirrorControlButton(modifier = Modifier.weight(1f), icon = Icons.Default.ScreenRotation, label = l10n("旋转屏幕", "Rotate"), enabled = enabled) {
                viewModel.rotateScreen(selectedDevice)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MirrorControlButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    TooltipArea(
        tooltip = {
            Surface(
                color = ControlVisualTokens.Surface,
                shape = RoundedCornerShape(UiTokens.RadiusMedium),
                border = androidx.compose.foundation.BorderStroke(1.dp, ControlVisualTokens.Border)
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = UiTokens.SpaceSmall, vertical = UiTokens.SpaceSmall),
                    style = MaterialTheme.typography.bodySmall,
                    color = ControlVisualTokens.Text,
                    fontSize = 11.5.sp
                )
            }
        }
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .heightIn(min = 36.dp)
                .border(1.dp, ControlVisualTokens.Border, RoundedCornerShape(UiTokens.RadiusMedium))
                .background(
                    color = if (enabled) ControlVisualTokens.Surface else ControlVisualTokens.Soft,
                    shape = RoundedCornerShape(UiTokens.RadiusMedium)
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(17.dp),
                tint = if (enabled) ControlVisualTokens.Text else ControlVisualTokens.Tertiary
            )
        }
    }
}

internal data class MirrorSwitchItemData(
    val title: String,
    val description: String? = null,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

@Composable
internal fun MirrorSwitchPairRow(
    title: String,
    checked: Boolean,
    description: String? = null,
    second: MirrorSwitchItemData? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MirrorSwitchCell(
                modifier = Modifier.weight(1f),
                title = title,
                description = description,
                checked = checked,
                onCheckedChange = onCheckedChange
            )
            if (second != null) {
                MirrorSwitchCell(
                    modifier = Modifier.weight(1f),
                    title = second.title,
                    description = second.description,
                    checked = second.checked,
                    onCheckedChange = second.onCheckedChange
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun MirrorSwitchCell(
    modifier: Modifier,
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(UiTokens.RadiusMedium)
    val background by animateColorAsState(
        targetValue = if (hovered) ControlVisualTokens.Soft.copy(alpha = 0.8f) else ControlVisualTokens.Soft.copy(alpha = 0.35f),
        animationSpec = tween(UiTokens.HoverDurationMillis),
        label = "switchRowBg"
    )
    Row(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(
                1.dp,
                if (hovered) ControlVisualTokens.Primary.copy(alpha = 0.35f) else ControlVisualTokens.Border,
                shape
            )
            .clickable(interactionSource = interactionSource, indication = null) { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = ControlVisualTokens.Text,
                fontWeight = FontWeight.Medium,
                fontSize = 12.5.sp
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ControlVisualTokens.Muted,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
internal fun IntSelectField(
    label: String,
    value: Int?,
    options: List<IntOption>,
    modifier: Modifier = Modifier,
    onSelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel(label)
        Box {
            SelectFieldTrigger(
                text = options.find { it.value == value }?.label ?: l10n("不限制", "No limit"),
                expanded = expanded,
                onClick = { expanded = true }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label, fontSize = 12.sp) },
                        onClick = {
                            expanded = false
                            onSelected(option.value)
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun StringSelectField(
    label: String,
    value: String,
    options: List<StringOption>,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel(label)
        Box {
            SelectFieldTrigger(
                text = options.find { it.value == value }?.label ?: value,
                expanded = expanded,
                onClick = { expanded = true }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label, fontSize = 12.sp) },
                        onClick = {
                            expanded = false
                            onSelected(option.value)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectFieldTrigger(
    text: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        targetValue = when {
            expanded -> ControlVisualTokens.Primary
            hovered -> ControlVisualTokens.Primary.copy(alpha = 0.45f)
            else -> ControlVisualTokens.Border
        },
        animationSpec = tween(UiTokens.HoverDurationMillis),
        label = "selectBorder"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(UiTokens.RadiusMedium))
            .background(ControlVisualTokens.Surface)
            .border(1.dp, borderColor, RoundedCornerShape(UiTokens.RadiusMedium))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            fontSize = 12.sp,
            color = ControlVisualTokens.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = CompatIconVectors.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(UiTokens.IconSmall),
            tint = ControlVisualTokens.Muted
        )
    }
}

@Composable
internal fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = ControlVisualTokens.Muted,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Medium
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceValueText(
    value: String,
    color: Color,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    var MirrorColors = mirrorColors()
    TooltipArea(
        tooltip = {
            Surface(
                color = MirrorColors.ContentBackground,
                shape = RoundedCornerShape(UiTokens.RadiusMedium),
                border = androidx.compose.foundation.BorderStroke(1.dp, MirrorColors.Divider)
            ) {
                Text(
                    text = value,
                    modifier = Modifier.padding(horizontal = UiTokens.SpaceSmall, vertical = UiTokens.SpaceSmall),
                    style = MaterialTheme.typography.bodySmall,
                    color = MirrorColors.PrimaryText,
                    fontSize = UiTokens.TextCaption
                )
            }
        }
    ) {
        Text(
            text = value,
            color = color,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            fontSize = UiTokens.TextBody,
            modifier = Modifier.width(220.dp)
        )
    }
}

@Composable
internal fun resolveMessageText(message: MsgContent?): String? {
    if (message == null) return null
    return when (message) {
        is MsgContent.Resource -> stringResource(message.stringResource, *message.args.toTypedArray())
        is MsgContent.Text -> message.text
    }
}

private fun resolveConnectionType(deviceId: String?): String {
    if (deviceId.isNullOrBlank()) return "--"
    return if (isWirelessAdbConnection(deviceId)) l10n("Wi‑Fi", "Wi‑Fi") else "USB"
}

private fun resolveConnectionStateLabel(state: String): String {
    return when (state.lowercase()) {
        "device" -> l10n("可用", "device")
        "offline" -> l10n("离线", "offline")
        "unauthorized" -> l10n("未授权", "unauthorized")
        "not_found" -> l10n("未找到设备", "not found")
        "disconnected" -> l10n("未连接", "disconnected")
        "unknown" -> l10n("未知", "unknown")
        else -> state
    }
}

internal fun profileHint(profile: MirrorLaunchProfile): String {
    return when (profile) {
        MirrorLaunchProfile.SMOOTH -> l10n("流畅优先：1280 / 60 / 8M", "Smooth: 1280 / 60 / 8M")
        MirrorLaunchProfile.CLEAR -> l10n("清晰优先：1920 / 60 / 16M", "Clear: 1920 / 60 / 16M")
        MirrorLaunchProfile.LOW_LATENCY -> l10n("低延迟：1280 / 60 / 4M", "Low latency: 1280 / 60 / 4M")
        MirrorLaunchProfile.CUSTOM -> l10n("自定义：手动配置分辨率、帧率和码率", "Custom: configure resolution, FPS, and bitrate manually")
    }
}

@Composable
private fun rememberRuntimeText(mirrorRunning: Boolean, startedAtMillis: Long?): String {
    var seconds by remember(mirrorRunning, startedAtMillis) { mutableIntStateOf(0) }

    LaunchedEffect(mirrorRunning, startedAtMillis) {
        if (!mirrorRunning || startedAtMillis == null) {
            seconds = 0
            return@LaunchedEffect
        }
        while (true) {
            seconds = ((System.currentTimeMillis() - startedAtMillis) / 1000L).toInt().coerceAtLeast(0)
            delay(1000)
        }
    }

    val hour = seconds / 3600
    val minute = (seconds % 3600) / 60
    val second = seconds % 60
    return "%02d:%02d:%02d".format(hour, minute, second)
}

@Composable
internal fun MirrorToast(
    visible: Boolean,
    message: MsgContent?
) {
    var MirrorColors = mirrorColors()
    if (!visible || message == null) return
    val text = resolveMessageText(message) ?: return

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            shape = RoundedCornerShape(UiTokens.RadiusMedium),
            color = MaterialTheme.colorScheme.inverseSurface,
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MirrorColors.Divider),
            modifier = Modifier.padding(bottom = UiTokens.SpaceXXLarge)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = UiTokens.SpaceLarge, vertical = UiTokens.SpaceSmall),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = UiTokens.TextBody
            )
        }
    }
}
