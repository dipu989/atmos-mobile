package dev.atmos.shared.ui.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.theme.Sage
import dev.atmos.shared.ui.theme.TextPrimary
import dev.atmos.shared.ui.theme.TextSecondary

@Composable
fun MyImpactCard(
    totalCO2SavedKg: Float,
    daysTracked: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4FBF7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Eco,
                    contentDescription = null,
                    tint = Sage,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "My Impact",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Sage,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Total CO₂ Saved",
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${totalCO2SavedKg.toStatString()} kg",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Sage,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Days Tracked",
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$daysTracked days",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                }
            }
        }
    }
}

private fun Float.toStatString(): String {
    if (this % 1f == 0f) return toInt().toString()
    val intPart = toInt()
    val decPart = ((this - intPart) * 10).toInt()
    return "$intPart.$decPart"
}
