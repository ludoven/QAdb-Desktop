package com.ludoven.adbtool.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.domain.terminal.TerminalLine
import com.ludoven.adbtool.domain.terminal.TerminalLineType
import com.ludoven.adbtool.ui.mac.*

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
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    LaunchedEffect(isRunning) {
        if (!isRunning) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .background(
                color = Color(0xFF0B0F14),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                SelectionContainer {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState
                    ) {
                        items(lines, key = { it.id }) { line ->
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = lineColor(line.type)
                            )
                        }
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { focusRequester.requestFocus() },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF58A6FF)
                )
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    singleLine = true,
                    cursorBrush = SolidColor(Color(0xFFE6EDF3)),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFE6EDF3),
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
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
                                text = "输入 ADB 命令",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF8B949E)
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

private fun lineColor(type: TerminalLineType): Color {
    return when (type) {
        TerminalLineType.PROMPT -> Color(0xFF8AB4F8)
        TerminalLineType.INPUT -> Color(0xFFE6EDF3)
        TerminalLineType.OUTPUT -> Color(0xFFD2D8E0)
        TerminalLineType.ERROR -> Color(0xFFFF7B72)
        TerminalLineType.STATUS -> Color(0xFF8B949E)
        TerminalLineType.SUCCESS -> Color(0xFF3FB950)
        TerminalLineType.COMMAND -> Color(0xFF79C0FF)
    }
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
