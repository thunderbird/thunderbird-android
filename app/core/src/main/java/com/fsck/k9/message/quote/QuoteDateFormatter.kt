package com.fsck.k9.message.quote

import android.content.res.Resources
import com.fsck.k9.K9
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Convert a date into a locale-specific date string suitable for use in a header for a quoted message.
 */
class QuoteDateFormatter(private val resources: Resources) {

    fun format(date: Date): String {
        return try {
            val dateFormat = createDateFormat()
            dateFormat.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    private fun createDateFormat(): DateFormat {
        return if (K9.isHideTimeZone) {
            DateFormat.getDateTimeInstance(DATE_STYLE, TIME_STYLE, Locale.ROOT).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        } else {
            val locale = resources.configuration.locale
            DateFormat.getDateTimeInstance(DATE_STYLE, TIME_STYLE, locale)
        }
    }

    companion object {
        private const val DATE_STYLE = DateFormat.LONG
        private const val TIME_STYLE = DateFormat.LONG
    }
}
