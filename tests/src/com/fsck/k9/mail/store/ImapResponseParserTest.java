package com.fsck.k9.mail.store;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import com.fsck.k9.mail.filter.PeekableInputStream;
import com.fsck.k9.mail.store.ImapResponseParser.ImapList;
import com.fsck.k9.mail.store.ImapResponseParser.ImapResponse;
import junit.framework.TestCase;

public class ImapResponseParserTest extends TestCase {

    public void testSimpleOkResponse() throws IOException {
        ImapResponseParser parser = createParser("* OK\r\n");
        ImapResponse response = parser.readResponse();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("OK", response.get(0));
    }

    public void testOkResponseWithText() throws IOException {
        ImapResponseParser parser = createParser("* OK Some text here\r\n");
        ImapResponse response = parser.readResponse();

        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("OK", response.get(0));
        assertEquals("Some text here", response.get(1));
    }

    public void testOkResponseWithRespTextCode() throws IOException {
        ImapResponseParser parser = createParser("* OK [UIDVALIDITY 3857529045]\r\n");
        ImapResponse response = parser.readResponse();

        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("OK", response.get(0));
        assertTrue(response.get(1) instanceof ImapList);

        ImapList respTextCode = (ImapList) response.get(1);
        assertEquals(2, respTextCode.size());
        assertEquals("UIDVALIDITY", respTextCode.get(0));
        assertEquals("3857529045", respTextCode.get(1));
    }

    public void testOkResponseWithRespTextCodeAndText() throws IOException {
        ImapResponseParser parser = createParser("* OK [token1 token2] {x} test [...]\r\n");
        ImapResponse response = parser.readResponse();

        assertNotNull(response);
        assertEquals(3, response.size());
        assertEquals("OK", response.get(0));
        assertTrue(response.get(1) instanceof ImapList);
        assertEquals("{x} test [...]", response.get(2));

        ImapList respTextCode = (ImapList) response.get(1);
        assertEquals(2, respTextCode.size());
        assertEquals("token1", respTextCode.get(0));
        assertEquals("token2", respTextCode.get(1));
    }

    public void testImapListMethods() throws IOException {
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

    private ImapResponseParser createParser(String response) {
        ByteArrayInputStream in = new ByteArrayInputStream(response.getBytes());
        PeekableInputStream pin = new PeekableInputStream(in);
        return new ImapResponseParser(pin);
    }
}
