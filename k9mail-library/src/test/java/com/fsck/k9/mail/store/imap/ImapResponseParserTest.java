package com.fsck.k9.mail.store.imap;

import com.fsck.k9.mail.filter.PeekableInputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.fsck.k9.mail.store.imap.ImapResponseParser.parseCapabilities;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ImapResponseParserTest  {

    @Test public void testSimpleOkResponse() throws IOException {
        ImapResponseParser parser = createParser("* OK\r\n");
        ImapResponse response = parser.readResponse();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("OK", response.get(0));
    }

    @Test public void testOkResponseWithText() throws IOException {
        ImapResponseParser parser = createParser("* OK Some text here\r\n");
        ImapResponse response = parser.readResponse();

        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("OK", response.get(0));
        assertEquals("Some text here", response.get(1));
    }

    @Test public void testOkResponseWithRespTextCode() throws IOException {
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

    @Test public void testOkResponseWithRespTextCodeAndText() throws IOException {
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


    @Test public void testReadStatusResponseWithOKResponse() throws Exception {
        ImapResponseParser parser = createParser("* COMMAND BAR BAZ\r\nTAG OK COMMAND completed\r\n");
        List<ImapResponse> responses = parser.readStatusResponse("TAG", null, null, null);

        assertEquals(2, responses.size());
        assertEquals(asList("COMMAND", "BAR", "BAZ"), responses.get(0));
        assertEquals(asList("OK", "COMMAND completed"), responses.get(1));
    }

    @Test(expected = ImapException.class)
    public void testReadStatusResponseWithErrorResponse() throws Exception {
        ImapResponseParser parser = createParser("* COMMAND BAR BAZ\r\nTAG ERROR COMMAND errored\r\n");
        parser.readStatusResponse("TAG", null, null, null);
    }

    @Test public void testParseCapabilities() throws Exception {
        ImapResponse capabilityResponse = new ImapResponse(null, false, null);
        capabilityResponse.addAll(Arrays.asList("CAPABILITY", "FOO", "BAR"));
        Set<String> capabilities = parseCapabilities(Arrays.asList(capabilityResponse));

        assertEquals(2, capabilities.size());
        assertTrue(capabilities.contains("FOO"));
        assertTrue(capabilities.contains("BAR"));
    }

    @Test public void testParseCapabilitiesWithInvalidResponse() throws Exception {
        ImapResponse capabilityResponse = new ImapResponse(null, false, null);
        capabilityResponse.addAll(Arrays.asList("FOO", "BAZ"));
        assertTrue(parseCapabilities(Arrays.asList(capabilityResponse)).isEmpty());
    }

    private ImapResponseParser createParser(String response) {
        ByteArrayInputStream in = new ByteArrayInputStream(response.getBytes());
        PeekableInputStream pin = new PeekableInputStream(in);
        return new ImapResponseParser(pin);
    }
}
