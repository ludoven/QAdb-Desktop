package com.ludoven.adbtool.widget

import com.ludoven.adbtool.ui.mac.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.UiTokens
import com.ludoven.adbtool.QadbColors

enum class InlineStatusTone {
    Info,
    Success,
    Warning,
    Danger
}

@Composable
fun PageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleFontFamily: FontFamily? = null,
    titleFontSize: androidx.compose.ui.unit.TextUnit = UiTokens.TextPageTitle,
    titleFontWeight: FontWeight = FontWeight.SemiBold,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXSmall)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = titleFontFamily,
                fontSize = titleFontSize,
                fontWeight = titleFontWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(UiTokens.SpaceLarge))
            trailing()
        }
    }
}

@Composable
fun EmptyStatePanel(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
            modifier = Modifier.padding(UiTokens.SpaceXXLarge)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
                            RoundedCornerShape(UiTokens.RadiusMedium)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(UiTokens.IconLarge)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun InlineStatusBanner(
    text: String,
    modifier: Modifier = Modifier,
    tone: InlineStatusTone = InlineStatusTone.Info,
    icon: ImageVector? = null
) {
    val accent = when (tone) {
        InlineStatusTone.Info -> MaterialTheme.colorScheme.primary
        InlineStatusTone.Success -> MaterialTheme.colorScheme.secondary
        InlineStatusTone.Warning -> MaterialTheme.colorScheme.tertiary
        InlineStatusTone.Danger -> MaterialTheme.colorScheme.error
    }
    val background = when (tone) {
        InlineStatusTone.Info -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        InlineStatusTone.Success -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f)
        InlineStatusTone.Warning -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.46f)
        InlineStatusTone.Danger -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.52f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(UiTokens.RadiusMedium))
            .padding(horizontal = UiTokens.SpaceMedium, vertical = UiTokens.SpaceSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(UiTokens.IconSmall)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = accent,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: InlineStatusTone = InlineStatusTone.Info
) {
    val foreground = when (tone) {
        InlineStatusTone.Info -> MaterialTheme.colorScheme.primary
        InlineStatusTone.Success -> MaterialTheme.colorScheme.secondary
        InlineStatusTone.Warning -> MaterialTheme.colorScheme.tertiary
        InlineStatusTone.Danger -> MaterialTheme.colorScheme.error
    }
    val background = when (tone) {
        InlineStatusTone.Info -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
        InlineStatusTone.Success -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f)
        InlineStatusTone.Warning -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.58f)
        InlineStatusTone.Danger -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.58f)
    }
    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(UiTokens.BadgeRadius))
            .padding(horizontal = UiTokens.SpaceSmall, vertical = UiTokens.SpaceXSmall),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DesktopToolbar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UiTokens.RadiusMedium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(UiTokens.ToolbarHeight)
                .padding(horizontal = UiTokens.SpaceSmall),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun ActionProgressButton(
    text: String,
    busyText: String,
    isBusy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isBusy,
        modifier = modifier.height(UiTokens.ControlHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = QadbColors.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = UiTokens.SpaceMedium, vertical = 0.dp)
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = QadbColors.onPrimary,
                strokeWidth = 2.dp
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = QadbColors.onPrimary,
                modifier = Modifier.size(UiTokens.IconSmall)
            )
        }
        Spacer(modifier = Modifier.width(UiTokens.SpaceSmall))
        Text(
            text = if (isBusy) busyText else text,
            color = QadbColors.onPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FramedStateSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(UiTokens.RadiusLarge),
        borderStroke = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.64f)
        )
    ) {
        content()
    }
}
