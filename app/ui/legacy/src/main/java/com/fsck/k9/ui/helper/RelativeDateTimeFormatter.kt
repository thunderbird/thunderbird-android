package com.fsck.k9.ui.helper

import android.content.Context
import android.text.format.DateUtils
import android.text.format.DateUtils.FORMAT_ABBREV_MONTH
import android.text.format.DateUtils.FORMAT_ABBREV_WEEKDAY
import android.text.format.DateUtils.FORMAT_NUMERIC_DATE
import android.text.format.DateUtils.FORMAT_SHOW_DATE
import android.text.format.DateUtils.FORMAT_SHOW_TIME
import android.text.format.DateUtils.FORMAT_SHOW_WEEKDAY
import android.text.format.DateUtils.FORMAT_SHOW_YEAR
import java.util.Calendar
import java.util.Calendar.DAY_OF_WEEK
import java.util.Calendar.YEAR
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Formatter to describe timestamps as a time relative to now.
 */
class RelativeDateTimeFormatter(
    private val context: Context,
    private val clock: Clock,
) {

    fun formatDate(timestamp: Long): String {
        val now = clock.now().toCalendar()
        val date = timestamp.toCalendar()
        val format = when {
            date.isToday() -> FORMAT_SHOW_TIME
            date.isWithinPastSevenDaysOf(now) -> FORMAT_SHOW_WEEKDAY or FORMAT_ABBREV_WEEKDAY
            date.isSameYearAs(now) -> FORMAT_SHOW_DATE or FORMAT_ABBREV_MONTH
            else -> FORMAT_SHOW_DATE or FORMAT_SHOW_YEAR or FORMAT_NUMERIC_DATE
        }
        return DateUtils.formatDateRange(context, timestamp, timestamp, format)
    }
}

private fun Long.toCalendar(): Calendar {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this
    return calendar
}

private fun Instant.toCalendar(): Calendar {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this.toEpochMilliseconds()
    return calendar
}

private fun Calendar.isToday() = DateUtils.isToday(this.timeInMillis)

private fun Calendar.isWithinPastSevenDaysOf(other: Calendar) = this.before(other) &&
    DateUtils.WEEK_IN_MILLIS > other.timeInMillis - this.timeInMillis &&
    this[DAY_OF_WEEK] != other[DAY_OF_WEEK]

private fun Calendar.isSameYearAs(other: Calendar) = this[YEAR] == other[YEAR]
