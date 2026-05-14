package dev.atmos.shared.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.atmos.shared.ui.profile.components.CommuteCard
import dev.atmos.shared.ui.profile.components.DailyGoalCard
import dev.atmos.shared.ui.profile.components.MyImpactCard
import dev.atmos.shared.ui.profile.components.ProfileHeaderCard
import dev.atmos.shared.ui.theme.SkyWhite

@Composable
fun ProfileScreen(
    state: ProfileUiState = previewProfileUiState,
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {},
) {
    Scaffold(containerColor = SkyWhite) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SkyWhite)
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ProfileHeaderCard(
                    displayName = state.displayName,
                    initials = state.initials,
                    email = state.email,
                    onBack = onBack,
                    onEdit = onEdit,
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
                )
            }
        }
    }
}
