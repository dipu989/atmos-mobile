package dev.atmos.shared.ui.tripdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.common.AtmosCard
import dev.atmos.shared.ui.home.RecentActivityEntry
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Peach
import dev.atmos.shared.ui.theme.Sage
import kotlin.math.ceil

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    entry: RecentActivityEntry,
    dailyGoalKgCO2: Float = 5.0f,
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val colors      = LocalAtmosColors.current
    val scrollState = rememberScrollState()
    val heroColor   = entry.mode.themeColor

    // Using Scaffold + TopAppBar so the back button lives in its own layout slot,
    // completely outside the scroll container and the Android gesture navigation zone.
    // The TopAppBar is surface-coloured so it blends seamlessly with the hero section.
    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint               = colors.textSecondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                ),
            )
        },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(scrollState)
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            // ── Hero ──────────────────────────────────────────────────────────
            // Height reduced to 256dp; the TopAppBar above it (≈56dp) provides the
            // same total visual area that the old 310dp hero + floating button did.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
                    .background(colors.surface),
            ) {
                // Atmospheric canvas background
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(
                        color  = heroColor.copy(alpha = 0.10f),
                        radius = size.width * 0.75f,
                        center = Offset(size.width * 0.88f, size.height * 0.08f),
                    )
                    drawCircle(
                        color  = heroColor.copy(alpha = 0.06f),
                        radius = size.width * 0.50f,
                        center = Offset(size.width * 0.08f, size.height * 0.90f),
                    )
                    drawCircle(
                        color  = heroColor.copy(alpha = 0.08f),
                        radius = size.width * 0.18f,
                        center = Offset(size.width * 0.18f, size.height * 0.16f),
                    )
                }

                // Centred hero content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Mode icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(entry.mode.iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = entry.mode.icon,
                        contentDescription = entry.mode.label,
                        tint               = entry.mode.iconTint,
                        modifier           = Modifier.size(28.dp),
                    )
                }

                Spacer(Modifier.height(14.dp))

                // CO₂ — THE hero number
                Text(
                    text  = entry.kgCO2.toDisplayString(),
                    style = TextStyle(
                        fontSize      = 72.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = entry.kgCO2.co2Color,
                        letterSpacing = (-2).sp,
                    ),
                )
                Text(
                    text  = "kg CO₂",
                    style = TextStyle(fontSize = 14.sp, color = colors.textSecondary),
                )

                Spacer(Modifier.height(14.dp))

                // Route
                Text(
                    text  = "${entry.origin}  →  ${entry.destination}",
                    style = TextStyle(
                        fontSize  = 15.sp,
                        color     = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    ),
                )

                Spacer(Modifier.height(10.dp))

                // Distance · Duration pills
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (entry.distanceKm > 0f) {
                        HeroPill("${entry.distanceKm.toDisplayString()} km")
                    }
                    HeroPill("${entry.durationMin} min")
                }

                Spacer(Modifier.height(8.dp))

                // Date & time
                Text(
                    text  = "${entry.dateLabel}  ·  ${entry.timeLabel}",
                    style = TextStyle(fontSize = 12.sp, color = colors.textTertiary),
                )
            }
        }

        // ── Scrollable content ────────────────────────────────────────────────
        Column(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp),
        ) {
            // Daily budget progress
            DailyBudgetCard(kgCO2 = entry.kgCO2, dailyGoal = dailyGoalKgCO2)

            // Impact context — zero emission gets a celebration; others get facts
            if (entry.kgCO2 == 0f) {
                ZeroEmissionCard()
            } else {
                ImpactContextCard(kgCO2 = entry.kgCO2)

                // Alternative transport — only when a greener option exists
                entry.mode.bestEcoAlternative()?.let { altMode ->
                    if (entry.distanceKm > 0f) {
                        AlternativeCard(entry = entry, altMode = altMode)
                    }
                }
            }

            // Trip metadata
            TripDetailsCard(entry = entry)

            // Actions
            TripActions(onEdit = onEdit, onDelete = onDelete)
        }
        } // end scrollable Column

    } // end Scaffold
}

// ── Hero pill ─────────────────────────────────────────────────────────────────

@Composable
private fun HeroPill(text: String) {
    val colors = LocalAtmosColors.current
    Text(
        text     = text,
        style    = TextStyle(
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium,
            color      = colors.textSecondary,
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface.copy(alpha = 0.75f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

// ── Daily budget card ─────────────────────────────────────────────────────────

@Composable
private fun DailyBudgetCard(kgCO2: Float, dailyGoal: Float) {
    val colors   = LocalAtmosColors.current
    val progress = (kgCO2 / dailyGoal).coerceIn(0f, 1f)
    val pct      = (progress * 100).toInt()
    val barColor = when {
        progress < 0.5f -> Sage
        progress < 0.8f -> HorizonBlue
        progress < 1f   -> Peach
        else            -> AlertRed
    }
    val ringTrack = colors.ringTrack

    AtmosCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text     = "Today's budget",
                style    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
                modifier = Modifier.weight(1f),
            )
            Text(
                text  = "$pct%",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = barColor),
            )
        }

        Spacer(Modifier.height(14.dp))

        // Progress bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        ) {
            drawRoundRect(color = ringTrack, cornerRadius = CornerRadius(4.dp.toPx()))
            if (progress > 0f) {
                drawRoundRect(
                    color        = barColor,
                    size         = size.copy(width = size.width * progress),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row {
            Text(
                text     = "${kgCO2.toDisplayString()} kg used",
                style    = TextStyle(fontSize = 13.sp, color = colors.textSecondary),
                modifier = Modifier.weight(1f),
            )
            Text(
                text  = "${dailyGoal.toDisplayString()} kg goal",
                style = TextStyle(fontSize = 13.sp, color = colors.textTertiary),
            )
        }
    }
}

// ── Zero emission celebration ─────────────────────────────────────────────────

@Composable
private fun ZeroEmissionCard() {
    val colors = LocalAtmosColors.current
    AtmosCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🌿", fontSize = 32.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text  = "Zero emissions",
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Sage),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text  = "This trip produced no CO₂. Every step counts toward a healthier planet.",
                    style = TextStyle(fontSize = 13.sp, color = colors.textSecondary, lineHeight = 19.sp),
                )
            }
        }
    }
}

// ── Impact context card ───────────────────────────────────────────────────────

@Composable
private fun ImpactContextCard(kgCO2: Float) {
    val colors      = LocalAtmosColors.current
    val treesNeeded = ceil(kgCO2 / 0.06f).toInt().coerceAtLeast(1)
    val ledHours    = ceil(kgCO2 / 0.03f).toInt().coerceAtLeast(1)
    val globalPct   = (kgCO2 / 4.0f * 100).toInt().coerceAtLeast(1)

    AtmosCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Text(
            text  = "What this means",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
        )
        Spacer(Modifier.height(16.dp))

        ImpactRow(
            emoji = "🌳",
            label = "Trees needed to offset (1 day)",
            value = "$treesNeeded trees",
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 13.dp), color = colors.divider)
        ImpactRow(
            emoji = "💡",
            label = "LED light hours equivalent",
            value = "$ledHours hours",
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 13.dp), color = colors.divider)
        ImpactRow(
            emoji = "🌍",
            label = "Of the global daily average",
            value = "$globalPct%",
        )
    }
}

@Composable
private fun ImpactRow(emoji: String, label: String, value: String) {
    val colors = LocalAtmosColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = emoji, fontSize = 22.sp, modifier = Modifier.width(34.dp))
        Text(
            text     = label,
            style    = TextStyle(fontSize = 14.sp, color = colors.textSecondary),
            modifier = Modifier.weight(1f),
        )
        Text(
            text  = value,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
        )
    }
}

// ── Alternative transport card ────────────────────────────────────────────────

@Composable
private fun AlternativeCard(entry: RecentActivityEntry, altMode: TransportModeType) {
    val colors  = LocalAtmosColors.current
    val altCO2  = (entry.distanceKm * altMode.emissionFactor).let {
        // round to 1 decimal for cleanliness
        (it * 10).toInt() / 10f
    }
    val savings     = (entry.kgCO2 - altCO2).coerceAtLeast(0f)
    val savingsPct  = if (entry.kgCO2 > 0f) (savings / entry.kgCO2 * 100).toInt() else 0

    AtmosCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Text(
            text  = "Greener alternative",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
        )
        Spacer(Modifier.height(16.dp))

        // Your trip bar
        ComparisonBar(
            icon    = entry.mode.icon,
            iconTint = entry.mode.iconTint,
            iconBg  = entry.mode.iconBg,
            label   = entry.mode.label,
            kgCO2   = entry.kgCO2,
            maxCO2  = entry.kgCO2,
            barColor = entry.mode.iconTint,
        )

        Spacer(Modifier.height(14.dp))

        // Alternative bar
        ComparisonBar(
            icon    = altMode.icon,
            iconTint = altMode.iconTint,
            iconBg  = altMode.iconBg,
            label   = altMode.label,
            kgCO2   = altCO2,
            maxCO2  = entry.kgCO2,
            barColor = altMode.iconTint,
        )

        Spacer(Modifier.height(16.dp))

        // Savings summary pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.subtleGreenBg)
                .padding(12.dp),
        ) {
            Text(
                text  = "Taking ${altMode.label} saves ${savings.toDisplayString()} kg · $savingsPct% less CO₂ 🌿",
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Sage),
            )
        }
    }
}

@Composable
private fun ComparisonBar(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    label: String,
    kgCO2: Float,
    maxCO2: Float,
    barColor: Color,
) {
    val colors      = LocalAtmosColors.current
    val fraction    = if (maxCO2 > 0f) (kgCO2 / maxCO2).coerceIn(0f, 1f) else 0f
    val minFraction = if (kgCO2 > 0f) 0.05f else 0f
    val ringTrack   = colors.ringTrack

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = TextStyle(fontSize = 13.sp, color = colors.textSecondary))
            Spacer(Modifier.height(5.dp))
            Canvas(Modifier.fillMaxWidth().height(6.dp)) {
                drawRoundRect(color = ringTrack, cornerRadius = CornerRadius(3.dp.toPx()))
                val draw = maxOf(fraction, minFraction)
                if (draw > 0f) {
                    drawRoundRect(
                        color        = barColor.copy(alpha = 0.75f),
                        size         = size.copy(width = size.width * draw),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text  = if (kgCO2 == 0f) "0 kg" else "${kgCO2.toDisplayString()} kg",
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
        )
    }
}

// ── Trip details card ─────────────────────────────────────────────────────────

@Composable
private fun TripDetailsCard(entry: RecentActivityEntry) {
    val colors = LocalAtmosColors.current

    AtmosCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Text(
            text  = "Trip details",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
        )
        Spacer(Modifier.height(16.dp))

        DetailRow(Icons.Outlined.CalendarToday, "Date",      entry.dateLabel)
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.divider)
        DetailRow(Icons.Outlined.Schedule,      "Time",      entry.timeLabel)
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.divider)
        DetailRow(entry.mode.icon,              "Mode",      entry.mode.label)
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.divider)
        DetailRow(
            icon  = if (entry.isAutoDetected) Icons.Outlined.MyLocation else Icons.Outlined.Edit,
            label = "Logged by",
            value = if (entry.isAutoDetected) "Auto-detected" else "Manual entry",
        )
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    val colors = LocalAtmosColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = colors.textSecondary,
            modifier           = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text     = label,
            style    = TextStyle(fontSize = 14.sp, color = colors.textSecondary),
            modifier = Modifier.weight(1f),
        )
        Text(
            text  = value,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary),
        )
    }
}

// ── Actions ───────────────────────────────────────────────────────────────────

@Composable
private fun TripActions(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors           = LocalAtmosColors.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier              = Modifier.fillMaxWidth(),
        verticalArrangement   = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedButton(
            onClick  = onEdit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            border   = BorderStroke(1.5.dp, HorizonBlue),
            colors   = ButtonDefaults.outlinedButtonColors(containerColor = colors.surface),
        ) {
            Icon(Icons.Outlined.Edit, null, tint = HorizonBlue, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text  = "Edit trip",
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = HorizonBlue),
            )
        }
        TextButton(
            onClick  = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text  = "Delete trip",
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AlertRed),
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title            = { Text("Delete trip?", color = colors.textPrimary) },
            text             = {
                Text(
                    "This will permanently remove this trip from your history.",
                    color = colors.textSecondary,
                )
            },
            confirmButton    = {
                Button(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors  = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    shape   = RoundedCornerShape(8.dp),
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton    = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor   = colors.surface,
        )
    }
}

// ── TransportModeType extensions (local to this screen) ───────────────────────

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

private val TransportModeType.iconBg: Color
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
        TransportModeType.AUTO_RICKSHAW,
        TransportModeType.TWO_WHEELER    -> Icons.AutoMirrored.Outlined.DirectionsBike
        TransportModeType.WALKING        -> Icons.AutoMirrored.Outlined.DirectionsWalk
    }

private val TransportModeType.label: String
    get() = when (this) {
        TransportModeType.DRIVING        -> "Car"
        TransportModeType.CAB            -> "Cab / Taxi"
        TransportModeType.TWO_WHEELER    -> "2-Wheeler"
        TransportModeType.AUTO_RICKSHAW  -> "Auto Rickshaw"
        TransportModeType.PUBLIC_TRANSIT -> "Bus"
        TransportModeType.BUS            -> "Bus"
        TransportModeType.TRAIN          -> "Train"
        TransportModeType.METRO          -> "Metro"
        TransportModeType.FLIGHT         -> "Flight"
        TransportModeType.CYCLING        -> "Bicycle"
        TransportModeType.WALKING        -> "Walk"
    }

// The brand colour that tints the hero canvas background
private val TransportModeType.themeColor: Color
    get() = when (this) {
        TransportModeType.CYCLING,
        TransportModeType.WALKING       -> Sage
        TransportModeType.PUBLIC_TRANSIT,
        TransportModeType.BUS,
        TransportModeType.METRO,
        TransportModeType.TRAIN         -> HorizonBlue
        TransportModeType.TWO_WHEELER,
        TransportModeType.AUTO_RICKSHAW -> Peach
        TransportModeType.DRIVING,
        TransportModeType.CAB,
        TransportModeType.FLIGHT        -> AlertRed
    }

private val TransportModeType.emissionFactor: Float
    get() = when (this) {
        TransportModeType.DRIVING        -> 0.21f
        TransportModeType.CAB            -> 0.21f
        TransportModeType.TWO_WHEELER    -> 0.11f
        TransportModeType.AUTO_RICKSHAW  -> 0.10f
        TransportModeType.BUS,
        TransportModeType.PUBLIC_TRANSIT -> 0.09f
        TransportModeType.TRAIN,
        TransportModeType.METRO          -> 0.04f
        TransportModeType.FLIGHT         -> 0.26f
        TransportModeType.CYCLING,
        TransportModeType.WALKING        -> 0f
    }

private fun TransportModeType.bestEcoAlternative(): TransportModeType? = when (this) {
    TransportModeType.DRIVING,
    TransportModeType.CAB,
    TransportModeType.FLIGHT            -> TransportModeType.METRO
    TransportModeType.TWO_WHEELER,
    TransportModeType.AUTO_RICKSHAW     -> TransportModeType.BUS
    TransportModeType.PUBLIC_TRANSIT,
    TransportModeType.BUS               -> TransportModeType.METRO
    TransportModeType.TRAIN,
    TransportModeType.METRO             -> TransportModeType.CYCLING
    TransportModeType.CYCLING,
    TransportModeType.WALKING           -> null
}

// ── Float helpers ─────────────────────────────────────────────────────────────

// CO₂ number colour: green → blue → amber → red by severity
private val Float.co2Color: Color
    get() = when {
        this == 0f  -> Sage
        this < 0.5f -> HorizonBlue
        this < 2f   -> Peach
        else        -> AlertRed
    }

private fun Float.toDisplayString(): String {
    if (this == 0f) return "0"
    if (this % 1f == 0f) return toInt().toString()
    val intPart = toInt()
    return "$intPart.${((this - intPart) * 10).toInt()}"
}
