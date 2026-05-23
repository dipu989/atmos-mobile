package dev.atmos.android

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import dev.atmos.shared.location.AndroidPermissionRequester
import dev.atmos.shared.location.LocalPermissionRequester
import dev.atmos.shared.location.TripDetectorHolder
import dev.atmos.shared.location.VehicleActivity
import dev.atmos.shared.ui.AtmosApp

class MainActivity : ComponentActivity() {

    // Registered before onCreate per ActivityResult API contract
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRequester.onFineLocationResult(granted)
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRequester.onBackgroundLocationResult(granted)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRequester.onNotificationResult(granted)
    }

    private lateinit var permissionRequester: AndroidPermissionRequester

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialise context holder so TripDetector.android.kt can access application context
        TripDetectorHolder.init(applicationContext)

        // Wire up permission launchers
        permissionRequester = AndroidPermissionRequester(
            fineLocationLauncher       = locationPermissionLauncher,
            backgroundLocationLauncher = backgroundLocationLauncher,
            notificationLauncher       = notificationPermissionLauncher,
        )

        setContent {
            CompositionLocalProvider(LocalPermissionRequester provides permissionRequester) {
                // TODO: TESTING — remove before shipping
                val detector = TripDetectorHolder.get()
                Box(modifier = Modifier.fillMaxSize()) {
                    AtmosApp()

                    // Compact debug pill — top-right corner, never overlaps content
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 48.dp, end = 8.dp)
                            .background(Color(0xCC111111), RoundedCornerShape(20.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .zIndex(99f),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { detector.handleTransition(VehicleActivity.IN_VEHICLE_ENTERED) },
                            modifier = Modifier.padding(0.dp)
                        ) { Text("🚗", fontSize = 14.sp) }
                        TextButton(
                            onClick = { detector.handleTransition(VehicleActivity.IN_VEHICLE_EXITED) },
                            modifier = Modifier.padding(0.dp)
                        ) { Text("🛑", fontSize = 14.sp) }
                        TextButton(
                            onClick = { detector.handleTransition(VehicleActivity.WALKING_ENTERED) },
                            modifier = Modifier.padding(0.dp)
                        ) { Text("🚶", fontSize = 14.sp) }
                    }
                }
            }
        }
    }
}
