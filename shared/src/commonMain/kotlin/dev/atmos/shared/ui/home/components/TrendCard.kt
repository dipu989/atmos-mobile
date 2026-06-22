package dev.atmos.shared.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.common.AtmosCard
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.home.HomeTrendPeriod
import dev.atmos.shared.ui.home.TransportModeEntry
import dev.atmos.shared.ui.home.WeeklyDataPoint
import dev.atmos.shared.ui.theme.AverageDash
import dev.atmos.shared.ui.theme.ChartFillBottom
import dev.atmos.shared.ui.theme.ChartFillTop
import dev.atmos.shared.ui.theme.ChartLine
import dev.atmos.shared.ui.theme.LocalAtmosColors

@Composable
fun TrendCard(
    data: List<WeeklyDataPoint>,
    period: HomeTrendPeriod = HomeTrendPeriod.DAILY,
    onPeriodChange: (HomeTrendPeriod) -> Unit = {},
    onViewStats: () -> Unit = {},
    transportBreakdown: List<TransportModeEntry> = emptyList(),
    onLogTrip: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalAtmosColors.current

    AtmosCard(modifier = modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "CO₂ Trend",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onViewStats,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = "View stats",
                    tint = HorizonBlue,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(
                    text = "All stats",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = HorizonBlue,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        TrendPeriodSelector(selected = period, onSelect = onPeriodChange)

        Spacer(Modifier.height(20.dp))

        if (data.isEmpty()) {
            EmptyTrendState(period = period)
        } else {
            TrendChart(
                data = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                data.forEach { point ->
                    Text(
                        text = point.dayLabel,
                        fontSize = 11.sp,
                        fontWeight = if (point.isToday) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (point.isToday) HorizonBlue else colors.textSecondary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(20.dp))

        TransportBreakdownSection(
            entries   = transportBreakdown,
            period    = period,
            onLogTrip = onLogTrip,
        )
    }
}

@Composable
private fun TrendPeriodSelector(
    selected: HomeTrendPeriod,
    onSelect: (HomeTrendPeriod) -> Unit,
) {
    val colors = LocalAtmosColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        HomeTrendPeriod.entries.forEach { entry ->
            val isSelected = entry == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isSelected) HorizonBlue else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(entry) },
                    )
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (entry) {
                        HomeTrendPeriod.DAILY       -> "Daily"
                        HomeTrendPeriod.WEEKLY      -> "Weekly"
                        HomeTrendPeriod.FORTNIGHTLY -> "Fortnightly"
                    },
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun EmptyTrendState(period: HomeTrendPeriod) {
    val colors = LocalAtmosColors.current
    val message = when (period) {
        HomeTrendPeriod.DAILY       -> "No trips in the last 7 days"
        HomeTrendPeriod.WEEKLY      -> "No trips in the last 6 weeks"
        HomeTrendPeriod.FORTNIGHTLY -> "No trips in the last 6 fortnights"
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.DirectionsRun,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = message,
            fontSize = 13.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TrendChart(
    data: List<WeeklyDataPoint>,
    modifier: Modifier = Modifier,
) {
    val values = data.map { it.kgCO2 }
    val average = values.average().toFloat()

    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas

        val canvasW = size.width
        val canvasH = size.height
        val paddingTop = 12.dp.toPx()
        val paddingBottom = 8.dp.toPx()
        val chartH = canvasH - paddingTop - paddingBottom

        val maxVal = (values.max() * 1.1f).coerceAtLeast(average * 1.2f)
        val minVal = 0f

        fun valueToY(v: Float): Float =
            paddingTop + chartH * (1f - ((v - minVal) / (maxVal - minVal)))

        fun indexToX(i: Int): Float =
            if (values.size == 1) canvasW / 2f
            else i * (canvasW / (values.size - 1).toFloat())

        val points = values.mapIndexed { i, v -> Offset(indexToX(i), valueToY(v)) }

        val avgY = valueToY(average)
        drawLine(
            color = AverageDash,
            start = Offset(0f, avgY),
            end = Offset(canvasW, avgY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()), 0f),
        )

        val linePath = buildSmoothPath(points)

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, canvasH)
            lineTo(points.first().x, canvasH)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(ChartFillTop, ChartFillBottom),
                startY = 0f,
                endY = canvasH,
            ),
        )

        drawPath(
            path = linePath,
            color = ChartLine,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )

        data.indexOfFirst { it.isToday }.takeIf { it >= 0 }?.let { todayIdx ->
            val todayPt = points[todayIdx]
            drawCircle(color = HorizonBlue, radius = 4.dp.toPx(), center = todayPt)
            drawCircle(color = androidx.compose.ui.graphics.Color.White, radius = 2.dp.toPx(), center = todayPt)
        }
    }
}

private fun buildSmoothPath(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points[0].x, points[0].y)
    if (points.size == 1) return@apply

    for (i in 0 until points.size - 1) {
        val p0 = if (i > 0) points[i - 1] else points[i]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else points[i + 1]

        val cp1 = Offset(p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f)
        val cp2 = Offset(p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f)

        cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
    }
}
