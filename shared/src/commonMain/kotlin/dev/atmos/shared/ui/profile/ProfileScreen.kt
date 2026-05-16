package dev.atmos.shared.ui.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.home.components.AtmosBottomBar
import dev.atmos.shared.ui.home.components.AtmosTab
import dev.atmos.shared.ui.profile.components.AccountCard
import dev.atmos.shared.ui.profile.components.CommuteCard
import dev.atmos.shared.ui.profile.components.DailyGoalCard
import dev.atmos.shared.ui.profile.components.MyImpactCard
import dev.atmos.shared.ui.profile.components.PreferencesCard
import dev.atmos.shared.ui.profile.components.ProfileHeaderCard
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Peach
import dev.atmos.shared.ui.theme.Sage
import kotlinx.coroutines.launch

// ── Transport options for the picker ─────────────────────────────────────────

private data class ProfileTransportOption(
    val mode: TransportModeType,
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color,
)

private val profileTransportOptions = listOf(
    ProfileTransportOption(TransportModeType.DRIVING,        "Car",           Icons.Outlined.DirectionsCar,               AlertRed,    Color(0xFFFFEEEC)),
    ProfileTransportOption(TransportModeType.CAB,            "Cab / Taxi",    Icons.Outlined.LocalTaxi,                   AlertRed,    Color(0xFFFFEEEC)),
    ProfileTransportOption(TransportModeType.TWO_WHEELER,    "2-Wheeler",     Icons.AutoMirrored.Outlined.DirectionsBike, Peach,       Color(0xFFFFF3EE)),
    ProfileTransportOption(TransportModeType.AUTO_RICKSHAW,  "Auto Rickshaw", Icons.AutoMirrored.Outlined.DirectionsBike, Peach,       Color(0xFFFFF3EE)),
    ProfileTransportOption(TransportModeType.PUBLIC_TRANSIT, "Bus",           Icons.Outlined.DirectionsBus,               HorizonBlue, Color(0xFFE8F2FA)),
    ProfileTransportOption(TransportModeType.TRAIN,          "Train",         Icons.Outlined.Train,                       HorizonBlue, Color(0xFFE8F2FA)),
    ProfileTransportOption(TransportModeType.METRO,          "Metro",         Icons.Outlined.Train,                       HorizonBlue, Color(0xFFE8F2FA)),
    ProfileTransportOption(TransportModeType.FLIGHT,         "Flight",        Icons.Outlined.Flight,                      AlertRed,    Color(0xFFFFEEEC)),
    ProfileTransportOption(TransportModeType.CYCLING,        "Bicycle",       Icons.AutoMirrored.Outlined.DirectionsBike, Sage,        Color(0xFFE8F7F0)),
    ProfileTransportOption(TransportModeType.WALKING,        "Walk",          Icons.AutoMirrored.Outlined.DirectionsWalk, Sage,        Color(0xFFE8F7F0)),
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState = previewProfileUiState,
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onAppearanceChange: (AppearanceMode) -> Unit = {},
    onNotificationsToggle: (Boolean) -> Unit = {},
    onSignOut: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onFabClick: () -> Unit = {},
) {
    val colors = LocalAtmosColors.current
    var selectedTab by remember { mutableStateOf(AtmosTab.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ── Local preference state (doesn't need to escape to AtmosApp) ──────────
    var homeAddress       by remember { mutableStateOf(state.home.address ?: "") }
    var workAddress       by remember { mutableStateOf(state.work.address ?: "") }
    var selectedTransport by remember {
        mutableStateOf(
            profileTransportOptions
                .firstOrNull { it.label.equals(state.preferences.defaultTransportLabel, ignoreCase = true) }
                ?.mode ?: TransportModeType.PUBLIC_TRANSIT,
        )
    }
    var selectedUnits by remember {
        mutableStateOf(
            if (state.preferences.unitsLabel.startsWith("Metric", ignoreCase = true)) "Metric (km)"
            else "Imperial (mi)",
        )
    }

    // ── Sheet / dialog visibility ─────────────────────────────────────────────
    var editingCommute     by remember { mutableStateOf<String?>(null) } // "home" | "work"
    var showTransportSheet by remember { mutableStateOf(false) }
    var showUnitsDialog    by remember { mutableStateOf(false) }

    fun showComingSoon() {
        scope.launch { snackbarHostState.showSnackbar("Coming soon") }
    }

    // Effective preferences passed to cards, incorporating local edits
    val effectivePreferences = state.preferences.copy(
        defaultTransportLabel = profileTransportOptions
            .firstOrNull { it.mode == selectedTransport }?.label
            ?: state.preferences.defaultTransportLabel,
        unitsLabel = selectedUnits,
    )

    Scaffold(
        containerColor = colors.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = colors.surface,
                    contentColor = colors.textPrimary,
                    actionColor = HorizonBlue,
                )
            }
        },
        bottomBar = {
            AtmosBottomBar(
                selectedTab = selectedTab,
                unreadInsights = 0,
                onTabSelected = { tab ->
                    selectedTab = tab
                    if (tab == AtmosTab.HOME) onNavigateToHome()
                },
                onFabClick = onFabClick,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ProfileHeaderCard(
                    displayName = state.displayName,
                    initials    = state.initials,
                    email       = state.email,
                    onBack      = onBack,
                    onEdit      = { showComingSoon() },
                )
            }

            item {
                MyImpactCard(
                    totalCO2SavedKg = state.totalCO2SavedKg,
                    daysTracked     = state.daysTracked,
                )
            }

            item {
                DailyGoalCard(
                    todayKgCO2     = state.todayKgCO2,
                    dailyGoalKgCO2 = state.dailyGoalKgCO2,
                )
            }

            item {
                CommuteCard(
                    home       = state.home.copy(address = homeAddress.takeIf { it.isNotBlank() }),
                    work       = state.work.copy(address = workAddress.takeIf { it.isNotBlank() }),
                    onEditHome = { editingCommute = "home" },
                    onEditWork = { editingCommute = "work" },
                )
            }

            item {
                PreferencesCard(
                    preferences           = effectivePreferences,
                    onNotificationsToggle = onNotificationsToggle,
                    onAppearanceChange    = onAppearanceChange,
                    onTransportClick      = { showTransportSheet = true },
                    onUnitsClick          = { showUnitsDialog = true },
                )
            }

            item {
                AccountCard(
                    onExportData    = { showComingSoon() },
                    onSignOut       = onSignOut,
                    onDeleteAccount = onDeleteAccount,
                )
            }
        }
    }

    // ── Commute edit bottom sheet ─────────────────────────────────────────────
    val isEditingHome = editingCommute == "home"
    if (editingCommute != null) {
        CommuteEditSheet(
            title        = if (isEditingHome) "Edit home address" else "Edit work address",
            currentValue = if (isEditingHome) homeAddress else workAddress,
            onSave       = { address ->
                if (isEditingHome) homeAddress = address else workAddress = address
                editingCommute = null
            },
            onDismiss    = { editingCommute = null },
        )
    }

    // ── Default transport bottom sheet ────────────────────────────────────────
    if (showTransportSheet) {
        TransportSelectionSheet(
            current   = selectedTransport,
            onSelect  = { mode -> selectedTransport = mode; showTransportSheet = false },
            onDismiss = { showTransportSheet = false },
        )
    }

    // ── Units dialog ──────────────────────────────────────────────────────────
    if (showUnitsDialog) {
        UnitsDialog(
            current   = selectedUnits,
            onSelect  = { units -> selectedUnits = units; showUnitsDialog = false },
            onDismiss = { showUnitsDialog = false },
        )
    }
}

// ── Commute edit sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommuteEditSheet(
    title: String,
    currentValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAtmosColors.current
    var text     by remember { mutableStateOf(currentValue) }
    var hasFocus by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue    = if (hasFocus) HorizonBlue else colors.divider,
        animationSpec  = tween(200),
        label          = "border",
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = colors.surface,
        tonalElevation   = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp),
        ) {
            Text(
                text  = title,
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary),
            )
            Spacer(Modifier.height(20.dp))

            BasicTextField(
                value         = text,
                onValueChange = { text = it },
                singleLine    = true,
                cursorBrush   = SolidColor(HorizonBlue),
                textStyle     = TextStyle(fontSize = 15.sp, color = colors.textPrimary),
                modifier      = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { hasFocus = it.isFocused },
                decorationBox = { innerField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.background)
                            .border(if (hasFocus) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector     = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint            = if (hasFocus) HorizonBlue else colors.textTertiary,
                            modifier        = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) {
                            if (text.isEmpty()) {
                                Text("Enter address", style = TextStyle(fontSize = 15.sp, color = colors.textTertiary))
                            }
                            innerField()
                        }
                    }
                },
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.dp, colors.divider),
                    colors   = ButtonDefaults.outlinedButtonColors(containerColor = colors.surface),
                ) {
                    Text("Cancel", color = colors.textSecondary, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick  = { onSave(text.trim()) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    enabled  = text.isNotBlank(),
                    colors   = ButtonDefaults.buttonColors(containerColor = HorizonBlue),
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Default transport sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransportSelectionSheet(
    current: TransportModeType,
    onSelect: (TransportModeType) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAtmosColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = colors.surface,
        tonalElevation   = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            Text(
                text     = "Default transport",
                style    = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
            HorizontalDivider(color = colors.divider)

            profileTransportOptions.forEach { option ->
                val isSelected = option.mode == current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option.mode) }
                        .background(if (isSelected) colors.insightBlueBg else colors.surface)
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(option.bg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = option.icon,
                            contentDescription = null,
                            tint               = option.tint,
                            modifier           = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text       = option.label,
                        style      = TextStyle(
                            fontSize   = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (isSelected) HorizonBlue else colors.textPrimary,
                        ),
                        modifier   = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Text("✓", color = HorizonBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                HorizontalDivider(color = colors.divider.copy(alpha = 0.5f))
            }
        }
    }
}

// ── Units dialog ──────────────────────────────────────────────────────────────

@Composable
private fun UnitsDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors  = LocalAtmosColors.current
    val options = listOf("Metric (km)", "Imperial (mi)")

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Units", color = colors.textPrimary) },
        text             = {
            Column {
                options.forEach { option ->
                    val isSelected = option == current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp),
                    ) {
                        Text(
                            text       = option,
                            style      = TextStyle(
                                fontSize   = 16.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (isSelected) HorizonBlue else colors.textPrimary,
                            ),
                            modifier   = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Text("✓", color = HorizonBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton    = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
        containerColor   = colors.surface,
    )
}
