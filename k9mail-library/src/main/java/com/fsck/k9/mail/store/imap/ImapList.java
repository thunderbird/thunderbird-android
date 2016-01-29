package com.fsck.k9.mail.store.imap;

import com.fsck.k9.mail.MessagingException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Represents an IMAP list response and is also the base class for the
 * ImapResponse.
 */
class ImapList extends ArrayList<Object> {
    private static final long serialVersionUID = -4067248341419617583L;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z", Locale.US);
    private static final DateFormat BAD_DATE_TIME_FORMAT = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.US);
    private static final DateFormat BAD_DATE_TIME_FORMAT_2 = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.US);
    private static final DateFormat BAD_DATE_TIME_FORMAT_3 = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US);

    public ImapList getList(int index) {
        return (ImapList)get(index);
    }

    public boolean isList(int index) {
        return inRange(index) && get(index) instanceof ImapList;
    }

    public Object getObject(int index) {
        return get(index);
    }

    public String getString(int index) {
        return (String)get(index);
    }

    public boolean isString(int index) {
        return inRange(index) && get(index) instanceof String;
    }

    public long getLong(int index) {
        return Long.parseLong(getString(index));
    }

    public int getNumber(int index) {
        return Integer.parseInt(getString(index));
    }

    public Date getDate(int index) throws MessagingException {
        return getDate(getString(index));
    }

    public Date getKeyedDate(String key) throws MessagingException {
        return getDate(getKeyedString(key));
    }

    private Date getDate(String value) throws MessagingException {
        try {
            if (value == null) {
                return null;
            }
            return parseDate(value);
        } catch (ParseException pe) {
            throw new MessagingException("Unable to parse IMAP datetime '" + value + "' ", pe);
        }
    }


    public Object getKeyedValue(String key) {
        for (int i = 0, count = size() - 1; i < count; i++) {
            if (ImapResponseParser.equalsIgnoreCase(get(i), key)) {
                return get(i + 1);
            }
        }
        return null;
    }

    public ImapList getKeyedList(String key) {
        return (ImapList)getKeyedValue(key);
    }

    public String getKeyedString(String key) {
        return (String)getKeyedValue(key);
    }

    public int getKeyedNumber(String key) {
        return Integer.parseInt(getKeyedString(key));
    }

    public boolean containsKey(String key) {
        if (key == null) {
            return false;
        }

        for (int i = 0, count = size() - 1; i < count; i++) {
            if (ImapResponseParser.equalsIgnoreCase(get(i), key)) {
                return true;
            }
        }
        return false;
    }

    public int getKeyIndex(String key) {
        for (int i = 0, count = size() - 1; i < count; i++) {
            if (ImapResponseParser.equalsIgnoreCase(get(i), key)) {
                return i;
            }
        }

        throw new IllegalArgumentException("getKeyIndex() only works for keys that are in the collection.");
    }

    private boolean inRange(int index) {
        return index >= 0 && index < size();
    }

    private Date parseDate(String value) throws ParseException {
        //TODO: clean this up a bit
        try {
            synchronized (DATE_FORMAT) {
                return DATE_FORMAT.parse(value);
            }
        } catch (Exception e) {
            try {
                synchronized (BAD_DATE_TIME_FORMAT) {
                    return BAD_DATE_TIME_FORMAT.parse(value);
                }
            } catch (Exception e2) {
                try {
                    synchronized (BAD_DATE_TIME_FORMAT_2) {
                        return BAD_DATE_TIME_FORMAT_2.parse(value);
                    }
                } catch (Exception e3) {
                    synchronized (BAD_DATE_TIME_FORMAT_3) {
                        return BAD_DATE_TIME_FORMAT_3.parse(value);
                    }
                }
            }
        }
    }
}
