package dev.atmos.shared.util

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The backend always stores and computes distance in km (emission factors are
 * km-based) — this only controls how distances are displayed/entered on screen.
 */
enum class DistanceUnit(val label: String) {
    KM("km"),
    MILES("mi"),
}

private const val KM_PER_MILE = 1.609344f

/** Converts a canonical km value into the given display unit. */
fun Float.toDisplayUnit(unit: DistanceUnit): Float = when (unit) {
    DistanceUnit.KM    -> this
    DistanceUnit.MILES -> this / KM_PER_MILE
}

/** Converts a value already in the given display unit back into canonical km. */
fun Float.fromDisplayUnit(unit: DistanceUnit): Float = when (unit) {
    DistanceUnit.KM    -> this
    DistanceUnit.MILES -> this * KM_PER_MILE
}

/** Numeric distance text in the active display unit, no unit suffix — for editable fields. */
fun Float.formatDistanceValue(unit: DistanceUnit): String = toDisplayUnit(unit).toDisplayString()

/** Distance text with unit suffix, e.g. "8.6 km" / "5.3 mi". */
fun Float.formatDistance(unit: DistanceUnit): String = "${formatDistanceValue(unit)} ${unit.label}"

/** Maps the backend's raw `distance_unit` preference value ("km" | "miles") to [DistanceUnit]. */
fun String?.asDistanceUnit(): DistanceUnit = if (this == "miles") DistanceUnit.MILES else DistanceUnit.KM

/** Active distance-unit preference. Provided once near the app root; defaults to km. */
val LocalDistanceUnit = staticCompositionLocalOf { DistanceUnit.KM }
