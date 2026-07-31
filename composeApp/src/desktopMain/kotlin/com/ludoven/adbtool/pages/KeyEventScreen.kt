package com.ludoven.adbtool.pages

import com.ludoven.adbtool.ui.mac.*

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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.TextFieldDefaults
import com.ludoven.adbtool.ui.mac.AlertDialog
import com.ludoven.adbtool.ui.mac.Button
import com.ludoven.adbtool.ui.mac.ButtonDefaults
import com.ludoven.adbtool.ui.mac.Checkbox
import com.ludoven.adbtool.ui.mac.HorizontalDivider
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.OutlinedButton
import com.ludoven.adbtool.ui.mac.OutlinedTextField
import com.ludoven.adbtool.ui.mac.OutlinedTextFieldDefaults
import com.ludoven.adbtool.ui.mac.Surface
import com.ludoven.adbtool.ui.mac.Switch
import com.ludoven.adbtool.ui.mac.Text
import com.ludoven.adbtool.ui.mac.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.QadbColors
import com.ludoven.adbtool.QadbPalette
import com.ludoven.adbtool.ui.icons.IconParkIcons
import com.ludoven.adbtool.viewmodel.KeyEventRecord
import com.ludoven.adbtool.viewmodel.KeyEventViewModel
import com.ludoven.adbtool.widget.InlineStatusBanner
import com.ludoven.adbtool.widget.InlineStatusTone
import com.ludoven.adbtool.util.l10n
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
    val tint: Color = QadbPalette.Blue
)

@Composable
fun KeyEventScreen(
    viewModel: KeyEventViewModel,
    selectedDevice: String? = null,
    deviceDisplayNames: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
    showHeader: Boolean = true,
    showConnectionWarning: Boolean = true,
    useInternalScroll: Boolean = true
) {
    val showToast by viewModel.showToast.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()
    val recentRecords by viewModel.recentKeyEvents.collectAsState()
    val textInput by viewModel.textInput.collectAsState()
    val isSendingText by viewModel.isSendingText.collectAsState()
    val textSendMessage by viewModel.textSendMessage.collectAsState()

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
    val contentSpacing = if (embedded) UiTokens.SpaceMedium else 12.dp
    val contentPadding = PaddingValues(if (embedded) 0.dp else 16.dp)
    val rootModifier = if (useInternalScroll) {
        modifier.fillMaxSize()
    } else {
        modifier.fillMaxWidth()
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

    Box(modifier = rootModifier) {
        if (useInternalScroll) {
            LazyColumn(
                state = scrollState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    KeyEventContent(
                        modifier = Modifier.fillMaxWidth(),
                        contentSpacing = contentSpacing,
                        selectedDevice = selectedDevice,
                        showHeader = showHeader,
                        showConnectionWarning = showConnectionWarning,
                        connectedText = connectedText,
                        customCode = customCode,
                        onCustomCodeChange = { customCode = it.filter(Char::isDigit) },
                        longPressMode = longPressMode,
                        showAdbCommand = showAdbCommand,
                        onLongPressModeChange = { longPressMode = it },
                        onShowAdbCommandChange = { showAdbCommand = it },
                        onSendCustomCode = {
                            val code = customCode.toIntOrNull() ?: return@KeyEventContent
                            dispatchKey(code, "KeyCode($code)")
                            customCode = ""
                        },
                        previewRecord = previewRecord,
                        recentRecords = recentRecords,
                        onAction = { action -> dispatchKey(action.code, action.commandName) },
                        onCopyPreview = { copyToClipboard(previewRecord.adbCommand) },
                        onClearRecent = viewModel::clearRecentKeyEvents,
                        textInput = textInput,
                        onTextInputChange = viewModel::updateTextInput,
                        isSendingText = isSendingText,
                        textSendMessage = textSendMessage,
                        onSendText = { viewModel.sendText(selectedDevice) }
                    )
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        } else {
            KeyEventContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                contentSpacing = contentSpacing,
                selectedDevice = selectedDevice,
                showHeader = showHeader,
                showConnectionWarning = showConnectionWarning,
                connectedText = connectedText,
                customCode = customCode,
                onCustomCodeChange = { customCode = it.filter(Char::isDigit) },
                longPressMode = longPressMode,
                showAdbCommand = showAdbCommand,
                onLongPressModeChange = { longPressMode = it },
                onShowAdbCommandChange = { showAdbCommand = it },
                onSendCustomCode = {
                    val code = customCode.toIntOrNull() ?: return@KeyEventContent
                    dispatchKey(code, "KeyCode($code)")
                    customCode = ""
                },
                previewRecord = previewRecord,
                recentRecords = recentRecords,
                onAction = { action -> dispatchKey(action.code, action.commandName) },
                onCopyPreview = { copyToClipboard(previewRecord.adbCommand) },
                onClearRecent = viewModel::clearRecentKeyEvents,
                textInput = textInput,
                onTextInputChange = viewModel::updateTextInput,
                isSendingText = isSendingText,
                textSendMessage = textSendMessage,
                onSendText = { viewModel.sendText(selectedDevice) }
            )
        }

        if (showToast) {
            toastMsg?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(UiTokens.RadiusLarge),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = UiTokens.SpaceXXLarge)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(horizontal = UiTokens.SpaceXLarge, vertical = UiTokens.SpaceMedium),
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
                Column(verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)) {
                    Text(l10n("按键: $lpName", "Key: $lpName"))
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
                    modifier = Modifier.fillMaxWidth().padding(UiTokens.SpaceSmall),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showLongPress = false }) {
                        Text(stringResource(Res.string.cancel))
                    }
                    Spacer(Modifier.width(UiTokens.SpaceSmall))
                    Button(
                        onClick = {
                            showLongPress = false
                            viewModel.sendLongPressEvent(lpCode, duration.toLongOrNull() ?: 1000L, lpName)
                        },
                        shape = RoundedCornerShape(UiTokens.RadiusSmall),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
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
private fun KeyEventContent(
    modifier: Modifier = Modifier,
    contentSpacing: Dp,
    selectedDevice: String?,
    showHeader: Boolean,
    showConnectionWarning: Boolean,
    connectedText: String,
    customCode: String,
    onCustomCodeChange: (String) -> Unit,
    longPressMode: Boolean,
    showAdbCommand: Boolean,
    onLongPressModeChange: (Boolean) -> Unit,
    onShowAdbCommandChange: (Boolean) -> Unit,
    onSendCustomCode: () -> Unit,
    previewRecord: KeyEventRecord,
    recentRecords: List<KeyEventRecord>,
    onAction: (KeyAction) -> Unit,
    onCopyPreview: () -> Unit,
    onClearRecent: () -> Unit,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    isSendingText: Boolean,
    textSendMessage: String?,
    onSendText: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(contentSpacing)
    ) {
        if (showHeader) {
            Header(connectedText = connectedText, connected = !selectedDevice.isNullOrBlank())
        }

        if (showConnectionWarning && selectedDevice.isNullOrBlank()) {
            InlineStatusBanner(
                text = l10n("当前没有选择设备。按键面板可预览命令，发送前需要先连接并选择设备。", "No device is selected. The key panel can preview commands, but sending needs a connected selected device."),
                tone = InlineStatusTone.Warning,
                icon = IconParkIcons.Info
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 760.dp
            val rightContent: @Composable ColumnScope.() -> Unit = {
                DeviceTextInputPanel(
                    text = textInput,
                    onTextChange = onTextInputChange,
                    isSending = isSendingText,
                    message = textSendMessage,
                    onSend = onSendText
                )
                AdvancedOperationsPanel(
                    customCode = customCode,
                    onCustomCodeChange = onCustomCodeChange,
                    longPressMode = longPressMode,
                    showAdbCommand = showAdbCommand,
                    onLongPressModeChange = onLongPressModeChange,
                    onShowAdbCommandChange = onShowAdbCommandChange,
                    onSend = onSendCustomCode,
                    previewRecord = previewRecord,
                    onCopyPreview = onCopyPreview
                )
                RecentSentPanel(
                    records = recentRecords,
                    onClear = onClearRecent
                )
            }

            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)) {
                    VisualKeyPanel(
                        showAdbCommand = showAdbCommand,
                        onAction = onAction
                    )
                    rightContent()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
                    ) {
                        VisualKeyPanel(
                            showAdbCommand = showAdbCommand,
                            onAction = onAction
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium),
                        content = rightContent
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
private fun DeviceTextInputPanel(
    text: String,
    onTextChange: (String) -> Unit,
    isSending: Boolean,
    message: String?,
    onSend: () -> Unit
) {
    SectionSurface(
        title = l10n("发送文本", "Send text"),
        icon = null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val textInputInteractionSource = remember { MutableInteractionSource() }
                val textInputColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    enabled = !isSending,
                    modifier = Modifier.weight(1f).height(UiTokens.ControlHeight),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    interactionSource = textInputInteractionSource,
                    decorationBox = { innerTextField ->
                        TextFieldDefaults.OutlinedTextFieldDecorationBox(
                            value = text,
                            visualTransformation = VisualTransformation.None,
                            innerTextField = innerTextField,
                            placeholder = { Text(l10n("输入要发送到设备的文本", "Text to send to device"), maxLines = 1) },
                            singleLine = true,
                            enabled = !isSending,
                            isError = false,
                            interactionSource = textInputInteractionSource,
                            colors = textInputColors,
                            contentPadding = PaddingValues(
                                horizontal = UiTokens.SpaceMedium,
                                vertical = UiTokens.SpaceSmall
                            ),
                            border = {
                                TextFieldDefaults.BorderBox(
                                    enabled = !isSending,
                                    isError = false,
                                    interactionSource = textInputInteractionSource,
                                    colors = textInputColors,
                                    shape = RoundedCornerShape(UiTokens.RadiusMedium)
                                )
                            }
                        )
                    }
                )
                Button(
                    onClick = onSend,
                    enabled = !isSending && text.isNotBlank(),
                    modifier = Modifier.height(UiTokens.ControlHeight),
                    shape = RoundedCornerShape(UiTokens.RadiusMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        disabledContentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(IconParkIcons.Send, contentDescription = null, modifier = Modifier.size(UiTokens.IconSmall))
                    Spacer(Modifier.width(UiTokens.SpaceSmall))
                    Text(if (isSending) l10n("发送中", "Sending") else l10n("发送", "Send"))
                }
            }
            message?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Header(connectedText: String, connected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = UiTokens.SpaceXSmall, vertical = UiTokens.SpaceXSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Keyboard,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(UiTokens.SpaceLarge))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)) {
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
            shape = RoundedCornerShape(UiTokens.RadiusLarge),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = UiTokens.SpaceMedium, vertical = UiTokens.SpaceSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (connected) QadbColors.success else MaterialTheme.colorScheme.outline)
                )
                Text(
                    text = connectedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
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
        title = l10n("遥控器", "Remote control"),
        icon = null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 608.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
        ) {
            DPad(onAction = onAction)

            Column(verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    listOf(navAction(), homeAction(), recentAction(), menuAction()),
                    listOf(powerAction(), volumeUpAction(), volumeDownAction(), muteAction()),
                    listOf(notificationAction(), quickSettingsAction(), screenToggleAction(), screenshotAction())
                ).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
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
    val down = KeyAction(KC.DPAD_DOWN, Res.string.key_down, "KEYCODE_DPAD_DOWN", IconParkIcons.ArrowDown)
    val left = KeyAction(KC.DPAD_LEFT, Res.string.key_left, "KEYCODE_DPAD_LEFT", Icons.AutoMirrored.Filled.KeyboardArrowLeft)
    val right = KeyAction(KC.DPAD_RIGHT, Res.string.key_right, "KEYCODE_DPAD_RIGHT", IconParkIcons.Right)
    val ok = KeyAction(KC.DPAD_OK, Res.string.confirm, "KEYCODE_DPAD_CENTER", IconParkIcons.CheckCircle)

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)) {
        DirectionButton(action = up, width = 112.dp, height = 56.dp) { onAction(up) }
        Row(horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall), verticalAlignment = Alignment.CenterVertically) {
            DirectionButton(action = left, width = 60.dp, height = 108.dp) { onAction(left) }
            OkButton { onAction(ok) }
            DirectionButton(action = right, width = 60.dp, height = 108.dp) { onAction(right) }
        }
        DirectionButton(action = down, width = 112.dp, height = 56.dp) { onAction(down) }
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
        shape = RoundedCornerShape(50),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
        onClick = onClick
    ) { hovered ->
        Icon(
            imageVector = action.icon,
            contentDescription = label,
            tint = if (hovered) MaterialTheme.colorScheme.primary else QadbColors.primary,
            modifier = Modifier.size(UiTokens.IconLarge)
        )
    }
}

@Composable
private fun OkButton(onClick: () -> Unit) {
    PressableSurface(
        modifier = Modifier.size(88.dp),
        shape = CircleShape,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
        onClick = onClick
    ) { hovered ->
        Text(
            text = "OK",
            color = if (hovered) MaterialTheme.colorScheme.primary else QadbColors.primary,
            fontWeight = FontWeight.Bold,
            fontSize = UiTokens.TextSection
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
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(UiTokens.RadiusMedium),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f),
        onClick = onClick
    ) { hovered ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = action.icon,
                contentDescription = label,
                tint = if (hovered) MaterialTheme.colorScheme.primary else action.tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(UiTokens.SpaceXSmall))
            Text(
                text = if (showAdbCommand) action.code.toString() else keyActionDisplayLabel(action, label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
private fun AdvancedOperationsPanel(
    customCode: String,
    onCustomCodeChange: (String) -> Unit,
    longPressMode: Boolean,
    showAdbCommand: Boolean,
    onLongPressModeChange: (Boolean) -> Unit,
    onShowAdbCommandChange: (Boolean) -> Unit,
    onSend: () -> Unit,
    previewRecord: KeyEventRecord,
    onCopyPreview: () -> Unit
) {
    SectionSurface(
        title = l10n("高级操作", "Advanced operations"),
        icon = null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.key_custom_keycode),
                    modifier = Modifier.wrapContentWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                val customCodeInteractionSource = remember { MutableInteractionSource() }
                val customCodeColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
                BasicTextField(
                    value = customCode,
                    onValueChange = onCustomCodeChange,
                    modifier = Modifier.weight(1f).height(UiTokens.ControlHeight),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    interactionSource = customCodeInteractionSource,
                    decorationBox = { innerTextField ->
                        TextFieldDefaults.OutlinedTextFieldDecorationBox(
                            value = customCode,
                            visualTransformation = VisualTransformation.None,
                            innerTextField = innerTextField,
                            placeholder = { Text(stringResource(Res.string.custom_keycode_hint), maxLines = 1) },
                            singleLine = true,
                            enabled = true,
                            isError = false,
                            interactionSource = customCodeInteractionSource,
                            colors = customCodeColors,
                            contentPadding = PaddingValues(
                                horizontal = UiTokens.SpaceMedium,
                                vertical = UiTokens.SpaceSmall
                            ),
                            border = {
                                TextFieldDefaults.BorderBox(
                                    enabled = true,
                                    isError = false,
                                    interactionSource = customCodeInteractionSource,
                                    colors = customCodeColors,
                                    shape = RoundedCornerShape(UiTokens.RadiusMedium)
                                )
                            }
                        )
                    }
                )
                Button(
                    onClick = onSend,
                    enabled = customCode.isNotBlank(),
                    modifier = Modifier.height(UiTokens.ControlHeight).widthIn(min = 64.dp),
                    shape = RoundedCornerShape(UiTokens.RadiusMedium),
                    contentPadding = PaddingValues(horizontal = UiTokens.SpaceMedium, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        disabledContentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(IconParkIcons.Send, contentDescription = null, modifier = Modifier.size(UiTokens.IconMedium))
                    Spacer(Modifier.width(UiTokens.SpaceSmall))
                    Text(stringResource(Res.string.key_send), fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            AdvancedSwitchRow(
                label = stringResource(Res.string.key_long_press_mode),
                checked = longPressMode,
                onCheckedChange = onLongPressModeChange
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            AdvancedSwitchRow(
                label = stringResource(Res.string.key_show_adb_command),
                checked = showAdbCommand,
                onCheckedChange = onShowAdbCommandChange
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(UiTokens.RadiusMedium),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier.padding(UiTokens.SpaceMedium),
                    verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
                ) {
                    Text(
                        text = stringResource(Res.string.key_command_preview),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = previewRecord.adbCommand,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = UiTokens.TextBody,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        OutlinedButton(
                            onClick = onCopyPreview,
                            shape = RoundedCornerShape(UiTokens.RadiusMedium),
                            contentPadding = PaddingValues(
                                horizontal = UiTokens.SpaceMedium,
                                vertical = UiTokens.SpaceSmall
                            )
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(UiTokens.IconSmall)
                            )
                            Spacer(Modifier.width(UiTokens.SpaceSmall))
                            Text(stringResource(Res.string.key_copy))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            IconParkIcons.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = UiTokens.SpaceXSmall).size(UiTokens.IconSmall)
        )
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RecentSentPanel(records: List<KeyEventRecord>, onClear: () -> Unit) {
    SectionSurface(
        title = l10n("最近操作", "Recent operations"),
        icon = IconParkIcons.Schedule,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                if (records.isNotEmpty()) {
                    TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = UiTokens.SpaceSmall)) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(UiTokens.IconSmall))
                        Spacer(Modifier.width(UiTokens.SpaceSmall))
                        Text(stringResource(Res.string.key_clear_records), fontSize = UiTokens.TextCaption)
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(UiTokens.RadiusMedium),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
            ) {
                if (records.isEmpty()) {
                    Text(
                        text = l10n("暂无发送记录", "No send history"),
                        modifier = Modifier.padding(horizontal = UiTokens.SpaceMedium, vertical = UiTokens.SpaceLarge),
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
        modifier = Modifier.fillMaxWidth().height(UiTokens.ControlHeight).padding(horizontal = UiTokens.SpaceMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = iconForCode(record.code),
            contentDescription = null,
            tint = if (record.code == KC.POWER) QadbColors.danger else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(UiTokens.IconMedium)
        )
        Spacer(Modifier.width(UiTokens.SpaceSmall))
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
    KC.HOME -> IconParkIcons.Home
    KC.BACK -> Icons.AutoMirrored.Filled.ArrowBack
    KC.POWER -> Icons.Default.PowerSettingsNew
    KC.VOL_UP -> Icons.AutoMirrored.Filled.VolumeUp
    KC.VOL_DOWN -> Icons.AutoMirrored.Filled.VolumeDown
    else -> Icons.Default.Keyboard
}

@Composable
private fun keyActionDisplayLabel(action: KeyAction, fallback: String): String = when (action.code) {
    KC.MENU -> l10n("菜单", "Menu")
    KC.POWER -> l10n("电源", "Power")
    KC.VOL_UP -> l10n("音量+", "Volume +")
    KC.VOL_DOWN -> l10n("音量-", "Volume -")
    else -> fallback
}

@Composable
private fun SectionSurface(
    title: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(UiTokens.RadiusLarge),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.68f)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(UiTokens.SpaceLarge)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(UiTokens.SpaceMedium))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.height(UiTokens.SpaceLarge))
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
    val bg by animateColorAsState(
        if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else if (hovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(UiTokens.HoverDurationMillis)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, if (hovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.40f) else borderColor, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content(hovered)
    }
}

private fun navAction() = KeyAction(KC.BACK, Res.string.key_back, "KEYCODE_BACK", Icons.AutoMirrored.Filled.ArrowBack, QadbPalette.Slate)
private fun homeAction() = KeyAction(KC.HOME, Res.string.key_home, "KEYCODE_HOME", IconParkIcons.Home, QadbPalette.Slate)
private fun recentAction() = KeyAction(KC.RECENT, Res.string.key_recent, "KEYCODE_APP_SWITCH", Icons.Default.ContentCopy, QadbPalette.Slate)
private fun menuAction() = KeyAction(KC.MENU, Res.string.key_menu, "KEYCODE_MENU", Icons.Default.Menu, QadbPalette.Slate)
private fun powerAction() = KeyAction(KC.POWER, Res.string.key_power, "KEYCODE_POWER", Icons.Default.PowerSettingsNew, QadbPalette.Orange)
private fun volumeUpAction() = KeyAction(KC.VOL_UP, Res.string.key_volume_up, "KEYCODE_VOLUME_UP", Icons.AutoMirrored.Filled.VolumeUp, QadbPalette.Blue)
private fun volumeDownAction() = KeyAction(KC.VOL_DOWN, Res.string.key_volume_down, "KEYCODE_VOLUME_DOWN", Icons.AutoMirrored.Filled.VolumeDown, QadbPalette.Blue)
private fun muteAction() = KeyAction(KC.VOL_MUTE, Res.string.key_volume_mute, "KEYCODE_VOLUME_MUTE", Icons.AutoMirrored.Filled.VolumeMute, QadbPalette.Blue)
private fun notificationAction() = KeyAction(KC.NOTIFICATION, Res.string.key_status_bar, "KEYCODE_NOTIFICATION", Icons.Default.Bookmark, QadbPalette.Purple)
private fun quickSettingsAction() = KeyAction(KC.SETTINGS, Res.string.key_quick_settings, "KEYCODE_SETTINGS", IconParkIcons.Setting, QadbPalette.Blue)
private fun screenToggleAction() = KeyAction(KC.WAKEUP, Res.string.key_screen_toggle, "KEYCODE_WAKEUP", Icons.Default.WbSunny, QadbPalette.Orange)
private fun screenshotAction() = KeyAction(KC.SYSRQ, Res.string.key_screenshot_short, "KEYCODE_SYSRQ", Icons.Default.CropFree, QadbPalette.Green)

private fun copyToClipboard(text: String) {
    runCatching {
        Toolkit.getDefaultToolkit()
            .systemClipboard
            .setContents(StringSelection(text), null)
    }
}
