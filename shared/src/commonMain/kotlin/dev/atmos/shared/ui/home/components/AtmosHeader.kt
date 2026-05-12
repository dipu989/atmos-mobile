package dev.atmos.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.home.UserProfile
import dev.atmos.shared.ui.theme.AvatarBg
import dev.atmos.shared.ui.theme.CardSurface
import dev.atmos.shared.ui.theme.TextPrimary
import dev.atmos.shared.ui.theme.TextSecondary

@Composable
fun AtmosHeader(
    greeting: String,
    dateLabel: String,
    user: UserProfile,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "$greeting, ${user.displayName.split(" ").first()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
            )
            Text(
                text = dateLabel,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
        }

        UserAvatar(initials = user.initials)
    }
}

@Composable
private fun UserAvatar(
    initials: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .background(color = AvatarBg, shape = CircleShape),
    ) {
        Text(
            text = initials,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = CardSurface,
        )
    }
}
