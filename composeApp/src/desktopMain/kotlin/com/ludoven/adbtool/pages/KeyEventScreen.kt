package com.ludoven.adbtool.pages

import adbtool_desktop.composeapp.generated.resources.Res
import adbtool_desktop.composeapp.generated.resources.cancel
import adbtool_desktop.composeapp.generated.resources.confirm
import adbtool_desktop.composeapp.generated.resources.connected
import adbtool_desktop.composeapp.generated.resources.custom_keycode_hint
import adbtool_desktop.composeapp.generated.resources.key_back
import adbtool_desktop.composeapp.generated.resources.key_clear
import adbtool_desktop.composeapp.generated.resources.key_clear_records
import adbtool_desktop.composeapp.generated.resources.key_command_preview
import adbtool_desktop.composeapp.generated.resources.key_command_preview_hint
import adbtool_desktop.composeapp.generated.resources.key_copy
import adbtool_desktop.composeapp.generated.resources.key_custom_keycode
import adbtool_desktop.composeapp.generated.resources.key_down
import adbtool_desktop.composeapp.generated.resources.key_event_subtitle
import adbtool_desktop.composeapp.generated.resources.key_event_title
import adbtool_desktop.composeapp.generated.resources.key_home
import adbtool_desktop.composeapp.generated.resources.key_left
import adbtool_desktop.composeapp.generated.resources.key_long_press_hint
import adbtool_desktop.composeapp.generated.resources.key_long_press_mode
import adbtool_desktop.composeapp.generated.resources.key_long_press_send
import adbtool_desktop.composeapp.generated.resources.key_long_press_title
import adbtool_desktop.composeapp.generated.resources.key_menu
import adbtool_desktop.composeapp.generated.resources.key_power
import adbtool_desktop.composeapp.generated.resources.key_quick_settings
import adbtool_desktop.composeapp.generated.resources.key_recent
import adbtool_desktop.composeapp.generated.resources.key_recent_sent
import adbtool_desktop.composeapp.generated.resources.key_right
import adbtool_desktop.composeapp.generated.resources.key_screen_toggle
import adbtool_desktop.composeapp.generated.resources.key_screenshot_short
import adbtool_desktop.composeapp.generated.resources.key_send
import adbtool_desktop.composeapp.generated.resources.key_show_adb_command
import adbtool_desktop.composeapp.generated.resources.key_status_bar
import adbtool_desktop.composeapp.generated.resources.key_up
import adbtool_desktop.composeapp.generated.resources.key_visual_panel
import adbtool_desktop.composeapp.generated.resources.key_volume_down
import adbtool_desktop.composeapp.generated.resources.key_volume_mute
import adbtool_desktop.composeapp.generated.resources.key_volume_up
import adbtool_desktop.composeapp.generated.resources.no_device
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.viewmodel.KeyEventRecord
import com.ludoven.adbtool.viewmodel.KeyEventViewModel
import com.ludoven.adbtool.widget.GlassCard
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private object KC {
    const val BACK = 4
    const val HOME = 3
    const val RECENT = 187
    const val POWER = 26
    const val MENU = 82
    const val VOL_UP = 24
    const val VOL_DOWN = 25
    const val VOL_MUTE = 91
    const val NOTIFICATION = 83
    const val SETTINGS = 176
    const val WAKEUP = 224
    const val SYSRQ = 120
    const val DPAD_UP = 19
    const val DPAD_DOWN = 20
    const val DPAD_LEFT = 21
    const val DPAD_RIGHT = 22
    const val DPAD_OK = 23
}

private data class KeyAction(
    val code: Int,
    val titleRes: StringResource,
    val commandName: String,
    val icon: ImageVector,
    val tint: Color = Color(0xFF2F6BFF)
)

@Composable
fun KeyEventScreen(
    viewModel: KeyEventViewModel,
    selectedDevice: String? = null,
    deviceDisplayNames: Map<String, String> = emptyMap()
) {
    val showToast by viewModel.showToast.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()
    val recentRecords by viewModel.recentKeyEvents.collectAsState()

    var customCode by remember { mutableStateOf("") }
    var longPressMode by remember { mutableStateOf(false) }
    var showAdbCommand by remember { mutableStateOf(false) }
    var showLongPress by remember { mutableStateOf(false) }
    var lpCode by remember { mutableStateOf(0) }
    var lpName by remember { mutableStateOf("") }
    var previewRecord by remember {
        mutableStateOf(KeyEventRecord(KC.HOME, "KEYCODE_HOME", ""))
    }

    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
    val deviceName = selectedDevice?.let { deviceDisplayNames[it].orEmpty().ifBlank { it } }
    val connectedText = if (deviceName.isNullOrBlank()) {
        stringResource(Res.string.no_device)
    } else {
        "${stringResource(Res.string.connected)} · $deviceName"
    }

    fun dispatchKey(code: Int, commandName: String) {
        previewRecord = KeyEventRecord(code, commandName, "")
        if (longPressMode) {
            lpCode = code
            lpName = commandName
            showLongPress = true
        } else {
            viewModel.sendKeyEvent(code, commandName)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = scrollState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Header(connectedText = connectedText, connected = !selectedDevice.isNullOrBlank())
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1.05f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VisualKeyPanel(
                            showAdbCommand = showAdbCommand,
                            onAction = { action -> dispatchKey(action.code, action.commandName) }
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CustomKeyCodePanel(
                            customCode = customCode,
                            onCustomCodeChange = { customCode = it.filter(Char::isDigit) },
                            longPressMode = longPressMode,
                            showAdbCommand = showAdbCommand,
                            onLongPressModeChange = { longPressMode = it },
                            onShowAdbCommandChange = { showAdbCommand = it },
                            onSend = {
                                val code = customCode.toIntOrNull() ?: return@CustomKeyCodePanel
                                dispatchKey(code, "KeyCode($code)")
                                customCode = ""
                            },
                            onClear = { customCode = "" }
                        )
                        CommandPreview(
                            record = previewRecord,
                            onCopy = { copyToClipboard(previewRecord.adbCommand) }
                        )
                        RecentSentPanel(
                            records = recentRecords,
                            onClear = viewModel::clearRecentKeyEvents
                        )
                    }
                }
            }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }

    if (showToast) {
        toastMsg?.let { msg ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shadowElevation = 6.dp,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showLongPress) {
        var duration by remember { mutableStateOf("1000") }
        AlertDialog(
            onDismissRequest = { showLongPress = false },
            title = { Text(stringResource(Res.string.key_long_press_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("按键: $lpName")
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it.filter(Char::isDigit) },
                        label = { Text(stringResource(Res.string.key_long_press_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(UiTokens.RadiusMedium)
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showLongPress = false }) {
                        Text(stringResource(Res.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            showLongPress = false
                            viewModel.sendLongPressEvent(lpCode, duration.toLongOrNull() ?: 1000L, lpName)
                        },
                        shape = RoundedCornerShape(UiTokens.RadiusSmall)
                    ) {
                        Text(stringResource(Res.string.key_long_press_send))
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun Header(connectedText: String, connected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Keyboard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(Res.string.key_event_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.key_event_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (connected) Color(0xFF2DBE60) else MaterialTheme.colorScheme.outline)
                )
                Text(
                    text = connectedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun VisualKeyPanel(
    showAdbCommand: Boolean,
    onAction: (KeyAction) -> Unit
) {
    SectionSurface(
        title = stringResource(Res.string.key_visual_panel),
        icon = Icons.Default.CropFree,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            DPad(onAction = onAction)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    listOf(navAction(), homeAction(), recentAction(), menuAction()),
                    listOf(powerAction(), volumeUpAction(), volumeDownAction(), muteAction()),
                    listOf(notificationAction(), quickSettingsAction(), screenToggleAction(), screenshotAction())
                ).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { action ->
                            LargeKeyButton(
                                action = action,
                                showAdbCommand = showAdbCommand,
                                modifier = Modifier.weight(1f),
                                onClick = { onAction(action) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DPad(onAction: (KeyAction) -> Unit) {
    val up = KeyAction(KC.DPAD_UP, Res.string.key_up, "KEYCODE_DPAD_UP", Icons.Default.KeyboardArrowUp)
    val down = KeyAction(KC.DPAD_DOWN, Res.string.key_down, "KEYCODE_DPAD_DOWN", Icons.Default.KeyboardArrowDown)
    val left = KeyAction(KC.DPAD_LEFT, Res.string.key_left, "KEYCODE_DPAD_LEFT", Icons.Default.KeyboardArrowLeft)
    val right = KeyAction(KC.DPAD_RIGHT, Res.string.key_right, "KEYCODE_DPAD_RIGHT", Icons.Default.KeyboardArrowRight)
    val ok = KeyAction(KC.DPAD_OK, Res.string.confirm, "KEYCODE_DPAD_CENTER", Icons.Default.CheckCircle)

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DirectionButton(action = up, width = 92.dp, height = 78.dp) { onAction(up) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DirectionButton(action = left, width = 96.dp, height = 80.dp) { onAction(left) }
            OkButton { onAction(ok) }
            DirectionButton(action = right, width = 96.dp, height = 80.dp) { onAction(right) }
        }
        DirectionButton(action = down, width = 92.dp, height = 78.dp) { onAction(down) }
    }
}

@Composable
private fun DirectionButton(
    action: KeyAction,
    width: Dp,
    height: Dp,
    onClick: () -> Unit
) {
    val label = stringResource(action.titleRes)
    PressableSurface(
        modifier = Modifier.width(width).height(height),
        shape = RoundedCornerShape(16.dp),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
        onClick = onClick
    ) { hovered ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = action.icon,
                contentDescription = label,
                tint = if (hovered) MaterialTheme.colorScheme.primary else Color(0xFF3578FF),
                modifier = Modifier.size(32.dp)
            )
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun OkButton(onClick: () -> Unit) {
    PressableSurface(
        modifier = Modifier.size(104.dp),
        shape = CircleShape,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
        onClick = onClick
    ) { hovered ->
        Text(
            text = "OK",
            color = if (hovered) MaterialTheme.colorScheme.primary else Color(0xFF3578FF),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun LargeKeyButton(
    action: KeyAction,
    showAdbCommand: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val label = stringResource(action.titleRes)
    PressableSurface(
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(12.dp),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f),
        onClick = onClick
    ) { hovered ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = action.icon,
                contentDescription = label,
                tint = if (hovered) MaterialTheme.colorScheme.primary else action.tint,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = if (showAdbCommand) action.code.toString() else label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CustomKeyCodePanel(
    customCode: String,
    onCustomCodeChange: (String) -> Unit,
    longPressMode: Boolean,
    showAdbCommand: Boolean,
    onLongPressModeChange: (Boolean) -> Unit,
    onShowAdbCommandChange: (Boolean) -> Unit,
    onSend: () -> Unit,
    onClear: () -> Unit
) {
    SectionSurface(
        title = stringResource(Res.string.key_custom_keycode),
        icon = Icons.Default.Code,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = customCode,
                onValueChange = onCustomCodeChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.custom_keycode_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSend,
                    enabled = customCode.isNotBlank(),
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF2F6BFF), Color(0xFF477DFF))),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(stringResource(Res.string.key_send), color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.key_clear))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = longPressMode, onCheckedChange = onLongPressModeChange)
                Text(
                    text = stringResource(Res.string.key_long_press_mode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp).size(16.dp))
                Spacer(Modifier.weight(1f))
                Switch(checked = showAdbCommand, onCheckedChange = onShowAdbCommandChange)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.key_show_adb_command),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
private fun CommandPreview(record: KeyEventRecord, onCopy: () -> Unit) {
    SectionSurface(
        title = stringResource(Res.string.key_command_preview),
        icon = Icons.Default.Code,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.adbCommand,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                OutlinedButton(
                    onClick = onCopy,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.key_copy))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.key_command_preview_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecentSentPanel(records: List<KeyEventRecord>, onClear: () -> Unit) {
    SectionSurface(
        title = stringResource(Res.string.key_recent_sent),
        icon = Icons.Default.Schedule,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                if (records.isNotEmpty()) {
                    TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(Res.string.key_clear_records), fontSize = 12.sp)
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
            ) {
                if (records.isEmpty()) {
                    Text(
                        text = "暂无发送记录",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column {
                        records.take(10).forEachIndexed { index, record ->
                            RecentRecordRow(record = record)
                            if (index != records.take(10).lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentRecordRow(record: KeyEventRecord) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = iconForCode(record.code),
            contentDescription = null,
            tint = if (record.code == KC.POWER) Color(0xFFFF4D4F) else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = record.displayText,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = record.sentAt,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun iconForCode(code: Int): ImageVector = when (code) {
    KC.HOME -> Icons.Default.Home
    KC.BACK -> Icons.Default.ArrowBack
    KC.POWER -> Icons.Default.PowerSettingsNew
    KC.VOL_UP -> Icons.Default.VolumeUp
    KC.VOL_DOWN -> Icons.Default.VolumeDown
    else -> Icons.Default.Keyboard
}

@Composable
private fun SectionSurface(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun PressableSurface(
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    borderColor: Color,
    onClick: () -> Unit,
    content: @Composable (hovered: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else if (hovered) 1.02f else 1f)
    val bg by animateColorAsState(
        if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else if (hovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        else MaterialTheme.colorScheme.surface
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(bg)
            .border(1.dp, if (hovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.40f) else borderColor, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content(hovered)
    }
}

private fun navAction() = KeyAction(KC.BACK, Res.string.key_back, "KEYCODE_BACK", Icons.Default.ArrowBack, Color(0xFF111827))
private fun homeAction() = KeyAction(KC.HOME, Res.string.key_home, "KEYCODE_HOME", Icons.Default.Home, Color(0xFF111827))
private fun recentAction() = KeyAction(KC.RECENT, Res.string.key_recent, "KEYCODE_APP_SWITCH", Icons.Default.ContentCopy, Color(0xFF111827))
private fun menuAction() = KeyAction(KC.MENU, Res.string.key_menu, "KEYCODE_MENU", Icons.Default.Menu, Color(0xFF111827))
private fun powerAction() = KeyAction(KC.POWER, Res.string.key_power, "KEYCODE_POWER", Icons.Default.PowerSettingsNew, Color(0xFFFF4D4F))
private fun volumeUpAction() = KeyAction(KC.VOL_UP, Res.string.key_volume_up, "KEYCODE_VOLUME_UP", Icons.Default.VolumeUp, Color(0xFF2F7DFF))
private fun volumeDownAction() = KeyAction(KC.VOL_DOWN, Res.string.key_volume_down, "KEYCODE_VOLUME_DOWN", Icons.Default.VolumeDown, Color(0xFF2F7DFF))
private fun muteAction() = KeyAction(KC.VOL_MUTE, Res.string.key_volume_mute, "KEYCODE_VOLUME_MUTE", Icons.Default.VolumeMute, Color(0xFF2F7DFF))
private fun notificationAction() = KeyAction(KC.NOTIFICATION, Res.string.key_status_bar, "KEYCODE_NOTIFICATION", Icons.Default.Bookmark, Color(0xFF9A5BFF))
private fun quickSettingsAction() = KeyAction(KC.SETTINGS, Res.string.key_quick_settings, "KEYCODE_SETTINGS", Icons.Default.Settings, Color(0xFF2F7DFF))
private fun screenToggleAction() = KeyAction(KC.WAKEUP, Res.string.key_screen_toggle, "KEYCODE_WAKEUP", Icons.Default.WbSunny, Color(0xFFFF9F1A))
private fun screenshotAction() = KeyAction(KC.SYSRQ, Res.string.key_screenshot_short, "KEYCODE_SYSRQ", Icons.Default.CropFree, Color(0xFF23B45D))

private fun copyToClipboard(text: String) {
    runCatching {
        Toolkit.getDefaultToolkit()
            .systemClipboard
            .setContents(StringSelection(text), null)
    }
}
