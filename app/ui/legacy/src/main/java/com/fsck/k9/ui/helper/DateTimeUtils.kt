package com.fsck.k9.ui.helper

import android.content.Context
import android.text.format.DateUtils
import java.util.Calendar
import kotlin.math.abs

/**
 * Format a timestamp for the date field in the message list.
 *
 * If the date is today, only the time will be shown.
 * If the date is from the past six days, only the weekday will be shown.
 * If the date is an earlier day from the current year, the day and month will be shown.
 * For all other dates the full date is shown (day, month, year).
 *
 * @param context the current context to determine correct formatting settings
 * @param messageDate the timestamp of the message in millis
 * @return the formatted timestamp
 */
fun formatMessageDate(context: Context, messageDate: Long): CharSequence {
    val now = System.currentTimeMillis()
    val nowCalendar = Calendar.getInstance()
    nowCalendar.timeInMillis = now
    val messageCalendar = Calendar.getInstance()
    messageCalendar.timeInMillis = messageDate
    return if (DateUtils.isToday(messageDate))
    // Today -> show time
        DateUtils.formatDateRange(context, messageDate, messageDate, DateUtils.FORMAT_SHOW_TIME)
    else if (DateUtils.WEEK_IN_MILLIS > abs(now - messageDate) && nowCalendar.get(Calendar.DAY_OF_WEEK) != messageCalendar.get(Calendar.DAY_OF_WEEK))
    // Past six days -> show weekday
        DateUtils.formatDateRange(context, messageDate, messageDate, DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY)
    else if (nowCalendar[Calendar.YEAR] == messageCalendar[Calendar.YEAR])
    // Current year -> show date without year
        DateUtils.formatDateRange(context, messageDate, messageDate, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
    else
    // Show date with year
        DateUtils.formatDateRange(context, messageDate, messageDate, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_NUMERIC_DATE)
}
