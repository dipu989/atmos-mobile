package dev.atmos.shared.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.common.AtmosCard
import dev.atmos.shared.ui.common.CircularProgressRing
import dev.atmos.shared.ui.theme.Sage
import dev.atmos.shared.ui.theme.TextPrimary
import dev.atmos.shared.ui.theme.TextSecondary

@Composable
fun DailyGoalCard(
    todayKgCO2: Float,
    dailyGoalKgCO2: Float,
    modifier: Modifier = Modifier,
) {
    val fraction = (todayKgCO2 / dailyGoalKgCO2).coerceIn(0f, 1f)

    AtmosCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 20.dp,
    ) {
        Text(
            text = "Daily Goal",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Progress ring
            CircularProgressRing(
                progress = fraction,
                size = 80.dp,
                stroke = 7.dp,
                progressColor = Sage,
            ) {
                Text(
                    text = todayKgCO2.toGoalString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            }

            // Current usage label
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current usage",
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${todayKgCO2.toGoalString()} kg CO₂",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            }

            // Goal chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = Color(0xFFF0F4F8),
                        shape = RoundedCornerShape(50),
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "${dailyGoalKgCO2.toGoalString()} kg CO₂",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit goal",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

private fun Float.toGoalString(): String {
    if (this % 1f == 0f) return toInt().toString()
    val intPart = toInt()
    val decPart = ((this - intPart) * 10).toInt()
    return "$intPart.$decPart"
}
