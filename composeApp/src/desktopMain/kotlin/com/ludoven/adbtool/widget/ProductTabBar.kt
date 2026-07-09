package com.ludoven.adbtool.widget

import com.ludoven.adbtool.ui.mac.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.UiTokens

data class ProductTab<T>(
    val key: T,
    val label: String,
    val icon: ImageVector
)

@Composable
fun <T> ProductTabBar(
    tabs: List<ProductTab<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isSelected = tab.key == selected
            Surface(
                onClick = { onSelected(tab.key) },
                shape = RoundedCornerShape(UiTokens.ControlHeight / 2),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = BorderStroke(
                    1.dp,
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .height(UiTokens.ToolbarHeight)
                        .padding(horizontal = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(UiTokens.IconSmall)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
