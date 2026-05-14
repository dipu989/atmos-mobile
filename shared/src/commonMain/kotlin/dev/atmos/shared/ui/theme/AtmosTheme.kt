package dev.atmos.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.atmos.shared.ui.profile.AppearanceMode

private val AtmosLightScheme = lightColorScheme(
    primary            = HorizonBlue,
    onPrimary          = LightAtmosColors.surface,
    primaryContainer   = LightAtmosColors.badgeBg,
    onPrimaryContainer = Sage,
    secondary          = Sage,
    onSecondary        = LightAtmosColors.surface,
    tertiary           = Peach,
    background         = LightAtmosColors.background,
    onBackground       = LightAtmosColors.textPrimary,
    surface            = LightAtmosColors.surface,
    onSurface          = LightAtmosColors.textPrimary,
    surfaceVariant     = LightAtmosColors.background,
    onSurfaceVariant   = LightAtmosColors.textSecondary,
    error              = AlertRed,
    outline            = LightAtmosColors.divider,
)

private val AtmosDarkScheme = darkColorScheme(
    primary            = HorizonBlue,
    onPrimary          = DarkAtmosColors.surface,
    primaryContainer   = DarkAtmosColors.badgeBg,
    onPrimaryContainer = Sage,
    secondary          = Sage,
    onSecondary        = DarkAtmosColors.surface,
    tertiary           = Peach,
    background         = DarkAtmosColors.background,
    onBackground       = DarkAtmosColors.textPrimary,
    surface            = DarkAtmosColors.surface,
    onSurface          = DarkAtmosColors.textPrimary,
    surfaceVariant     = DarkAtmosColors.background,
    onSurfaceVariant   = DarkAtmosColors.textSecondary,
    error              = AlertRed,
    outline            = DarkAtmosColors.divider,
)

@Composable
fun AtmosTheme(
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (appearanceMode) {
        AppearanceMode.LIGHT  -> false
        AppearanceMode.DARK   -> true
        AppearanceMode.SYSTEM -> systemDark
    }

    val atmosColors = if (useDark) DarkAtmosColors else LightAtmosColors
    val colorScheme = if (useDark) AtmosDarkScheme else AtmosLightScheme

    CompositionLocalProvider(LocalAtmosColors provides atmosColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = AtmosTypography,
            shapes      = AtmosShapes,
            content     = content,
        )
    }
}
