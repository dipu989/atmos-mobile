package dev.atmos.shared

import dev.atmos.shared.ui.common.ShareLauncher
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

class IosShareLauncher : ShareLauncher {
    override fun share(fileName: String, content: String) {
        val activityVC = UIActivityViewController(
            activityItems = listOf(content),
            applicationActivities = null,
        )
        UIApplication.sharedApplication.keyWindow
            ?.rootViewController
            ?.presentViewController(activityVC, animated = true, completion = null)
    }
}
