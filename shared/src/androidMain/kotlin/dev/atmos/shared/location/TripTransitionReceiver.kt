package dev.atmos.shared.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

/**
 * Receives Activity Recognition Transition events from Google Play Services.
 * Registered in AndroidManifest.xml — fires even when the app process is dead.
 *
 * Translates raw Play Services events into [VehicleActivity] and forwards them to
 * [AndroidTripDetector] via its companion object (static callback).
 */
class TripTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return

        val result = ActivityTransitionResult.extractResult(intent) ?: return

        for (event in result.transitionEvents) {
            val vehicleActivity = when {
                event.activityType == DetectedActivity.IN_VEHICLE &&
                        event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER ->
                    VehicleActivity.IN_VEHICLE_ENTERED

                event.activityType == DetectedActivity.IN_VEHICLE &&
                        event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT ->
                    VehicleActivity.IN_VEHICLE_EXITED

                event.activityType == DetectedActivity.WALKING &&
                        event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER ->
                    VehicleActivity.WALKING_ENTERED

                else -> continue
            }

            AndroidTripDetector.handleTransition(context, vehicleActivity)
        }
    }
}
