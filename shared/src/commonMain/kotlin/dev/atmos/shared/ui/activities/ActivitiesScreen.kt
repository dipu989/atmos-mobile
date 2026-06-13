package dev.atmos.shared.ui.activities

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.common.AtmosCard
import dev.atmos.shared.ui.home.RecentActivityEntry
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.home.components.ActivityRow
import dev.atmos.shared.ui.home.components.AtmosBottomBar
import dev.atmos.shared.ui.home.components.AtmosTab
import dev.atmos.shared.ui.home.previewAllActivities
import dev.atmos.shared.ui.logactivity.displayLabel
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors

@Composable
fun ActivitiesScreen(
    groupedEntries: List<Pair<String, List<RecentActivityEntry>>> = previewAllActivities,
    onNavigateToHome: () -> Unit = {},
    onTripClick: (RecentActivityEntry) -> Unit = {},
    onDelete: (RecentActivityEntry) -> Unit = {},
    onFabClick: () -> Unit = {},
) {
    val colors = LocalAtmosColors.current
    var selectedTab by remember { mutableStateOf(AtmosTab.ACTIVITIES) }
    var selectedMode by remember { mutableStateOf<TransportModeType?>(null) }

    val availableModes = remember(groupedEntries) {
        groupedEntries.flatMap { it.second }.map { it.mode }.toSet()
            .sortedBy { it.ordinal }
    }
    val filteredEntries = remember(groupedEntries, selectedMode) {
        val mode = selectedMode ?: return@remember groupedEntries
        groupedEntries.mapNotNull { (date, entries) ->
            entries.filter { it.mode == mode }.takeIf { it.isNotEmpty() }?.let { date to it }
        }
    }

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
                    val total = filteredEntries.sumOf { it.second.size }
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

            // ── Mode filter chips ─────────────────────────────────────────────
            if (availableModes.size > 1) {
                item(key = "filter_chips") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ModeFilterChip(
                            label    = "All",
                            selected = selectedMode == null,
                            onClick  = { selectedMode = null },
                        )
                        availableModes.forEach { mode ->
                            ModeFilterChip(
                                label    = mode.displayLabel,
                                selected = selectedMode == mode,
                                onClick  = { selectedMode = if (selectedMode == mode) null else mode },
                            )
                        }
                    }
                }
            }

            // ── Date groups or empty state ────────────────────────────────────
            if (filteredEntries.isEmpty()) {
                item {
                    if (selectedMode != null) {
                        FilterEmptyState(
                            modeName = selectedMode!!.displayLabel,
                            onClear  = { selectedMode = null },
                        )
                    } else {
                        ActivitiesEmptyState(onLogTrip = onFabClick)
                    }
                }
            } else {
                filteredEntries.forEach { (dateLabel, entries) ->
                    item(key = "header_$dateLabel") {
                        DateSectionHeader(label = dateLabel, entries = entries)
                    }

                    item(key = "card_$dateLabel") {
                        AtmosCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = 0.dp,
                        ) {
                            entries.forEachIndexed { index, entry ->
                                key(entry.sessionId) {
                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { value ->
                                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                                onDelete(entry)
                                                true
                                            } else false
                                        },
                                    )
                                    SwipeToDismissBox(
                                        state = dismissState,
                                        enableDismissFromStartToEnd = false,
                                        backgroundContent = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(AlertRed)
                                                    .padding(end = 20.dp),
                                                contentAlignment = Alignment.CenterEnd,
                                            ) {
                                                Icon(
                                                    imageVector        = Icons.Outlined.Delete,
                                                    contentDescription = "Delete trip",
                                                    tint               = Color.White,
                                                    modifier           = Modifier.size(20.dp),
                                                )
                                            }
                                        },
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(colors.surface)
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                        ) {
                                            ActivityRow(
                                                entry   = entry,
                                                onClick = { onTripClick(entry) },
                                            )
                                        }
                                    }
                                }
                                if (index < entries.lastIndex) {
                                    HorizontalDivider(
                                        modifier  = Modifier.padding(horizontal = 16.dp),
                                        thickness = 1.dp,
                                        color     = colors.divider,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Mode filter chip ──────────────────────────────────────────────────────────

@Composable
private fun ModeFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAtmosColors.current
    FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = {
            Text(
                text       = label,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = HorizonBlue.copy(alpha = 0.12f),
            selectedLabelColor     = HorizonBlue,
            containerColor         = colors.chipBg,
            labelColor             = colors.textSecondary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled             = true,
            selected            = selected,
            borderColor         = colors.divider,
            selectedBorderWidth = 0.dp,
            borderWidth         = 1.dp,
        ),
    )
}

// ── Filter empty state ────────────────────────────────────────────────────────

@Composable
private fun FilterEmptyState(modeName: String, onClear: () -> Unit) {
    val colors = LocalAtmosColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 40.dp),
        ) {
            Text(
                text       = "No $modeName trips",
                fontSize   = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color      = colors.textPrimary,
                textAlign  = TextAlign.Center,
            )
            Text(
                text       = "You haven't logged any $modeName trips yet",
                fontSize   = 13.sp,
                color      = colors.textSecondary,
                textAlign  = TextAlign.Center,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onClear,
                shape   = RoundedCornerShape(12.dp),
                border  = BorderStroke(1.5.dp, HorizonBlue),
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = HorizonBlue),
            ) {
                Text(text = "Show all trips", fontWeight = FontWeight.Medium)
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

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun ActivitiesEmptyState(onLogTrip: () -> Unit) {
    val colors = LocalAtmosColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 40.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.chipBg),
            ) {
                Icon(
                    imageVector        = Icons.Outlined.History,
                    contentDescription = null,
                    tint               = colors.textSecondary,
                    modifier           = Modifier.size(28.dp),
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text       = "No trips logged yet",
                fontSize   = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color      = colors.textPrimary,
                textAlign  = TextAlign.Center,
            )

            Text(
                text       = "Your travel history will appear here once you log a trip",
                fontSize   = 13.sp,
                color      = colors.textSecondary,
                textAlign  = TextAlign.Center,
                lineHeight = 19.sp,
            )

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onLogTrip,
                shape   = RoundedCornerShape(12.dp),
                border  = BorderStroke(1.5.dp, HorizonBlue),
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = HorizonBlue),
            ) {
                Text(
                    text       = "Log your first trip",
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
