package com.fsck.k9.mailstore;

import android.net.Uri;

import com.fsck.k9.ical.ICalData;
import com.fsck.k9.mail.Part;

import biweekly.property.Attendee;
import biweekly.property.Organizer;
import biweekly.property.RecurrenceRule;

public class ICalendarViewInfo {
    public final Part part;
    public final boolean isContentAvailable;
    public final long size;
    public final Uri uri;

    public final ICalData iCalData;

    public ICalendarViewInfo(Part part, boolean isContentAvailable, long size,
                             Uri uri,
                             ICalData iCalData) {
        this.part = part;
        this.isContentAvailable = isContentAvailable;
        this.size = size;
        this.uri = uri;

        this.iCalData = iCalData;
    }
}
