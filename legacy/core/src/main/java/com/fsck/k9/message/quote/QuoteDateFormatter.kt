package com.fsck.k9.message.quote

import java.text.DateFormat
import java.util.Date
import java.util.TimeZone
import net.thunderbird.core.preference.GeneralSettingsManager

/**
 * Convert a date into a locale-specific date string suitable for use in a header for a quoted message.
 */
class QuoteDateFormatter(
    private val generalSettingsManager: GeneralSettingsManager,
) {

    fun format(date: Date): String {
        return try {
            val dateFormat = createDateFormat()
            dateFormat.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    private fun createDateFormat(): DateFormat {
        return DateFormat.getDateTimeInstance(DATE_STYLE, TIME_STYLE).apply {
            if (generalSettingsManager.getSettings().privacy.isHideTimeZone) {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
    }

    companion object {
        private const val DATE_STYLE = DateFormat.LONG
        private const val TIME_STYLE = DateFormat.LONG
    }
}
