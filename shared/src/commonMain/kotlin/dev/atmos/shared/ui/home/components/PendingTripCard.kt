package dev.atmos.shared.ui.home.components

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
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.location.PendingSessionEntry
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Peach
import dev.atmos.shared.ui.theme.Sage

// ── Card ──────────────────────────────────────────────────────────────────────

/**
 * Shown on HomeScreen after the detector auto-ends a session (stationarity timeout).
 * Displays the session's legs and totals; user can confirm, edit, or dismiss.
 *
 * - Single-leg sessions: shows the mode label + total distance/duration
 * - Multi-leg sessions:  shows a legs emoji strip ("🚗 8.6 km · 🚶 0.6 km") + totals
 */
@Composable
fun PendingTripCard(
    session: PendingSessionEntry,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors      = LocalAtmosColors.current
    val primaryMode = session.legs.firstOrNull()?.mode ?: TransportModeType.DRIVING
    val isMultiLeg  = session.legs.size > 1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = colors.surface, shape = RoundedCornerShape(16.dp)),
    ) {
        // ── Header: "Trip detected" badge + dismiss ───────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colors.insightBlueBg,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).background(HorizonBlue, CircleShape))
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector        = Icons.Outlined.MyLocation,
                contentDescription = null,
                tint               = HorizonBlue,
                modifier           = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text       = "Trip detected",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = HorizonBlue,
                modifier   = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Text(text = "✕", fontSize = 13.sp, color = colors.textSecondary)
            }
        }

        // ── Trip details ──────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            // Primary mode icon circle — always uses the first leg's mode
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(48.dp)
                    .background(primaryMode.iconBackground, CircleShape),
            ) {
                Icon(
                    imageVector        = primaryMode.icon,
                    contentDescription = null,
                    tint               = primaryMode.iconTint,
                    modifier           = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (isMultiLeg) {
                    // "🚗 8.6 km  ·  🚶 0.6 km"
                    Text(
                        text = session.legs.joinToString("  ·  ") { leg ->
                            "${leg.mode.emoji} ${leg.distanceKm.toDisplayString()} km"
                        },
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = colors.textPrimary,
                    )
                } else {
                    Text(
                        text       = primaryMode.displayLabel,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = colors.textPrimary,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text     = "${session.totalDistKm.toDisplayString()} km · ${session.totalDurationMin} min",
                    fontSize = 13.sp,
                    color    = colors.textSecondary,
                )
            }
        }

        HorizontalDivider(thickness = 1.dp, color = colors.divider)

        // ── Actions ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                Text(
                    text       = "Edit",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = HorizonBlue,
                )
            }
            Button(
                onClick  = onConfirm,
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Sage),
            ) {
                Text(
                    text       = "Confirm ✓",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                )
            }
        }
    }
}

// ── TransportModeType display helpers ────────────────────────────────────────

private val TransportModeType.displayLabel: String
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

private val TransportModeType.iconTint: Color
    get() = when (this) {
        TransportModeType.DRIVING,
        TransportModeType.CAB,
        TransportModeType.FLIGHT         -> AlertRed
        TransportModeType.PUBLIC_TRANSIT,
        TransportModeType.BUS,
        TransportModeType.METRO,
        TransportModeType.TRAIN          -> HorizonBlue
        TransportModeType.CYCLING,
        TransportModeType.WALKING        -> Sage
        TransportModeType.TWO_WHEELER,
        TransportModeType.AUTO_RICKSHAW  -> Peach
    }

private val TransportModeType.iconBackground: Color
    get() = when (this) {
        TransportModeType.DRIVING,
        TransportModeType.CAB,
        TransportModeType.FLIGHT         -> Color(0xFFFFEEEC)
        TransportModeType.PUBLIC_TRANSIT,
        TransportModeType.BUS,
        TransportModeType.METRO,
        TransportModeType.TRAIN          -> Color(0xFFE8F2FA)
        TransportModeType.CYCLING,
        TransportModeType.WALKING        -> Color(0xFFE8F7F0)
        TransportModeType.TWO_WHEELER,
        TransportModeType.AUTO_RICKSHAW  -> Color(0xFFFFF3EE)
    }

private val TransportModeType.icon: ImageVector
    get() = when (this) {
        TransportModeType.DRIVING        -> Icons.Outlined.DirectionsCar
        TransportModeType.CAB            -> Icons.Outlined.LocalTaxi
        TransportModeType.FLIGHT         -> Icons.Outlined.Flight
        TransportModeType.PUBLIC_TRANSIT,
        TransportModeType.BUS            -> Icons.Outlined.DirectionsBus
        TransportModeType.METRO,
        TransportModeType.TRAIN          -> Icons.Outlined.Train
        TransportModeType.CYCLING,
        TransportModeType.AUTO_RICKSHAW  -> Icons.AutoMirrored.Outlined.DirectionsBike
        TransportModeType.WALKING        -> Icons.AutoMirrored.Outlined.DirectionsWalk
        TransportModeType.TWO_WHEELER    -> Icons.AutoMirrored.Outlined.DirectionsBike
    }

private fun Float.toDisplayString(): String {
    if (this % 1f == 0f) return toInt().toString()
    val intPart = toInt()
    return "$intPart.${((this - intPart) * 10).toInt()}"
}
