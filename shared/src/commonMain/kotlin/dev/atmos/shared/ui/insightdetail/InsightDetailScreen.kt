package dev.atmos.shared.ui.insightdetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.home.InsightEntry
import dev.atmos.shared.ui.home.InsightType
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Peach
import dev.atmos.shared.ui.theme.Sage

// ── Theme helpers for InsightType ─────────────────────────────────────────────

private val InsightType.themeColor: Color
    get() = when (this) {
        InsightType.STREAK,
        InsightType.TIP         -> Sage
        InsightType.MILESTONE,
        InsightType.COMPARISON  -> HorizonBlue
        InsightType.ANOMALY     -> Peach
    }

private val InsightType.themeBg: Color
    get() = when (this) {
        InsightType.STREAK,
        InsightType.TIP         -> Color(0xFFE8F7F0)
        InsightType.MILESTONE,
        InsightType.COMPARISON  -> Color(0xFFE8F2FA)
        InsightType.ANOMALY     -> Color(0xFFFFF3EE)
    }

private val InsightType.icon: ImageVector
    get() = when (this) {
        InsightType.STREAK      -> Icons.Outlined.AutoAwesome
        InsightType.TIP         -> Icons.Outlined.Lightbulb
        InsightType.MILESTONE   -> Icons.Outlined.EmojiEvents
        InsightType.COMPARISON  -> Icons.AutoMirrored.Outlined.TrendingDown
        InsightType.ANOMALY     -> Icons.Outlined.TrackChanges
    }

private val InsightType.badgeLabel: String
    get() = when (this) {
        InsightType.STREAK      -> "STREAK"
        InsightType.TIP         -> "TIP"
        InsightType.MILESTONE   -> "MILESTONE"
        InsightType.COMPARISON  -> "COMPARISON"
        InsightType.ANOMALY     -> "ANOMALY"
    }

private val InsightType.heroSubtitle: String
    get() = when (this) {
        InsightType.STREAK      -> "You're on a roll"
        InsightType.TIP         -> "Save more, emit less"
        InsightType.MILESTONE   -> "Goal progress"
        InsightType.COMPARISON  -> "How you stack up"
        InsightType.ANOMALY     -> "Unusual pattern detected"
    }

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightDetailScreen(
    entry: InsightEntry,
    onBack: () -> Unit = {},
    onLogTrip: () -> Unit = {},           // TIP insights — open LogActivitySheet
    onNavigateToActivities: () -> Unit = {}, // ANOMALY insights — review trips
    onNavigateToProfile: () -> Unit = {}, // MILESTONE insights — view goals
) {
    val colors      = LocalAtmosColors.current
    val scrollState = rememberScrollState()
    val heroColor   = entry.type.themeColor

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    // Type badge centred in the title slot
                    Box(
                        modifier = Modifier
                            .background(
                                color = heroColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(20.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text       = entry.type.badgeLabel,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = heroColor,
                            letterSpacing = 0.8.sp,
                        )
                    }
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
                    containerColor = colors.surface,
                ),
            )
        },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(scrollState)
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            // ── Hero ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(colors.surface),
            ) {
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(
                        color  = heroColor.copy(alpha = 0.09f),
                        radius = size.width * 0.70f,
                        center = Offset(size.width * 0.88f, size.height * 0.10f),
                    )
                    drawCircle(
                        color  = heroColor.copy(alpha = 0.06f),
                        radius = size.width * 0.45f,
                        center = Offset(size.width * 0.08f, size.height * 0.88f),
                    )
                    drawCircle(
                        color  = heroColor.copy(alpha = 0.05f),
                        radius = size.width * 0.20f,
                        center = Offset(size.width * 0.65f, size.height * 0.72f),
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(heroColor.copy(alpha = 0.14f)),
                    ) {
                        Icon(
                            imageVector        = entry.type.icon,
                            contentDescription = null,
                            tint               = heroColor,
                            modifier           = Modifier.size(32.dp),
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text      = entry.type.heroSubtitle,
                        fontSize  = 13.sp,
                        color     = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Title
                Text(
                    text       = entry.title,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = colors.textPrimary,
                    lineHeight = 28.sp,
                )

                // Body
                Text(
                    text       = entry.body,
                    fontSize   = 14.sp,
                    color      = colors.textSecondary,
                    lineHeight = 21.sp,
                )

                // ── Contextual visual per type ─────────────────────────────────
                when (entry.type) {
                    InsightType.STREAK     -> StreakVisual(streakCount = entry.streakCount)
                    InsightType.TIP        -> TipVisual(savingsPct = entry.savingsPct)
                    InsightType.MILESTONE  -> MilestoneVisual(progressPct = entry.goalProgressPct)
                    InsightType.COMPARISON -> ComparisonVisual(comparisonPct = entry.comparisonPct)
                    InsightType.ANOMALY    -> AnomalyVisual()
                }

                // ── CTA ───────────────────────────────────────────────────────
                InsightCTA(
                    entry                = entry,
                    onLogTrip            = onLogTrip,
                    onNavigateToActivities = onNavigateToActivities,
                    onNavigateToProfile  = onNavigateToProfile,
                    heroColor            = heroColor,
                )
            }
        }
    }
}

// ── Contextual visuals ────────────────────────────────────────────────────────

@Composable
private fun StreakVisual(streakCount: Int) {
    val colors = LocalAtmosColors.current
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text       = "This week",
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = colors.textSecondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            dayLabels.forEachIndexed { index, day ->
                val logged = index < streakCount
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (logged) Sage else colors.chipBg),
                    ) {
                        if (logged) {
                            Icon(
                                imageVector        = Icons.Outlined.Check,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text      = day,
                        fontSize  = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color     = if (logged) Sage else colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun TipVisual(savingsPct: Int) {
    val colors = LocalAtmosColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE8F7F0))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🌿", fontSize = 32.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text      = "Potential saving",
                    fontSize  = 12.sp,
                    color     = Sage.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text       = "$savingsPct% less CO₂",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Sage,
                )
                Text(
                    text     = "on this trip if you switch modes",
                    fontSize = 12.sp,
                    color    = Sage.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun MilestoneVisual(progressPct: Int) {
    val colors   = LocalAtmosColors.current
    val progress = (progressPct / 100f).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text       = "Monthly Goal",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = colors.textSecondary,
                modifier   = Modifier.weight(1f),
            )
            Text(
                text       = "$progressPct%",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = HorizonBlue,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        ) {
            drawRoundRect(color = colors.ringTrack, cornerRadius = CornerRadius(4.dp.toPx()))
            if (progress > 0f) {
                drawRoundRect(
                    color        = HorizonBlue,
                    size         = size.copy(width = size.width * progress),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
        }

        Text(
            text     = "$progressPct of 100 kg goal this month",
            fontSize = 12.sp,
            color    = colors.textSecondary,
        )
    }
}

@Composable
private fun ComparisonVisual(comparisonPct: Int) {
    val colors   = LocalAtmosColors.current
    val isBetter = comparisonPct < 0
    val absValue = kotlin.math.abs(comparisonPct)
    val label    = if (isBetter) "$absValue% below your average — great work!" else "$absValue% above your average this period"
    val bgColor  = if (isBetter) Color(0xFFE8F7F0) else Color(0xFFFFF3EE)
    val textColor = if (isBetter) Sage else Peach
    val emoji    = if (isBetter) "📉" else "📈"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Text(
                text       = label,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = textColor,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun AnomalyVisual() {
    val colors = LocalAtmosColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF3EE))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "⚠️", fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text       = "What we noticed",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Peach,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text       = "This is higher than your usual pattern. Consider switching to a lower-emission mode for some of these trips.",
                    fontSize   = 13.sp,
                    color      = colors.textSecondary,
                    lineHeight = 19.sp,
                )
            }
        }
    }
}

// ── CTA per insight type ──────────────────────────────────────────────────────

@Composable
private fun InsightCTA(
    entry: InsightEntry,
    onLogTrip: () -> Unit,
    onNavigateToActivities: () -> Unit,
    onNavigateToProfile: () -> Unit,
    heroColor: Color,
) {
    when (entry.type) {
        InsightType.TIP -> {
            Button(
                onClick  = onLogTrip,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Sage),
            ) {
                Text(
                    text       = "🌿  Log a greener trip",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(vertical = 5.dp),
                )
            }
        }

        InsightType.MILESTONE -> {
            OutlinedButton(
                onClick  = onNavigateToProfile,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = HorizonBlue),
            ) {
                Text(
                    text       = "View daily goal",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier.padding(vertical = 5.dp),
                )
            }
        }

        InsightType.ANOMALY -> {
            OutlinedButton(
                onClick  = onNavigateToActivities,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Peach),
            ) {
                Text(
                    text       = "Review trips",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier.padding(vertical = 5.dp),
                )
            }
        }

        InsightType.STREAK,
        InsightType.COMPARISON -> {
            // No button needed — the visual tells the full story
        }
    }
}
