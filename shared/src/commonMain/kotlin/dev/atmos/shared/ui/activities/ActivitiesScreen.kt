package dev.atmos.shared.ui.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
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
import dev.atmos.shared.ui.common.AtmosCard
import dev.atmos.shared.ui.home.RecentActivityEntry
import dev.atmos.shared.ui.home.components.ActivityRow
import dev.atmos.shared.ui.home.components.AtmosBottomBar
import dev.atmos.shared.ui.home.components.AtmosTab
import dev.atmos.shared.ui.home.previewAllActivities
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors

@Composable
fun ActivitiesScreen(
    groupedEntries: List<Pair<String, List<RecentActivityEntry>>> = previewAllActivities,
    onNavigateToHome: () -> Unit = {},
    onTripClick: (RecentActivityEntry) -> Unit = {},
    onFabClick: () -> Unit = {},
) {
    val colors = LocalAtmosColors.current
    var selectedTab by remember { mutableStateOf(AtmosTab.ACTIVITIES) }

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            AtmosBottomBar(
                selectedTab = selectedTab,
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
            // ── Screen header ─────────────────────────────────────────────────
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Activities",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    val total = groupedEntries.sumOf { it.second.size }
                    if (total > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "· $total",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = HorizonBlue,
                        )
                    }
                }
            }

            // ── Date groups ───────────────────────────────────────────────────
            groupedEntries.forEach { (dateLabel, entries) ->
                item(key = "header_$dateLabel") {
                    DateSectionHeader(label = dateLabel, entries = entries)
                }

                item(key = "card_$dateLabel") {
                    AtmosCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = 4.dp,
                    ) {
                        entries.forEachIndexed { index, entry ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                ActivityRow(
                                    entry = entry,
                                    onClick = { onTripClick(entry) },
                                )
                            }
                            if (index < entries.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 1.dp,
                                    color = colors.divider,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Date section header ────────────────────────────────────────────────────────

@Composable
private fun DateSectionHeader(
    label: String,
    entries: List<RecentActivityEntry>,
) {
    val colors = LocalAtmosColors.current
    val totalKg = entries.sumOf { it.kgCO2.toDouble() }.toFloat()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${totalKg.toKgString()} kg CO₂",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textTertiary,
        )
    }
}

private fun Float.toKgString(): String {
    if (this == 0f) return "0"
    if (this >= 10f) return toInt().toString()
    if (this % 1f == 0f) return toInt().toString()
    val intPart = toInt()
    return "$intPart.${((this - intPart) * 10).toInt()}"
}
