package com.fsck.k9.message.quote

import android.content.res.Resources
import com.fsck.k9.K9
import com.fsck.k9.mail.Message
import java.text.DateFormat
import java.util.Locale
import java.util.TimeZone

class QuoteHelper(private val resources: Resources) {

    /**
     * Extract the date from a message and convert it into a locale-specific
     * date string suitable for use in a header for a quoted message.
     *
     * @return A string with the formatted date/time
     */
    fun getSentDateText(message: Message): String {
        return try {
            val dateFormat = createDateFormat()
            val date = message.sentDate

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
        // amount of extra buffer to allocate to accommodate quoting headers or prefixes
        const val QUOTE_BUFFER_LENGTH = 512

        private const val DATE_STYLE = DateFormat.LONG
        private const val TIME_STYLE = DateFormat.LONG
    }
}
