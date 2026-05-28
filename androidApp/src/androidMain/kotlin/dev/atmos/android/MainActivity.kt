package dev.atmos.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.atmos.shared.auth.GoogleSignInHolder
import dev.atmos.shared.location.TripDetectorHolder
import dev.atmos.shared.ui.AtmosApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialise holders before setContent so that LaunchedEffect / lazy vals
        // inside AtmosApp can access context and the activity reference.
        TripDetectorHolder.init(applicationContext)
        GoogleSignInHolder.init(this)

        setContent {
            AtmosApp()
        }
    }
}
