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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(UiTokens.RadiusMedium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f))
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = tab.key == selected
                var isFocused by remember(tab.key) { mutableStateOf(false) }
                Surface(
                    onClick = { onSelected(tab.key) },
                    modifier = Modifier
                        .semantics {
                            role = Role.Tab
                            this.selected = isSelected
                        }
                        .onFocusChanged { isFocused = it.isFocused },
                    shape = RoundedCornerShape(UiTokens.RadiusSmall),
                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = if (isSelected) 1.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .height(UiTokens.ControlHeight)
                            .padding(horizontal = UiTokens.SpaceMedium),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(UiTokens.IconSmall)
                        )
                        Spacer(modifier = Modifier.width(UiTokens.SpaceSmall))
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
}
