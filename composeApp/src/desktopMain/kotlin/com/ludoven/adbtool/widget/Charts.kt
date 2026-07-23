package com.ludoven.adbtool.widget

import com.ludoven.adbtool.UiTokens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ludoven.adbtool.ui.mac.MaterialTheme
import com.ludoven.adbtool.ui.mac.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.ludoven.adbtool.ui.mac.bodyMedium
import com.ludoven.adbtool.ui.mac.bodySmall

data class BarChartItem(
    val label: String,
    val value: Float,
    val displayValue: String = ""
)

data class LineChartSeries(
    val label: String,
    val data: List<Float>,
    val lineColor: Color
)

@Composable
fun RealtimeLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
    label: String = "",
    chartHeight: Dp = 180.dp,
    showGrid: Boolean = true,
    showAxisLabels: Boolean = true
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelTextStyle = MaterialTheme.typography.bodySmall.copy(
        color = textColor,
        fontSize = UiTokens.TextMicro
    )

    Box(modifier = modifier.height(chartHeight)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val paddingBottom = 20.dp.toPx()
            val paddingLeft = if (showAxisLabels) 30.dp.toPx() else 8.dp.toPx()
            val paddingTop = 8.dp.toPx()
            val paddingRight = 8.dp.toPx()

            val chartWidth = canvasWidth - paddingLeft - paddingRight
            val chartHeight = canvasHeight - paddingTop - paddingBottom

            if (showGrid) {
                val gridLevels = listOf(25f, 50f, 75f)
                gridLevels.forEach { level ->
                    val y = paddingTop + chartHeight * (1f - level / 100f)
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(canvasWidth - paddingRight, y),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                    if (showAxisLabels) {
                        val textResult = textMeasurer.measure(
                            "${level.toInt()}%",
                            style = labelTextStyle
                        )
                        drawText(
                            textResult,
                            topLeft = Offset(0f, y - textResult.size.height / 2f)
                        )
                    }
                }
            }

            // Draw the line chart
            if (data.isNotEmpty()) {
                val stepX = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth
                val points = data.mapIndexed { index, value ->
                    Offset(
                        x = paddingLeft + index * stepX,
                        y = paddingTop + chartHeight * (1f - value.coerceIn(0f, 100f) / 100f)
                    )
                }

                // Build smooth path for line
                val linePath = buildSmoothPath(points)

                // Fill below the line
                val fillPath = Path().apply {
                    if (points.isNotEmpty()) {
                        addPath(linePath)
                        lineTo(points.last().x, paddingTop + chartHeight)
                        lineTo(points.first().x, paddingTop + chartHeight)
                        close()
                    }
                }
                drawPath(fillPath, fillColor, style = Fill)

                // Draw the line
                drawPath(
                    linePath,
                    lineColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // Current value label at top-right
        if (label.isNotBlank()) {
            Text(
                text = label,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = UiTokens.SpaceXSmall, end = UiTokens.SpaceXSmall),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = lineColor
            )
        }
    }
}

@Composable
fun MultiRealtimeLineChart(
    series: List<LineChartSeries>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 96.dp
) {
    val visibleSeries = series.filter { it.data.isNotEmpty() }
    if (visibleSeries.isEmpty()) return

    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val legendTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 8.dp.toPx()
            val chartWidth = canvasWidth - padding * 2
            val chartHeightPx = canvasHeight - padding * 2

            listOf(25f, 50f, 75f).forEach { level ->
                val y = padding + chartHeightPx * (1f - level / 100f)
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(canvasWidth - padding, y),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
            }

            visibleSeries.forEach { currentSeries ->
                if (currentSeries.data.size == 1) {
                    val y = padding + chartHeightPx * (1f - currentSeries.data.first().coerceIn(0f, 100f) / 100f)
                    drawCircle(
                        color = currentSeries.lineColor,
                        radius = 3.dp.toPx(),
                        center = Offset(canvasWidth / 2f, y)
                    )
                } else {
                    val stepX = chartWidth / (currentSeries.data.size - 1)
                    val points = currentSeries.data.mapIndexed { index, value ->
                        Offset(
                            x = padding + index * stepX,
                            y = padding + chartHeightPx * (1f - value.coerceIn(0f, 100f) / 100f)
                        )
                    }
                    drawPath(
                        path = buildSmoothPath(points),
                        color = currentSeries.lineColor,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceMedium)
        ) {
            visibleSeries.forEach { currentSeries ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
                ) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(12.dp)
                            .background(currentSeries.lineColor, RoundedCornerShape(UiTokens.BadgeRadius))
                    )
                    Text(
                        text = currentSeries.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = legendTextColor
                    )
                }
            }
        }
    }
}

private fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    if (points.size == 1) {
        path.moveTo(points[0].x, points[0].y)
        return path
    }

    path.moveTo(points[0].x, points[0].y)

    for (i in 0 until points.size - 1) {
        val current = points[i]
        val next = points[i + 1]
        val controlX1 = (current.x + next.x) / 2f
        val controlY1 = current.y
        val controlX2 = (current.x + next.x) / 2f
        val controlY2 = next.y
        path.cubicTo(controlX1, controlY1, controlX2, controlY2, next.x, next.y)
    }

    return path
}

@Composable
fun HorizontalBarChart(
    items: List<BarChartItem>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    maxItems: Int = 10
) {
    val displayItems = items.take(maxItems)
    if (displayItems.isEmpty()) return

    val maxValue = displayItems.maxOf { it.value }.coerceAtLeast(1f)
    val labelTextColor = MaterialTheme.colorScheme.onSurface
    val valueTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceSmall)
    ) {
        displayItems.forEach { item ->
            val animatedProgress by animateFloatAsState(
                targetValue = item.value / maxValue,
                animationSpec = tween(durationMillis = 400)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Label on left
                Text(
                    text = item.label,
                    modifier = Modifier.width(120.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = labelTextColor,
                    maxLines = 1
                )

                // Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(UiTokens.SpaceXSmall)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(barColor.copy(alpha = 0.7f), RoundedCornerShape(UiTokens.SpaceXSmall))
                    )
                }

                // Value display
                Text(
                    text = item.displayValue.ifBlank { "${"%.1f".format(item.value)}" },
                    modifier = Modifier.width(60.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = valueTextColor,
                    maxLines = 1
                )
            }
        }
    }
}
