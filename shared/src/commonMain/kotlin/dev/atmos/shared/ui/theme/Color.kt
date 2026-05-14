package dev.atmos.shared.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Brand palette (static — same in light and dark) ──────────────────────────
val HorizonBlue   = Color(0xFF4A90C4)
val Sage          = Color(0xFF3DAB82)
val Peach         = Color(0xFFE89066)
val AlertRed      = Color(0xFFE86B5F)

// ── Nav (static) ──────────────────────────────────────────────────────────────
val NavActive    = HorizonBlue
val NavInactive  = Color(0xFFB0BEC5)

// ── Semantic (static) ─────────────────────────────────────────────────────────
val AvatarBg         = Color(0xFF4A8FA0)
val ChartLine        = Color(0xFF3A5F8A)
val ChartFillTop     = Color(0x204A90C4)
val ChartFillBottom  = Color(0x004A90C4)
val AverageDash      = Color(0xFFCDD6E0)

// ── Theme-aware colors ────────────────────────────────────────────────────────

data class AtmosColors(
    val background: Color,
    val surface: Color,
    val navSurface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val ringTrack: Color,
    val badgeBg: Color,
    val chipBg: Color,
    val subtleGreenBg: Color,
    val insightBlueBg: Color,
    val insightGreenBg: Color,
)

val LightAtmosColors = AtmosColors(
    background     = Color(0xFFF5F7FA),
    surface        = Color(0xFFFFFFFF),
    navSurface     = Color(0xFFFFFFFF),
    textPrimary    = Color(0xFF1A2332),
    textSecondary  = Color(0xFF8A9BB0),
    textTertiary   = Color(0xFFB0BEC5),
    divider        = Color(0xFFF0F2F5),
    ringTrack      = Color(0xFFE8F0EC),
    badgeBg        = Color(0xFFE8F7F2),
    chipBg         = Color(0xFFF0F4F8),
    subtleGreenBg  = Color(0xFFF4FBF7),
    insightBlueBg  = Color(0xFFE8F2FA),
    insightGreenBg = Color(0xFFE8F7F0),
)

val DarkAtmosColors = AtmosColors(
    background     = Color(0xFF0F1923),
    surface        = Color(0xFF1A2535),
    navSurface     = Color(0xFF1A2535),
    textPrimary    = Color(0xFFE8EDF5),
    textSecondary  = Color(0xFF6A7F9A),
    textTertiary   = Color(0xFF4A5F75),
    divider        = Color(0xFF253040),
    ringTrack      = Color(0xFF1A3028),
    badgeBg        = Color(0xFF1A3025),
    chipBg         = Color(0xFF253040),
    subtleGreenBg  = Color(0xFF1A3028),
    insightBlueBg  = Color(0xFF1A2E45),
    insightGreenBg = Color(0xFF1A3028),
)

val LocalAtmosColors = staticCompositionLocalOf { LightAtmosColors }

// ── Legacy aliases kept for compilation — prefer LocalAtmosColors.current ─────
// These reference light values and are only used where @Composable context is
// unavailable (e.g. Canvas drawing), or in constants that don't vary by theme.
val SkyWhite      get() = LightAtmosColors.background
val CardSurface   get() = LightAtmosColors.surface
val NavSurface    get() = LightAtmosColors.navSurface
val TextPrimary   get() = LightAtmosColors.textPrimary
val TextSecondary get() = LightAtmosColors.textSecondary
val TextTertiary  get() = LightAtmosColors.textTertiary
val Divider       get() = LightAtmosColors.divider
val RingTrack     get() = LightAtmosColors.ringTrack
val BadgeBg       get() = LightAtmosColors.badgeBg
