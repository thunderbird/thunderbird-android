package com.fsck.k9.ical;

import java.util.ArrayList;

import biweekly.ICalendar;
import com.fsck.k9.mail.internet.MessageExtractor;

import biweekly.Biweekly;

public class ICalParser {
    public static final String MIME_TYPE = "text/calendar";

    public static ICalData parse(ICalPart part) {

        String iCalText = MessageExtractor.getTextFromPart(part.getPart());

        if (iCalText == null) {
            return new ICalData(new ArrayList<ICalendar>());
        }

        return new ICalData(Biweekly.parse(iCalText).all());

    }
}
