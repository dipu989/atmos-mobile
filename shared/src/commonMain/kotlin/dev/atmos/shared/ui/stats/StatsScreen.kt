package dev.atmos.shared.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.common.AtmosCard
import dev.atmos.shared.ui.common.ShimmerBox
import dev.atmos.shared.ui.home.TransportModeEntry
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Peach
import dev.atmos.shared.ui.theme.Sage
import dev.atmos.shared.util.LocalDistanceUnit
import dev.atmos.shared.util.formatDistance

// KMP-safe decimal formatters (no String.format / JVM dependency).
// Truncating (not rounding) to match the toDisplayString() convention used for
// every other numeric display in the app, so this screen doesn't show CO2/kg
// rounded one way and distance rounded another.
private fun Float.fmt1dp(): String {
    val scaled = (this * 10f).toInt()
    return "${scaled / 10}.${scaled % 10}"
}

private fun Float.fmt2dp(): String {
    val scaled = (this * 100f).toInt()
    return "${scaled / 100}.${(scaled % 100).toString().padStart(2, '0')}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: StatsUiState = previewStatsUiState,
    onBack: () -> Unit = {},
    onPeriodChange: (StatsPeriod) -> Unit = {},
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    canGoNext: Boolean = false,
    onRetry: () -> Unit = {},
) {
    val colors = LocalAtmosColors.current

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Stats",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Period selector ───────────────────────────────────────────────
            item {
                PeriodSelector(
                    selected = state.period,
                    onSelect = onPeriodChange,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // ── Period navigator ──────────────────────────────────────────────
            item {
                PeriodNavigator(
                    label     = state.periodLabel,
                    canGoPrev = state.canGoPrev,
                    canGoNext = canGoNext,
                    onPrev    = onPrev,
                    onNext    = onNext,
                )
            }

            if (state.isLoading) {
                item { SummaryCardSkeleton() }
                if (state.period == StatsPeriod.WEEK) {
                    item { BarChartSkeleton() }
                }
                item { BreakdownSkeleton() }
                return@LazyColumn
            }

            if (state.isError) {
                item { StatsErrorState(onRetry = onRetry) }
                return@LazyColumn
            }

            val summary = state.summary
            if (summary == null || (summary.totalKgCo2 == 0f && summary.activityCount == 0)) {
                item { EmptyPeriodState(period = state.period) }
                return@LazyColumn
            }

            // ── Summary card ──────────────────────────────────────────────────
            item {
                SummaryCard(summary = summary, period = state.period)
            }

            // ── Bar chart — week view only ────────────────────────────────────
            if (state.period == StatsPeriod.WEEK && state.barPoints.isNotEmpty()) {
                item {
                    BarChartCard(points = state.barPoints)
                }
            }

            // ── Transport breakdown ───────────────────────────────────────────
            if (summary.breakdown.isNotEmpty()) {
                item {
                    BreakdownCard(breakdown = summary.breakdown)
                }
            }
        }
    }
}

// ── Period selector ───────────────────────────────────────────────────────────

@Composable
private fun PeriodSelector(
    selected: StatsPeriod,
    onSelect: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAtmosColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StatsPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) HorizonBlue else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(period) },
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (period) {
                        StatsPeriod.DAY   -> "Day"
                        StatsPeriod.WEEK  -> "Week"
                        StatsPeriod.MONTH -> "Month"
                    },
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else colors.textSecondary,
                )
            }
        }
    }
}

// ── Period navigator ──────────────────────────────────────────────────────────

@Composable
private fun PeriodNavigator(
    label: String,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = LocalAtmosColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onPrev,
            enabled = canGoPrev,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBackIosNew,
                contentDescription = "Previous period",
                tint = if (canGoPrev) colors.textPrimary else colors.textSecondary.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp),
            )
        }

        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
        )

        IconButton(
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = "Next period",
                tint = if (canGoNext) colors.textPrimary else colors.textSecondary.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ── Summary card ──────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(summary: StatsSummary, period: StatsPeriod) {
    val colors = LocalAtmosColors.current
    val unit = LocalDistanceUnit.current

    AtmosCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = when (period) {
                StatsPeriod.DAY   -> "Daily impact"
                StatsPeriod.WEEK  -> "Weekly total"
                StatsPeriod.MONTH -> "Monthly total"
            },
            fontSize = 13.sp,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = summary.totalKgCo2.fmt1dp(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                lineHeight = 40.sp,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "kg CO₂e",
                fontSize = 15.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        if (summary.trendPct != null) {
            TrendChip(direction = summary.trendDirection, pct = summary.trendPct)
            Spacer(Modifier.height(10.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            MetaStat(label = "Distance", value = summary.totalDistKm.formatDistance(unit))
            MetaStat(label = "Trips",    value = summary.activityCount.toString())
            if (summary.prevTotalKgCo2 > 0f) {
                MetaStat(label = "Prev period", value = "${summary.prevTotalKgCo2.fmt1dp()} kg")
            }
        }
    }
}

@Composable
private fun TrendChip(direction: String, pct: Float) {
    val (icon, chipColor) = when (direction) {
        "down" -> Icons.AutoMirrored.Outlined.TrendingDown to Sage
        "up"   -> Icons.AutoMirrored.Outlined.TrendingUp   to AlertRed
        else   -> Icons.AutoMirrored.Outlined.TrendingFlat to Peach
    }
    val label = when (direction) {
        "flat" -> "No change vs last period"
        "down" -> "↓ ${(-pct).toInt()}% vs last period"
        else   -> "↑ ${pct.toInt()}% vs last period"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(chipColor.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = chipColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = chipColor)
    }
}

@Composable
private fun MetaStat(label: String, value: String) {
    val colors = LocalAtmosColors.current
    Column {
        Text(text = value,  fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Text(text = label,  fontSize = 11.sp, color = colors.textSecondary)
    }
}

// ── Bar chart card ────────────────────────────────────────────────────────────

@Composable
private fun BarChartCard(points: List<StatsBarPoint>) {
    val colors = LocalAtmosColors.current

    AtmosCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Daily breakdown",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(16.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            val maxVal = points.maxOfOrNull { it.kgCo2 }?.coerceAtLeast(0.1f) ?: 0.1f
            val canvasW = size.width
            val canvasH = size.height
            val count = points.size
            val gapTotal = canvasW * 0.35f
            val gap = gapTotal / (count + 1).toFloat()
            val barW = (canvasW - gapTotal) / count.toFloat()

            points.forEachIndexed { i, point ->
                val barH = (point.kgCo2 / maxVal) * canvasH
                val x = gap + i * (barW + gap)
                val y = canvasH - barH
                val color = if (point.isCurrent) HorizonBlue else HorizonBlue.copy(alpha = 0.32f)

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barW, barH.coerceAtLeast(4.dp.toPx())),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            points.forEach { point ->
                Text(
                    text = point.label,
                    fontSize = 11.sp,
                    fontWeight = if (point.isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (point.isCurrent) HorizonBlue else colors.textSecondary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Transport breakdown ───────────────────────────────────────────────────────

@Composable
private fun BreakdownCard(breakdown: List<TransportModeEntry>) {
    val colors = LocalAtmosColors.current
    val maxDist = breakdown.maxOfOrNull { it.distanceKm }?.coerceAtLeast(0.1f) ?: 0.1f

    AtmosCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "By transport mode",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(12.dp))

        breakdown.forEachIndexed { i, entry ->
            BreakdownRow(entry = entry, maxDist = maxDist)
            if (i < breakdown.lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun BreakdownRow(entry: TransportModeEntry, maxDist: Float) {
    val colors = LocalAtmosColors.current
    val unit = LocalDistanceUnit.current
    val barColor = when {
        entry.mode.isHighEmission -> AlertRed
        entry.mode.isZeroEmission -> Sage
        else                      -> Peach
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.displayName,
                fontSize = 13.sp,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = entry.distanceKm.formatDistance(unit),
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                )
                Text(
                    text = if (entry.kgCO2 < 0.05f) "0 g CO₂" else "${entry.kgCO2.fmt2dp()} kg",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.divider),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (entry.distanceKm / maxDist).coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor),
            )
        }
    }
}

// ── Loading skeletons ─────────────────────────────────────────────────────────

@Composable
private fun SummaryCardSkeleton() {
    AtmosCard(modifier = Modifier.fillMaxWidth()) {
        ShimmerBox(Modifier.width(100.dp).height(12.dp))
        Spacer(Modifier.height(12.dp))
        ShimmerBox(Modifier.width(180.dp).height(40.dp), RoundedCornerShape(8.dp))
        Spacer(Modifier.height(10.dp))
        ShimmerBox(Modifier.width(160.dp).height(26.dp), RoundedCornerShape(8.dp))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            repeat(3) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ShimmerBox(Modifier.width(50.dp).height(14.dp))
                    ShimmerBox(Modifier.width(40.dp).height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun BarChartSkeleton() {
    AtmosCard(modifier = Modifier.fillMaxWidth()) {
        ShimmerBox(Modifier.width(120.dp).height(14.dp))
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            listOf(0.45f, 0.9f, 0.3f, 0.7f, 0.65f, 0.25f, 0.4f).forEach { fraction ->
                ShimmerBox(
                    Modifier.width(28.dp).fillMaxHeight(fraction),
                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun BreakdownSkeleton() {
    AtmosCard(modifier = Modifier.fillMaxWidth()) {
        ShimmerBox(Modifier.width(140.dp).height(14.dp))
        Spacer(Modifier.height(16.dp))
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShimmerBox(Modifier.width(80.dp).height(13.dp))
                ShimmerBox(Modifier.width(80.dp).height(13.dp))
            }
            Spacer(Modifier.height(6.dp))
            ShimmerBox(Modifier.fillMaxWidth().height(4.dp), RoundedCornerShape(2.dp))
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ── Empty / error states ──────────────────────────────────────────────────────

@Composable
private fun EmptyPeriodState(period: StatsPeriod) {
    val colors = LocalAtmosColors.current
    val (title, subtitle) = when (period) {
        StatsPeriod.DAY -> "No trips today" to "Log a trip today to see stats for this period."
        StatsPeriod.WEEK -> "No trips this week" to "Log a trip this week to see stats for this period."
        StatsPeriod.MONTH -> "No trips this month" to "Log a trip this month to see stats for this period."
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.DirectionsRun,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
        )
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = colors.textSecondary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatsErrorState(onRetry: () -> Unit) {
    val colors = LocalAtmosColors.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = "Couldn't load stats",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(HorizonBlue)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRetry,
                )
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text(text = "Retry", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
