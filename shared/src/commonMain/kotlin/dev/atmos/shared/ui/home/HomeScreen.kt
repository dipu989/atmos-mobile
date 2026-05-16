package dev.atmos.shared.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import dev.atmos.shared.ui.home.components.AtmosHeader
import dev.atmos.shared.ui.home.components.AtmosTab
import dev.atmos.shared.ui.home.components.InsightsSection
import dev.atmos.shared.ui.home.components.PendingTripCard
import dev.atmos.shared.ui.home.components.RecentActivityCard
import dev.atmos.shared.ui.home.components.TodayImpactCard
import dev.atmos.shared.ui.home.components.TransportBreakdownCard
import dev.atmos.shared.ui.home.components.WeeklyTrendCard
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    state: HomeUiState = previewHomeUiState,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onFabClick: () -> Unit = {},
    onEditPendingTrip: (PendingTripEntry) -> Unit = {},
) {
    val colors = LocalAtmosColors.current
    var selectedTab by remember { mutableStateOf(AtmosTab.HOME) }
    var pendingTrip by remember { mutableStateOf(state.pendingTrip) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                unreadInsights = state.unreadInsightsCount,
                onTabSelected = { tab ->
                    selectedTab = tab
                    if (tab == AtmosTab.INSIGHTS) onNavigateToInsights()
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
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AtmosHeader(
                    greeting = state.greeting,
                    dateLabel = state.dateLabel,
                    user = state.user,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    onAvatarClick = onNavigateToProfile,
                )
            }

            // Pending trip confirmation — shown at the top when auto-detection fires
            pendingTrip?.let { trip ->
                item {
                    PendingTripCard(
                        trip = trip,
                        onConfirm = {
                            pendingTrip = null
                            scope.launch { snackbarHostState.showSnackbar("Trip confirmed — nice work!") }
                        },
                        onEdit = { onEditPendingTrip(trip) },
                        onDismiss = { pendingTrip = null },
                    )
                }
            }

            item { TodayImpactCard(impact = state.todayImpact) }
            item { WeeklyTrendCard(data = state.weeklyTrend) }
            item { TransportBreakdownCard(entries = state.transportBreakdown) }
            item { RecentActivityCard(entries = state.recentActivity) }

            item {
                InsightsSection(
                    entries = state.insights,
                    unreadCount = state.unreadInsightsCount,
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
