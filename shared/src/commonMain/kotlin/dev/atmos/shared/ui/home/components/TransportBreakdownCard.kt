package dev.atmos.shared.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.common.AtmosCard
import dev.atmos.shared.ui.home.HomeTrendPeriod
import dev.atmos.shared.ui.home.TransportModeEntry
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Peach
import dev.atmos.shared.ui.theme.Sage

// ── Color mappings ────────────────────────────────────────────────────────────

private val TransportModeType.barColor: Color
    get() = when (this) {
        TransportModeType.DRIVING,
        TransportModeType.CAB,
        TransportModeType.FLIGHT      -> AlertRed
        TransportModeType.PUBLIC_TRANSIT,
        TransportModeType.BUS,
        TransportModeType.METRO,
        TransportModeType.TRAIN       -> HorizonBlue
        TransportModeType.CYCLING,
        TransportModeType.WALKING     -> Sage
        TransportModeType.TWO_WHEELER,
        TransportModeType.AUTO_RICKSHAW -> Peach
    }

private val TransportModeType.iconTint: Color get() = barColor

private val TransportModeType.iconBackground: Color
    get() = when (this) {
        TransportModeType.DRIVING,
        TransportModeType.CAB,
        TransportModeType.FLIGHT      -> Color(0xFFFFEEEC)
        TransportModeType.PUBLIC_TRANSIT,
        TransportModeType.BUS,
        TransportModeType.METRO,
        TransportModeType.TRAIN       -> Color(0xFFE8F2FA)
        TransportModeType.CYCLING,
        TransportModeType.WALKING     -> Color(0xFFE8F7F0)
        TransportModeType.TWO_WHEELER,
        TransportModeType.AUTO_RICKSHAW -> Color(0xFFFFF3EE)
    }

private val TransportModeType.icon: ImageVector
    get() = when (this) {
        TransportModeType.DRIVING       -> Icons.Outlined.DirectionsCar
        TransportModeType.CAB           -> Icons.Outlined.LocalTaxi
        TransportModeType.FLIGHT        -> Icons.Outlined.Flight
        TransportModeType.PUBLIC_TRANSIT,
        TransportModeType.BUS           -> Icons.Outlined.DirectionsBus
        TransportModeType.METRO,
        TransportModeType.TRAIN         -> Icons.Outlined.Train
        TransportModeType.CYCLING,
        TransportModeType.AUTO_RICKSHAW -> Icons.AutoMirrored.Outlined.DirectionsBike
        TransportModeType.WALKING       -> Icons.AutoMirrored.Outlined.DirectionsWalk
        TransportModeType.TWO_WHEELER   -> Icons.AutoMirrored.Outlined.DirectionsBike
    }

// ── Card ──────────────────────────────────────────────────────────────────────

@Composable
fun TransportBreakdownCard(
    entries: List<TransportModeEntry>,
    period: HomeTrendPeriod = HomeTrendPeriod.DAILY,
    onLogTrip: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalAtmosColors.current

    AtmosCard(modifier = modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Text(
            text = "Transport Breakdown",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = when (period) {
                HomeTrendPeriod.DAILY       -> "Today's activity by mode"
                HomeTrendPeriod.WEEKLY      -> "This week's activity by mode"
                HomeTrendPeriod.FORTNIGHTLY -> "This fortnight's activity by mode"
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = colors.textSecondary,
        )

        Spacer(Modifier.height(20.dp))

        if (entries.isEmpty()) {
            // ── Inline empty state ────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector        = Icons.Outlined.PieChart,
                        contentDescription = null,
                        tint               = colors.textSecondary,
                        modifier           = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when (period) {
                            HomeTrendPeriod.DAILY       -> "No trips today"
                            HomeTrendPeriod.WEEKLY      -> "No trips this week"
                            HomeTrendPeriod.FORTNIGHTLY -> "No trips this fortnight"
                        },
                        fontSize = 14.sp,
                        color    = colors.textSecondary,
                    )
                    TextButton(onClick = onLogTrip) {
                        Text(
                            text       = "Log one now →",
                            fontSize   = 13.sp,
                            color      = HorizonBlue,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        } else {
            entries.forEachIndexed { index, entry ->
                TransportModeRow(entry = entry)
                if (index < entries.lastIndex) Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// ── Single mode row ───────────────────────────────────────────────────────────

@Composable
private fun TransportModeRow(
    entry: TransportModeEntry,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAtmosColors.current
    val rowBg = if (entry.mode.isZeroEmission) colors.subtleGreenBg else Color.Transparent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBg)
            .then(
                if (entry.mode.isZeroEmission)
                    Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                else Modifier,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(color = entry.mode.iconBackground, shape = CircleShape),
            ) {
                Icon(
                    imageVector = entry.mode.icon,
                    contentDescription = entry.displayName,
                    tint = entry.mode.iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = "${entry.distanceKm.toDistanceString()} km",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textSecondary,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${entry.kgCO2.toKgString()} kg",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                    if (entry.mode.isZeroEmission) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Eco,
                            contentDescription = "Zero emission",
                            tint = Sage,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(Modifier.height(1.dp))
                Text(
                    text = "${entry.percentage}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        AnimatedProgressBar(
            fraction = entry.percentage / 100f,
            color = entry.mode.barColor,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Animated progress bar ─────────────────────────────────────────────────────

@Composable
private fun AnimatedProgressBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors  = LocalAtmosColors.current
    val barHeight = 4.dp
    val barShape  = RoundedCornerShape(50)

    var animTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(fraction) { animTarget = fraction }

    val animatedFraction by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = tween(durationMillis = 800),
        label = "progressBar",
    )

    Box(modifier = modifier.height(barHeight).background(colors.divider, barShape)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction)
                .height(barHeight)
                .background(color, barShape),
        )
    }
}

// ── Formatting helpers ────────────────────────────────────────────────────────

private fun Float.toKgString(): String {
    if (this == 0f) return "0"
    if (this % 1f == 0f) return toInt().toString()
    val intPart = toInt()
    return "$intPart.${((this - intPart) * 10).toInt()}"
}

private fun Float.toDistanceString(): String {
    if (this % 1f == 0f) return toInt().toString()
    val intPart = toInt()
    return "$intPart.${((this - intPart) * 10).toInt()}"
}
