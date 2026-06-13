package dev.atmos.shared.location

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSOperationQueue
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

// ── Shared CLAuthorizationStatus → LocationPermissionState mapping ────────────

/**
 * Maps the current iOS authorization status of this [CLLocationManager] to
 * [LocationPermissionState] and writes the result to [TripDetectorState].
 *
 * Extracted to a package-level function so both [IosPermissionRequester] and
 * [IosTripDetector] use identical logic — a single source of truth.
 *
 * Notable mappings:
 * - `kCLAuthorizationStatusRestricted` → [LocationPermissionState.DENIED]:
 *   parental controls / MDM policy prevents the user from ever granting access;
 *   the UI should show a locked state, not an "ask me" state.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun CLLocationManager.syncPermissionStateToStore() {
    val state = when (authorizationStatus) {
        kCLAuthorizationStatusAuthorizedAlways    -> LocationPermissionState.GRANTED
        kCLAuthorizationStatusAuthorizedWhenInUse -> LocationPermissionState.BACKGROUND_ONLY
        kCLAuthorizationStatusDenied,
        kCLAuthorizationStatusRestricted          -> LocationPermissionState.DENIED
        kCLAuthorizationStatusNotDetermined       -> LocationPermissionState.UNKNOWN
        else                                      -> LocationPermissionState.UNKNOWN
    }
    TripDetectorState.updatePermissionState(state)
}

// ── IosPermissionRequester ────────────────────────────────────────────────────

/**
 * iOS implementation of [PermissionRequester].
 *
 * - [requestLocation] presents the system "Allow location" dialog via
 *   [CLLocationManager.requestAlwaysAuthorization] and updates
 *   [TripDetectorState.permissionState] when the user responds.
 * - [requestNotification] presents the system notification-permission dialog via
 *   [UNUserNotificationCenter.requestAuthorizationWithOptions] and updates
 *   [TripDetectorState.notificationsGranted] when the user responds.
 *
 * Both [locationManager] and [locationDelegate] are stored as instance fields so
 * they are not garbage-collected before the system callback fires — iOS holds the
 * CLLocationManager delegate weakly.
 *
 * An `init` block eagerly syncs the current grant state into [TripDetectorState]
 * so the onboarding permission pills show the correct value on re-launch (instead
 * of staying UNKNOWN until the user taps the pill again).
 */
@OptIn(ExperimentalForeignApi::class)
class IosPermissionRequester : PermissionRequester {

    private val locationDelegate = PermissionLocationDelegate()
    private val locationManager = CLLocationManager().also { it.delegate = locationDelegate }

    // Retained strongly — UNUserNotificationCenter holds the delegate weakly.
    private val notificationDelegate = AtmosNotificationDelegate()

    init {
        // Must be set before any notification is posted so foreground banners are not
        // silently suppressed (iOS drops notifications in the foreground unless a delegate
        // is present and opts in to .banner presentation).
        UNUserNotificationCenter.currentNotificationCenter().delegate = notificationDelegate

        // Sync whatever authorization state the OS already has on app launch.
        locationManager.syncPermissionStateToStore()

        // getNotificationSettingsWithCompletionHandler may call back on a background
        // queue — always dispatch the state write back to the main thread.
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                val status = settings?.authorizationStatus
                // Accept both Authorized (2) and Provisional (3): both deliver real
                // notifications — Provisional just does so quietly without user prompts.
                val granted = status == UNAuthorizationStatusAuthorized ||
                              status == UNAuthorizationStatusProvisional
                NSOperationQueue.mainQueue.addOperationWithBlock {
                    TripDetectorState.updateNotificationsGranted(granted)
                }
            }
    }

    override fun requestLocation() {
        locationManager.requestAlwaysAuthorization()
    }

    override fun requestNotification() {
        val options = UNAuthorizationOptionAlert or
                      UNAuthorizationOptionSound or
                      UNAuthorizationOptionBadge
        // requestAuthorizationWithOptions calls back on the main thread per Apple docs,
        // but we dispatch explicitly as a defensive guarantee.
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(options) { granted, _ ->
                NSOperationQueue.mainQueue.addOperationWithBlock {
                    TripDetectorState.updateNotificationsGranted(granted)
                }
            }
    }
}

// ── AtmosNotificationDelegate ─────────────────────────────────────────────────

/**
 * Allows Atmos notifications to be displayed as banners even when the app is in the
 * foreground. Without this delegate iOS silently suppresses all local notifications.
 */
@OptIn(ExperimentalForeignApi::class)
private class AtmosNotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (ULong) -> Unit,
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionSound
        )
    }
}

// ── PermissionLocationDelegate ────────────────────────────────────────────────

/**
 * Minimal [CLLocationManagerDelegateProtocol] whose sole job is to push the
 * current iOS authorization status into [TripDetectorState] when it changes.
 *
 * Must extend [NSObject] — this is an Objective-C protocol requirement.
 */
@OptIn(ExperimentalForeignApi::class)
private class PermissionLocationDelegate : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        manager.syncPermissionStateToStore()
    }
}
