package com.fsck.k9.ical;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.filter.Base64OutputStream;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.Viewable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.parameter.ParticipationLevel;
import biweekly.property.Attendee;
import biweekly.property.Organizer;
import biweekly.property.RecurrenceRule;

public class ICalPart {
    private final Part underlyingPart;

    public ICalPart(Part part) throws MessagingException {
        this.underlyingPart = part;
    }

    public Part getPart() {
        return this.underlyingPart;
    }
}
