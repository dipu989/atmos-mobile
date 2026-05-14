package dev.atmos.shared.ui.profile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.ModeNight
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import dev.atmos.shared.ui.profile.AppearanceMode
import dev.atmos.shared.ui.profile.ProfilePreferences
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.TextPrimary
import dev.atmos.shared.ui.theme.TextSecondary

@Composable
fun PreferencesCard(
    preferences: ProfilePreferences,
    modifier: Modifier = Modifier,
) {
    AtmosCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 20.dp,
    ) {
        Text(
            text = "Preferences",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )

        Spacer(Modifier.height(12.dp))

        // Push Notifications
        PreferenceToggleRow(
            icon = Icons.Outlined.NotificationsNone,
            label = "Push Notifications",
            checked = preferences.pushNotificationsEnabled,
            onCheckedChange = {},
        )

        RowDivider()

        // Appearance
        PreferenceNavRow(
            icon = Icons.Outlined.ModeNight,
            label = "Appearance",
            value = preferences.appearanceMode.displayLabel,
        )

        RowDivider()

        // Default Transport
        PreferenceNavRow(
            icon = Icons.Outlined.DirectionsCar,
            label = "Default Transport",
            value = preferences.defaultTransportLabel,
        )

        RowDivider()

        // Units
        PreferenceNavRow(
            icon = Icons.Outlined.Language,
            label = "Units",
            value = preferences.unitsLabel,
        )
    }
}

// ── Row variants ──────────────────────────────────────────────────────────────

@Composable
private fun PreferenceToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = HorizonBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCDD5DE),
            ),
        )
    }
}

@Composable
private fun PreferenceNavRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = Color(0xFFF0F2F5),
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private val AppearanceMode.displayLabel: String
    get() = when (this) {
        AppearanceMode.LIGHT  -> "Light"
        AppearanceMode.DARK   -> "Dark"
        AppearanceMode.SYSTEM -> "System"
    }
