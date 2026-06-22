package dev.atmos.shared.ui.home

import dev.atmos.shared.location.LatLng
import dev.atmos.shared.location.haversineKm

/** Trips whose origin/destination is within this radius of a saved Home/Work
 *  location are labeled "Home"/"Work" instead of showing the raw address. */
const val COMMUTE_LABEL_RADIUS_KM = 0.15

/**
 * Returns "Home" or "Work" if ([lat], [lng]) is within [COMMUTE_LABEL_RADIUS_KM] of
 * the corresponding saved location; otherwise returns [rawAddress] unchanged.
 *
 * Each home/work coordinate pair is nil-guarded independently, matching the
 * convention used for activity coordinates elsewhere in this app — a saved Home
 * address with only one of lat/lng populated is treated as unset, not guessed at.
 * Home is checked before Work, so if both happen to be within range of the same
 * point, Home wins (arbitrary but deterministic).
 */
fun commuteDisplayLabel(
    rawAddress: String,
    lat: Double?,
    lng: Double?,
    homeLat: Double?,
    homeLng: Double?,
    workLat: Double?,
    workLng: Double?,
): String {
    if (lat == null || lng == null) return rawAddress
    val point = LatLng(lat, lng)

    if (homeLat != null && homeLng != null &&
        haversineKm(point, LatLng(homeLat, homeLng)) <= COMMUTE_LABEL_RADIUS_KM
    ) {
        return "Home"
    }
    if (workLat != null && workLng != null &&
        haversineKm(point, LatLng(workLat, workLng)) <= COMMUTE_LABEL_RADIUS_KM
    ) {
        return "Work"
    }
    return rawAddress
}
