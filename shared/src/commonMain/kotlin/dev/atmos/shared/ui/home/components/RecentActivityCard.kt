package dev.atmos.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.common.AtmosCard
import dev.atmos.shared.ui.home.RecentActivityEntry
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.home.commuteDisplayLabel
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Peach
import dev.atmos.shared.ui.theme.Sage

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
    homeLat: Double? = null,
    homeLng: Double? = null,
    workLat: Double? = null,
    workLng: Double? = null,
    modifier: Modifier = Modifier,
    onTripClick: (RecentActivityEntry) -> Unit = {},
    onLogTrip: () -> Unit = {},
) {
    val colors = LocalAtmosColors.current

    AtmosCard(modifier = modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Text(
            text = "Recent Activity",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Your latest journeys",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = colors.textSecondary,
        )

        Spacer(Modifier.height(16.dp))

        if (entries.isEmpty()) {
            // ── Inline empty state ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector        = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint               = colors.textSecondary,
                        modifier           = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text     = "No trips today",
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
                ActivityRow(
                    entry   = entry,
                    homeLat = homeLat,
                    homeLng = homeLng,
                    workLat = workLat,
                    workLng = workLng,
                    onClick = { onTripClick(entry) },
                )
                if (index < entries.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = colors.divider,
                    )
                }
            }
        }
    }
}

// ── Single activity row ───────────────────────────────────────────────────────

@Composable
internal fun ActivityRow(
    entry: RecentActivityEntry,
    homeLat: Double? = null,
    homeLng: Double? = null,
    workLat: Double? = null,
    workLng: Double? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalAtmosColors.current
    val originLabel = commuteDisplayLabel(
        entry.origin, entry.originLat, entry.originLng, homeLat, homeLng, workLat, workLng,
    )
    val destinationLabel = commuteDisplayLabel(
        entry.destination, entry.destLat, entry.destLng, homeLat, homeLng, workLat, workLng,
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
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

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                if (entry.destination.isBlank()) {
                    Text(
                        text = originLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    AddressTimelineDots(
                        dotColor = entry.mode.iconTint,
                        lineColor = colors.divider,
                        modifier = Modifier.padding(top = 5.dp, end = 8.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = originLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = destinationLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                when {
                    entry.source == "gps+receipt" -> {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Matched",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Sage,
                            modifier = Modifier
                                .background(Color(0xFFE8F7F0), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                    !entry.isAutoDetected -> {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Manual",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary,
                            modifier = Modifier
                                .background(colors.chipBg, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(12.dp),
                )
                Text(text = entry.timeLabel, fontSize = 12.sp, color = colors.textSecondary)
                Text(text = "·", fontSize = 12.sp, color = colors.textSecondary)
                Text(text = "${entry.durationMin} min", fontSize = 12.sp, color = colors.textSecondary)
                Text(text = "·", fontSize = 12.sp, color = colors.textSecondary)
                Text(
                    text = "${entry.kgCO2.toKgString()} kg CO₂",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun AddressTimelineDots(
    dotColor: Color,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(dotColor, CircleShape),
        )
        Box(
            modifier = Modifier
                .padding(vertical = 2.dp)
                .width(1.5.dp)
                .height(16.dp)
                .background(lineColor),
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(lineColor, CircleShape),
        )
    }
}

private fun Float.toKgString(): String {
    if (this == 0f) return "0"
    if (this % 1f == 0f) return toInt().toString()
    val intPart = toInt()
    return "$intPart.${((this - intPart) * 10).toInt()}"
}
