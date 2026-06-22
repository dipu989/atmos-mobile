package dev.atmos.shared.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.atmos.shared.ui.theme.LocalAtmosColors

// ── Shimmer primitive ─────────────────────────────────────────────────────────

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    val atmosColors = LocalAtmosColors.current
    val base      = atmosColors.chipBg
    val highlight = atmosColors.surface

    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_progress",
    )

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                val sweep  = size.width * 0.8f
                val startX = progress * (size.width + sweep) - sweep
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(base, highlight, base),
                        start  = Offset(startX, 0f),
                        end    = Offset(startX + sweep, 0f),
                    ),
                    size = size,
                )
            },
    )
}

// ── Skeleton helpers ──────────────────────────────────────────────────────────

@Composable
private fun ShimmerText(width: Dp, height: Dp = 14.dp, modifier: Modifier = Modifier) {
    ShimmerBox(
        modifier = modifier
            .width(width)
            .height(height),
        shape    = RoundedCornerShape(7.dp),
    )
}

@Composable
private fun ShimmerCircle(size: Dp) {
    ShimmerBox(modifier = Modifier.size(size), shape = CircleShape)
}

// ── Skeleton cards ────────────────────────────────────────────────────────────

/** Mirrors TodayImpactCard */
@Composable
fun TodayImpactSkeleton(modifier: Modifier = Modifier) {
    AtmosCard(modifier = modifier.fillMaxWidth(), contentPadding = 20.dp) {
        ShimmerText(width = 160.dp, height = 16.dp)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerCircle(72.dp)
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerText(width = 100.dp, height = 36.dp)
                ShimmerText(width = 70.dp, height = 12.dp)
            }
        }
        Spacer(Modifier.height(16.dp))
        ShimmerBox(
            modifier = Modifier.fillMaxWidth().height(8.dp),
            shape    = RoundedCornerShape(4.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row {
            ShimmerText(width = 90.dp, height = 12.dp, modifier = Modifier.weight(1f))
            ShimmerText(width = 70.dp, height = 12.dp)
        }
    }
}

/** Mirrors WeeklyTrendCard */
@Composable
fun WeeklyTrendSkeleton(modifier: Modifier = Modifier) {
    AtmosCard(modifier = modifier.fillMaxWidth(), contentPadding = 20.dp) {
        ShimmerText(width = 120.dp, height = 16.dp)
        Spacer(Modifier.height(4.dp))
        ShimmerText(width = 80.dp, height = 12.dp)
        Spacer(Modifier.height(14.dp))
        // Period selector placeholder
        ShimmerBox(
            modifier = Modifier.fillMaxWidth().height(32.dp),
            shape    = RoundedCornerShape(10.dp),
        )
        Spacer(Modifier.height(20.dp))
        // Bar chart placeholder
        Row(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Bottom,
        ) {
            listOf(0.6f, 1f, 0.5f, 0.9f, 0.8f, 0.3f, 0.55f).forEach { fraction ->
                ShimmerBox(
                    modifier = Modifier.width(28.dp).fillMaxWidth(fraction).height((80 * fraction).dp),
                    shape    = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            repeat(7) { ShimmerText(width = 20.dp, height = 10.dp) }
        }
    }
}

/** Mirrors TransportBreakdownCard */
@Composable
fun TransportBreakdownSkeleton(modifier: Modifier = Modifier) {
    AtmosCard(modifier = modifier.fillMaxWidth(), contentPadding = 20.dp) {
        ShimmerText(width = 150.dp, height = 16.dp)
        Spacer(Modifier.height(4.dp))
        ShimmerText(width = 100.dp, height = 12.dp)
        Spacer(Modifier.height(20.dp))
        repeat(3) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth(),
            ) {
                ShimmerCircle(36.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerText(width = 80.dp, height = 13.dp)
                    ShimmerBox(
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        shape    = RoundedCornerShape(3.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                ShimmerText(width = 36.dp, height = 13.dp)
            }
            if (it < 2) Spacer(Modifier.height(16.dp))
        }
    }
}

/** Mirrors RecentActivityCard */
@Composable
fun RecentActivitySkeleton(modifier: Modifier = Modifier) {
    AtmosCard(modifier = modifier.fillMaxWidth(), contentPadding = 20.dp) {
        ShimmerText(width = 140.dp, height = 16.dp)
        Spacer(Modifier.height(4.dp))
        ShimmerText(width = 100.dp, height = 12.dp)
        Spacer(Modifier.height(20.dp))
        repeat(3) { index ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerCircle(48.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerText(width = 150.dp, height = 14.dp)
                    ShimmerText(width = 110.dp, height = 12.dp)
                }
            }
            if (index < 2) {
                Spacer(Modifier.height(12.dp))
                ShimmerBox(
                    modifier = Modifier.fillMaxWidth().height(1.dp),
                    shape    = RoundedCornerShape(1.dp),
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/** Mirrors InsightsSection (2 cards) */
@Composable
fun InsightsSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ShimmerText(width = 80.dp, height = 16.dp, modifier = Modifier.padding(horizontal = 4.dp))
        repeat(2) {
            AtmosCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerCircle(52.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShimmerText(width = 130.dp, height = 14.dp)
                        ShimmerText(width = 180.dp, height = 12.dp)
                        ShimmerText(width = 140.dp, height = 12.dp)
                    }
                }
            }
        }
    }
}

