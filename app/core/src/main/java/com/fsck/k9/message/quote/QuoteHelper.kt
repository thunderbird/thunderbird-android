package com.fsck.k9.message.quote;


import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.content.res.Resources;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Message;


class QuoteHelper {
    // amount of extra buffer to allocate to accommodate quoting headers or prefixes
    static final int QUOTE_BUFFER_LENGTH = 512;

    private static final int DATE_STYLE = DateFormat.LONG;
    private static final int TIME_STYLE = DateFormat.LONG;


    private final Resources resources;


    QuoteHelper(Resources resources) {
        this.resources = resources;
    }


    /**
     * Extract the date from a message and convert it into a locale-specific
     * date string suitable for use in a header for a quoted message.
     *
     * @return A string with the formatted date/time
     */
    String getSentDateText(Message message) {
        try {
            DateFormat dateFormat = createDateFormat();
            Date date = message.getSentDate();

            return dateFormat.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    private DateFormat createDateFormat() {
        DateFormat dateFormat;
        if (K9.hideTimeZone()) {
            dateFormat = DateFormat.getDateTimeInstance(DATE_STYLE, TIME_STYLE, Locale.ROOT);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        } else {
            Locale locale = resources.getConfiguration().locale;
            dateFormat = DateFormat.getDateTimeInstance(DATE_STYLE, TIME_STYLE, locale);
        }

        return dateFormat;
    }
}
