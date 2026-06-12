package dev.atmos.shared.ui.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.ModeNight
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.atmos.shared.ui.theme.LocalAtmosColors

@Composable
fun PreferencesCard(
    preferences: ProfilePreferences,
    onNotificationsToggle: (Boolean) -> Unit = {},
    onWeeklyReportToggle: (Boolean) -> Unit = {},
    onDataSharingToggle: (Boolean) -> Unit = {},
    onAppearanceChange: (AppearanceMode) -> Unit = {},
    onTransportClick: () -> Unit = {},
    onUnitsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalAtmosColors.current
    var showAppearanceDialog by remember { mutableStateOf(false) }

    AtmosCard(modifier = modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Text(
            text = "Preferences",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )

        Spacer(Modifier.height(12.dp))

        PreferenceToggleRow(
            icon = Icons.Outlined.NotificationsNone,
            label = "Push Notifications",
            checked = preferences.pushNotificationsEnabled,
            onCheckedChange = onNotificationsToggle,
        )

        RowDivider()

        PreferenceToggleRow(
            icon = Icons.Outlined.BarChart,
            label = "Weekly Report",
            checked = preferences.weeklyReportEnabled,
            onCheckedChange = onWeeklyReportToggle,
        )

        RowDivider()

        PreferenceToggleRow(
            icon = Icons.Outlined.Share,
            label = "Data Sharing",
            checked = preferences.dataSharingEnabled,
            onCheckedChange = onDataSharingToggle,
        )

        RowDivider()

        PreferenceNavRow(
            icon = Icons.Outlined.ModeNight,
            label = "Appearance",
            value = preferences.appearanceMode.displayLabel,
            onClick = { showAppearanceDialog = true },
        )

        RowDivider()

        PreferenceNavRow(
            icon = Icons.Outlined.DirectionsCar,
            label = "Default Transport",
            value = preferences.defaultTransportLabel,
            onClick = onTransportClick,
        )

        RowDivider()

        PreferenceNavRow(
            icon = Icons.Outlined.Language,
            label = "Units",
            value = preferences.unitsLabel,
            onClick = onUnitsClick,
        )
    }

    if (showAppearanceDialog) {
        AppearanceDialog(
            current = preferences.appearanceMode,
            onSelect = { mode ->
                showAppearanceDialog = false
                onAppearanceChange(mode)
            },
            onDismiss = { showAppearanceDialog = false },
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
    val colors = LocalAtmosColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = label, fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
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
    onClick: () -> Unit,
) {
    val colors = LocalAtmosColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = label, fontSize = 15.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 14.sp, color = colors.textSecondary)
        Spacer(Modifier.width(4.dp))
        Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(thickness = 1.dp, color = LocalAtmosColors.current.divider)
}

// ── Appearance dialog ─────────────────────────────────────────────────────────

@Composable
private fun AppearanceDialog(
    current: AppearanceMode,
    onSelect: (AppearanceMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAtmosColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appearance", color = colors.textPrimary) },
        text = {
            Column {
                AppearanceMode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 12.dp),
                    ) {
                        Text(
                            text = mode.displayLabel,
                            fontSize = 16.sp,
                            fontWeight = if (mode == current) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (mode == current) HorizonBlue else colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        if (mode == current) {
                            Text(text = "✓", color = HorizonBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
        containerColor = colors.surface,
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private val AppearanceMode.displayLabel: String
    get() = when (this) {
        AppearanceMode.LIGHT  -> "Light"
        AppearanceMode.DARK   -> "Dark"
        AppearanceMode.SYSTEM -> "System"
    }
