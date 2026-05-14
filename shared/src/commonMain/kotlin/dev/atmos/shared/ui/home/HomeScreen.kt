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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.atmos.shared.ui.home.components.AtmosBottomBar
import dev.atmos.shared.ui.home.components.AtmosHeader
import dev.atmos.shared.ui.home.components.AtmosTab
import dev.atmos.shared.ui.home.components.InsightsSection
import dev.atmos.shared.ui.home.components.RecentActivityCard
import dev.atmos.shared.ui.home.components.TodayImpactCard
import dev.atmos.shared.ui.home.components.TransportBreakdownCard
import dev.atmos.shared.ui.home.components.WeeklyTrendCard
import dev.atmos.shared.ui.theme.SkyWhite

@Composable
fun HomeScreen(
    state: HomeUiState = previewHomeUiState,
    onNavigateToProfile: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(AtmosTab.HOME) }

    Scaffold(
        containerColor = SkyWhite,
        bottomBar = {
            AtmosBottomBar(
                selectedTab = selectedTab,
                unreadInsights = state.unreadInsightsCount,
                onTabSelected = { selectedTab = it },
                onFabClick = { /* navigate to log activity */ },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SkyWhite)
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

            item {
                TodayImpactCard(impact = state.todayImpact)
            }

            item {
                WeeklyTrendCard(data = state.weeklyTrend)
            }

            item {
                TransportBreakdownCard(entries = state.transportBreakdown)
            }

            item {
                RecentActivityCard(entries = state.recentActivity)
            }

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
