package com.ludoven.adbtool.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.ui.mac.*

@Composable
fun TerminalInput(
    prompt: String,
    value: String,
    isRunning: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onHistoryPrev: () -> Unit,
    onHistoryNext: () -> Unit,
    onClearOutput: () -> Unit,
    onInterrupt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF111722), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = prompt,
            color = Color(0xFF58A6FF),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .onPreviewKeyEvent { event ->
                    handleInputKeyEvent(
                        event = event,
                        isRunning = isRunning,
                        onSubmit = onSubmit,
                        onHistoryPrev = onHistoryPrev,
                        onHistoryNext = onHistoryNext,
                        onClearOutput = onClearOutput,
                        onInterrupt = onInterrupt
                    )
                },
            placeholder = {
                Text(
                    text = "输入 ADB 命令",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF253047),
                unfocusedBorderColor = Color(0xFF253047),
                focusedContainerColor = Color(0xFF0F141C),
                unfocusedContainerColor = Color(0xFF0F141C)
            )
        )
        Button(
            onClick = onSubmit,
            enabled = !isRunning,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF238636),
                contentColor = Color.White
            )
        ) {
            Text("运行")
        }
        Button(
            onClick = onInterrupt,
            enabled = isRunning,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFDA3633),
                contentColor = Color.White
            )
        ) {
            Text("停止")
        }
        Spacer(modifier = Modifier.width(2.dp))
    }
}

private fun handleInputKeyEvent(
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
        (event.key == Key.Enter || event.key == Key.NumPadEnter) && !isRunning -> {
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
