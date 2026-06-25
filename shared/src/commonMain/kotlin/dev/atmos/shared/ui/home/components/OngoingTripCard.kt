package dev.atmos.shared.ui.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.location.OngoingSessionUiState
import dev.atmos.shared.location.SessionPhase
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Peach
import dev.atmos.shared.ui.theme.Sage
import dev.atmos.shared.util.LocalDistanceUnit
import dev.atmos.shared.util.formatDistance
import dev.atmos.shared.util.formatDistanceValue

/**
 * Live trip card shown on HomeScreen while a session is being tracked.
 *
 * Three visual variants driven by [OngoingSessionUiState.phase]:
 *   • [SessionPhase.SessionStarting] → compact pill: "Detecting trip…"
 *   • [SessionPhase.Active]          → full card with distance, elapsed time, legs strip
 *   • [SessionPhase.LegEnding]       → full card + countdown banner
 */
@Composable
fun OngoingTripCard(
    state: OngoingSessionUiState,
    onStopAndSave: () -> Unit,
    onDiscard: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.phase) {
        is SessionPhase.SessionStarting -> DetectingPill(
            onDiscard = onDiscard,
            modifier  = modifier,
        )

        is SessionPhase.Active -> ActiveCard(
            state         = state,
            onStopAndSave = onStopAndSave,
            onDiscard     = onDiscard,
            modifier      = modifier,
        )

        is SessionPhase.LegEnding -> LegEndingCard(
            state         = state,
            secondsLeft   = state.secondsUntilClose ?: 60,
            onResume      = onResume,
            onStopAndSave = onStopAndSave,
            onDiscard     = onDiscard,
            modifier      = modifier,
        )

        // Idle / SessionClosing — nothing to show; caller should not render this card
        else -> Unit
    }
}

// ── SessionStarting: compact detecting pill ───────────────────────────────────

@Composable
private fun DetectingPill(
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAtmosColors.current
    val dotAlpha by PulsingAlpha()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Peach.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Pulsing dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(dotAlpha)
                .background(Peach, CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Detecting trip…",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onDiscard,
            modifier = Modifier.size(28.dp),
        ) {
            Text(text = "✕", fontSize = 13.sp, color = colors.textSecondary)
        }
    }
}

// ── Active: full card ─────────────────────────────────────────────────────────

@Composable
private fun ActiveCard(
    state: OngoingSessionUiState,
    onStopAndSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors  = LocalAtmosColors.current
    val unit = LocalDistanceUnit.current
    val dotAlpha by PulsingAlpha()
    val mode = (state.phase as? SessionPhase.Active)?.currentMode
        ?: state.currentLeg?.mode

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(16.dp)),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Sage.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(dotAlpha)
                    .background(Sage, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (mode != null) mode.label else "Trip in progress",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Sage,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${state.elapsedMin} min",
                fontSize = 13.sp,
                color = colors.textSecondary,
            )
        }

        // ── Stats row ─────────────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            StatCell(
                value = state.totalDistKm.formatDistanceValue(unit),
                unit  = unit.label,
            )
            VerticalDividerLine()
            StatCell(
                value = "${state.elapsedMin}",
                unit  = "min",
            )
            if (state.currentLeg != null) {
                VerticalDividerLine()
                StatCell(
                    value = state.currentLeg.distanceKm.formatDistanceValue(unit),
                    unit  = "${mode?.emoji ?: "📍"} leg",
                )
            }
        }

        // ── Completed legs strip ──────────────────────────────────────────────
        if (state.completedLegs.isNotEmpty()) {
            HorizontalDivider(thickness = 1.dp, color = colors.divider)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                state.completedLegs.forEach { leg ->
                    Text(
                        text = "${leg.mode.emoji} ${leg.distanceKm.formatDistance(unit)}",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = colors.divider)

        // ── Actions ───────────────────────────────────────────────────────────
        CardActions(onDiscard = onDiscard, onStopAndSave = onStopAndSave)
    }
}

// ── LegEnding: countdown banner + card ───────────────────────────────────────

@Composable
private fun LegEndingCard(
    state: OngoingSessionUiState,
    secondsLeft: Int,
    onResume: () -> Unit,
    onStopAndSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAtmosColors.current
    val unit = LocalDistanceUnit.current
    val dotAlpha by PulsingAlpha()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(16.dp)),
    ) {
        // ── Countdown banner ──────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Peach.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(dotAlpha)
                    .background(Peach, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                tint = Peach,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Ending in ${secondsLeft}s",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Peach,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onResume,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 8.dp, vertical = 2.dp
                ),
            ) {
                Text(
                    text = "Resume",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Peach,
                )
            }
            Spacer(Modifier.width(4.dp))
            TextButton(
                onClick = onStopAndSave,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 8.dp, vertical = 2.dp
                ),
            ) {
                Text(
                    text = "End now",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary,
                )
            }
        }

        // ── Stats row (same as Active) ────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            StatCell(value = state.totalDistKm.formatDistanceValue(unit), unit = unit.label)
            VerticalDividerLine()
            StatCell(value = "${state.elapsedMin}", unit = "min")
        }

        if (state.completedLegs.isNotEmpty()) {
            HorizontalDivider(thickness = 1.dp, color = colors.divider)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                state.completedLegs.forEach { leg ->
                    Text(
                        text = "${leg.mode.emoji} ${leg.distanceKm.formatDistance(unit)}",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = colors.divider)
        CardActions(onDiscard = onDiscard, onStopAndSave = onStopAndSave)
    }
}

// ── Shared sub-composables ────────────────────────────────────────────────────

@Composable
private fun CardActions(
    onDiscard: () -> Unit,
    onStopAndSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(
            onClick = onDiscard,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "Discard",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = LocalAtmosColors.current.textSecondary,
            )
        }
        Button(
            onClick = onStopAndSave,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Sage),
        ) {
            Text(
                text = "Stop & save ✓",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun StatCell(value: String, unit: String) {
    val colors = LocalAtmosColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
        Text(
            text = unit,
            fontSize = 11.sp,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun VerticalDividerLine() {
    Box(
        modifier = Modifier
            .height(36.dp)
            .width(1.dp)
            .background(LocalAtmosColors.current.divider),
    )
}

/** Shared 0.4→1.0 alpha pulse, 1.5 s cycle. */
@Composable
private fun PulsingAlpha(): androidx.compose.runtime.State<Float> {
    val transition = rememberInfiniteTransition(label = "pulse")
    return transition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
}

// ── Local helpers ─────────────────────────────────────────────────────────────

private val TransportModeType.label: String
    get() = when (this) {
        TransportModeType.DRIVING        -> "Driving"
        TransportModeType.WALKING        -> "Walking"
        TransportModeType.CAB            -> "Cab"
        TransportModeType.TWO_WHEELER    -> "Two-wheeler"
        TransportModeType.AUTO_RICKSHAW  -> "Auto"
        TransportModeType.PUBLIC_TRANSIT -> "Transit"
        TransportModeType.BUS            -> "Bus"
        TransportModeType.METRO          -> "Metro"
        TransportModeType.TRAIN          -> "Train"
        TransportModeType.CYCLING        -> "Cycling"
        TransportModeType.FLIGHT         -> "Flight"
    }

private val TransportModeType.emoji: String
    get() = when (this) {
        TransportModeType.DRIVING        -> "🚗"
        TransportModeType.WALKING        -> "🚶"
        TransportModeType.CAB            -> "🚕"
        TransportModeType.TWO_WHEELER    -> "🛵"
        TransportModeType.AUTO_RICKSHAW  -> "🛺"
        TransportModeType.PUBLIC_TRANSIT,
        TransportModeType.BUS            -> "🚌"
        TransportModeType.METRO          -> "🚇"
        TransportModeType.TRAIN          -> "🚆"
        TransportModeType.CYCLING        -> "🚲"
        TransportModeType.FLIGHT         -> "✈️"
    }
