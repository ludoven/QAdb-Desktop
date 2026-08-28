package com.ludoven.adbtool.widget

import com.ludoven.adbtool.ui.mac.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.QadbColors
import com.ludoven.adbtool.QadbTokens
import com.ludoven.adbtool.UiTokens

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

// ─────────────────────────────────────────────────────────────────────────────
// Diagnostics empty-state illustrations
// ─────────────────────────────────────────────────────────────────────────────

enum class DiagnosticsEmptyKind {
    /** No ADB device selected / connected */
    NoDevice,
    /** Capture is running but no log lines have arrived yet */
    WaitingForLogs,
    /** Filter is active but nothing matches */
    NoMatchingLogs,
    /** Process list returned empty */
    EmptyProcessList
}

/**
 * Opinionated empty-state panel for the Diagnostics page.
 * Renders a small Canvas illustration matched to [kind], followed by title and description.
 */
@Composable
fun DiagnosticsEmptyState(
    kind: DiagnosticsEmptyKind,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium),
            modifier = Modifier.padding(UiTokens.SpaceXXLarge)
        ) {
            when (kind) {
                DiagnosticsEmptyKind.NoDevice -> NoDeviceIllustration()
                DiagnosticsEmptyKind.WaitingForLogs -> WaitingForLogsIllustration()
                DiagnosticsEmptyKind.NoMatchingLogs -> NoMatchingLogsIllustration()
                DiagnosticsEmptyKind.EmptyProcessList -> EmptyProcessListIllustration()
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp)
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(UiTokens.SpaceSmall))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = QadbColors.onPrimary
                    )
                ) {
                    Text(text = actionLabel, color = QadbColors.onPrimary)
                }
            }
        }
    }
}

// ── Illustration: No Device ───────────────────────────────────────────────────
@Composable
private fun NoDeviceIllustration() {
    val brand = QadbTokens.brand
    val muted = QadbTokens.textMuted

    Canvas(modifier = Modifier.size(80.dp, 64.dp)) {
        val w = size.width
        val h = size.height
        val strokeStyle = Stroke(width = 2.2f, cap = StrokeCap.Round)

        val phoneW = w * 0.34f
        val phoneH = h * 0.60f
        val phoneL = (w - phoneW) / 2f
        val phoneT = h * 0.04f
        drawRoundRect(
            color = brand,
            topLeft = Offset(phoneL, phoneT),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(8f),
            style = strokeStyle
        )
        drawCircle(
            color = brand,
            radius = 3.5f,
            center = Offset(w / 2f, phoneT + phoneH - 10f)
        )
        val cableX = w / 2f
        val cableTop = phoneT + phoneH + 5f
        val cableMid = (cableTop + h - 14f) / 2f
        drawLine(color = muted, start = Offset(cableX, cableTop), end = Offset(cableX, cableMid - 5f), strokeWidth = 2f)
        drawLine(color = muted, start = Offset(cableX, cableMid + 5f), end = Offset(cableX, h - 14f), strokeWidth = 2f)
        drawRoundRect(
            color = muted,
            topLeft = Offset(cableX - 7f, h - 14f),
            size = Size(14f, 7f),
            cornerRadius = CornerRadius(2f),
            style = Stroke(width = 1.8f)
        )
    }
}

// ── Illustration: Waiting for Logs (EQ bars) ──────────────────────────────────
@Composable
private fun WaitingForLogsIllustration() {
    val brand = QadbTokens.brand
    val transition = rememberInfiniteTransition(label = "eq")
    val phases = remember { listOf(0, 160, 80, 240, 40) }
    val heights = phases.mapIndexed { i, phase ->
        transition.animateFloat(
            initialValue = 0.22f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 720, delayMillis = phase, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$i"
        ).value
    }

    Canvas(modifier = Modifier.size(80.dp, 52.dp)) {
        val w = size.width
        val h = size.height
        val barCount = heights.size
        val barW = w * 0.10f
        val gap = (w - barW * barCount) / (barCount + 1)
        val maxBarH = h * 0.80f
        heights.forEachIndexed { i, scale ->
            val barH = maxBarH * scale
            val x = gap + i * (barW + gap)
            drawRoundRect(
                color = brand.copy(alpha = (0.55f + 0.45f * scale).coerceIn(0f, 1f)),
                topLeft = Offset(x, h - barH),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f)
            )
        }
    }
}

// ── Illustration: No Matching Logs (magnifier + slash) ────────────────────────
@Composable
private fun NoMatchingLogsIllustration() {
    val warning = QadbTokens.warning
    val muted = QadbTokens.textMuted

    Canvas(modifier = Modifier.size(72.dp, 60.dp)) {
        val w = size.width
        val h = size.height
        val cx = w * 0.42f
        val cy = h * 0.42f
        val r = w * 0.29f
        val strokeW = 2.4f

        drawCircle(color = warning, radius = r, center = Offset(cx, cy), style = Stroke(width = strokeW, cap = StrokeCap.Round))
        val cos45 = 0.7071f
        val hx = cx + r * cos45
        val hy = cy + r * cos45
        drawLine(color = warning, start = Offset(hx, hy), end = Offset(hx + r * 0.52f, hy + r * 0.52f), strokeWidth = strokeW + 0.8f, cap = StrokeCap.Round)
        val off = r * 0.44f
        drawLine(color = muted, start = Offset(cx - off, cy + off), end = Offset(cx + off, cy - off), strokeWidth = strokeW, cap = StrokeCap.Round)
    }
}

// ── Illustration: Empty Process List (skeleton rows) ──────────────────────────
@Composable
private fun EmptyProcessListIllustration() {
    val muted = QadbTokens.textMuted

    Canvas(modifier = Modifier.size(80.dp, 52.dp)) {
        val w = size.width
        val h = size.height
        val rowH = h * 0.20f
        val gap = (h - rowH * 3f) / 4f
        val cornerR = CornerRadius(rowH / 2f)

        listOf(w * 0.82f, w * 0.54f, w * 0.68f).forEachIndexed { i, barW ->
            val top = gap + i * (rowH + gap)
            drawRoundRect(color = muted.copy(alpha = 0.10f), topLeft = Offset(0f, top), size = Size(barW, rowH), cornerRadius = cornerR)
            drawRoundRect(color = muted.copy(alpha = 0.35f), topLeft = Offset(0f, top), size = Size(barW, rowH), cornerRadius = cornerR, style = Stroke(width = 1.6f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// File browser empty-state illustrations
// ─────────────────────────────────────────────────────────────────────────────

enum class FileEmptyKind {
    NoDevice,
    EmptyFolder,
    NoMatchingFiles
}

/**
 * Opinionated empty-state panel for the File Browser page.
 */
@Composable
fun FileEmptyState(
    kind: FileEmptyKind,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium),
            modifier = Modifier.padding(UiTokens.SpaceXXLarge)
        ) {
            when (kind) {
                FileEmptyKind.NoDevice -> NoDeviceIllustration()
                FileEmptyKind.EmptyFolder -> EmptyFolderIllustration()
                FileEmptyKind.NoMatchingFiles -> NoMatchingLogsIllustration()
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp)
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(UiTokens.SpaceSmall))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = QadbColors.onPrimary
                    )
                ) {
                    Text(text = actionLabel, color = QadbColors.onPrimary)
                }
            }
        }
    }
}

// ── Illustration: Empty Folder ────────────────────────────────────────────────
@Composable
private fun EmptyFolderIllustration() {
    val brand = QadbTokens.brand
    val muted = QadbTokens.textMuted

    Canvas(modifier = Modifier.size(80.dp, 60.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 2.2f
        val strokeStyle = Stroke(width = strokeW, cap = StrokeCap.Round)

        val folderLeft = w * 0.12f
        val folderRight = w * 0.88f
        val folderTop = h * 0.22f
        val folderBottom = h * 0.86f
        val folderW = folderRight - folderLeft
        val folderH = folderBottom - folderTop

        val tabW = folderW * 0.38f
        val tabH = h * 0.12f
        val tabTop = folderTop - tabH

        val tabPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(folderLeft + 4f, folderTop)
            lineTo(folderLeft + 4f, tabTop + 4f)
            quadraticBezierTo(folderLeft + 4f, tabTop, folderLeft + 8f, tabTop)
            lineTo(folderLeft + tabW - 8f, tabTop)
            quadraticBezierTo(folderLeft + tabW - 4f, tabTop, folderLeft + tabW, folderTop)
        }
        drawPath(tabPath, color = brand.copy(alpha = 0.5f), style = strokeStyle)

        drawRoundRect(
            color = brand,
            topLeft = Offset(folderLeft, folderTop),
            size = Size(folderW, folderH),
            cornerRadius = CornerRadius(6f),
            style = strokeStyle
        )

        drawRoundRect(
            color = brand.copy(alpha = 0.06f),
            topLeft = Offset(folderLeft, folderTop),
            size = Size(folderW, folderH),
            cornerRadius = CornerRadius(6f)
        )

        val innerMargin = folderW * 0.22f
        val line1Y = folderTop + folderH * 0.40f
        val line2Y = folderTop + folderH * 0.65f
        drawLine(
            color = muted.copy(alpha = 0.45f),
            start = Offset(folderLeft + innerMargin, line1Y),
            end = Offset(folderRight - innerMargin, line1Y),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = muted.copy(alpha = 0.30f),
            start = Offset(folderLeft + innerMargin + folderW * 0.1f, line2Y),
            end = Offset(folderRight - innerMargin - folderW * 0.1f, line2Y),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round
        )
    }
}
