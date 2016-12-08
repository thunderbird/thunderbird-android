package com.fsck.k9.ical;

import android.util.Log;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MessageExtractor;

import biweekly.Biweekly;
import biweekly.ICalendar;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;

public class ICalParser {
    public static final String MIME_TYPE = "text/calendar";

    public static ICalData parse(ICalPart part) {

        String iCalText = MessageExtractor.getTextFromPart(part.getPart());

        //TODO: Handle more than one entry
        ICalendar iCal = Biweekly.parse(iCalText).first();

        if (iCal != null) {
            return new ICalData(iCal);
        } else {
            Log.e(LOG_TAG, "No iCalendar in part:" + part);
            return null;
        }
    }
}
