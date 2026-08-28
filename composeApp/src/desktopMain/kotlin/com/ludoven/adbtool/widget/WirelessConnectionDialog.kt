package com.ludoven.adbtool.widget

import adbtool_desktop.composeapp.generated.resources.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ludoven.adbtool.QadbColors
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.ui.icons.IconParkIcons
import com.ludoven.adbtool.util.WirelessAdbIssue
import com.ludoven.adbtool.util.WirelessAdbOperation
import com.ludoven.adbtool.util.WirelessAdbOperationResult
import com.ludoven.adbtool.util.l10n
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private enum class WirelessDialogMode {
    CONNECT,
    PAIR
}

@Immutable
private data class WirelessDialogColors(
    val surface: Color,
    val surfaceHover: Color,
    val surfaceDisabled: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val border: Color,
    val borderStrong: Color,
    val accent: Color,
    val onAccent: Color,
    val success: Color,
    val successSurface: Color,
    val danger: Color,
    val dangerSurface: Color
)

private val DialogTitleStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 22.sp,
    fontWeight = FontWeight.Bold
)
private val SectionLabelStyle = TextStyle(
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.SemiBold
)
private val BodyStyle = TextStyle(fontSize = 13.sp, lineHeight = 19.sp)
private val CaptionStyle = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
private val ActionStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.SemiBold
)

@Composable
private fun wirelessDialogColors() = WirelessDialogColors(
    surface = QadbColors.surface,
    surfaceHover = QadbColors.surfaceHover,
    surfaceDisabled = QadbColors.disabledSurface,
    textPrimary = QadbColors.textPrimary,
    textSecondary = QadbColors.textSecondary,
    textDisabled = QadbColors.textDisabled,
    border = QadbColors.border,
    borderStrong = QadbColors.borderStrong,
    accent = QadbColors.primary,
    onAccent = QadbColors.onPrimary,
    success = QadbColors.success,
    successSurface = QadbColors.successSurface,
    danger = QadbColors.danger,
    dangerSurface = QadbColors.errorSurface
)

@Composable
fun WirelessConnectionDialog(
    history: List<String>,
    onConnect: suspend (String) -> WirelessAdbOperationResult,
    onPair: suspend (String, String) -> WirelessAdbOperationResult,
    onRemoveHistory: (String) -> Unit,
    onDevicesChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf(WirelessDialogMode.CONNECT) }
    var connectAddress by remember { mutableStateOf(history.firstOrNull().orEmpty()) }
    var pairingAddress by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<WirelessAdbOperationResult?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val colors = wirelessDialogColors()

    fun startConnect(address: String) {
        if (isRunning) return
        connectAddress = address
        result = null
        isRunning = true
        coroutineScope.launch {
            val nextResult = runCatching { onConnect(address) }.getOrElse { error ->
                WirelessAdbOperationResult(
                    operation = WirelessAdbOperation.CONNECT,
                    success = false,
                    issue = WirelessAdbIssue.COMMAND_FAILED,
                    detail = error.message.orEmpty()
                )
            }
            result = nextResult
            isRunning = false
            if (nextResult.success) onDevicesChanged()
        }
    }

    fun startPair() {
        if (isRunning) return
        result = null
        isRunning = true
        coroutineScope.launch {
            val nextResult = runCatching { onPair(pairingAddress, pairingCode) }.getOrElse { error ->
                WirelessAdbOperationResult(
                    operation = WirelessAdbOperation.PAIR,
                    success = false,
                    issue = WirelessAdbIssue.COMMAND_FAILED,
                    detail = error.message.orEmpty()
                )
            }
            result = nextResult
            isRunning = false
            if (nextResult.success) onDevicesChanged()
        }
    }

    Dialog(onDismissRequest = { if (!isRunning) onDismiss() }) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .width(520.dp)
                .heightIn(max = 680.dp)
                .shadow(24.dp, shape)
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.border, shape)
        ) {
            // 1. Dialog Header
            WirelessDialogHeader(
                colors = colors,
                enabled = !isRunning,
                onDismiss = onDismiss
            )

            WirelessDivider(colors.border.copy(alpha = 0.6f))

            // 2. Dialog Content Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mode Segmented Capsule Selector
                WirelessModeSelector(
                    selectedMode = mode,
                    enabled = !isRunning,
                    colors = colors,
                    onSelected = {
                        mode = it
                        result = null
                    }
                )

                if (mode == WirelessDialogMode.CONNECT) {
                    DesktopTextField(
                        value = connectAddress,
                        onValueChange = {
                            connectAddress = it
                            result = null
                        },
                        label = stringResource(Res.string.wireless_address_label),
                        placeholder = stringResource(Res.string.wireless_address_hint),
                        icon = IconParkIcons.Link,
                        enabled = !isRunning,
                        colors = colors
                    )

                    WirelessHelperTip(
                        text = l10n(
                            "提示：手机与电脑需连接同一 Wi-Fi。在手机「设置 > 开发者选项 > 无线调试」中查看 IP 地址与端口号。",
                            "Tip: Connect phone and PC to same Wi-Fi. View IP & port in Settings > Developer Options > Wireless Debugging."
                        ),
                        colors = colors
                    )

                    if (history.isNotEmpty()) {
                        WirelessConnectionHistory(
                            history = history,
                            enabled = !isRunning,
                            colors = colors,
                            onReconnect = ::startConnect,
                            onRemove = onRemoveHistory
                        )
                    }
                } else {
                    DesktopTextField(
                        value = pairingAddress,
                        onValueChange = {
                            pairingAddress = it
                            result = null
                        },
                        label = stringResource(Res.string.wireless_pair_address_label),
                        placeholder = stringResource(Res.string.wireless_pair_address_hint),
                        icon = IconParkIcons.Link,
                        enabled = !isRunning,
                        colors = colors
                    )
                    DesktopTextField(
                        value = pairingCode,
                        onValueChange = { nextValue ->
                            pairingCode = nextValue.filter(Char::isDigit).take(6)
                            result = null
                        },
                        label = stringResource(Res.string.wireless_pairing_code_label),
                        placeholder = stringResource(Res.string.wireless_pairing_code_hint),
                        icon = IconParkIcons.Code,
                        enabled = !isRunning,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = colors
                    )

                    WirelessHelperTip(
                        text = l10n(
                            "提示：在手机「无线调试 > 使用配对码配对设备」弹窗中，查看 6 位配对码以及对应的配对端口号。",
                            "Tip: Tap 'Pair device with pairing code' in Wireless Debugging to view 6-digit code and pairing port."
                        ),
                        colors = colors
                    )
                }

                result?.let { WirelessOperationStatus(it, colors) }
            }

            WirelessDivider(colors.border.copy(alpha = 0.6f))

            // 3. Dialog Footer
            WirelessDialogFooter(
                mode = mode,
                isRunning = isRunning,
                colors = colors,
                onDismiss = onDismiss,
                onConfirm = {
                    if (mode == WirelessDialogMode.CONNECT) {
                        startConnect(connectAddress)
                    } else {
                        startPair()
                    }
                }
            )
        }
    }
}

@Composable
private fun WirelessDialogHeader(
    colors: WirelessDialogColors,
    enabled: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            WirelessIcon(
                imageVector = IconParkIcons.Wifi,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            BasicText(
                text = stringResource(Res.string.wireless_dialog_title),
                style = DialogTitleStyle.copy(color = colors.textPrimary)
            )
            BasicText(
                text = stringResource(Res.string.wireless_dialog_subtitle),
                style = CaptionStyle.copy(color = colors.textSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        DesktopIconButton(
            icon = IconParkIcons.Close,
            contentDescription = stringResource(Res.string.cancel),
            enabled = enabled,
            colors = colors,
            onClick = onDismiss
        )
    }
}

/**
 * Modern Segmented Capsule Switcher (iOS / macOS style).
 */
@Composable
private fun WirelessModeSelector(
    selectedMode: WirelessDialogMode,
    enabled: Boolean,
    colors: WirelessDialogColors,
    onSelected: (WirelessDialogMode) -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceHover.copy(alpha = 0.6f))
            .border(1.dp, colors.border.copy(alpha = 0.5f), shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WirelessModeTab(
            text = stringResource(Res.string.wireless_mode_connect),
            icon = IconParkIcons.Wifi,
            selected = selectedMode == WirelessDialogMode.CONNECT,
            enabled = enabled,
            colors = colors,
            onClick = { onSelected(WirelessDialogMode.CONNECT) },
            modifier = Modifier.weight(1f)
        )
        WirelessModeTab(
            text = stringResource(Res.string.wireless_mode_pair),
            icon = IconParkIcons.ShieldCheck,
            selected = selectedMode == WirelessDialogMode.PAIR,
            enabled = enabled,
            colors = colors,
            onClick = { onSelected(WirelessDialogMode.PAIR) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WirelessModeTab(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    colors: WirelessDialogColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val tabShape = RoundedCornerShape(8.dp)

    val background = when {
        selected -> colors.surface
        hovered && enabled -> colors.surface.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    val textColor = when {
        !enabled -> colors.textDisabled
        selected -> colors.accent
        else -> colors.textSecondary
    }
    val borderModifier = if (selected) {
        Modifier.border(1.dp, colors.border.copy(alpha = 0.4f), tabShape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .height(34.dp)
            .shadow(if (selected) 2.dp else 0.dp, tabShape, clip = false)
            .clip(tabShape)
            .background(background)
            .then(borderModifier)
            .hoverable(interactionSource, enabled)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Tab,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WirelessIcon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(15.dp)
            )
            BasicText(
                text = text,
                style = ActionStyle.copy(
                    color = textColor,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun DesktopTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
    colors: WirelessDialogColors,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(10.dp)

    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.border.copy(alpha = 0.4f)
            focused -> colors.accent
            hovered -> colors.accent.copy(alpha = 0.45f)
            else -> colors.borderStrong.copy(alpha = 0.65f)
        },
        animationSpec = tween(UiTokens.HoverDurationMillis)
    )
    val textColor = if (enabled) colors.textPrimary else colors.textDisabled

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BasicText(
            text = label,
            style = SectionLabelStyle.copy(
                color = if (focused && enabled) colors.accent else colors.textPrimary
            )
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(shape)
                .background(if (enabled) colors.surface else colors.surfaceDisabled)
                .border(if (focused) 1.5.dp else 1.dp, borderColor, shape)
                .hoverable(interactionSource),
            enabled = enabled,
            singleLine = true,
            interactionSource = interactionSource,
            keyboardOptions = keyboardOptions,
            cursorBrush = SolidColor(colors.accent),
            textStyle = BodyStyle.copy(color = textColor),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    icon?.let {
                        WirelessIcon(
                            imageVector = it,
                            contentDescription = null,
                            tint = if (focused && enabled) colors.accent else (if (enabled) colors.textSecondary else colors.textDisabled),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            BasicText(
                                text = placeholder,
                                style = BodyStyle.copy(color = colors.textDisabled),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
private fun WirelessHelperTip(
    text: String,
    colors: WirelessDialogColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceHover.copy(alpha = 0.45f))
            .border(1.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WirelessIcon(
            imageVector = IconParkIcons.Bulb,
            contentDescription = null,
            tint = Color(0xFFFF9F0A),
            modifier = Modifier.size(14.dp)
        )
        BasicText(
            text = text,
            style = CaptionStyle.copy(color = colors.textSecondary, lineHeight = 16.sp)
        )
    }
}

@Composable
private fun WirelessConnectionHistory(
    history: List<String>,
    enabled: Boolean,
    colors: WirelessDialogColors,
    onReconnect: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicText(
            text = stringResource(Res.string.wireless_recent_title),
            style = SectionLabelStyle.copy(color = colors.textSecondary)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.border.copy(alpha = 0.6f), shape)
        ) {
            history.forEachIndexed { index, endpoint ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WirelessIcon(
                        imageVector = IconParkIcons.Time,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    BasicText(
                        text = endpoint,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        style = BodyStyle.copy(color = colors.textPrimary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    DesktopLinkAction(
                        text = stringResource(Res.string.wireless_use_history),
                        enabled = enabled,
                        color = colors.accent,
                        disabledColor = colors.textDisabled,
                        onClick = { onReconnect(endpoint) }
                    )
                    Spacer(Modifier.width(8.dp))
                    DesktopLinkAction(
                        text = stringResource(Res.string.wireless_remove_history),
                        enabled = enabled,
                        color = colors.textSecondary,
                        disabledColor = colors.textDisabled,
                        onClick = { onRemove(endpoint) }
                    )
                }
                if (index < history.lastIndex) {
                    WirelessDivider(colors.border.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun WirelessOperationStatus(
    result: WirelessAdbOperationResult,
    colors: WirelessDialogColors
) {
    val containerColor = if (result.success) colors.successSurface else colors.dangerSurface
    val contentColor = if (result.success) colors.success else colors.danger
    val shape = RoundedCornerShape(10.dp)
    val message = when {
        result.success && result.operation == WirelessAdbOperation.CONNECT ->
            stringResource(Res.string.wireless_connect_success, result.endpoint.orEmpty())
        result.success -> stringResource(Res.string.wireless_pair_success, result.endpoint.orEmpty())
        else -> wirelessIssueMessage(result.issue)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .border(1.dp, contentColor.copy(alpha = 0.25f), shape)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WirelessIcon(
            imageVector = if (result.success) IconParkIcons.CheckCircle else IconParkIcons.ShieldAlert,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BasicText(
                text = message,
                style = BodyStyle.copy(color = contentColor, fontWeight = FontWeight.SemiBold)
            )
            if (!result.success && result.detail.isNotBlank()) {
                BasicText(
                    text = result.detail,
                    style = CaptionStyle.copy(color = colors.textSecondary),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WirelessDialogFooter(
    mode: WirelessDialogMode,
    isRunning: Boolean,
    colors: WirelessDialogColors,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DesktopActionButton(
            text = stringResource(Res.string.cancel),
            primary = false,
            enabled = !isRunning,
            colors = colors,
            onClick = onDismiss
        )
        Spacer(Modifier.width(10.dp))
        DesktopActionButton(
            text = stringResource(
                if (mode == WirelessDialogMode.CONNECT) {
                    Res.string.wireless_connect_action
                } else {
                    Res.string.wireless_pair_action
                }
            ),
            primary = true,
            enabled = !isRunning,
            loading = isRunning,
            colors = colors,
            onClick = onConfirm
        )
    }
}

@Composable
private fun DesktopActionButton(
    text: String,
    primary: Boolean,
    enabled: Boolean,
    colors: WirelessDialogColors,
    onClick: () -> Unit,
    loading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(10.dp)

    val background by animateColorAsState(
        targetValue = when {
            !enabled -> colors.surfaceDisabled
            primary && pressed -> colors.accent.copy(alpha = 0.78f)
            primary && hovered -> colors.accent.copy(alpha = 0.88f)
            primary -> colors.accent
            pressed -> colors.accent.copy(alpha = 0.14f)
            hovered -> colors.accent.copy(alpha = 0.08f)
            else -> colors.surface
        },
        animationSpec = tween(UiTokens.HoverDurationMillis)
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.border.copy(alpha = 0.4f)
            primary -> colors.accent
            hovered -> colors.accent.copy(alpha = 0.65f)
            else -> colors.borderStrong.copy(alpha = 0.7f)
        },
        animationSpec = tween(UiTokens.HoverDurationMillis)
    )
    val elevation by animateDpAsState(
        targetValue = if (hovered && enabled && !loading) (if (primary) 3.dp else 1.dp) else 0.dp,
        animationSpec = tween(UiTokens.HoverDurationMillis)
    )

    val contentColor = when {
        !enabled -> colors.textDisabled
        primary -> colors.onAccent
        hovered -> colors.accent
        else -> colors.textPrimary
    }

    Row(
        modifier = Modifier
            .widthIn(min = 92.dp)
            .height(38.dp)
            .shadow(elevation, shape, clip = false)
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .hoverable(interactionSource, enabled)
            .pointerHoverIcon(if (enabled && !loading) PointerIcon.Hand else PointerIcon.Default)
            .focusable(enabled, interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            DesktopLoadingIndicator(
                color = contentColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        BasicText(text = text, style = ActionStyle.copy(color = contentColor))
    }
}

@Composable
private fun DesktopLinkAction(
    text: String,
    enabled: Boolean,
    color: Color,
    disabledColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (hovered && enabled) color.copy(alpha = 0.08f) else Color.Transparent)
            .border(
                1.dp,
                if (hovered && enabled) color.copy(alpha = 0.3f) else Color.Transparent,
                shape
            )
            .hoverable(interactionSource, enabled)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = CaptionStyle.copy(
                color = if (enabled) color else disabledColor,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun DesktopIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    colors: WirelessDialogColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(shape)
            .background(if (hovered && enabled) colors.surfaceHover else Color.Transparent)
            .hoverable(interactionSource, enabled)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        WirelessIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (hovered && enabled) colors.textPrimary else (if (enabled) colors.textSecondary else colors.textDisabled),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun WirelessIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier
    )
}

@Composable
private fun DesktopLoadingIndicator(
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition()
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    Canvas(modifier) {
        drawArc(
            color = color,
            startAngle = rotation - 90f,
            sweepAngle = 265f,
            useCenter = false,
            style = Stroke(width = size.minDimension * 0.13f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun WirelessDivider(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

@Composable
private fun wirelessIssueMessage(issue: WirelessAdbIssue?): String = stringResource(
    when (issue) {
        WirelessAdbIssue.EMPTY_ADDRESS -> Res.string.wireless_error_empty_address
        WirelessAdbIssue.INVALID_ADDRESS -> Res.string.wireless_error_invalid_address
        WirelessAdbIssue.PORT_REQUIRED -> Res.string.wireless_error_port_required
        WirelessAdbIssue.INVALID_PORT -> Res.string.wireless_error_invalid_port
        WirelessAdbIssue.INVALID_PAIRING_CODE -> Res.string.wireless_error_invalid_code
        WirelessAdbIssue.TIMEOUT -> Res.string.wireless_error_timeout
        WirelessAdbIssue.UNREACHABLE -> Res.string.wireless_error_unreachable
        WirelessAdbIssue.AUTHENTICATION -> Res.string.wireless_error_authentication
        WirelessAdbIssue.NAME_RESOLUTION -> Res.string.wireless_error_resolution
        WirelessAdbIssue.COMMAND_FAILED, null -> Res.string.wireless_error_command
    }
)
