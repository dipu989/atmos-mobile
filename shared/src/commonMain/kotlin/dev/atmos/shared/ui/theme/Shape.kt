package dev.atmos.shared.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AtmosShapes = Shapes(
    // Chips, badges
    extraSmall = RoundedCornerShape(6.dp),
    // Buttons
    small      = RoundedCornerShape(10.dp),
    // Cards
    medium     = RoundedCornerShape(16.dp),
    // Bottom sheets
    large      = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

// Named aliases used directly in composables
val CardShape  = RoundedCornerShape(16.dp)
val ChipShape  = RoundedCornerShape(50.dp)
val AvatarShape = RoundedCornerShape(50.dp)
