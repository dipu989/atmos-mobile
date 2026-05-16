package dev.atmos.shared.ui.logactivity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.ModeOfTravel
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Peach
import dev.atmos.shared.ui.theme.Sage
import dev.atmos.shared.util.currentDateLabel
import dev.atmos.shared.util.currentTimeLabel

// ── Entry point ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogActivitySheet(
    onDismiss: () -> Unit,
    onTripLogged: (LoggedTrip) -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val colors = LocalAtmosColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        tonalElevation = 0.dp,
    ) {
        LogActivityContent(
            onDismiss = onDismiss,
            onTripLogged = onTripLogged,
        )
    }
}

// ── Sheet content ─────────────────────────────────────────────────────────────

@Composable
private fun LogActivityContent(
    onDismiss: () -> Unit,
    onTripLogged: (LoggedTrip) -> Unit,
) {
    val colors = LocalAtmosColors.current
    var selectedMode by remember { mutableStateOf(TransportModeType.DRIVING) }
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var originError by remember { mutableStateOf(false) }
    var destinationError by remember { mutableStateOf(false) }

    val routeReady = origin.isNotBlank() && destination.isNotBlank()
    // Distance and CO₂ will be calculated from the route once backend logic is wired up.
    // For now distanceKm stays 0f — the estimate card is hidden until then.
    val distanceKm = 0f
    val estimatedCO2 = 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Log Trip",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Text(text = "✕", fontSize = 16.sp, color = colors.textSecondary)
            }
        }

        // ── Transport mode ────────────────────────────────────────────────────
        SectionLabel("How did you travel?")
        ModeSelector(
            selectedMode = selectedMode,
            onModeSelected = { selectedMode = it },
        )

        // ── Route ─────────────────────────────────────────────────────────────
        SectionLabel("Where did you go?")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InputField(
                value = origin,
                onValueChange = { origin = it; originError = false },
                placeholder = "Starting point",
                leadingIcon = Icons.Outlined.MyLocation,
                isError = originError,
            )
            // Swap button
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(colors.background, CircleShape)
                        .clickable {
                            val tmp = origin
                            origin = destination
                            destination = tmp
                        },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapVert,
                        contentDescription = "Swap",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            InputField(
                value = destination,
                onValueChange = { destination = it; destinationError = false },
                placeholder = "Destination",
                leadingIcon = Icons.Outlined.LocationOn,
                isError = destinationError,
            )
        }

        // ── Distance (auto-calculated) ────────────────────────────────────────
        DistanceAutoRow(routeReady = routeReady)

        // ── Date & Time ───────────────────────────────────────────────────────
        SectionLabel("When?")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoPill(
                icon = Icons.Outlined.Schedule,
                label = currentDateLabel(),
                modifier = Modifier.weight(1f),
            )
            InfoPill(
                icon = Icons.Outlined.Schedule,
                label = currentTimeLabel(),
                modifier = Modifier.weight(1f),
            )
        }

        // ── CO₂ estimate ──────────────────────────────────────────────────────
        if (distanceKm > 0f) {
            Co2EstimateCard(
                estimatedCO2 = estimatedCO2,
                distanceKm = distanceKm,
                modeName = selectedMode.displayLabel,
            )
        }

        // ── CTA ───────────────────────────────────────────────────────────────
        Button(
            onClick = {
                originError = origin.isBlank()
                destinationError = destination.isBlank()

                if (!originError && !destinationError) {
                    onTripLogged(
                        LoggedTrip(
                            mode = selectedMode,
                            origin = origin.trim(),
                            destination = destination.trim(),
                            distanceKm = distanceKm,       // 0f until backend wired
                            estimatedKgCO2 = estimatedCO2, // 0f until backend wired
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HorizonBlue),
        ) {
            Text(
                text = "Log Trip",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }

        // Inline error hint
        if (originError || destinationError) {
            Text(
                text = "Please fill in all fields before logging.",
                fontSize = 13.sp,
                color = AlertRed,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Mode selector ─────────────────────────────────────────────────────────────

private data class ModeOption(
    val mode: TransportModeType,
    val label: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
)

private val modeOptions = listOf(
    ModeOption(TransportModeType.DRIVING,        "Car",     Icons.Outlined.DirectionsCar,                  AlertRed,     Color(0xFFFFEEEC)),
    ModeOption(TransportModeType.PUBLIC_TRANSIT, "Bus",     Icons.Outlined.DirectionsBus,                  HorizonBlue,  Color(0xFFE8F2FA)),
    ModeOption(TransportModeType.TRAIN,          "Train",   Icons.Outlined.Train,                          HorizonBlue,  Color(0xFFE8F2FA)),
    ModeOption(TransportModeType.METRO,          "Metro",   Icons.Outlined.Train,                          HorizonBlue,  Color(0xFFE8F2FA)),
    ModeOption(TransportModeType.CYCLING,        "Bike",    Icons.AutoMirrored.Outlined.DirectionsBike,    Sage,         Color(0xFFE8F7F0)),
    ModeOption(TransportModeType.WALKING,        "Walk",    Icons.AutoMirrored.Outlined.DirectionsWalk,    Sage,         Color(0xFFE8F7F0)),
    ModeOption(TransportModeType.CAB,            "Cab",     Icons.Outlined.LocalTaxi,                     AlertRed,     Color(0xFFFFEEEC)),
    ModeOption(TransportModeType.TWO_WHEELER,    "2W",      Icons.AutoMirrored.Outlined.DirectionsBike,    Peach,        Color(0xFFFFF3EE)),
    ModeOption(TransportModeType.AUTO_RICKSHAW,  "Auto",    Icons.AutoMirrored.Outlined.DirectionsBike,    Peach,        Color(0xFFFFF3EE)),
    ModeOption(TransportModeType.FLIGHT,         "Flight",  Icons.Outlined.Flight,                         AlertRed,     Color(0xFFFFEEEC)),
)

@Composable
private fun ModeSelector(
    selectedMode: TransportModeType,
    onModeSelected: (TransportModeType) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        items(modeOptions) { option ->
            ModeChip(
                option = option,
                isSelected = selectedMode == option.mode,
                onClick = { onModeSelected(option.mode) },
            )
        }
    }
}

@Composable
private fun ModeChip(
    option: ModeOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAtmosColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(68.dp)
            .background(
                color = if (isSelected) colors.insightBlueBg else colors.surface,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) HorizonBlue else colors.divider,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (isSelected) HorizonBlue.copy(alpha = 0.15f) else option.iconBg,
                    shape = CircleShape,
                ),
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.label,
                tint = if (isSelected) HorizonBlue else option.iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = option.label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) HorizonBlue else colors.textSecondary,
        )
    }
}

// ── Input field ───────────────────────────────────────────────────────────────

@Composable
private fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
) {
    val colors = LocalAtmosColors.current
    var isFocused by remember { mutableStateOf(false) }

    val borderColor = when {
        isError   -> AlertRed
        isFocused -> HorizonBlue
        else      -> colors.divider
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = if (isFocused) HorizonBlue else colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(text = placeholder, fontSize = 15.sp, color = colors.textTertiary)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Normal,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(HorizonBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
            )
        }
    }
}

// ── Date / time pill ──────────────────────────────────────────────────────────

@Composable
private fun InfoPill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAtmosColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(colors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, fontSize = 13.sp, color = colors.textSecondary)
    }
}

// ── CO₂ estimate card ─────────────────────────────────────────────────────────

@Composable
private fun Co2EstimateCard(
    estimatedCO2: Float,
    distanceKm: Float,
    modeName: String,
) {
    val colors = LocalAtmosColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.subtleGreenBg, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(Sage.copy(alpha = 0.15f), CircleShape),
        ) {
            Icon(
                imageVector = Icons.Outlined.Eco,
                contentDescription = null,
                tint = Sage,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Estimated Impact",
                fontSize = 12.sp,
                color = Sage,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (estimatedCO2 == 0f) "Zero emissions 🌿"
                else "${estimatedCO2.toDisplayString()} kg CO₂",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${distanceKm.toDisplayString()} km",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
            )
            Text(
                text = "by $modeName",
                fontSize = 12.sp,
                color = colors.textSecondary,
            )
        }
    }
}

// ── Distance auto-calculate row ───────────────────────────────────────────────

@Composable
private fun DistanceAutoRow(routeReady: Boolean) {
    val colors = LocalAtmosColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Straighten,
            contentDescription = null,
            tint = if (routeReady) HorizonBlue else colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (routeReady) "Calculating distance…" else "Distance calculated from route",
            fontSize = 15.sp,
            color = if (routeReady) colors.textSecondary else colors.textTertiary,
            modifier = Modifier.weight(1f),
        )
        if (routeReady) {
            Text(
                text = "Auto",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = HorizonBlue,
                modifier = Modifier
                    .background(colors.insightBlueBg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = LocalAtmosColors.current.textSecondary,
        modifier = Modifier.padding(bottom = 2.dp),
    )
}

// ── Data ──────────────────────────────────────────────────────────────────────

data class LoggedTrip(
    val mode: TransportModeType,
    val origin: String,
    val destination: String,
    val distanceKm: Float,
    val estimatedKgCO2: Float,
)

// ── Emission factors (kg CO₂ per km) ─────────────────────────────────────────

val TransportModeType.emissionFactor: Float
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
        TransportModeType.WALKING        -> 0.00f
    }

val TransportModeType.displayLabel: String
    get() = when (this) {
        TransportModeType.DRIVING        -> "Car"
        TransportModeType.CAB            -> "Cab"
        TransportModeType.PUBLIC_TRANSIT -> "Bus"
        TransportModeType.BUS            -> "Bus"
        TransportModeType.TRAIN          -> "Train"
        TransportModeType.METRO          -> "Metro"
        TransportModeType.CYCLING        -> "Bike"
        TransportModeType.WALKING        -> "Walk"
        TransportModeType.TWO_WHEELER    -> "Two-Wheeler"
        TransportModeType.AUTO_RICKSHAW  -> "Auto"
        TransportModeType.FLIGHT         -> "Flight"
    }

// ── Formatting ────────────────────────────────────────────────────────────────

private fun Float.toDisplayString(): String {
    if (this == 0f) return "0"
    if (this % 1f == 0f) return toInt().toString()
    val intPart = toInt()
    val dec = ((this - intPart) * 10).toInt()
    return "$intPart.$dec"
}
