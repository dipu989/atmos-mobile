package dev.atmos.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AtmosColorScheme = lightColorScheme(
    primary          = HorizonBlue,
    onPrimary        = CardSurface,
    primaryContainer = BadgeBg,
    onPrimaryContainer = Sage,
    secondary        = Sage,
    onSecondary      = CardSurface,
    tertiary         = Peach,
    background       = SkyWhite,
    onBackground     = TextPrimary,
    surface          = CardSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = SkyWhite,
    onSurfaceVariant = TextSecondary,
    error            = AlertRed,
    outline          = Divider,
)

@Composable
fun AtmosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AtmosColorScheme,
        typography  = AtmosTypography,
        shapes      = AtmosShapes,
        content     = content,
    )
}
