package com.ludoven.adbtool.ui.terminal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.QadbColors
import com.ludoven.adbtool.QadbPalette
import com.ludoven.adbtool.QadbTokens
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.domain.terminal.TerminalLine
import com.ludoven.adbtool.domain.terminal.TerminalLineType
import com.ludoven.adbtool.ui.mac.Surface
import com.ludoven.adbtool.ui.mac.Text
import com.ludoven.adbtool.util.l10n
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern Developer-Grade Terminal Console Window.
 *
 * Designed with macOS traffic lights, clean monospace syntax highlighting,
 * inline interactive prompt at the tail of the stream, and a welcoming
 * vector empty state when no output is present.
 */
@Composable
fun TerminalOutput(
    lines: List<TerminalLine>,
    prompt: String,
    input: String,
    isRunning: Boolean,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onHistoryPrev: () -> Unit,
    onHistoryNext: () -> Unit,
    onClearOutput: () -> Unit,
    onInterrupt: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    sessionTitle: String = "qadb@terminal",
    chrome: (@Composable () -> Unit)? = null
) {
    val focusRequester = remember { FocusRequester() }
    var pendingFocusRequest by remember { mutableStateOf(true) }

    LaunchedEffect(isRunning) {
        if (!isRunning) {
            pendingFocusRequest = true
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = TerminalTokens.Background,
        border = BorderStroke(1.dp, TerminalTokens.Border)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // macOS Window Header
            TerminalWindowHeader(
                sessionTitle = sessionTitle,
                lineCount = lines.size,
                isRunning = isRunning
            )

            // Optional Chrome / Toolbar strip
            if (chrome != null) {
                chrome()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(TerminalTokens.Border)
                )
            }

            // Output & Input Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { focusRequester.requestFocus() }
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    state = listState
                ) {
                    if (lines.isEmpty()) {
                        item(key = "terminal_empty_welcome") {
                            TerminalEmptyWelcome(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp)
                            )
                        }
                    } else {
                        itemsIndexed(lines, key = { _, it -> it.id }) { index, line ->
                            TerminalLineRow(
                                line = line,
                                lineNumber = index + 1
                            )
                        }
                    }

                    item(key = "terminal_input_row") {
                        ConsoleInputRow(
                            prompt = prompt,
                            input = input,
                            isRunning = isRunning,
                            onInputChange = onInputChange,
                            onSubmit = onSubmit,
                            onHistoryPrev = onHistoryPrev,
                            onHistoryNext = onHistoryNext,
                            onClearOutput = onClearOutput,
                            onInterrupt = onInterrupt,
                            focusRequester = focusRequester,
                            pendingFocusRequest = pendingFocusRequest,
                            onFocusConsumed = { pendingFocusRequest = false }
                        )
                    }
                }

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
}

// ── macOS Window Header ───────────────────────────────────────────────────────

@Composable
private fun TerminalWindowHeader(
    sessionTitle: String,
    lineCount: Int,
    isRunning: Boolean
) {
    val isMacOs = remember {
        System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalTokens.HeaderBg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (isMacOs) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(Color(0xFFFF5F56), Color(0xFFFFBD2E), Color(0xFF27C93F)).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        } else {
            Text(
                text = ">_",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TerminalTokens.TextMuted
            )
        }

        // Center Session Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = sessionTitle,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = TerminalTokens.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right Status Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isRunning) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(QadbPalette.TerminalSuccess)
                )
                Text(
                    text = l10n("执行中…", "Running…"),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = QadbPalette.TerminalSuccess
                )
            } else {
                Text(
                    text = if (lineCount > 0) l10n("${lineCount} 行", "${lineCount} lines") else l10n("就绪", "Ready"),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    color = TerminalTokens.TextMuted.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ── Line Output Row ───────────────────────────────────────────────────────────

@Composable
private fun TerminalLineRow(
    line: TerminalLine,
    lineNumber: Int
) {
    val (textColor, backgroundColor) = lineStyle(line.type)
    val timestamp = remember(line.timestamp) { formatTimestamp(line.timestamp) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 1.5.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Line number
        Text(
            text = lineNumber.toString().padStart(4, ' '),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TerminalTokens.LineNumber,
            modifier = Modifier
                .widthIn(min = 36.dp)
                .padding(end = 10.dp)
        )

        // Timestamp
        Text(
            text = timestamp,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TerminalTokens.LineNumber.copy(alpha = 0.75f),
            modifier = Modifier.padding(end = 10.dp)
        )

        // Line Content
        SelectionContainer {
            Text(
                text = buildAnnotatedMessage(line),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Interactive Console Input Row ─────────────────────────────────────────────

@Composable
private fun ConsoleInputRow(
    prompt: String,
    input: String,
    isRunning: Boolean,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onHistoryPrev: () -> Unit,
    onHistoryNext: () -> Unit,
    onClearOutput: () -> Unit,
    onInterrupt: () -> Unit,
    focusRequester: FocusRequester,
    pendingFocusRequest: Boolean,
    onFocusConsumed: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = prompt,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TerminalTokens.Prompt,
            modifier = Modifier.widthIn(min = 0.dp)
        )

        BasicTextField(
            value = input,
            onValueChange = onInputChange,
            singleLine = true,
            cursorBrush = SolidColor(TerminalTokens.Cursor),
            textStyle = TextStyle(
                color = TerminalTokens.Text,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onGloballyPositioned {
                    if (pendingFocusRequest) {
                        focusRequester.requestFocus()
                        onFocusConsumed()
                    }
                }
                .onPreviewKeyEvent { event ->
                    handleTerminalInlineKeyEvent(
                        event = event,
                        isRunning = isRunning,
                        onSubmit = onSubmit,
                        onHistoryPrev = onHistoryPrev,
                        onHistoryNext = onHistoryNext,
                        onClearOutput = onClearOutput,
                        onInterrupt = onInterrupt
                    )
                },
            decorationBox = { innerTextField ->
                if (input.isEmpty()) {
                    Text(
                        text = l10n("输入 ADB 命令 (Enter 执行, ↑/↓ 历史)…", "Enter ADB command (Enter to run, ↑/↓ for history)…"),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TerminalTokens.TextMuted.copy(alpha = 0.45f)
                    )
                }
                innerTextField()
            }
        )
    }
}

// ── Vector Empty State Illustration ───────────────────────────────────────────

@Composable
private fun EmptyTerminalIllustration() {
    val brand = QadbTokens.brand
    val success = QadbTokens.success
    val muted = TerminalTokens.TextMuted
    val isMacOs = remember {
        System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)
    }

    Canvas(modifier = Modifier.size(88.dp, 64.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8f, cap = StrokeCap.Round)

        // Console Window frame
        val padX = 8f
        val padY = 6f
        val winW = w - padX * 2
        val winH = h - padY * 2

        drawRoundRect(
            color = brand.copy(alpha = 0.75f),
            topLeft = Offset(padX, padY),
            size = Size(winW, winH),
            cornerRadius = CornerRadius(8f),
            style = stroke
        )

        // Header bar divider
        val headerH = 14f
        drawLine(
            color = brand.copy(alpha = 0.35f),
            start = Offset(padX, padY + headerH),
            end = Offset(padX + winW, padY + headerH),
            strokeWidth = 1.2f
        )

        // Keep traffic-light decoration exclusive to macOS.
        val dotRadius = 2.2f
        val headerColors = if (isMacOs) {
            listOf(Color(0xFFFF5F56), Color(0xFFFFBD2E), Color(0xFF27C93F))
        } else {
            List(3) { brand.copy(alpha = 0.45f) }
        }
        headerColors.forEachIndexed { index, color ->
            drawCircle(
                color = color.copy(alpha = 0.8f),
                radius = dotRadius,
                center = Offset(padX + 7f + index * 7f, padY + headerH / 2)
            )
        }

        // Prompt line: `> ` and simulated command blocks
        val promptY = padY + headerH + 12f
        drawLine(
            color = success,
            start = Offset(padX + 8f, promptY - 4f),
            end = Offset(padX + 13f, promptY),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = success,
            start = Offset(padX + 13f, promptY),
            end = Offset(padX + 8f, promptY + 4f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )

        // Command line bar
        drawRoundRect(
            color = brand.copy(alpha = 0.6f),
            topLeft = Offset(padX + 18f, promptY - 3f),
            size = Size(winW * 0.42f, 6f),
            cornerRadius = CornerRadius(2f)
        )

        // Output lines
        val line1Y = promptY + 12f
        drawRoundRect(
            color = muted.copy(alpha = 0.35f),
            topLeft = Offset(padX + 8f, line1Y - 2.5f),
            size = Size(winW * 0.65f, 5f),
            cornerRadius = CornerRadius(1.5f)
        )

        val line2Y = line1Y + 9f
        drawRoundRect(
            color = muted.copy(alpha = 0.25f),
            topLeft = Offset(padX + 8f, line2Y - 2.5f),
            size = Size(winW * 0.45f, 5f),
            cornerRadius = CornerRadius(1.5f)
        )

        // Blinking cursor
        drawRect(
            color = success.copy(alpha = 0.9f),
            topLeft = Offset(padX + 18f + winW * 0.42f + 4f, promptY - 4f),
            size = Size(3f, 8f)
        )
    }
}

@Composable
private fun TerminalEmptyWelcome(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EmptyTerminalIllustration()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = l10n("ADB 控制台就绪", "ADB Console Ready"),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TerminalTokens.Text
            )
            Text(
                text = l10n(
                    "在下方输入 ADB 命令或点击上方快捷芯片开始调试",
                    "Enter ADB command below or click quick snippets above to start"
                ),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.5.sp,
                color = TerminalTokens.TextMuted
            )
        }

        // Shortcut Hints Pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(UiTokens.RadiusSmall))
                .background(TerminalTokens.Border.copy(alpha = 0.5f))
                .border(1.dp, TerminalTokens.Border, RoundedCornerShape(UiTokens.RadiusSmall))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            ShortcutHintTag(key = "↑/↓", desc = l10n("历史记录", "History"))
            Box(modifier = Modifier.size(1.dp, 10.dp).background(TerminalTokens.LineNumber.copy(alpha = 0.5f)))
            ShortcutHintTag(key = "⌘L", desc = l10n("清屏", "Clear"))
            Box(modifier = Modifier.size(1.dp, 10.dp).background(TerminalTokens.LineNumber.copy(alpha = 0.5f)))
            ShortcutHintTag(key = "⌘C", desc = l10n("中断", "Kill"))
        }
    }
}

@Composable
private fun ShortcutHintTag(key: String, desc: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(TerminalTokens.Background)
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = key,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = QadbPalette.TerminalBlue
            )
        }
        Text(
            text = desc,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.5.sp,
            color = TerminalTokens.TextMuted
        )
    }
}

// ── Line Styles & Annotations ─────────────────────────────────────────────────

private data class LineStyle(val color: Color, val background: Color)

@Composable
private fun lineStyle(type: TerminalLineType): LineStyle {
    val (text, bg) = when (type) {
        TerminalLineType.PROMPT -> TerminalTokens.Prompt to Color.Transparent
        TerminalLineType.INPUT -> TerminalTokens.Text to Color.Transparent
        TerminalLineType.OUTPUT -> TerminalTokens.Text to Color.Transparent
        TerminalLineType.ERROR -> TerminalTokens.Error to QadbColors.errorSurface.copy(alpha = 0.45f)
        TerminalLineType.STATUS -> TerminalTokens.Status to Color.Transparent
        TerminalLineType.SUCCESS -> TerminalTokens.Success to TerminalTokens.Success.copy(alpha = 0.08f)
        TerminalLineType.COMMAND -> TerminalTokens.Prompt to Color.Transparent
    }
    return LineStyle(text, bg)
}

private fun buildAnnotatedMessage(line: TerminalLine): AnnotatedString {
    return when (line.type) {
        TerminalLineType.PROMPT, TerminalLineType.COMMAND -> buildAnnotatedString {
            withStyle(SpanStyle(color = TerminalTokens.Prompt, fontWeight = FontWeight.Bold)) {
                append("❯ ")
            }
            append(line.text)
        }
        else -> AnnotatedString(line.text)
    }
}

private fun formatTimestamp(millis: Long): String {
    if (millis <= 0L) return "        "
    return TIMESTAMP_FORMATTER.get()?.format(Date(millis)) ?: "        "
}

private val TIMESTAMP_FORMATTER: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
}

private fun handleTerminalInlineKeyEvent(
    event: KeyEvent,
    isRunning: Boolean,
    onSubmit: () -> Unit,
    onHistoryPrev: () -> Unit,
    onHistoryNext: () -> Unit,
    onClearOutput: () -> Unit,
    onInterrupt: () -> Unit
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val modifierPressed = event.isMetaPressed || event.isCtrlPressed

    return when {
        event.key == Key.Enter || event.key == Key.NumPadEnter -> {
            onSubmit()
            true
        }
        event.key == Key.DirectionUp -> {
            onHistoryPrev()
            true
        }
        event.key == Key.DirectionDown -> {
            onHistoryNext()
            true
        }
        modifierPressed && event.key == Key.L -> {
            onClearOutput()
            true
        }
        modifierPressed && event.key == Key.C && isRunning -> {
            onInterrupt()
            true
        }
        else -> false
    }
}

// ── Terminal Tokens ───────────────────────────────────────────────────────────

private object TerminalTokens {
    val Background = Color(0xFF0F172A)
    val HeaderBg = Color(0xFF1E293B)
    val Border = Color(0xFF334155)
    val LineNumber = Color(0xFF64748B)
    val Text = Color(0xFFF1F5F9)
    val TextMuted = Color(0xFF94A3B8)
    val Prompt = Color(0xFF38BDF8)
    val Success = Color(0xFF4ADE80)
    val Error = Color(0xFFF87171)
    val Status = Color(0xFF2DD4BF)
    val Cursor = Color(0xFF38BDF8)
}
