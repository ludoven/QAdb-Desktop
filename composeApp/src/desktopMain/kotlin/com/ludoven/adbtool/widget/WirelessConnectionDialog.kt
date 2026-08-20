package com.ludoven.adbtool.widget

import adbtool_desktop.composeapp.generated.resources.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
    fontSize = 17.sp,
    lineHeight = 22.sp,
    fontWeight = FontWeight.SemiBold
)
private val SectionLabelStyle = TextStyle(
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.Medium
)
private val BodyStyle = TextStyle(fontSize = 13.sp, lineHeight = 19.sp)
private val CaptionStyle = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
private val ActionStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Medium
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
        Column(
            modifier = Modifier
                .width(560.dp)
                .heightIn(max = 720.dp)
                .shadow(18.dp, RoundedCornerShape(UiTokens.RadiusLarge))
                .background(colors.surface, RoundedCornerShape(UiTokens.RadiusLarge))
                .border(UiTokens.BorderWidth, colors.border, RoundedCornerShape(UiTokens.RadiusLarge))
        ) {
            WirelessDialogHeader(
                colors = colors,
                enabled = !isRunning,
                onDismiss = onDismiss
            )
            WirelessDivider(colors.border)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 570.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(UiTokens.SpaceXLarge),
                verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceLarge)
            ) {
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
                        enabled = !isRunning,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = colors
                    )
                }

                result?.let { WirelessOperationStatus(it, colors) }
            }
            WirelessDivider(colors.border)
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
            .padding(
                start = UiTokens.SpaceXLarge,
                top = UiTokens.SpaceLarge,
                end = UiTokens.SpaceMedium,
                bottom = UiTokens.SpaceLarge
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WirelessIcon(
            imageVector = IconParkIcons.Wifi,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(UiTokens.IconLarge)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = UiTokens.SpaceMedium),
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

@Composable
private fun WirelessModeSelector(
    selectedMode: WirelessDialogMode,
    enabled: Boolean,
    colors: WirelessDialogColors,
    onSelected: (WirelessDialogMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(UiTokens.BorderWidth, colors.border, RoundedCornerShape(UiTokens.RadiusSmall))
            .padding(horizontal = UiTokens.SpaceXSmall),
        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)
    ) {
        WirelessModeTab(
            text = stringResource(Res.string.wireless_mode_connect),
            selected = selectedMode == WirelessDialogMode.CONNECT,
            enabled = enabled,
            colors = colors,
            onClick = { onSelected(WirelessDialogMode.CONNECT) },
            modifier = Modifier.weight(1f)
        )
        WirelessModeTab(
            text = stringResource(Res.string.wireless_mode_pair),
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
    selected: Boolean,
    enabled: Boolean,
    colors: WirelessDialogColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val textColor = when {
        !enabled -> colors.textDisabled
        selected -> colors.accent
        else -> colors.textSecondary
    }
    Column(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(UiTokens.RadiusSmall))
            .background(if (hovered && enabled) colors.surfaceHover else Color.Transparent)
            .hoverable(interactionSource, enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Tab,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            BasicText(text = text, style = ActionStyle.copy(color = textColor))
        }
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(2.dp)
                .background(if (selected) colors.accent else Color.Transparent)
        )
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
    val shape = RoundedCornerShape(UiTokens.RadiusSmall)
    val borderColor = when {
        !enabled -> colors.border.copy(alpha = 0.55f)
        focused -> colors.accent
        else -> colors.borderStrong
    }
    val textColor = if (enabled) colors.textPrimary else colors.textDisabled

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BasicText(
            text = label,
            style = SectionLabelStyle.copy(
                color = if (focused && enabled) colors.accent else colors.textSecondary
            )
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(UiTokens.InputHeight)
                .clip(shape)
                .background(if (enabled) colors.surface else colors.surfaceDisabled)
                .border(UiTokens.BorderWidth, borderColor, shape),
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
                        .padding(horizontal = UiTokens.SpaceMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    icon?.let {
                        WirelessIcon(
                            imageVector = it,
                            contentDescription = null,
                            tint = if (enabled) colors.textSecondary else colors.textDisabled,
                            modifier = Modifier.size(UiTokens.IconSmall)
                        )
                        Spacer(Modifier.width(UiTokens.SpaceSmall))
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
private fun WirelessConnectionHistory(
    history: List<String>,
    enabled: Boolean,
    colors: WirelessDialogColors,
    onReconnect: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicText(
            text = stringResource(Res.string.wireless_recent_title),
            style = SectionLabelStyle.copy(color = colors.textSecondary)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(UiTokens.BorderWidth, colors.border, RoundedCornerShape(UiTokens.RadiusSmall))
        ) {
            history.forEachIndexed { index, endpoint ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = UiTokens.ListRowHeight)
                        .padding(horizontal = UiTokens.SpaceMedium, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WirelessIcon(
                        imageVector = IconParkIcons.Time,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(UiTokens.IconSmall)
                    )
                    BasicText(
                        text = endpoint,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = UiTokens.SpaceMedium),
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
                    Spacer(Modifier.width(UiTokens.SpaceSmall))
                    DesktopLinkAction(
                        text = stringResource(Res.string.wireless_remove_history),
                        enabled = enabled,
                        color = colors.textSecondary,
                        disabledColor = colors.textDisabled,
                        onClick = { onRemove(endpoint) }
                    )
                }
                if (index < history.lastIndex) WirelessDivider(colors.border)
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
    val message = when {
        result.success && result.operation == WirelessAdbOperation.CONNECT ->
            stringResource(Res.string.wireless_connect_success, result.endpoint.orEmpty())
        result.success -> stringResource(Res.string.wireless_pair_success, result.endpoint.orEmpty())
        else -> wirelessIssueMessage(result.issue)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(UiTokens.RadiusSmall))
            .border(
                UiTokens.BorderWidth,
                contentColor.copy(alpha = 0.22f),
                RoundedCornerShape(UiTokens.RadiusSmall)
            )
            .padding(UiTokens.SpaceMedium),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)
    ) {
        BasicText(
            text = message,
            style = BodyStyle.copy(color = contentColor, fontWeight = FontWeight.Medium)
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
            .padding(horizontal = UiTokens.SpaceXLarge, vertical = UiTokens.SpaceMedium),
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
        Spacer(Modifier.width(UiTokens.SpaceSmall))
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
    val shape = RoundedCornerShape(UiTokens.RadiusSmall)
    val background = when {
        !enabled -> colors.surfaceDisabled
        primary && pressed -> colors.accent.copy(alpha = 0.82f)
        primary && hovered -> colors.accent.copy(alpha = 0.90f)
        primary -> colors.accent
        pressed -> colors.surfaceHover.copy(alpha = 0.82f)
        hovered -> colors.surfaceHover
        else -> colors.surface
    }
    val contentColor = when {
        !enabled -> colors.textDisabled
        primary -> colors.onAccent
        else -> colors.textPrimary
    }

    Row(
        modifier = Modifier
            .widthIn(min = 88.dp)
            .height(36.dp)
            .clip(shape)
            .background(background)
            .border(
                UiTokens.BorderWidth,
                if (primary && enabled) colors.accent else colors.borderStrong,
                shape
            )
            .hoverable(interactionSource, enabled)
            .focusable(enabled, interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = UiTokens.SpaceLarge),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            DesktopLoadingIndicator(
                color = contentColor,
                modifier = Modifier.size(UiTokens.IconSmall)
            )
            Spacer(Modifier.width(UiTokens.SpaceSmall))
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
    BasicText(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(UiTokens.RadiusSmall))
            .background(if (hovered && enabled) color.copy(alpha = 0.08f) else Color.Transparent)
            .hoverable(interactionSource, enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        style = CaptionStyle.copy(
            color = if (enabled) color else disabledColor,
            fontWeight = FontWeight.Medium
        )
    )
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
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(UiTokens.RadiusSmall))
            .background(if (hovered && enabled) colors.surfaceHover else Color.Transparent)
            .hoverable(interactionSource, enabled)
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
            tint = if (enabled) colors.textSecondary else colors.textDisabled,
            modifier = Modifier.size(UiTokens.IconMedium)
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
            .height(UiTokens.CompactDividerWidth)
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
