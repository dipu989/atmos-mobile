package dev.atmos.shared.ui.logactivity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import dev.atmos.shared.network.PlaceSearchService
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.home.emissionFactor
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Peach
import dev.atmos.shared.ui.theme.Sage
import dev.atmos.shared.util.currentDateLabel
import dev.atmos.shared.util.currentTimeLabel
import dev.atmos.shared.util.LocalDistanceUnit
import dev.atmos.shared.util.formatDistance
import dev.atmos.shared.util.formatDistanceValue
import dev.atmos.shared.util.fromDisplayUnit

private val placeSearchService = PlaceSearchService()

// ── Pre-fill model (used by pending trip Edit) ────────────────────────────────

data class LogActivityPrefill(
    val origin: String = "",
    val destination: String = "",
    val mode: TransportModeType? = null,
    /** Pre-fill the distance field when editing an existing confirmed trip. 0f = leave blank. */
    val distanceKm: Float = 0f,
    // Coords pre-filled when editing a trip that already has GPS data.
    val originLat: Double? = null,
    val originLng: Double? = null,
    val destLat: Double? = null,
    val destLng: Double? = null,
)

// ── Entry point ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogActivitySheet(
    onDismiss: () -> Unit,
    onTripLogged: (LoggedTrip) -> Unit = {},
    prefill: LogActivityPrefill? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val colors = LocalAtmosColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        tonalElevation = 0.dp,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Fixed fraction of the available sheet height so it stays put
            // regardless of how many autocomplete suggestions are showing.
            val sheetHeight = maxHeight * 0.85f

            LogActivityContent(
                onDismiss = onDismiss,
                onTripLogged = onTripLogged,
                prefill = prefill,
                modifier = Modifier.height(sheetHeight),
            )
        }
    }
}

// ── Sheet content ─────────────────────────────────────────────────────────────

@Composable
private fun LogActivityContent(
    onDismiss: () -> Unit,
    onTripLogged: (LoggedTrip) -> Unit,
    prefill: LogActivityPrefill? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAtmosColors.current
    val unit = LocalDistanceUnit.current
    var selectedMode by remember { mutableStateOf(prefill?.mode ?: TransportModeType.DRIVING) }
    var origin by remember { mutableStateOf(prefill?.origin ?: "") }
    var destination by remember { mutableStateOf(prefill?.destination ?: "") }
    // Resolved coordinates from Google Places autocomplete.
    var originSelection by remember {
        mutableStateOf(
            if (prefill?.originLat != null && prefill.originLng != null)
                PlaceSelection(prefill.origin, prefill.originLat, prefill.originLng)
            else null
        )
    }
    var destSelection by remember {
        mutableStateOf(
            if (prefill?.destLat != null && prefill.destLng != null)
                PlaceSelection(prefill.destination, prefill.destLat, prefill.destLng)
            else null
        )
    }
    var originError by remember { mutableStateOf(false) }
    var destinationError by remember { mutableStateOf(false) }
    var distanceError by remember { mutableStateOf(false) }

    // Distance: user-editable; pre-filled when editing an existing trip.
    // The field always shows/accepts the active display unit — distanceKm (canonical,
    // backend-facing) is converted from it below and back again wherever it's set.
    val prefillDistStr = prefill?.distanceKm?.let { if (it > 0f) it.formatDistanceValue(unit) else "" } ?: ""
    var distanceText by remember(prefill) { mutableStateOf(prefillDistStr) }
    val distanceKm = (distanceText.toFloatOrNull() ?: 0f).fromDisplayUnit(unit)
    val estimatedCO2 = distanceKm * selectedMode.emissionFactor

    // Auto-calculated once origin + destination are both selected; re-armed whenever
    // a new place is picked. Typing into the field directly disarms it so a later
    // mode switch doesn't clobber a manual value.
    var distanceAutoFillEnabled by remember { mutableStateOf(true) }
    var isCalculatingDistance by remember { mutableStateOf(false) }

    LaunchedEffect(originSelection, destSelection, selectedMode) {
        val originPlace = originSelection
        val destPlace = destSelection
        val mode = selectedMode.distanceMatrixMode
        if (originPlace == null || destPlace == null || mode == null || !distanceAutoFillEnabled) {
            return@LaunchedEffect
        }
        isCalculatingDistance = true
        try {
            placeSearchService
                .distance(originPlace.lat, originPlace.lng, destPlace.lat, destPlace.lng, mode)
                .onSuccess { result ->
                    if (result.found) {
                        distanceText = result.distanceKm.toFloat().formatDistanceValue(unit)
                        distanceError = false
                    }
                }
        } finally {
            isCalculatingDistance = false
        }
    }

    Column(
        modifier = modifier
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
            PlaceAutocompleteField(
                value = origin,
                onValueChange = { origin = it; originError = false },
                onPlaceSelected = {
                    originSelection = it
                    if (it != null) distanceAutoFillEnabled = true
                },
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
                            val tmpText = origin
                            val tmpSel = originSelection
                            origin = destination
                            originSelection = destSelection
                            destination = tmpText
                            destSelection = tmpSel
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
            PlaceAutocompleteField(
                value = destination,
                onValueChange = { destination = it; destinationError = false },
                onPlaceSelected = {
                    destSelection = it
                    if (it != null) distanceAutoFillEnabled = true
                },
                placeholder = "Destination",
                leadingIcon = Icons.Outlined.LocationOn,
                isError = destinationError,
            )
        }

        // ── Distance ──────────────────────────────────────────────────────────
        SectionLabel("Distance (${unit.label})")
        InputField(
            value        = distanceText,
            onValueChange = { input ->
                // Allow digits and at most one decimal point
                val filtered = input.filter { it.isDigit() || it == '.' }
                val dotCount = filtered.count { it == '.' }
                if (dotCount <= 1) {
                    distanceText = filtered
                    distanceError = false
                    distanceAutoFillEnabled = false
                }
            },
            placeholder  = "e.g. 8.6",
            leadingIcon  = Icons.Outlined.Straighten,
            keyboardType = KeyboardType.Decimal,
            isError      = distanceError,
            isLoading    = isCalculatingDistance,
        )

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
                originError      = origin.isBlank()
                destinationError = destination.isBlank()
                distanceError    = distanceKm <= 0f

                if (!originError && !destinationError && !distanceError) {
                    onTripLogged(
                        LoggedTrip(
                            mode           = selectedMode,
                            origin         = origin.trim(),
                            destination    = destination.trim(),
                            distanceKm     = distanceKm,
                            estimatedKgCO2 = estimatedCO2,
                            originLat      = originSelection?.lat,
                            originLng      = originSelection?.lng,
                            destLat        = destSelection?.lat,
                            destLng        = destSelection?.lng,
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
    isLoading: Boolean = false,
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
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 1.5.dp,
                color = colors.textSecondary,
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
    val unit = LocalDistanceUnit.current

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
                text = distanceKm.formatDistance(unit),
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
    // Resolved coordinates — non-null when the user selected a place from autocomplete.
    val originLat: Double? = null,
    val originLng: Double? = null,
    val destLat: Double? = null,
    val destLng: Double? = null,
)

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

/** Google Distance Matrix travel mode for auto-calculating distance. Null skips auto-calc (e.g. flights have no road/transit route). */
private val TransportModeType.distanceMatrixMode: String?
    get() = when (this) {
        TransportModeType.DRIVING, TransportModeType.CAB,
        TransportModeType.TWO_WHEELER, TransportModeType.AUTO_RICKSHAW -> "driving"
        TransportModeType.CYCLING                                     -> "bicycling"
        TransportModeType.WALKING                                     -> "walking"
        TransportModeType.PUBLIC_TRANSIT, TransportModeType.BUS,
        TransportModeType.METRO, TransportModeType.TRAIN               -> "transit"
        TransportModeType.FLIGHT                                      -> null
    }

// ── Formatting ────────────────────────────────────────────────────────────────

private fun Float.toDisplayString(): String {
    if (this == 0f) return "0"
    if (this % 1f == 0f) return toInt().toString()
    val intPart = toInt()
    val dec = ((this - intPart) * 10).toInt()
    return "$intPart.$dec"
}
