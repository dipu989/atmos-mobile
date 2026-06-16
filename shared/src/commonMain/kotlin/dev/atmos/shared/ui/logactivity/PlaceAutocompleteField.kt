package dev.atmos.shared.ui.logactivity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.network.PlaceResult
import dev.atmos.shared.network.PlaceSearchService
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import kotlinx.coroutines.delay

private val placeSearchService = PlaceSearchService()

// ── PlaceSelection ────────────────────────────────────────────────────────────

/** Holds the display name and resolved coordinates for a user-selected place. */
data class PlaceSelection(
    val name: String,
    val lat: Double,
    val lng: Double,
)

// ── PlaceAutocompleteField ────────────────────────────────────────────────────

/**
 * A text field that queries the backend's /places/autocomplete endpoint after
 * a 400 ms debounce and shows a dropdown of up to 5 suggestions.
 *
 * When the user taps a suggestion the [onPlaceSelected] callback fires with
 * the place name and its lat/lng so callers can capture coordinates for dedup.
 * Clearing the field back to blank calls [onPlaceSelected] with null.
 */
@Composable
fun PlaceAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    onPlaceSelected: (PlaceSelection?) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val colors = LocalAtmosColors.current
    var isFocused by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    // Track whether the current text was typed by the user (needs a search)
    // vs set by a selection (search not needed).
    var skipNextSearch by remember { mutableStateOf(false) }

    // Debounced search — fires 400 ms after the user stops typing.
    LaunchedEffect(value) {
        if (skipNextSearch) {
            skipNextSearch = false
            return@LaunchedEffect
        }
        if (value.length < 3) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(400)
        isSearching = true
        placeSearchService.search(value)
            .onSuccess { results -> suggestions = results }
            .onFailure { suggestions = emptyList() }
        isSearching = false
    }

    val borderColor = when {
        isError   -> AlertRed
        isFocused -> HorizonBlue
        else      -> colors.divider
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Text input ────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
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
                    onValueChange = { typed ->
                        onValueChange(typed)
                        // User is typing — clear any previously selected coords.
                        onPlaceSelected(null)
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Normal,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    cursorBrush = SolidColor(HorizonBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { state ->
                            isFocused = state.isFocused
                            if (!state.isFocused) suggestions = emptyList()
                        },
                )
            }
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp,
                    color = colors.textSecondary,
                )
            }
        }

        // ── Suggestions dropdown ──────────────────────────────────────────────
        if (isFocused && suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(colors.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, colors.divider, RoundedCornerShape(12.dp)),
            ) {
                suggestions.forEachIndexed { index, place ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                skipNextSearch = true
                                onValueChange(place.name)
                                onPlaceSelected(PlaceSelection(place.name, place.lat, place.lng))
                                suggestions = emptyList()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = place.name,
                            fontSize = 14.sp,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (index < suggestions.lastIndex) {
                        HorizontalDivider(
                            color = colors.divider,
                            modifier = Modifier.padding(horizontal = 14.dp),
                        )
                    }
                }
            }
        }
    }
}
