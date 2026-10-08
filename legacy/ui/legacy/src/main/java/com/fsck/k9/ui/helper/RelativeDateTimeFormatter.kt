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
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListDateTimeFormat

/**
 * Formatter to describe timestamps as a time relative to now.
 *
 * @param context The context to use for formatting dates.
 * @param clock The clock to use for getting the current time.
 */
class RelativeDateTimeFormatter(
    private val context: Context,
    private val clock: Clock,
) {
    /**
     * Format a date using the given [dateTimeFormat].
     *
     * @param timestamp The timestamp to format.
     * @param dateTimeFormat The date format to use.
     */
    fun formatDate(
        timestamp: Long,
        dateTimeFormat: MessageListDateTimeFormat,
    ): String {
        val timeZone = TimeZone.currentSystemDefault()
        val now = clock.now().toLocalDateTime(timeZone)

        return formatDate(
            timestamp = timestamp,
            dateTimeFormat = dateTimeFormat,
            now = now,
            timeZone = timeZone,
        )
    }

    private fun formatDate(
        timestamp: Long,
        dateTimeFormat: MessageListDateTimeFormat,
        now: LocalDateTime,
        timeZone: TimeZone,
    ): String {
        val date = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone)

        return when (dateTimeFormat) {
            MessageListDateTimeFormat.Contextual -> {
                val flags = when {
                    date.isSameDayAs(now) -> FORMAT_SHOW_TIME
                    date.isWithinPastSevenDaysOf(now) -> FORMAT_SHOW_WEEKDAY or FORMAT_ABBREV_WEEKDAY
                    date.isSameYearAs(now) -> FORMAT_SHOW_DATE or FORMAT_ABBREV_MONTH
                    else -> FORMAT_SHOW_DATE or FORMAT_SHOW_YEAR or FORMAT_NUMERIC_DATE
                }
                DateUtils.formatDateRange(context, timestamp, timestamp, flags)
            }
            MessageListDateTimeFormat.Full -> {
                val flags = FORMAT_SHOW_TIME or FORMAT_SHOW_DATE or FORMAT_SHOW_YEAR or FORMAT_NUMERIC_DATE
                DateUtils.formatDateRange(context, timestamp, timestamp, flags)
            }
            MessageListDateTimeFormat.ISO -> {
                isoDateTimeFormat.format(date)
            }
        }
    }

    private fun LocalDateTime.isSameDayAs(other: LocalDateTime): Boolean {
        return date == other.date
    }

    private fun LocalDateTime.isWithinPastSevenDaysOf(other: LocalDateTime): Boolean {
        val daysUntil = date.daysUntil(other.date)
        return daysUntil in 1 until DAYS_PER_WEEK
    }

    private fun LocalDateTime.isSameYearAs(other: LocalDateTime): Boolean {
        return year == other.year
    }

    private companion object {
        private const val DAYS_PER_WEEK = 7

        val isoDateTimeFormat = LocalDateTime.Format {
            year()
            char('-')
            monthNumber()
            char('-')
            day()
            char(' ')
            hour()
            char(':')
            minute()
        }
    }
}
