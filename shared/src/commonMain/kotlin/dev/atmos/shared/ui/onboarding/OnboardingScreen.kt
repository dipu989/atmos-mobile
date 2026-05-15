package dev.atmos.shared.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Sage

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit = {},
    onAlreadyHaveAccount: () -> Unit = {},
) {
    val colors = LocalAtmosColors.current
    var currentPage by remember { mutableStateOf(0) }
    var notificationsEnabled by remember { mutableStateOf(false) }
    var locationEnabled by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // ── Animated page content ─────────────────────────────────────────────
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "onboarding_page",
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 180.dp),
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> HowItWorksPage()
                2 -> PermissionsPage(
                    notificationsEnabled = notificationsEnabled,
                    locationEnabled = locationEnabled,
                    onNotificationsToggle = { notificationsEnabled = it },
                    onLocationToggle = { locationEnabled = it },
                )
            }
        }

        // ── Bottom controls ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PaginationDots(currentPage = currentPage, totalPages = 3)

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (currentPage < 2) currentPage++ else onGetStarted()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HorizonBlue),
            ) {
                Text(
                    text = when (currentPage) {
                        0    -> "Get Started"
                        1    -> "Next"
                        else -> "Allow & Continue"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }

            when (currentPage) {
                0 -> TextButton(onClick = onAlreadyHaveAccount) {
                    Text(
                        text = "I already have an account",
                        fontSize = 14.sp,
                        color = LocalAtmosColors.current.textSecondary,
                    )
                }
                2 -> TextButton(onClick = onGetStarted) {
                    Text(
                        text = "Skip for now",
                        fontSize = 14.sp,
                        color = LocalAtmosColors.current.textSecondary,
                    )
                }
                else -> Spacer(Modifier.height(36.dp))
            }
        }
    }
}

// ── Page 1: Welcome ───────────────────────────────────────────────────────────

@Composable
private fun WelcomePage() {
    val colors = LocalAtmosColors.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero illustration
        HeroIllustration(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
        )

        Spacer(Modifier.height(36.dp))

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Your commute,\ntracked automatically",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Atmos detects your trips in the background\nand calculates your carbon footprint for you.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )
        }
    }
}

// ── Hero illustration ─────────────────────────────────────────────────────────

@Composable
private fun HeroIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        HorizonBlue.copy(alpha = 0.18f),
                        Sage.copy(alpha = 0.06f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Decorative canvas layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawDecorativeCircles()
            drawCitySkyline()
        }

        // Central focal element
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = HorizonBlue.copy(alpha = 0.12f),
                    shape = CircleShape,
                ),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(84.dp)
                    .background(
                        color = HorizonBlue.copy(alpha = 0.18f),
                        shape = CircleShape,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Eco,
                    contentDescription = null,
                    tint = Sage,
                    modifier = Modifier.size(42.dp),
                )
            }
        }

        // Floating accent dots
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 32.dp, top = 40.dp)
                .size(10.dp)
                .background(HorizonBlue.copy(alpha = 0.4f), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 48.dp, top = 60.dp)
                .size(7.dp)
                .background(Sage.copy(alpha = 0.5f), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 60.dp, bottom = 36.dp)
                .size(8.dp)
                .background(Sage.copy(alpha = 0.4f), CircleShape),
        )
    }
}

private fun DrawScope.drawDecorativeCircles() {
    // Large blue circle — top right
    drawCircle(
        color = Color(0xFF4A90C4).copy(alpha = 0.10f),
        radius = 130.dp.toPx(),
        center = Offset(size.width + 20.dp.toPx(), -20.dp.toPx()),
    )
    // Medium green circle — bottom left
    drawCircle(
        color = Color(0xFF3DAB82).copy(alpha = 0.10f),
        radius = 90.dp.toPx(),
        center = Offset(-20.dp.toPx(), size.height + 10.dp.toPx()),
    )
    // Small blue circle — mid left
    drawCircle(
        color = Color(0xFF4A90C4).copy(alpha = 0.08f),
        radius = 50.dp.toPx(),
        center = Offset(30.dp.toPx(), size.height * 0.4f),
    )
}

private fun DrawScope.drawCitySkyline() {
    val buildingColor = Color(0xFF1A2332).copy(alpha = 0.07f)
    val baseY = size.height

    // Each triple: startX fraction, width fraction, height px
    val buildings = listOf(
        Triple(0.00f, 0.07f, 55f),
        Triple(0.08f, 0.05f, 85f),
        Triple(0.14f, 0.08f, 42f),
        Triple(0.23f, 0.06f, 100f),
        Triple(0.30f, 0.09f, 62f),
        Triple(0.40f, 0.05f, 78f),
        Triple(0.46f, 0.10f, 38f),
        Triple(0.57f, 0.06f, 90f),
        Triple(0.64f, 0.08f, 58f),
        Triple(0.73f, 0.05f, 72f),
        Triple(0.79f, 0.09f, 48f),
        Triple(0.89f, 0.06f, 82f),
        Triple(0.96f, 0.04f, 60f),
    )

    buildings.forEach { (startFrac, widthFrac, heightDp) ->
        val x = startFrac * size.width
        val w = widthFrac * size.width
        val h = heightDp.dp.toPx()
        drawRect(
            color = buildingColor,
            topLeft = Offset(x, baseY - h),
            size = Size(w - 2.dp.toPx(), h),
        )
    }
}

// ── Page 2: How it works ──────────────────────────────────────────────────────

@Composable
private fun HowItWorksPage() {
    val colors = LocalAtmosColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "How Atmos works",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "No manual effort — it just works",
            fontSize = 15.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(40.dp))

        FeatureRow(
            icon = Icons.Outlined.MyLocation,
            iconBg = colors.insightBlueBg,
            iconTint = HorizonBlue,
            title = "Auto-detects your trips",
            subtitle = "Atmos runs in the background and detects every journey automatically.",
        )

        Spacer(Modifier.height(28.dp))

        FeatureRow(
            icon = Icons.Outlined.BarChart,
            iconBg = colors.insightBlueBg,
            iconTint = HorizonBlue,
            title = "Confirm in one tap",
            subtitle = "Review auto-detected trips and approve or edit with a single tap.",
        )

        Spacer(Modifier.height(28.dp))

        FeatureRow(
            icon = Icons.Outlined.Lightbulb,
            iconBg = colors.insightGreenBg,
            iconTint = Sage,
            title = "Get personalised insights",
            subtitle = "Streaks, tips, and weekly trends to help you cut your emissions.",
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
) {
    val colors = LocalAtmosColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .background(iconBg, CircleShape),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(26.dp),
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = colors.textSecondary,
                lineHeight = 18.sp,
            )
        }
    }
}

// ── Page 3: Permissions ───────────────────────────────────────────────────────

@Composable
private fun PermissionsPage(
    notificationsEnabled: Boolean,
    locationEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    onLocationToggle: (Boolean) -> Unit,
) {
    val colors = LocalAtmosColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "A couple of quick\npermissions",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Location is required for auto-detection.\nNotifications keep you informed after each trip.",
            fontSize = 15.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )

        Spacer(Modifier.height(36.dp))

        PermissionCard(
            icon = Icons.Outlined.NotificationsNone,
            iconBg = colors.insightBlueBg,
            iconTint = HorizonBlue,
            title = "Push Notifications",
            subtitle = "Get notified when a trip is detected and ready to confirm.",
            checked = notificationsEnabled,
            onCheckedChange = onNotificationsToggle,
        )

        Spacer(Modifier.height(12.dp))

        PermissionCard(
            icon = Icons.Outlined.LocationOn,
            iconBg = colors.insightGreenBg,
            iconTint = Sage,
            title = "Location Access",
            subtitle = "Always-on access so Atmos can detect trips even when the app is closed.",
            checked = locationEnabled,
            onCheckedChange = onLocationToggle,
        )
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalAtmosColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBg, CircleShape),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp,
                )
            }

            Spacer(Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = HorizonBlue,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFCDD5DE),
                ),
            )
        }
    }
}

// ── Pagination dots ───────────────────────────────────────────────────────────

@Composable
private fun PaginationDots(currentPage: Int, totalPages: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalPages) { index ->
            val isActive = index == currentPage
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (isActive) 20.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) HorizonBlue
                        else LocalAtmosColors.current.textTertiary,
                    ),
            )
        }
    }
}
