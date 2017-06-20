package com.fsck.k9.mail.store.imap.response;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fsck.k9.mail.store.imap.ImapResponse;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class CapabilityResponseTest {

    @Test
    public void parse_withProperResponseContainingCapabilityCode() throws Exception {
        ImapResponse response = createImapResponse("* OK [CAPABILITY IMAP4rev1 IDLE] Welcome");

        CapabilityResponse result = CapabilityResponse.parse(null, Collections.singletonList(response));

        assertNotNull(result);
        assertEquals(Sets.newSet("IMAP4REV1", "IDLE"), result.getCapabilities());
    }

    @Test
    public void parse_withTaggedResponse_shouldReturnNull() throws Exception {
        ImapResponse response = createImapResponse("1 OK");

        CapabilityResponse result = CapabilityResponse.parse(null, Collections.singletonList(response));

        assertNull(result);
    }

    @Test
    public void parse_withoutOkResponse_shouldReturnNull() throws Exception {
        ImapResponse response = createImapResponse("* BAD Go Away");

        CapabilityResponse result = CapabilityResponse.parse(null, Collections.singletonList(response));

        assertNull(result);
    }

    @Test
    public void parse_withOkResponseWithoutList_shouldReturnNull() throws Exception {
        ImapResponse response = createImapResponse("* OK Welcome");

        CapabilityResponse result = CapabilityResponse.parse(null, Collections.singletonList(response));

        assertNull(result);
    }

    @Test
    public void parse_withProperCapabilityResponse() throws Exception {
        ImapResponse response = createImapResponse("* CAPABILITY IMAP4rev1 STARTTLS AUTH=GSSAPI XPIG-LATIN");

        CapabilityResponse result = CapabilityResponse.parse(null, Collections.singletonList(response));

        assertNotNull(result);
        assertEquals(Sets.newSet("IMAP4REV1", "STARTTLS", "AUTH=GSSAPI", "XPIG-LATIN"), result.getCapabilities());
    }

    @Test
    public void parse_withListInCapabilityResponse_shouldReturnNull() throws Exception {
        ImapResponse response = createImapResponse("* CAPABILITY IMAP4rev1 []");

        CapabilityResponse result = CapabilityResponse.parse(null, Collections.singletonList(response));

        assertNull(result);
    }

    @Test
    public void parse_withoutCapabilityResponse_shouldReturnNull() throws Exception {
        ImapResponse response = createImapResponse("* EXISTS 1");

        CapabilityResponse result = CapabilityResponse.parse(null, Collections.singletonList(response));

        assertNull(result);
    }

    @Test
    public void parse_withEmptyResponseList_shouldReturnNull() throws Exception {
        List<ImapResponse> responses = Collections.emptyList();

        CapabilityResponse result = CapabilityResponse.parse(null, responses);

        assertNull(result);
    }

    @Test
    public void parse_withoutCapabilityResponseInResponseList_shouldReturnNull() throws Exception {
        List<ImapResponse> responses = Collections.singletonList(createImapResponse("* EXISTS 42"));

        CapabilityResponse result = CapabilityResponse.parse(null, responses);

        assertNull(result);
    }

    @Test
    public void parse_withSingleCapabilityResponseInResponseList() throws Exception {
        ImapResponse response = createImapResponse("* CAPABILITY IMAP4rev1 LOGINDISABLED STARTTLS");
        List<ImapResponse> responses = Collections.singletonList(response);

        CapabilityResponse result = CapabilityResponse.parse(null, responses);

        assertNotNull(result);
        assertEquals(Sets.newSet("IMAP4REV1", "STARTTLS", "LOGINDISABLED"), result.getCapabilities());
    }

    @Test
    public void parse_withCapabilityResponseInResponseList() throws Exception {
        ImapResponse responseOne = createImapResponse("* EXPUNGE 4");
        ImapResponse responseTwo = createImapResponse("* CAPABILITY IMAP4rev1 IDLE");
        List<ImapResponse> responses = Arrays.asList(responseOne, responseTwo);

        CapabilityResponse result = CapabilityResponse.parse(null, responses);

        assertNotNull(result);
        assertEquals(Sets.newSet("IMAP4REV1", "IDLE"), result.getCapabilities());
    }

}
