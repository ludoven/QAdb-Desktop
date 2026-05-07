package com.ludoven.adbtool.widget

import com.ludoven.adbtool.ui.mac.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.ludoven.adbtool.ui.mac.ButtonDefaults
import com.ludoven.adbtool.ui.mac.Card
import com.ludoven.adbtool.ui.mac.CardDefaults
import com.ludoven.adbtool.ui.mac.Icon
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.OutlinedButton
import com.ludoven.adbtool.ui.mac.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ludoven.adbtool.UiTokens

@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    active: Boolean = true
) {
    val accent = if (active) Color(0xFF2DBE60) else MaterialTheme.colorScheme.onSurfaceVariant
    val background = if (active) Color(0xFFE9F9EF) else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(accent, RoundedCornerShape(999.dp))
        )
        Text(
            text = text,
            color = accent,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun OutlineActionButton(
    text: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = if (enabled) 0.28f else 0.16f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = tint,
            disabledContentColor = tint.copy(alpha = 0.45f)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(16.dp))
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    progress: Float = 0f
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 112.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            MetricLinearBar(
                progress = progress,
                accentColor = accentColor
            )
            if (!supportingText.isNullOrBlank()) {
                Text(
                    text = supportingText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MetricLinearBar(
    progress: Float,
    accentColor: Color
) {
    val safeProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                RoundedCornerShape(999.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(safeProgress)
                .height(8.dp)
                .background(accentColor, RoundedCornerShape(999.dp))
        )
    }
}

@Composable
fun DashboardPanel(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(UiTokens.RadiusSmall)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun DeviceInfoCell(
    label: String,
    value: String,
    icon: ImageVector? = null,
    tint: Color? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null && tint != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(tint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LabeledValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    pillColor: Color? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (pillColor != null) {
            Box(
                modifier = Modifier
                    .background(pillColor.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = pillColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = valueColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(160)
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isHovered) accentColor.copy(alpha = 0.14f) else accentColor.copy(alpha = 0.08f),
        animationSpec = tween(180)
    )
    Card(
        modifier = modifier
            .scale(scale)
            .hoverable(interactionSource)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.dp, Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 80.dp)
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
