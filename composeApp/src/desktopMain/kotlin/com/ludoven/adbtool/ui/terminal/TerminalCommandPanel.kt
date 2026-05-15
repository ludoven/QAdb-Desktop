package com.ludoven.adbtool.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.ui.mac.*
import com.ludoven.adbtool.util.l10n
import com.ludoven.adbtool.widget.GlassCard

@Composable
fun TerminalCommandPanel(
    onInsert: (String) -> Unit,
    onRun: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val common = listOf("devices", "shell", "install", "logcat", "screenshot")
    val app = listOf("start <packageName>", "stop <packageName>", "clear <packageName>", "restart <packageName>")
    val system = listOf("battery", "size", "density", "activity")

    GlassCard(modifier = modifier.width(300.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Section(l10n("常用", "Common"), common, onInsert, onRun)
            Section(l10n("应用", "Apps"), app, onInsert, onRun)
            Section(l10n("系统", "System"), system, onInsert, onRun)
        }
    }
}

@Composable
private fun Section(
    title: String,
    commands: List<String>,
    onInsert: (String) -> Unit,
    onRun: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        commands.forEach { cmd ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onInsert(cmd) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(cmd)
                }
                Button(
                    onClick = { onRun(cmd) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(l10n("运行", "Run"))
                }
            }
        }
    }
}
