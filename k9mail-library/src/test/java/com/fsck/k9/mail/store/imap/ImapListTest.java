package com.fsck.k9.mail.store.imap;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ImapListTest {
    @Test public void testImapListMethods() throws IOException {
        ImapList list = new ImapList();
        list.add("ONE");
        list.add("TWO");
        list.add("THREE");

        assertTrue(list.containsKey("ONE"));
        assertTrue(list.containsKey("TWO"));
        assertFalse(list.containsKey("THREE"));
        assertFalse(list.containsKey("nonexistent"));

        assertEquals("TWO", list.getKeyedValue("ONE"));
        assertEquals("THREE", list.getKeyedValue("TWO"));
        assertNull(list.getKeyedValue("THREE"));
        assertNull(list.getKeyedValue("nonexistent"));

        assertEquals(0, list.getKeyIndex("ONE"));
        assertEquals(1, list.getKeyIndex("TWO"));

        try {
            list.getKeyIndex("THREE");
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) { /* do nothing */ }

        try {
            list.getKeyIndex("nonexistent");
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) { /* do nothing */ }
    }
}
