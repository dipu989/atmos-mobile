package dev.atmos.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.common.AtmosCard
import dev.atmos.shared.ui.common.CircularProgressRing
import dev.atmos.shared.ui.home.TodayImpact
import dev.atmos.shared.ui.theme.ChipShape
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Sage
import kotlin.math.abs

@Composable
fun TodayImpactCard(
    impact: TodayImpact,
    modifier: Modifier = Modifier,
) {
    val progress = (impact.kgCO2 / impact.dailyGoalKgCO2).coerceIn(0f, 1f)
    val isBelow  = impact.percentVsWeeklyAvg < 0
    val colors   = LocalAtmosColors.current

    AtmosCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Today's Impact",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
            )
            PercentageBadge(
                percent = abs(impact.percentVsWeeklyAvg),
                isBelow = isBelow,
            )
        }

        Spacer(Modifier.height(24.dp))

        CircularProgressRing(
            progress = progress,
            size = 176.dp,
            stroke = 11.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            ImpactNumber(kgCO2 = impact.kgCO2)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = buildComparisonText(abs(impact.percentVsWeeklyAvg), isBelow),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = colors.textSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun ImpactNumber(kgCO2: Float) {
    val colors = LocalAtmosColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.wrapContentSize(),
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                ) { append(kgCO2.toDisplayString()) }
            },
            lineHeight = 52.sp,
        )
        Text(
            text = buildAnnotatedString {
                append("kg CO")
                withStyle(SpanStyle(fontSize = 10.sp)) { append("₂") }
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun PercentageBadge(percent: Int, isBelow: Boolean) {
    val colors = LocalAtmosColors.current
    val arrow = if (isBelow) "↓" else "↑"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color = colors.badgeBg, shape = ChipShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = "$arrow $percent%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Sage,
        )
    }
}

private fun buildComparisonText(percent: Int, isBelow: Boolean): String =
    if (isBelow) "$percent% below your weekly average"
    else "$percent% above your weekly average"

private fun Float.toDisplayString(): String {
    val rounded = (this * 10).toInt()
    return if (rounded % 10 == 0) "${rounded / 10}" else "${rounded / 10}.${rounded % 10}"
}
