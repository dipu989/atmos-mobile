package dev.atmos.shared.util

import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

fun currentGreeting(): String {
    val hour = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .hour
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else      -> "Good evening"
    }
}

fun currentTimeLabel(): String {
    val time = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val hour12 = when {
        time.hour == 0  -> 12
        time.hour > 12  -> time.hour - 12
        else            -> time.hour
    }
    val minute = time.minute.toString().padStart(2, '0')
    val amPm   = if (time.hour < 12) "AM" else "PM"
    return "$hour12:$minute $amPm"
}

fun currentDateLabel(): String {
    val date = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    return "${date.dayOfWeek.displayName}, ${date.month.displayName} ${date.dayOfMonth}"
}

/**
 * Formats an epoch-millisecond timestamp as a 12-hour clock string, e.g. "8:45 AM".
 * Used for per-trip time labels in the Activities screen.
 */
fun formatTimestamp(ms: Long): String {
    val time = Instant.fromEpochMilliseconds(ms)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val hour12 = when {
        time.hour == 0  -> 12
        time.hour > 12  -> time.hour - 12
        else            -> time.hour
    }
    val minute = time.minute.toString().padStart(2, '0')
    val amPm   = if (time.hour < 12) "AM" else "PM"
    return "$hour12:$minute $amPm"
}

/**
 * Returns a human-readable date group label for a given epoch-ms timestamp.
 * Examples: "Today", "Yesterday", "Mon, Jun 2"
 */
fun formatDateGroupLabel(ms: Long): String {
    val tz        = TimeZone.currentSystemDefault()
    val date      = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz).date
    val today     = Clock.System.now().toLocalDateTime(tz).date
    val yesterday = today.minus(1, DateTimeUnit.DAY)
    return when (date) {
        today     -> "Today"
        yesterday -> "Yesterday"
        else      -> "${date.dayOfWeek.shortName}, ${date.month.shortName} ${date.dayOfMonth}"
    }
}

// ── Long display names (used in header) ──────────────────────────────────────

private val DayOfWeek.displayName: String
    get() = when (this) {
        DayOfWeek.MONDAY    -> "Monday"
        DayOfWeek.TUESDAY   -> "Tuesday"
        DayOfWeek.WEDNESDAY -> "Wednesday"
        DayOfWeek.THURSDAY  -> "Thursday"
        DayOfWeek.FRIDAY    -> "Friday"
        DayOfWeek.SATURDAY  -> "Saturday"
        DayOfWeek.SUNDAY    -> "Sunday"
    }

private val Month.displayName: String
    get() = when (this) {
        Month.JANUARY   -> "January"
        Month.FEBRUARY  -> "February"
        Month.MARCH     -> "March"
        Month.APRIL     -> "April"
        Month.MAY       -> "May"
        Month.JUNE      -> "June"
        Month.JULY      -> "July"
        Month.AUGUST    -> "August"
        Month.SEPTEMBER -> "September"
        Month.OCTOBER   -> "October"
        Month.NOVEMBER  -> "November"
        Month.DECEMBER  -> "December"
    }

// ── Short names (used in date group labels) ───────────────────────────────────

internal val DayOfWeek.shortName: String
    get() = when (this) {
        DayOfWeek.MONDAY    -> "Mon"
        DayOfWeek.TUESDAY   -> "Tue"
        DayOfWeek.WEDNESDAY -> "Wed"
        DayOfWeek.THURSDAY  -> "Thu"
        DayOfWeek.FRIDAY    -> "Fri"
        DayOfWeek.SATURDAY  -> "Sat"
        DayOfWeek.SUNDAY    -> "Sun"
    }

internal val Month.shortName: String
    get() = when (this) {
        Month.JANUARY   -> "Jan"
        Month.FEBRUARY  -> "Feb"
        Month.MARCH     -> "Mar"
        Month.APRIL     -> "Apr"
        Month.MAY       -> "May"
        Month.JUNE      -> "Jun"
        Month.JULY      -> "Jul"
        Month.AUGUST    -> "Aug"
        Month.SEPTEMBER -> "Sep"
        Month.OCTOBER   -> "Oct"
        Month.NOVEMBER  -> "Nov"
        Month.DECEMBER  -> "Dec"
    }
