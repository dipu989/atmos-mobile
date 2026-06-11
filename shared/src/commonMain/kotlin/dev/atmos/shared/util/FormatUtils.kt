package dev.atmos.shared.util

/**
 * Formats a Float for display, showing up to one decimal place.
 * Whole numbers are returned without a decimal point (e.g. 5.0f → "5").
 * Non-whole numbers show exactly one decimal digit (e.g. 3.4f → "3.4").
 */
internal fun Float.toDisplayString(): String {
    if (this % 1f == 0f) return toInt().toString()
    val intPart = toInt()
    return "$intPart.${((this - intPart) * 10).toInt()}"
}
