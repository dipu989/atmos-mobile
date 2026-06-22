package dev.atmos.shared.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.home.components.AtmosBottomBar
import dev.atmos.shared.ui.home.components.AtmosHeader
import dev.atmos.shared.ui.home.components.AtmosTab
import dev.atmos.shared.ui.home.components.InsightsSection
import dev.atmos.shared.ui.home.components.OngoingTripCard
import dev.atmos.shared.ui.home.components.PendingTripCard
import dev.atmos.shared.ui.home.components.RecentActivityCard
import dev.atmos.shared.ui.home.components.TodayImpactCard
import dev.atmos.shared.ui.home.components.TransportBreakdownCard
import dev.atmos.shared.ui.home.components.WeeklyTrendCard
import dev.atmos.shared.ui.common.InsightsSkeleton
import dev.atmos.shared.ui.common.RecentActivitySkeleton
import dev.atmos.shared.ui.common.TodayImpactSkeleton
import dev.atmos.shared.ui.common.TransportBreakdownSkeleton
import dev.atmos.shared.ui.common.WeeklyTrendSkeleton
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    state: HomeUiState = previewHomeUiState,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToActivities: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onRetry: () -> Unit = {},
    onFabClick: () -> Unit = {},
    onConfirmPendingSession: () -> Unit = {},
    onDismissPendingSession: () -> Unit = {},
    onEditPendingSession: () -> Unit = {},
    onTripClick: (RecentActivityEntry) -> Unit = {},
    onInsightClick: (InsightEntry) -> Unit = {},
    onStopAndSave: () -> Unit = {},
    onDiscard: () -> Unit = {},
    onResume: () -> Unit = {},
    onTrendPeriodChange: (HomeTrendPeriod) -> Unit = {},
) {
    val colors = LocalAtmosColors.current
    var selectedTab by remember { mutableStateOf(AtmosTab.HOME) }
    var pendingSession by remember { mutableStateOf(state.pendingSession) }
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
                onTabSelected = { tab ->
                    selectedTab = tab
                    if (tab == AtmosTab.ACTIVITIES) onNavigateToActivities()
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

            // Ongoing trip — shown while a session is actively being tracked
            state.ongoingSession?.let { session ->
                item {
                    OngoingTripCard(
                        state         = session,
                        onStopAndSave = onStopAndSave,
                        onDiscard     = onDiscard,
                        onResume      = onResume,
                    )
                }
            }

            // Pending trip confirmation — shown when detector auto-ends a session
            if (state.ongoingSession == null) pendingSession?.let { session ->
                item {
                    PendingTripCard(
                        session   = session,
                        onConfirm = {
                            pendingSession = null
                            onConfirmPendingSession()
                            scope.launch { snackbarHostState.showSnackbar("Trip confirmed — nice work!") }
                        },
                        onEdit    = {
                            onEditPendingSession()
                        },
                        onDismiss = {
                            pendingSession = null
                            onDismissPendingSession()
                        },
                    )
                }
            }

            if (state.isError) {
                // ── Error state ───────────────────────────────────────────────
                item { HomeErrorState(onRetry = onRetry) }
            } else if (state.isLoading) {
                // ── Skeleton loading state ────────────────────────────────────
                item { TodayImpactSkeleton() }
                item { WeeklyTrendSkeleton() }
                item { TransportBreakdownSkeleton() }
                item { RecentActivitySkeleton() }
                item { InsightsSkeleton() }
            } else if (state.recentActivity.isEmpty() && pendingSession == null) {
                // ── First-run empty state ─────────────────────────────────────
                item { HomeEmptyState(onLogTrip = onFabClick) }
            } else {
                // ── Populated state ───────────────────────────────────────────
                item { TodayImpactCard(impact = state.todayImpact) }
                item {
                    WeeklyTrendCard(
                        data = state.weeklyTrend,
                        period = state.trendPeriod,
                        onPeriodChange = onTrendPeriodChange,
                        onViewStats = onNavigateToStats,
                    )
                }
                item {
                    TransportBreakdownCard(
                        entries = state.transportBreakdown,
                        period = state.trendPeriod,
                        onLogTrip = onFabClick,
                    )
                }
                item {
                    RecentActivityCard(
                        entries = state.recentActivity,
                        onTripClick = onTripClick,
                        onLogTrip = onFabClick,
                    )
                }
                item {
                    InsightsSection(
                        entries        = state.insights,
                        unreadCount    = state.unreadInsightsCount,
                        onInsightClick = onInsightClick,
                        onViewAll      = onNavigateToInsights,
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ── First-run empty state ─────────────────────────────────────────────────────

@Composable
private fun HomeEmptyState(onLogTrip: () -> Unit) {
    val colors = LocalAtmosColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Atmospheric background — mirrors the Trip Detail hero language
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color  = HorizonBlue.copy(alpha = 0.07f),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.88f, size.height * 0.12f),
            )
            drawCircle(
                color  = HorizonBlue.copy(alpha = 0.05f),
                radius = size.width * 0.40f,
                center = Offset(size.width * 0.08f, size.height * 0.85f),
            )
            drawCircle(
                color  = HorizonBlue.copy(alpha = 0.04f),
                radius = size.width * 0.20f,
                center = Offset(size.width * 0.75f, size.height * 0.75f),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(HorizonBlue.copy(alpha = 0.12f)),
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Eco,
                    contentDescription = null,
                    tint               = HorizonBlue,
                    modifier           = Modifier.size(32.dp),
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text       = "Your journey starts here",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = colors.textPrimary,
                textAlign  = TextAlign.Center,
            )

            Text(
                text       = "Log your first trip to track your carbon footprint and unlock personalised insights",
                fontSize   = 14.sp,
                color      = colors.textSecondary,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = onLogTrip,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = HorizonBlue),
            ) {
                Text(
                    text       = "Log a trip",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(vertical = 6.dp),
                )
            }
        }
    }
}

// ── Error state ───────────────────────────────────────────────────────────────

@Composable
private fun HomeErrorState(onRetry: () -> Unit) {
    val colors = LocalAtmosColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color  = AlertRed.copy(alpha = 0.05f),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.88f, size.height * 0.12f),
            )
            drawCircle(
                color  = AlertRed.copy(alpha = 0.04f),
                radius = size.width * 0.40f,
                center = Offset(size.width * 0.08f, size.height * 0.85f),
            )
            drawCircle(
                color  = AlertRed.copy(alpha = 0.03f),
                radius = size.width * 0.20f,
                center = Offset(size.width * 0.75f, size.height * 0.75f),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(AlertRed.copy(alpha = 0.10f)),
            ) {
                Icon(
                    imageVector        = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint               = AlertRed,
                    modifier           = Modifier.size(32.dp),
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text       = "Couldn't load your data",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = colors.textPrimary,
                textAlign  = TextAlign.Center,
            )

            Text(
                text       = "Check your connection and try again",
                fontSize   = 14.sp,
                color      = colors.textSecondary,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick  = onRetry,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                border   = androidx.compose.foundation.BorderStroke(1.5.dp, HorizonBlue),
            ) {
                Text(
                    text       = "Try again",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = HorizonBlue,
                    modifier   = Modifier.padding(vertical = 6.dp),
                )
            }
        }
    }
}
