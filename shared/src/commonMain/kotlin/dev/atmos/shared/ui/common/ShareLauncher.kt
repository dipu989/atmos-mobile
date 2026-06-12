package dev.atmos.shared.ui.common

import androidx.compose.runtime.compositionLocalOf

interface ShareLauncher {
    fun share(fileName: String, content: String)
}

object NoOpShareLauncher : ShareLauncher {
    override fun share(fileName: String, content: String) {}
}

val LocalShareLauncher = compositionLocalOf<ShareLauncher> { NoOpShareLauncher }
