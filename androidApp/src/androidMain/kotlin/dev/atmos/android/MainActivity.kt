package dev.atmos.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.atmos.shared.ui.home.HomeScreen
import dev.atmos.shared.ui.theme.AtmosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AtmosTheme {
                HomeScreen()
            }
        }
    }
}
