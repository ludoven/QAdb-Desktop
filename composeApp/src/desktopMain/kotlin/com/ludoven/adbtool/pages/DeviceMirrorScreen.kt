package com.ludoven.adbtool.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.entity.MirrorLaunchProfile
import com.ludoven.adbtool.entity.MsgContent
import com.ludoven.adbtool.ui.mac.Button
import com.ludoven.adbtool.ui.mac.ButtonDefaults
import com.ludoven.adbtool.ui.mac.DropdownMenu
import com.ludoven.adbtool.ui.mac.DropdownMenuItem
import com.ludoven.adbtool.ui.mac.HorizontalDivider
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.OutlinedButton
import com.ludoven.adbtool.ui.mac.OutlinedTextField
import com.ludoven.adbtool.ui.mac.Surface
import com.ludoven.adbtool.ui.mac.Switch
import com.ludoven.adbtool.ui.mac.Text
import com.ludoven.adbtool.ui.mac.bodyMedium
import com.ludoven.adbtool.ui.mac.bodySmall
import com.ludoven.adbtool.ui.mac.headlineSmall
import com.ludoven.adbtool.ui.mac.titleMedium
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

private data class MirrorSelectOption(
    val label: String,
    val value: Int?
)

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

    var bitRateText by remember(settings.videoBitRate) { mutableStateOf(settings.videoBitRate) }
    val scrollState = rememberScrollState()

    val placeholder = l10n("请选择", "Select")
    val maxSizeOptions = listOf(
        MirrorSelectOption(placeholder, null),
        MirrorSelectOption("720", 720),
        MirrorSelectOption("1080", 1080),
        MirrorSelectOption("1280", 1280),
        MirrorSelectOption("1920", 1920),
        MirrorSelectOption("2560", 2560)
    )
    val maxFpsOptions = listOf(
        MirrorSelectOption(placeholder, null),
        MirrorSelectOption("30", 30),
        MirrorSelectOption("45", 45),
        MirrorSelectOption("60", 60),
        MirrorSelectOption("90", 90),
        MirrorSelectOption("120", 120)
    )

    val statusDeviceId = selectedDevice?.takeIf { it.isNotBlank() }
    val runningDeviceId = activeDeviceId?.takeIf { it.isNotBlank() } ?: statusDeviceId
    val commandPreview = viewModel.buildCommandPreview(runningDeviceId)
    val runtimeText = rememberRuntimeText(mirrorRunning, mirrorStartedAt)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 26.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderSection(statusDeviceId = statusDeviceId)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1.55f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionTitle(l10n("镜像配置", "Mirror Configuration"))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))

                    DeviceInfoRow(deviceId = statusDeviceId)

                    SectionGroupTitle(l10n("镜像模式", "Mirror Mode"))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MirrorModeChip(
                            modifier = Modifier.weight(1f),
                            label = l10n("流畅优先", "Smooth"),
                            selected = settings.launchProfile == MirrorLaunchProfile.SMOOTH
                        ) { viewModel.selectProfile(MirrorLaunchProfile.SMOOTH) }
                        MirrorModeChip(
                            modifier = Modifier.weight(1f),
                            label = l10n("清晰优先", "Clear"),
                            selected = settings.launchProfile == MirrorLaunchProfile.CLEAR
                        ) { viewModel.selectProfile(MirrorLaunchProfile.CLEAR) }
                        MirrorModeChip(
                            modifier = Modifier.weight(1f),
                            label = l10n("低延迟", "Low latency"),
                            selected = settings.launchProfile == MirrorLaunchProfile.LOW_LATENCY
                        ) { viewModel.selectProfile(MirrorLaunchProfile.LOW_LATENCY) }
                        MirrorModeChip(
                            modifier = Modifier.weight(1f),
                            label = l10n("自定义", "Custom"),
                            selected = settings.launchProfile == MirrorLaunchProfile.CUSTOM
                        ) { viewModel.selectProfile(MirrorLaunchProfile.CUSTOM) }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    SectionGroupTitle(l10n("常用选项", "Common Options"))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        MirrorSwitchItem(
                            label = l10n("始终置顶", "Always on top"),
                            checked = settings.alwaysOnTop
                        ) { viewModel.updateSettings(settings.copy(alwaysOnTop = it)) }
                        MirrorSwitchItem(
                            label = l10n("同步设备音频", "Sync device audio"),
                            checked = settings.audioEnabled
                        ) { viewModel.updateSettings(settings.copy(audioEnabled = it)) }
                        MirrorSwitchItem(
                            label = l10n("保持设备亮屏", "Keep device awake"),
                            checked = settings.stayAwake
                        ) { viewModel.updateSettings(settings.copy(stayAwake = it)) }
                        MirrorSwitchItem(
                            label = l10n("显示触摸反馈", "Show touches"),
                            checked = settings.showTouches
                        ) { viewModel.updateSettings(settings.copy(showTouches = it)) }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    SectionGroupTitle(l10n("窗口选项", "Window Options"))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        MirrorSwitchItem(
                            label = l10n("启动后全屏", "Start fullscreen"),
                            checked = settings.fullscreen
                        ) { viewModel.updateSettings(settings.copy(fullscreen = it)) }
                        MirrorSwitchItem(
                            label = l10n("隐藏窗口边框", "Hide window border"),
                            checked = settings.borderless
                        ) { viewModel.updateSettings(settings.copy(borderless = it)) }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    SectionGroupTitle(l10n("设备屏幕选项", "Device Screen Options"))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        MirrorSwitchItem(
                            label = l10n("镜像时关闭设备屏幕", "Turn off screen while mirroring"),
                            checked = settings.turnScreenOffOnStart
                        ) { viewModel.updateSettings(settings.copy(turnScreenOffOnStart = it)) }
                        MirrorSwitchItem(
                            label = l10n("结束后关闭设备屏幕", "Turn off screen when mirror ends"),
                            checked = settings.powerOffOnClose
                        ) { viewModel.updateSettings(settings.copy(powerOffOnClose = it)) }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        MirrorSelectField(
                            modifier = Modifier.weight(1f),
                            label = l10n("最大分辨率", "Max resolution"),
                            value = settings.maxSize,
                            placeholder = placeholder,
                            options = maxSizeOptions,
                            onSelected = {
                                viewModel.updateSettings(
                                    settings.copy(maxSize = it, launchProfile = MirrorLaunchProfile.CUSTOM)
                                )
                            }
                        )
                        MirrorSelectField(
                            modifier = Modifier.weight(1f),
                            label = l10n("最大帧率", "Max FPS"),
                            value = settings.maxFps,
                            placeholder = placeholder,
                            options = maxFpsOptions,
                            onSelected = {
                                viewModel.updateSettings(
                                    settings.copy(maxFps = it, launchProfile = MirrorLaunchProfile.CUSTOM)
                                )
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            FormLabel(l10n("视频码率", "Video bitrate"))
                            OutlinedTextField(
                                value = bitRateText,
                                onValueChange = { value ->
                                    bitRateText = value
                                    viewModel.updateSettings(
                                        settings.copy(videoBitRate = value, launchProfile = MirrorLaunchProfile.CUSTOM)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Text(
                        text = l10n(
                            "重点：当前页面只用于配置参数并启动外部 scrcpy 窗口，不提供内嵌实时画面预览。",
                            "This page configures parameters and launches an external scrcpy window; it does not embed live preview."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            if (mirrorRunning) viewModel.stopMirror() else viewModel.openMirror(statusDeviceId)
                        },
                        enabled = mirrorRunning || statusDeviceId != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (mirrorRunning) Color(0xFFDC2626) else Color(0xFF1677FF),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (mirrorRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (mirrorRunning) l10n("停止镜像窗口", "Stop Mirror Window")
                            else l10n("打开镜像窗口", "Open Mirror Window"),
                            color = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionTitle(l10n("镜像窗口状态", "Mirror Window Status"))
                    Text(
                        text = if (mirrorRunning) {
                            l10n("镜像窗口运行中", "Mirror window is running")
                        } else {
                            l10n(
                                "镜像窗口未启动，点击打开镜像窗口后将启动独立 scrcpy 窗口",
                                "Mirror window is not started. Click Open Mirror Window to launch a standalone scrcpy window."
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    StatusRow(
                        label = l10n("设备", "Device"),
                        value = runningDeviceId ?: l10n("未连接设备", "No device connected")
                    )
                    StatusRow(
                        label = l10n("码率", "Bitrate"),
                        value = settings.videoBitRate.ifBlank { "-" }
                    )
                    StatusRow(
                        label = l10n("音频", "Audio"),
                        value = if (settings.audioEnabled) l10n("已同步", "Synced") else l10n("已关闭", "Off")
                    )
                    StatusRow(
                        label = l10n("运行时长", "Runtime"),
                        value = if (mirrorRunning) runtimeText else "--"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    Text(
                        text = l10n("scrcpy 命令预览", "scrcpy Command Preview"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = commandPreview,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = false,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val copyLabel = l10n("复制命令", "Copy Command")
                    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(commandPreview))
                            viewModel.notifyCommandCopied()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = copyLabel, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(copyLabel)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            RuntimeControls(selectedDevice = statusDeviceId, viewModel = viewModel)
        }

        MirrorToast(visible = showDialog, message = dialogMessage)
    }
}

@Composable
private fun HeaderSection(statusDeviceId: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = l10n("设备镜像", "Device Mirror"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = l10n(
                    "通过 scrcpy 在独立窗口中查看并控制 Android 设备",
                    "Use scrcpy to view and control Android devices in a standalone window"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            color = Color(0xFFF3F4F6),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = statusDeviceId?.let { l10n("已连接 $it", "Connected $it") }
                    ?: l10n("未连接设备", "No device connected"),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (statusDeviceId == null) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF14532D)
            )
        }
    }
}

@Composable
private fun DeviceInfoRow(deviceId: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = l10n("当前设备：${deviceId ?: "-"}", "Current device: ${deviceId ?: "-"}"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (deviceId == null) "--" else "USB",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun SectionGroupTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun MirrorModeChip(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Color(0xFFEAF2FF) else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(label, fontSize = 13.sp)
    }
}

@Composable
private fun MirrorSwitchItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.width(180.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MirrorSelectField(
    label: String,
    value: Int?,
    placeholder: String,
    options: List<MirrorSelectOption>,
    modifier: Modifier = Modifier,
    onSelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        FormLabel(label)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value?.toString() ?: placeholder,
                        color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
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
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RuntimeControls(
    selectedDevice: String?,
    viewModel: DeviceMirrorViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(l10n("设备快捷控制", "Device Quick Controls"))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MirrorControlButton(icon = Icons.Default.ArrowBack, label = l10n("返回", "Back")) {
                viewModel.sendKeyEvent(selectedDevice, MirrorKeyCode.BACK)
            }
            MirrorControlButton(icon = Icons.Default.Home, label = l10n("主页", "Home")) {
                viewModel.sendKeyEvent(selectedDevice, MirrorKeyCode.HOME)
            }
            MirrorControlButton(icon = Icons.Default.ViewAgenda, label = l10n("最近任务", "Recent")) {
                viewModel.sendKeyEvent(selectedDevice, MirrorKeyCode.RECENT)
            }
            MirrorControlButton(icon = Icons.Default.PowerSettingsNew, label = l10n("电源", "Power")) {
                viewModel.sendKeyEvent(selectedDevice, MirrorKeyCode.POWER)
            }
            MirrorControlButton(icon = Icons.Default.VolumeDown, label = l10n("音量减", "Vol -")) {
                viewModel.sendKeyEvent(selectedDevice, MirrorKeyCode.VOLUME_DOWN)
            }
            MirrorControlButton(icon = Icons.Default.VolumeUp, label = l10n("音量加", "Vol +")) {
                viewModel.sendKeyEvent(selectedDevice, MirrorKeyCode.VOLUME_UP)
            }
            MirrorControlButton(icon = Icons.Default.CameraAlt, label = l10n("截图", "Screenshot")) {
                viewModel.takeScreenSnapshot(selectedDevice)
            }
            MirrorControlButton(icon = Icons.Default.ScreenRotation, label = l10n("旋转屏幕", "Rotate")) {
                viewModel.rotateScreen(selectedDevice)
            }
        }
    }
}

@Composable
private fun MirrorControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
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

    val text = when (message) {
        is MsgContent.Resource -> stringResource(message.stringResource, *message.args.toTypedArray())
        is MsgContent.Text -> message.text
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            shadowElevation = 6.dp,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp
            )
        }
    }
}
