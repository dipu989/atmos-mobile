package dev.atmos.shared.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.home.InsightEntry
import dev.atmos.shared.ui.home.components.AtmosBottomBar
import dev.atmos.shared.ui.home.components.AtmosTab
import dev.atmos.shared.ui.home.components.InsightCard
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors

@Composable
fun InsightsScreen(
    entries: List<InsightEntry>,
    onNavigateToHome: () -> Unit = {},
    onFabClick: () -> Unit = {},
) {
    val colors = LocalAtmosColors.current
    var selectedTab by remember { mutableStateOf(AtmosTab.INSIGHTS) }

    Scaffold(
        containerColor = colors.background,
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
                top = 24.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Insights",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    if (entries.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "· ${entries.size}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = HorizonBlue,
                        )
                    }
                }
            }

            items(entries) { entry ->
                InsightCard(entry = entry)
            }
        }
    }
}
