package dev.atmos.shared.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.common.AtmosCard
import dev.atmos.shared.ui.home.WeeklyDataPoint
import dev.atmos.shared.ui.theme.AverageDash
import dev.atmos.shared.ui.theme.ChartFillBottom
import dev.atmos.shared.ui.theme.ChartFillTop
import dev.atmos.shared.ui.theme.ChartLine
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors

@Composable
fun WeeklyTrendCard(
    data: List<WeeklyDataPoint>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAtmosColors.current

    AtmosCard(modifier = modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Text(
            text = "Weekly Trend",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Last 7 days of activity",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = colors.textSecondary,
        )

        Spacer(Modifier.height(20.dp))

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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
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
