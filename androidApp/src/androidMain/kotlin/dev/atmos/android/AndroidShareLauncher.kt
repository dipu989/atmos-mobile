package dev.atmos.android

import android.content.Context
import android.content.Intent
import dev.atmos.shared.ui.common.ShareLauncher

class AndroidShareLauncher(private val context: Context) : ShareLauncher {
    override fun share(fileName: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        // FLAG_ACTIVITY_NEW_TASK must be on the chooser intent itself, not the inner intent,
        // because createChooser() wraps it in a new Intent that won't inherit the flag.
        context.startActivity(
            Intent.createChooser(intent, "Export data").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
