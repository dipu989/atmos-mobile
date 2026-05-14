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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.common.AtmosCard
import dev.atmos.shared.ui.home.RecentActivityEntry
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.Peach
import dev.atmos.shared.ui.theme.Sage
import dev.atmos.shared.ui.theme.TextPrimary
import dev.atmos.shared.ui.theme.TextSecondary

// ── Color / icon mappings ─────────────────────────────────────────────────────

private val TransportModeType.iconTint: Color
    get() = when (this) {
        TransportModeType.DRIVING,
        TransportModeType.CAB,
        TransportModeType.FLIGHT        -> AlertRed
        TransportModeType.PUBLIC_TRANSIT,
        TransportModeType.BUS,
        TransportModeType.METRO,
        TransportModeType.TRAIN         -> HorizonBlue
        TransportModeType.CYCLING,
        TransportModeType.WALKING       -> Sage
        TransportModeType.TWO_WHEELER,
        TransportModeType.AUTO_RICKSHAW -> Peach
    }

private val TransportModeType.iconBackground: Color
    get() = when (this) {
        TransportModeType.DRIVING,
        TransportModeType.CAB,
        TransportModeType.FLIGHT        -> Color(0xFFFFEEEC)
        TransportModeType.PUBLIC_TRANSIT,
        TransportModeType.BUS,
        TransportModeType.METRO,
        TransportModeType.TRAIN         -> Color(0xFFE8F2FA)
        TransportModeType.CYCLING,
        TransportModeType.WALKING       -> Color(0xFFE8F7F0)
        TransportModeType.TWO_WHEELER,
        TransportModeType.AUTO_RICKSHAW -> Color(0xFFFFF3EE)
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

// ── Card ──────────────────────────────────────────────────────────────────────

@Composable
fun RecentActivityCard(
    entries: List<RecentActivityEntry>,
    modifier: Modifier = Modifier,
) {
    AtmosCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 20.dp,
    ) {
        Text(
            text = "Recent Activity",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Your latest journeys",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = TextSecondary,
        )

        Spacer(Modifier.height(16.dp))

        entries.forEachIndexed { index, entry ->
            ActivityRow(entry = entry)
            if (index < entries.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 1.dp,
                    color = Color(0xFFF0F2F5),
                )
            }
        }
    }
}

// ── Single activity row ───────────────────────────────────────────────────────

@Composable
private fun ActivityRow(
    entry: RecentActivityEntry,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        // Mode icon circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(color = entry.mode.iconBackground, shape = CircleShape),
        ) {
            Icon(
                imageVector = entry.mode.icon,
                contentDescription = null,
                tint = entry.mode.iconTint,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(14.dp))

        // Route + meta
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${entry.origin} → ${entry.destination}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = entry.timeLabel,
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
                Text(text = "·", fontSize = 12.sp, color = TextSecondary)
                Text(
                    text = "${entry.durationMin} min",
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
                Text(text = "·", fontSize = 12.sp, color = TextSecondary)
                Text(
                    text = "${entry.kgCO2.toKgString()} kg CO₂",
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            }
        }
    }
}

// ── Formatting helper ─────────────────────────────────────────────────────────

private fun Float.toKgString(): String {
    if (this == 0f) return "0"
    if (this % 1f == 0f) return toInt().toString()
    val intPart = toInt()
    val decPart = ((this - intPart) * 10).toInt()
    return "$intPart.$decPart"
}
