package dev.atmos.shared.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.atmos.shared.ui.home.components.AtmosBottomBar
import dev.atmos.shared.ui.home.components.AtmosTab
import dev.atmos.shared.ui.profile.components.AccountCard
import dev.atmos.shared.ui.profile.components.CommuteCard
import dev.atmos.shared.ui.profile.components.DailyGoalCard
import dev.atmos.shared.ui.profile.components.MyImpactCard
import dev.atmos.shared.ui.profile.components.PreferencesCard
import dev.atmos.shared.ui.profile.components.ProfileHeaderCard
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import kotlinx.coroutines.launch

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

    fun showComingSoon() {
        scope.launch { snackbarHostState.showSnackbar("Coming soon") }
    }

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
                    initials = state.initials,
                    email = state.email,
                    onBack = onBack,
                    onEdit = { showComingSoon() },
                )
            }

            item {
                MyImpactCard(
                    totalCO2SavedKg = state.totalCO2SavedKg,
                    daysTracked = state.daysTracked,
                )
            }

            item {
                DailyGoalCard(
                    todayKgCO2 = state.todayKgCO2,
                    dailyGoalKgCO2 = state.dailyGoalKgCO2,
                )
            }

            item {
                CommuteCard(
                    home = state.home,
                    work = state.work,
                    onEditHome = { showComingSoon() },
                    onEditWork = { showComingSoon() },
                )
            }

            item {
                PreferencesCard(
                    preferences = state.preferences,
                    onNotificationsToggle = onNotificationsToggle,
                    onAppearanceChange = onAppearanceChange,
                    onTransportClick = { showComingSoon() },
                    onUnitsClick = { showComingSoon() },
                )
            }

            item {
                AccountCard(
                    onExportData = { showComingSoon() },
                    onSignOut = onSignOut,
                    onDeleteAccount = onDeleteAccount,
                )
            }
        }
    }
}
