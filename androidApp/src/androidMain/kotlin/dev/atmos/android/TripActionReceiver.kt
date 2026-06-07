package dev.atmos.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.atmos.shared.location.TripDetectionServiceConsts
import dev.atmos.shared.location.TripDetectorHolder

/**
 * Handles notification action button taps from the foreground trip-tracking notification.
 *
 * Buttons are registered in [TripDetectionService.buildEndTripAction] and
 * [TripDetectionService.buildDiscardAction]. Each fires a broadcast with the
 * action string set to [TripDetectionServiceConsts.ACTION_END_TRIP] or
 * [TripDetectionServiceConsts.ACTION_DISCARD].
 *
 * The receiver re-initialises [TripDetectorHolder] in case the app process was killed
 * and the OS woke us up only to handle this broadcast.
 */
class TripActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        TripDetectorHolder.init(context)
        val detector = TripDetectorHolder.get()
        when (intent.action) {
            TripDetectionServiceConsts.ACTION_END_TRIP -> detector.manualEndAndSave()
            TripDetectionServiceConsts.ACTION_DISCARD  -> detector.discardSession()
        }
    }
}
