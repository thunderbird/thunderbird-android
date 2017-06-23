package com.fsck.k9.mail.store.imap;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class CapabilityResponseTest {

    @Test
    public void parse_withProperResponseContainingCapabilityCode() throws Exception {
        CapabilityResponse result = parse("* OK [CAPABILITY IMAP4rev1 IDLE] Welcome");

        assertNotNull(result);
        assertEquals(Sets.newSet("IMAP4REV1", "IDLE"), result.getCapabilities());
    }

    @Test
    public void parse_withTaggedResponse_shouldReturnNull() throws Exception {
        CapabilityResponse result = parse("1 OK");

        assertNull(result);
    }

    @Test
    public void parse_withoutOkResponse_shouldReturnNull() throws Exception {
        CapabilityResponse result = parse("* BAD Go Away");

        assertNull(result);
    }

    @Test
    public void parse_withOkResponseWithoutList_shouldReturnNull() throws Exception {
        CapabilityResponse result = parse("* OK Welcome");

        assertNull(result);
    }

    @Test
    public void parse_withProperCapabilityResponse() throws Exception {
        ImapList list = createImapResponse("* CAPABILITY IMAP4rev1 STARTTLS AUTH=GSSAPI XPIG-LATIN");

        CapabilityResponse result = CapabilityResponse.parse(list);

        assertNotNull(result);
        assertEquals(Sets.newSet("IMAP4REV1", "STARTTLS", "AUTH=GSSAPI", "XPIG-LATIN"), result.getCapabilities());
    }

    @Test
    public void parse_withListInCapabilityResponse_shouldReturnNull() throws Exception {
        ImapList list = createImapResponse("* CAPABILITY IMAP4rev1 []");

        CapabilityResponse result = CapabilityResponse.parse(list);

        assertNull(result);
    }

    @Test
    public void parse_withoutCapabilityResponse_shouldReturnNull() throws Exception {
        ImapList list = createImapResponse("* EXISTS 1");

        CapabilityResponse result = CapabilityResponse.parse(list);

        assertNull(result);
    }

    @Test
    public void parse_withEmptyResponseList_shouldReturnNull() throws Exception {
        List<ImapResponse> responses = Collections.emptyList();

        CapabilityResponse result = CapabilityResponse.parse(responses);

        assertNull(result);
    }

    @Test
    public void parse_withoutCapabilityResponseInResponseList_shouldReturnNull() throws Exception {
        List<ImapResponse> responses = Collections.singletonList(createImapResponse("* EXISTS 42"));

        CapabilityResponse result = CapabilityResponse.parse(responses);

        assertNull(result);
    }

    @Test
    public void parse_withSingleCapabilityResponseInResponseList() throws Exception {
        ImapResponse response = createImapResponse("* CAPABILITY IMAP4rev1 LOGINDISABLED STARTTLS");
        List<ImapResponse> responses = Collections.singletonList(response);

        CapabilityResponse result = CapabilityResponse.parse(responses);

        assertNotNull(result);
        assertEquals(Sets.newSet("IMAP4REV1", "STARTTLS", "LOGINDISABLED"), result.getCapabilities());
    }

    @Test
    public void parse_withCapabilityResponseInResponseList() throws Exception {
        ImapResponse responseOne = createImapResponse("* EXPUNGE 4");
        ImapResponse responseTwo = createImapResponse("* CAPABILITY IMAP4rev1 IDLE");
        List<ImapResponse> responses = Arrays.asList(responseOne, responseTwo);

        CapabilityResponse result = CapabilityResponse.parse(responses);

        assertNotNull(result);
        assertEquals(Sets.newSet("IMAP4REV1", "IDLE"), result.getCapabilities());
    }

    private CapabilityResponse parse(String responseText) throws IOException {
        ImapResponse response = createImapResponse(responseText);
        List<ImapResponse> responses = Collections.singletonList(response);

        return CapabilityResponse.parse(responses);
    }
}
