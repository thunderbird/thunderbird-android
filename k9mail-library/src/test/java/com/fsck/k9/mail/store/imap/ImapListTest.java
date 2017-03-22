package com.fsck.k9.mail.store.imap;

import com.fsck.k9.mail.MessagingException;

import org.junit.Test;

import java.io.IOException;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ImapListTest {

    private ImapList buildSampleList() {
        ImapList list = new ImapList();
        list.add("ONE");
        list.add("TWO");
        list.add("THREE");
        return list;
    }

    @Test public void containsKey_returnsTrueForKeys() throws IOException {
        ImapList list = buildSampleList();

        assertTrue(list.containsKey("ONE"));
        assertTrue(list.containsKey("TWO"));
        assertFalse(list.containsKey("THREE"));
        assertFalse(list.containsKey("nonexistent"));
    }

    @Test public void containsKey_returnsFalseForStringThatCantBeKey() throws IOException {
        ImapList list = buildSampleList();

        assertFalse(list.containsKey("THREE"));
    }

    @Test public void containsKey_returnsFalseForStringNotInList() throws IOException {
        ImapList list = buildSampleList();

        assertFalse(list.containsKey("nonexistent"));
    }

    @Test
    public void getKeyedValue_providesCorrespondingValues() {
        ImapList list = buildSampleList();

        assertEquals("TWO", list.getKeyedValue("ONE"));
        assertEquals("THREE", list.getKeyedValue("TWO"));
        assertNull(list.getKeyedValue("THREE"));
        assertNull(list.getKeyedValue("nonexistent"));
    }

    @Test
    public void getKeyIndex_providesIndexForKeys() {
        ImapList list = buildSampleList();

        assertEquals(0, list.getKeyIndex("ONE"));
        assertEquals(1, list.getKeyIndex("TWO"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getKeyIndex_throwsExceptionForValue() {
        ImapList list = buildSampleList();

        list.getKeyIndex("THREE");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getKeyIndex_throwsExceptionForNonExistantKey() {
        ImapList list = buildSampleList();

        list.getKeyIndex("nonexistent");
    }

    @Test
    public void getDate_returnsCorrectDateForValidString() throws MessagingException {
        ImapList list = new ImapList();
        list.add("INTERNALDATE");
        list.add("10-Mar-2000 12:02:01 GMT");

        Calendar c = Calendar.getInstance();
        c.setTime(list.getDate(1));

        assertEquals(2000, c.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, c.get(Calendar.MONTH));
        assertEquals(10, c.get(Calendar.DAY_OF_MONTH));
    }

    @Test(expected = MessagingException.class)
    public void getDate_throwsExceptionForInvalidDate() throws MessagingException {
        ImapList list = new ImapList();
        list.add("INTERNALDATE");
        list.add("InvalidDate");

        list.getDate(1);
    }

    @Test
    public void getDate_returnsNullForNIL() throws MessagingException {
        ImapList list = new ImapList();
        list.add("INTERNALDATE");
        list.add("NIL");

        assertNull(list.getDate(1));
    }

    @Test
    public void getKeyedDate_returnsCorrectDateForValidString() throws MessagingException {
        ImapList list = new ImapList();
        list.add("INTERNALDATE");
        list.add("10-Mar-2000 12:02:01 GMT");

        Calendar c = Calendar.getInstance();
        c.setTime(list.getKeyedDate("INTERNALDATE"));

        assertEquals(2000, c.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, c.get(Calendar.MONTH));
        assertEquals(10, c.get(Calendar.DAY_OF_MONTH));
    }

    @Test(expected = MessagingException.class)
    public void getKeyedDate_throwsExceptionForInvalidDate() throws MessagingException {
        ImapList list = new ImapList();
        list.add("INTERNALDATE");
        list.add("InvalidDate");

        list.getKeyedDate("INTERNALDATE");
    }

    @Test
    public void getKeyedDate_returnsNullForNIL() throws MessagingException {
        ImapList list = new ImapList();
        list.add("INTERNALDATE");
        list.add("NIL");

        assertNull(list.getKeyedDate("INTERNALDATE"));
    }
}
