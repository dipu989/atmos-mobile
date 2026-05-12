package dev.atmos.shared.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.atmos.shared.ui.theme.RingTrack
import dev.atmos.shared.ui.theme.Sage

/**
 * Animated circular progress ring used in TodayImpactCard.
 *
 * @param progress  0f..1f fraction of ring filled
 * @param size      overall diameter of the ring
 * @param stroke    stroke width
 * @param trackColor unfilled arc color
 * @param progressColor filled arc color
 * @param content   composable placed at center (typically the number + unit)
 */
@Composable
fun CircularProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    stroke: Dp = 12.dp,
    trackColor: Color = RingTrack,
    progressColor: Color = Sage,
    content: @Composable () -> Unit = {},
) {
    var animTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(progress) { animTarget = progress }

    val animatedProgress by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = tween(durationMillis = 900),
        label = "ringProgress",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size),
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = stroke.toPx()
            val inset    = strokePx / 2f

            // Track — full circle
            drawArc(
                color      = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter  = false,
                style      = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft    = androidx.compose.ui.geometry.Offset(inset, inset),
                size       = androidx.compose.ui.geometry.Size(
                    this.size.width - strokePx,
                    this.size.height - strokePx,
                ),
            )

            // Progress arc
            drawArc(
                color      = progressColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter  = false,
                style      = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft    = androidx.compose.ui.geometry.Offset(inset, inset),
                size       = androidx.compose.ui.geometry.Size(
                    this.size.width - strokePx,
                    this.size.height - strokePx,
                ),
            )
        }

        content()
    }
}
