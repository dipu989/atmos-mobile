package dev.atmos.shared.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
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

fun currentDateLabel(): String {
    val date = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    return "${date.dayOfWeek.displayName}, ${date.month.displayName} ${date.dayOfMonth}"
}

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
