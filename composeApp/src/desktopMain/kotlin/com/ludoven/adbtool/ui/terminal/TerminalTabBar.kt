package com.ludoven.adbtool.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.ui.mac.*

@Composable
fun TerminalTabBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1723), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ADB Console",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF58A6FF),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Tabs (预留)",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8B949E)
        )
    }
}
