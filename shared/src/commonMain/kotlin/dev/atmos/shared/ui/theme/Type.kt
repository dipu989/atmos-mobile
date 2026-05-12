package dev.atmos.shared.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── DM Sans font family ───────────────────────────────────────────────────────
// Place font files in:
//   shared/src/commonMain/composeResources/font/
//     DmSans_Regular.ttf
//     DmSans_Medium.ttf
//     DmSans_SemiBold.ttf
//     DmSans_Bold.ttf
// Download from: https://fonts.google.com/specimen/DM+Sans
//
// Then uncomment below and remove the DefaultFontFamily fallback:
//
// import dev.atmos.shared.generated.resources.*
// import org.jetbrains.compose.resources.Font
//
// @Composable
// fun dmSansFamily() = FontFamily(
//     Font(Res.font.DmSans_Regular,  FontWeight.Normal),
//     Font(Res.font.DmSans_Medium,   FontWeight.Medium),
//     Font(Res.font.DmSans_SemiBold, FontWeight.SemiBold),
//     Font(Res.font.DmSans_Bold,     FontWeight.Bold),
// )

val AtmosTypography = Typography(
    // Used for hero numbers (3.4)
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1).sp,
        color = TextPrimary,
    ),
    // Card titles — "Today's Impact", "Weekly Trend"
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
        color = TextPrimary,
    ),
    // Section labels — "Good morning, John"
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = TextPrimary,
    ),
    // Body / subtitles
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = TextSecondary,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = TextSecondary,
    ),
    // Chart axis labels, captions
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = TextSecondary,
    ),
    // Badge text, tab labels
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondary,
    ),
)
