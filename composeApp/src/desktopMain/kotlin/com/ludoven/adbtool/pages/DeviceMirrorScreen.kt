package com.ludoven.adbtool.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ScreenRotation
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.entity.MirrorLaunchProfile
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.ui.mac.Button
import com.ludoven.adbtool.ui.mac.ButtonDefaults
import com.ludoven.adbtool.ui.mac.DropdownMenu
import com.ludoven.adbtool.ui.mac.DropdownMenuItem
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.IconButton
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.OutlinedButton
import com.ludoven.adbtool.ui.mac.Surface
import com.ludoven.adbtool.ui.mac.Switch
import com.ludoven.adbtool.ui.mac.Text
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

private object MirrorColors {
    val PageBackground = Color(0xFFFFFFFF)
    val ContentBackground = Color(0xFFFFFFFF)
    val PrimaryText = Color(0xFF111827)
    val SecondaryText = Color(0xFF4B5563)
    val Divider = Color(0xFFCBD5E1)
    val Primary = Color(0xFF2563EB)
    val Danger = Color(0xFFDC2626)
    val Success = Color(0xFF16A34A)
}

private data class IntOption(
    val label: String,
    val value: Int?
)

private data class StringOption(
    val label: String,
    val value: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceMirrorScreen(
    viewModel: DeviceMirrorViewModel,
    selectedDevice: String?
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MirrorColors.PageBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeaderSection(
                connected = hasDevice,
                connectionType = resolveConnectionType(statusDeviceId)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(30.dp)
            ) {
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

                if (mirrorErrorText != null) {
                    Text(
                        text = mirrorErrorText,
                        color = MirrorColors.Danger,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp
                    )
                } else if (!hasDevice) {
                    Text(
                        text = l10n(
                            "连接 Android 设备后即可启动镜像",
                            "Connect an Android device to start mirroring."
                        ),
                        color = MirrorColors.SecondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp
                    )
                }

                Section(
                    title = l10n("镜像模式", "Mirror Mode")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MirrorModeSegment(
                            modifier = Modifier.weight(1f),
                            label = l10n("流畅优先", "Smooth"),
                            selected = settings.launchProfile == MirrorLaunchProfile.SMOOTH,
                            onClick = { viewModel.selectProfile(MirrorLaunchProfile.SMOOTH) }
                        )
                        MirrorModeSegment(
                            modifier = Modifier.weight(1f),
                            label = l10n("清晰优先", "Clear"),
                            selected = settings.launchProfile == MirrorLaunchProfile.CLEAR,
                            onClick = { viewModel.selectProfile(MirrorLaunchProfile.CLEAR) }
                        )
                        MirrorModeSegment(
                            modifier = Modifier.weight(1f),
                            label = l10n("低延迟", "Low latency"),
                            selected = settings.launchProfile == MirrorLaunchProfile.LOW_LATENCY,
                            onClick = { viewModel.selectProfile(MirrorLaunchProfile.LOW_LATENCY) }
                        )
                        MirrorModeSegment(
                            modifier = Modifier.weight(1f),
                            label = l10n("自定义", "Custom"),
                            selected = settings.launchProfile == MirrorLaunchProfile.CUSTOM,
                            onClick = { viewModel.selectProfile(MirrorLaunchProfile.CUSTOM) }
                        )
                    }

                    Text(
                        text = profileHint(settings.launchProfile),
                        color = MirrorColors.SecondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp
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
                                viewModel.updateSettings(
                                    settings.copy(maxSize = it, launchProfile = MirrorLaunchProfile.CUSTOM)
                                )
                            },
                            onMaxFpsSelected = {
                                viewModel.updateSettings(
                                    settings.copy(maxFps = it, launchProfile = MirrorLaunchProfile.CUSTOM)
                                )
                            },
                            onBitRateSelected = {
                                viewModel.updateSettings(
                                    settings.copy(videoBitRate = it, launchProfile = MirrorLaunchProfile.CUSTOM)
                                )
                            }
                        )
                    }
                }

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
                            fontSize = 13.sp
                        )
                    }
                    RuntimeControls(
                        selectedDevice = runningDeviceId,
                        enabled = controlsEnabled,
                        viewModel = viewModel
                    )
                }

                Section(
                    title = l10n("基础选项", "Basic Options")
                ) {
                    MirrorSwitchPairRow(
                        title = l10n("始终置顶", "Always on top"),
                        description = l10n("镜像窗口保持在其他窗口上方", "Keep the mirror window above others."),
                        checked = settings.alwaysOnTop,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(alwaysOnTop = it)) },
                        second = MirrorSwitchItemData(
                            title = l10n("同步设备音频", "Sync device audio"),
                            description = l10n("镜像时同步设备声音到桌面", "Play device audio on desktop."),
                            checked = settings.audioEnabled,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(audioEnabled = it)) }
                        )
                    )
                    MirrorSwitchPairRow(
                        title = l10n("保持设备亮屏", "Keep device awake"),
                        description = l10n("镜像期间尽量防止设备息屏", "Prevent screen sleep while mirroring."),
                        checked = settings.stayAwake,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(stayAwake = it)) },
                        second = MirrorSwitchItemData(
                            title = l10n("显示触摸反馈", "Show touches"),
                            description = l10n("在设备端显示触摸位置反馈", "Show touch indicators on device."),
                            checked = settings.showTouches,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(showTouches = it)) }
                        )
                    )
                }

                Section(
                    title = l10n("窗口选项", "Window Options")
                ) {
                    MirrorSwitchPairRow(
                        title = l10n("启动后全屏", "Start fullscreen"),
                        checked = settings.fullscreen,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(fullscreen = it)) },
                        second = MirrorSwitchItemData(
                            title = l10n("隐藏窗口边框", "Hide window border"),
                            checked = settings.borderless,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(borderless = it)) }
                        )
                    )
                }

                Section(
                    title = l10n("设备屏幕选项", "Device Screen Options")
                ) {
                    MirrorSwitchPairRow(
                        title = l10n("镜像时关闭设备屏幕", "Turn off screen while mirroring"),
                        checked = settings.turnScreenOffOnStart,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(turnScreenOffOnStart = it)) },
                        second = MirrorSwitchItemData(
                            title = l10n("结束后关闭设备屏幕", "Turn off screen when mirror ends"),
                            checked = settings.powerOffOnClose,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(powerOffOnClose = it)) }
                        )
                    )
                }
            }
        }

        MirrorToast(visible = showDialog, message = dialogMessage)
    }
}

@Composable
private fun HeaderSection(
    connected: Boolean,
    connectionType: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = l10n("设备镜像", "Device Mirror"),
                style = MaterialTheme.typography.headlineSmall,
                color = MirrorColors.PrimaryText,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = l10n(
                    "通过 scrcpy 在独立窗口中查看并控制 Android 设备",
                    "Use scrcpy to view and control Android devices in a standalone window"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MirrorColors.SecondaryText,
                fontSize = 14.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (connected) Color(0xFFEFF6FF) else Color(0xFFF3F4F6),
            border = androidx.compose.foundation.BorderStroke(1.dp, MirrorColors.Divider)
        ) {
            Text(
                text = if (connected) l10n("已连接 · $connectionType", "Connected · $connectionType")
                else l10n("未连接", "Disconnected"),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (connected) Color(0xFF1D4ED8) else MirrorColors.SecondaryText,
                fontSize = 13.sp
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 108.dp, max = 136.dp)
            .border(1.dp, MirrorColors.Divider, RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1.35f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                statusColor = if (deviceConnectionStateLabel.contains("device", ignoreCase = true) || deviceConnectionStateLabel.contains("可用")) {
                    MirrorColors.Success
                } else {
                    MirrorColors.Danger
                }
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (mirrorRunning) MirrorColors.Danger else MirrorColors.Primary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = if (mirrorRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when {
                    mirrorRunning -> l10n("停止镜像", "Stop Mirror")
                    hasDevice -> l10n("打开镜像", "Open Mirror")
                    else -> l10n("请选择设备", "Select device")
                },
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
    statusColor: Color = MirrorColors.PrimaryText,
    withStatusDot: Boolean = false,
    ellipsize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MirrorColors.SecondaryText,
            fontSize = 13.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (withStatusDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, RoundedCornerShape(999.dp))
                )
            }
            if (ellipsize) {
                DeviceValueText(value = value, color = statusColor)
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
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
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MirrorColors.PrimaryText,
            fontWeight = FontWeight.Medium
        )
        content()
    }
}

@Composable
private fun MirrorModeSegment(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = modifier.heightIn(min = 36.dp),
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Color(0xFFEAF2FF) else Color.White,
            contentColor = if (selected) MirrorColors.Primary else MirrorColors.PrimaryText
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun CustomAdvancedParameters(
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = l10n("高级参数", "Advanced Parameters"),
            style = MaterialTheme.typography.titleMedium,
            color = MirrorColors.PrimaryText,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                "推荐默认使用 1280 分辨率、60 帧、8M 码率，可在流畅度和清晰度之间取得较好平衡。",
                "The default 1280 / 60 / 8M setting provides a practical balance between smoothness and clarity."
            ),
            color = MirrorColors.SecondaryText,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MirrorControlButton(modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Filled.ArrowBack, label = l10n("返回", "Back"), enabled = enabled) {
                viewModel.sendKeyEvent(selectedDevice, MirrorKeyCode.BACK)
            }
            MirrorControlButton(modifier = Modifier.weight(1f), icon = Icons.Default.Home, label = l10n("主页", "Home"), enabled = enabled) {
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MirrorControlButton(modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Filled.VolumeDown, label = l10n("音量减", "Vol -"), enabled = enabled) {
                viewModel.sendKeyEvent(selectedDevice, MirrorKeyCode.VOLUME_DOWN)
            }
            MirrorControlButton(modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Filled.VolumeUp, label = l10n("音量加", "Vol +"), enabled = enabled) {
                viewModel.sendKeyEvent(selectedDevice, MirrorKeyCode.VOLUME_UP)
            }
            MirrorControlButton(modifier = Modifier.weight(1f), icon = Icons.Default.CameraAlt, label = l10n("截图", "Screenshot"), enabled = enabled) {
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
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MirrorColors.Divider)
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MirrorColors.PrimaryText,
                    fontSize = 12.sp
                )
            }
        }
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .heightIn(min = 36.dp)
                .border(1.dp, MirrorColors.Divider, RoundedCornerShape(8.dp))
                .background(
                    color = if (enabled) Color.White else Color(0xFFF4F4F5),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(17.dp),
                tint = if (enabled) MirrorColors.PrimaryText else MirrorColors.SecondaryText.copy(alpha = 0.55f)
            )
        }
    }
}

private data class MirrorSwitchItemData(
    val title: String,
    val description: String? = null,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

@Composable
private fun MirrorSwitchPairRow(
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
                .heightIn(min = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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
private fun MirrorSwitchCell(
    modifier: Modifier,
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MirrorColors.PrimaryText,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MirrorColors.SecondaryText,
                    fontSize = 12.sp
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun IntSelectField(
    label: String,
    value: Int?,
    options: List<IntOption>,
    modifier: Modifier = Modifier,
    onSelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(9.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = MirrorColors.PrimaryText
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = options.find { it.value == value }?.label ?: l10n("不限制", "No limit"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = MirrorColors.PrimaryText
                    )
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MirrorColors.SecondaryText)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label, fontSize = 13.sp) },
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
private fun StringSelectField(
    label: String,
    value: String,
    options: List<StringOption>,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(9.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = MirrorColors.PrimaryText
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = options.find { it.value == value }?.label ?: value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = MirrorColors.PrimaryText
                    )
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MirrorColors.SecondaryText)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label, fontSize = 13.sp) },
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
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MirrorColors.SecondaryText,
        fontSize = 13.sp
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceValueText(
    value: String,
    color: Color,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    TooltipArea(
        tooltip = {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MirrorColors.Divider)
            ) {
                Text(
                    text = value,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MirrorColors.PrimaryText,
                    fontSize = 12.sp
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
            fontSize = 13.sp,
            modifier = Modifier.width(220.dp)
        )
    }
}

@Composable
private fun resolveMessageText(message: MsgContent?): String? {
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

private fun profileHint(profile: MirrorLaunchProfile): String {
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
private fun MirrorToast(
    visible: Boolean,
    message: MsgContent?
) {
    if (!visible || message == null) return
    val text = resolveMessageText(message) ?: return

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MirrorColors.Divider),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp
            )
        }
    }
}
