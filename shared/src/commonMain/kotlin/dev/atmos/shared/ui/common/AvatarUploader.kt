package dev.atmos.shared.ui.common

import androidx.compose.runtime.compositionLocalOf

interface AvatarUploader {
    fun launch(userId: String, onComplete: (Result<String>) -> Unit)
}

object NoOpAvatarUploader : AvatarUploader {
    override fun launch(userId: String, onComplete: (Result<String>) -> Unit) = Unit
}

val LocalAvatarUploader = compositionLocalOf<AvatarUploader> { NoOpAvatarUploader }
