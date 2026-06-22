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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
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
import dev.atmos.shared.util.toDisplayString
import dev.atmos.shared.ui.home.components.AtmosBottomBar
import dev.atmos.shared.ui.home.components.AtmosTab
import dev.atmos.shared.network.ActivityService
import dev.atmos.shared.ui.profile.components.AccountCard
import dev.atmos.shared.ui.profile.components.CommuteCard
import dev.atmos.shared.ui.profile.components.DailyGoalCard
import dev.atmos.shared.ui.profile.components.ExportDataSheet
import dev.atmos.shared.ui.profile.components.GmailCard
import dev.atmos.shared.ui.profile.components.MyImpactCard
import dev.atmos.shared.ui.profile.components.PreferencesCard
import dev.atmos.shared.ui.profile.components.ProfileHeaderCard
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.AvatarBg
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
    onNavigateToActivities: () -> Unit = {},
    onAppearanceChange: (AppearanceMode) -> Unit = {},
    onNotificationsToggle: (Boolean, onError: (String) -> Unit) -> Unit = { _, _ -> },
    onWeeklyReportToggle: (Boolean, onError: (String) -> Unit) -> Unit = { _, _ -> },
    onDataSharingToggle: (Boolean, onError: (String) -> Unit) -> Unit = { _, _ -> },
    onGoalChange: (Float) -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onSaveName: (name: String, onSuccess: () -> Unit, onError: () -> Unit) -> Unit = { _, _, _ -> },
    onSignOut: () -> Unit = {},
    onDeleteAccount: (confirmation: String, onError: (String) -> Unit) -> Unit = { _, _ -> },
    onFabClick: () -> Unit = {},
    onHomeChange: (name: String, lat: Double?, lng: Double?, onError: (String) -> Unit) -> Unit = { _, _, _, _ -> },
    onWorkChange: (name: String, lat: Double?, lng: Double?, onError: (String) -> Unit) -> Unit = { _, _, _, _ -> },
    onTransportChange: (String, onError: (String) -> Unit) -> Unit = { _, _ -> },
    onUnitsChange: (String, onError: (String) -> Unit) -> Unit = { _, _ -> },
    gmailIsLoading: Boolean = false,
    onGmailConnect: () -> Unit = {},
    onGmailDisconnect: (onError: (String) -> Unit) -> Unit = { _ -> },
) {
    val colors = LocalAtmosColors.current
    var selectedTab by remember { mutableStateOf(AtmosTab.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ── Local preference state (doesn't need to escape to AtmosApp) ──────────
    var homeAddress       by remember { mutableStateOf(state.home.address ?: "") }
    var homeAddressLat    by remember { mutableStateOf(state.home.lat) }
    var homeAddressLng    by remember { mutableStateOf(state.home.lng) }
    var workAddress       by remember { mutableStateOf(state.work.address ?: "") }
    var workAddressLat    by remember { mutableStateOf(state.work.lat) }
    var workAddressLng    by remember { mutableStateOf(state.work.lng) }
    var selectedTransport by remember(state.preferences.defaultTransportLabel) {
        mutableStateOf(
            profileTransportOptions
                .firstOrNull { it.label.equals(state.preferences.defaultTransportLabel, ignoreCase = true) }
                ?.mode ?: TransportModeType.PUBLIC_TRANSIT,
        )
    }
    var selectedUnits by remember(state.preferences.unitsLabel) {
        mutableStateOf(
            if (state.preferences.unitsLabel.startsWith("Metric", ignoreCase = true)) "Metric (km)"
            else "Imperial (mi)",
        )
    }

    // ── Sheet / dialog visibility ─────────────────────────────────────────────
    var editingCommute     by remember { mutableStateOf<String?>(null) } // "home" | "work"
    var showTransportSheet by remember { mutableStateOf(false) }
    var showUnitsDialog    by remember { mutableStateOf(false) }
    var showEditProfile    by remember { mutableStateOf(false) }
    var showGoalDialog     by remember { mutableStateOf(false) }
    var showExportSheet    by remember { mutableStateOf(false) }
    val activityService    = remember { ActivityService() }

    // Local profile state (updated on save)
    var localDisplayName by remember { mutableStateOf(state.displayName) }
    val localInitials = remember(localDisplayName) {
        localDisplayName.toInitials().ifEmpty { state.initials }
    }

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
                onTabSelected = { tab ->
                    selectedTab = tab
                    when (tab) {
                        AtmosTab.HOME       -> onNavigateToHome()
                        AtmosTab.ACTIVITIES -> onNavigateToActivities()
                    }
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
                        displayName   = localDisplayName,
                        initials      = localInitials,
                        email         = state.email,
                        avatarUrl     = state.avatarUrl,
                        onBack        = onBack,
                        onEdit        = { showEditProfile = true },
                        onAvatarClick = onAvatarClick,
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
                        onEditGoal     = { showGoalDialog = true },
                    )
                }

                item {
                    CommuteCard(
                        home       = state.home.copy(address = homeAddress.takeIf { it.isNotBlank() }, lat = homeAddressLat, lng = homeAddressLng),
                        work       = state.work.copy(address = workAddress.takeIf { it.isNotBlank() }, lat = workAddressLat, lng = workAddressLng),
                        onEditHome = { editingCommute = "home" },
                        onEditWork = { editingCommute = "work" },
                    )
                }

                item {
                    PreferencesCard(
                        preferences           = effectivePreferences,
                        onNotificationsToggle = { enabled ->
                            onNotificationsToggle(enabled) { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        onWeeklyReportToggle  = { enabled ->
                            onWeeklyReportToggle(enabled) { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        onDataSharingToggle   = { enabled ->
                            onDataSharingToggle(enabled) { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                        onAppearanceChange    = onAppearanceChange,
                        onTransportClick      = { showTransportSheet = true },
                        onUnitsClick          = { showUnitsDialog = true },
                    )
                }

                item {
                    GmailCard(
                        connected      = state.gmailConnected,
                        connectedEmail = state.gmailEmail,
                        isLoading      = gmailIsLoading,
                        onConnectClick = onGmailConnect,
                        onDisconnectClick = {
                            onGmailDisconnect { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        },
                    )
                }

                item {
                    AccountCard(
                        onExportData    = { showExportSheet = true },
                        onSignOut       = onSignOut,
                        onDeleteAccount = { confirmation, onError -> onDeleteAccount(confirmation, onError) },
                    )
                }
        }
    }

    // ── Export data bottom sheet ──────────────────────────────────────────────
    if (showExportSheet) {
        ExportDataSheet(
            activityService = activityService,
            onDismiss       = { showExportSheet = false },
        )
    }

    // ── Commute edit bottom sheet ─────────────────────────────────────────────
    val isEditingHome = editingCommute == "home"
    if (editingCommute != null) {
        CommuteEditSheet(
            title        = if (isEditingHome) "Edit home address" else "Edit work address",
            currentValue = if (isEditingHome) homeAddress else workAddress,
            onSave       = { address, lat, lng ->
                if (isEditingHome) {
                    val prev    = homeAddress
                    val prevLat = homeAddressLat
                    val prevLng = homeAddressLng
                    homeAddress    = address
                    homeAddressLat = lat
                    homeAddressLng = lng
                    onHomeChange(address, lat, lng) { msg ->
                        homeAddress    = prev
                        homeAddressLat = prevLat
                        homeAddressLng = prevLng
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                } else {
                    val prev    = workAddress
                    val prevLat = workAddressLat
                    val prevLng = workAddressLng
                    workAddress    = address
                    workAddressLat = lat
                    workAddressLng = lng
                    onWorkChange(address, lat, lng) { msg ->
                        workAddress    = prev
                        workAddressLat = prevLat
                        workAddressLng = prevLng
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                }
                editingCommute = null
            },
            onDismiss    = { editingCommute = null },
        )
    }

    // ── Default transport bottom sheet ────────────────────────────────────────
    if (showTransportSheet) {
        TransportSelectionSheet(
            current   = selectedTransport,
            onSelect  = { mode ->
                val prevMode = selectedTransport
                selectedTransport = mode
                val label = profileTransportOptions.firstOrNull { it.mode == mode }?.label ?: mode.name
                onTransportChange(label) { msg ->
                    selectedTransport = prevMode
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
                showTransportSheet = false
            },
            onDismiss = { showTransportSheet = false },
        )
    }

    // ── Units dialog ──────────────────────────────────────────────────────────
    if (showUnitsDialog) {
        UnitsDialog(
            current   = selectedUnits,
            onSelect  = { units ->
                val prev = selectedUnits
                selectedUnits = units
                onUnitsChange(units) { msg ->
                    selectedUnits = prev
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
                showUnitsDialog = false
            },
            onDismiss = { showUnitsDialog = false },
        )
    }

    // ── Goal edit dialog ──────────────────────────────────────────────────────
    if (showGoalDialog) {
        GoalEditDialog(
            current   = state.dailyGoalKgCO2,
            onSelect  = { goal -> onGoalChange(goal); showGoalDialog = false },
            onDismiss = { showGoalDialog = false },
        )
    }

    // ── Edit profile sheet ────────────────────────────────────────────────────
    if (showEditProfile) {
        EditProfileSheet(
            displayName   = localDisplayName,
            initials      = localInitials,
            email         = state.email,
            onSave        = { name ->
                localDisplayName = name
                showEditProfile  = false
                onSaveName(
                    name,
                    { scope.launch { snackbarHostState.showSnackbar("Profile updated") } },
                    { scope.launch { snackbarHostState.showSnackbar("Update failed. Try again.") } },
                )
            },
            onDismiss     = { showEditProfile = false },
            onChangePhoto = { showEditProfile = false; onAvatarClick() },
        )
    }
}

// ── Commute edit sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommuteEditSheet(
    title: String,
    currentValue: String,
    onSave: (name: String, lat: Double?, lng: Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAtmosColors.current
    var text      by remember { mutableStateOf(currentValue) }
    var selection by remember { mutableStateOf<dev.atmos.shared.ui.logactivity.PlaceSelection?>(null) }

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

            dev.atmos.shared.ui.logactivity.PlaceAutocompleteField(
                value           = text,
                onValueChange   = { text = it },
                onPlaceSelected = { selection = it },
                placeholder     = "Search for a place",
                leadingIcon     = Icons.Outlined.LocationOn,
            )

            if (text.isNotBlank() && text != currentValue && selection == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = "Select a suggestion from the list to save",
                    style = TextStyle(fontSize = 12.sp, color = colors.textSecondary),
                )
            }

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
                    onClick  = { onSave(text.trim(), selection?.lat, selection?.lng) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    enabled  = text.isNotBlank() && (selection != null || text == currentValue),
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

// ── Goal edit dialog ──────────────────────────────────────────────────────────

@Composable
private fun GoalEditDialog(
    current: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors  = LocalAtmosColors.current
    val options = listOf(2f, 3f, 5f, 7f, 10f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Daily CO₂ goal", color = colors.textPrimary) },
        text             = {
            Column {
                options.forEach { goal ->
                    val isSelected = goal == current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(goal) }
                            .padding(vertical = 12.dp),
                    ) {
                        Text(
                            text     = "${goal.toDisplayString()} kg CO₂ / day",
                            style    = TextStyle(
                                fontSize   = 16.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (isSelected) HorizonBlue else colors.textPrimary,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Text("✓", color = HorizonBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton    = {},
        dismissButton    = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
        containerColor   = colors.surface,
    )
}

// ── Edit profile sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    displayName: String,
    initials: String,
    email: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    onChangePhoto: () -> Unit = {},
) {
    val colors = LocalAtmosColors.current
    var name      by remember { mutableStateOf(displayName) }
    var hasFocus  by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    // Derive live initials as the user edits the name field
    val liveInitials = name.toInitials().ifEmpty { initials }

    val borderColor by animateColorAsState(
        targetValue = when {
            showError && name.isBlank() -> AlertRed
            hasFocus                    -> HorizonBlue
            else                        -> colors.divider
        },
        animationSpec = tween(200),
        label         = "editProfileBorder",
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Sheet title ───────────────────────────────────────────────────
            Text(
                text     = "Edit profile",
                style    = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))

            // ── Avatar with camera badge ──────────────────────────────────────
            Box(modifier = Modifier.clickable(onClick = onChangePhoto).padding(4.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(80.dp)
                        .background(color = AvatarBg, shape = CircleShape),
                ) {
                    Text(
                        text       = liveInitials,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .align(Alignment.BottomEnd)
                        .size(26.dp)
                        .background(HorizonBlue, CircleShape)
                        .border(2.dp, colors.surface, CircleShape),
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.CameraAlt,
                        contentDescription = "Change photo",
                        tint               = Color.White,
                        modifier           = Modifier.size(13.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text       = "Change photo",
                fontSize   = 12.sp,
                color      = HorizonBlue,
                fontWeight = FontWeight.Medium,
                modifier   = Modifier.clickable(onClick = onChangePhoto),
            )

            Spacer(Modifier.height(24.dp))

            // ── Display name field ────────────────────────────────────────────
            Text(
                text     = "Display name",
                style    = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value         = name,
                onValueChange = { name = it; if (it.isNotBlank()) showError = false },
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
                            .border(
                                width = if (hasFocus || (showError && name.isBlank())) 2.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Person,
                            contentDescription = null,
                            tint               = if (hasFocus) HorizonBlue else colors.textTertiary,
                            modifier           = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) {
                            if (name.isEmpty()) {
                                Text(
                                    text  = "Your display name",
                                    style = TextStyle(fontSize = 15.sp, color = colors.textTertiary),
                                )
                            }
                            innerField()
                        }
                    }
                },
            )
            if (showError && name.isBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text     = "Name cannot be empty",
                    fontSize = 12.sp,
                    color    = AlertRed,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Read-only email field ─────────────────────────────────────────
            Text(
                text     = "Email",
                style    = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.chipBg)
                    .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint               = colors.textTertiary,
                    modifier           = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text  = email,
                    style = TextStyle(fontSize = 15.sp, color = colors.textTertiary),
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Save button ───────────────────────────────────────────────────
            Button(
                onClick  = {
                    if (name.isBlank()) showError = true
                    else onSave(name.trim())
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = HorizonBlue),
            ) {
                Text(
                    text       = "Save changes",
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Cancel ────────────────────────────────────────────────────────
            TextButton(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Text(
                    text       = "Cancel",
                    color      = colors.textSecondary,
                    fontWeight = FontWeight.Medium,
                )
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
        confirmButton    = {},
        dismissButton    = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
        containerColor   = colors.surface,
    )
}
