package dev.atmos.shared.ui.insights

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.common.ShimmerBox
import dev.atmos.shared.ui.home.InsightEntry
import dev.atmos.shared.ui.home.components.InsightCard
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    entries: List<InsightEntry>,
    selectedFilter: String = "Week",
    onFilterChange: (String) -> Unit = {},
    isLoading: Boolean = false,
    onBack: () -> Unit = {},
    onInsightClick: (InsightEntry) -> Unit = {},
) {
    val colors = LocalAtmosColors.current
    val filters = listOf("Week", "Month", "Year")

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Insights",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint               = colors.textSecondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(innerPadding),
        ) {
            // ── Time filter ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.chipBg),
            ) {
                filters.forEach { filter ->
                    val isSelected = filter == selectedFilter
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) HorizonBlue else Color.Transparent)
                            .clickable(enabled = !isLoading) { onFilterChange(filter) }
                            .padding(vertical = 10.dp),
                    ) {
                        Text(
                            text       = filter,
                            fontSize   = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color      = if (isSelected) Color.White else colors.textSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = colors.divider)

            if (isLoading) {
                InsightsLoadingSkeleton()
            } else {
                LazyColumn(
                    modifier        = Modifier.fillMaxSize(),
                    contentPadding  = PaddingValues(
                        start  = 16.dp,
                        end    = 16.dp,
                        top    = 16.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(entries) { entry ->
                        InsightCard(entry = entry, onClick = { onInsightClick(entry) })
                    }
                }
            }
        }
    }
}

// ── Loading skeleton ──────────────────────────────────────────────────────────

@Composable
private fun InsightsLoadingSkeleton() {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(5) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                shape    = RoundedCornerShape(16.dp),
            )
        }
    }
}
