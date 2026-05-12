package dev.atmos.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.CardSurface
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.NavActive
import dev.atmos.shared.ui.theme.NavInactive
import dev.atmos.shared.ui.theme.NavSurface

enum class AtmosTab { HOME, INSIGHTS }

@Composable
fun AtmosBottomBar(
    selectedTab: AtmosTab,
    unreadInsights: Int,
    onTabSelected: (AtmosTab) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, spotColor = Color(0x1A000000)),
    ) {
        // Nav surface
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavSurface)
                .padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Home tab (left half)
            NavTab(
                icon = Icons.Outlined.Home,
                label = "Home",
                selected = selectedTab == AtmosTab.HOME,
                onClick = { onTabSelected(AtmosTab.HOME) },
                modifier = Modifier.weight(1f),
            )

            // Center spacer for FAB
            Spacer(Modifier.weight(1f))

            // Insights tab (right half)
            NavTabWithBadge(
                icon = Icons.Outlined.Lightbulb,
                label = "Insights",
                selected = selectedTab == AtmosTab.INSIGHTS,
                badgeCount = unreadInsights,
                onClick = { onTabSelected(AtmosTab.INSIGHTS) },
                modifier = Modifier.weight(1f),
            )
        }

        // FAB — floats centered above nav bar
        FloatingActionButton(
            onClick = onFabClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
                .size(56.dp),
            shape = CircleShape,
            containerColor = HorizonBlue,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp,
            ),
        ) {
            Text(
                text = "+",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = CardSurface,
            )
        }
    }
}

@Composable
private fun NavTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) NavActive else NavInactive,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) NavActive else NavInactive,
        )
    }
}

@Composable
private fun NavTabWithBadge(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) NavActive else NavInactive,
                modifier = Modifier.size(22.dp),
            )
            if (badgeCount > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .size(16.dp)
                        .background(AlertRed, CircleShape),
                ) {
                    Text(
                        text = badgeCount.coerceAtMost(99).toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CardSurface,
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) NavActive else NavInactive,
        )
    }
}
