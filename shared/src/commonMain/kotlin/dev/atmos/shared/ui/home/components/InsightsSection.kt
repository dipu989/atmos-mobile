package dev.atmos.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.common.AtmosCard
import dev.atmos.shared.ui.home.InsightEntry
import dev.atmos.shared.ui.home.InsightType
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Sage

// ── Icon / color mappings ─────────────────────────────────────────────────────

private val InsightType.icon: ImageVector
    get() = when (this) {
        InsightType.STREAK      -> Icons.Outlined.AutoAwesome
        InsightType.TIP         -> Icons.AutoMirrored.Outlined.TrendingDown
        InsightType.MILESTONE,
        InsightType.COMPARISON,
        InsightType.ANOMALY     -> Icons.Outlined.TrackChanges
    }

private val InsightType.iconTint: Color
    get() = when (this) {
        InsightType.TIP -> Sage
        else            -> HorizonBlue
    }

// ── Section ───────────────────────────────────────────────────────────────────

@Composable
fun InsightsSection(
    entries: List<InsightEntry>,
    unreadCount: Int,
    modifier: Modifier = Modifier,
    onInsightClick: (InsightEntry) -> Unit = {},
    onViewAll: () -> Unit = {},
) {
    val colors = LocalAtmosColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Text(
                text = "Insights",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
            if (unreadCount > 0) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "· $unreadCount",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HorizonBlue,
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick      = onViewAll,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 4.dp,
                    vertical   = 0.dp,
                ),
            ) {
                Text(
                    text       = "See all",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color      = HorizonBlue,
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector        = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint               = HorizonBlue,
                    modifier           = Modifier.size(15.dp),
                )
            }
        }

        if (entries.isEmpty()) {
            // ── Inline empty state ────────────────────────────────────────────
            AtmosCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .background(color = colors.insightBlueBg, shape = CircleShape),
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint               = HorizonBlue,
                            modifier           = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text       = "Insights on the way",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = colors.textPrimary,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text       = "Log a few trips and we'll surface personalised tips, streaks, and milestones",
                            fontSize   = 13.sp,
                            color      = colors.textSecondary,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        } else {
            entries.forEach { entry ->
                InsightCard(entry = entry, onClick = { onInsightClick(entry) })
            }
        }
    }
}

// ── Single insight card ───────────────────────────────────────────────────────

@Composable
fun InsightCard(
    entry: InsightEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val colors = LocalAtmosColors.current
    val iconBg = if (entry.type == InsightType.TIP) colors.insightGreenBg else colors.insightBlueBg

    AtmosCard(modifier = modifier.fillMaxWidth().clickable(onClick = onClick), contentPadding = 16.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .background(color = iconBg, shape = CircleShape),
            ) {
                Icon(
                    imageVector = entry.type.icon,
                    contentDescription = null,
                    tint = entry.type.iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entry.body,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textSecondary,
                    lineHeight = 18.sp,
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
