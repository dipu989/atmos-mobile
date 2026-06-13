package dev.atmos.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.russhwolf.settings.Settings
import dev.atmos.shared.auth.GoogleSignInHolder
import dev.atmos.shared.network.FcmTokenStore
import dev.atmos.shared.location.LocalPermissionRequester
import dev.atmos.shared.location.LocationPermissionState
import dev.atmos.shared.location.NoOpPermissionRequester
import dev.atmos.shared.location.PermissionRequester
import dev.atmos.shared.location.TripDetectorHolder
import dev.atmos.shared.location.TripDetectorState
import dev.atmos.shared.ui.AtmosApp
import dev.atmos.shared.ui.common.LocalShareLauncher

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialise holders before setContent so that LaunchedEffect / lazy vals
        // inside AtmosApp can access context and the activity reference.
        TripDetectorHolder.init(applicationContext)
        GoogleSignInHolder.init(this)

        // Seed FcmTokenStore from persisted Settings immediately (synchronous) so that
        // AtmosApp's device-registration LaunchedEffect sees the token on first composition.
        // Then ask Firebase for the current token — this refreshes it if it has rotated since
        // the last launch, writing the fresh value to both Settings and FcmTokenStore.
        val settings = Settings()
        FcmTokenStore.update(settings.getStringOrNull("fcm_token"))
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            settings.putString("fcm_token", token)
            FcmTokenStore.update(token)
        }

        // Sync permission state with TripDetectorState so the onboarding pills
        // reflect real grant status even on subsequent app launches.
        val locationAlreadyGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (locationAlreadyGranted) {
            TripDetectorState.updatePermissionState(LocationPermissionState.GRANTED)
        }
        val notifAlreadyGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true   // Pre-Android-13: notifications are always available
        TripDetectorState.updateNotificationsGranted(notifAlreadyGranted)

        setContent {
            // ── Location permission launcher ──────────────────────────────────
            val locationLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
                TripDetectorState.updatePermissionState(
                    if (granted) LocationPermissionState.GRANTED else LocationPermissionState.DENIED
                )
            }

            // ── Notification permission launcher (Android 13+) ────────────────
            val notifLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                TripDetectorState.updateNotificationsGranted(granted)
            }

            // ── Provide the requester to the whole composition tree ───────────
            val permReq: PermissionRequester = remember {
                object : PermissionRequester {
                    override fun requestLocation() {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        )
                    }
                    override fun requestNotification() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            // Pre-Android-13: permission is implicit — mark granted immediately
                            TripDetectorState.updateNotificationsGranted(true)
                        }
                    }
                }
            }

            val shareLauncher = remember { AndroidShareLauncher(applicationContext) }
            CompositionLocalProvider(
                LocalPermissionRequester provides permReq,
                LocalShareLauncher       provides shareLauncher,
            ) {
                AtmosApp()
            }
        }
    }
}
