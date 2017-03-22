package com.fsck.k9.mail.store.imap;


import com.fsck.k9.mail.Flag;
import org.junit.Test;

import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.internal.util.collections.Sets.newSet;


public class PermanentFlagsResponseTest {
    @Test
    public void parse_withPermanentFlagsResponse_shouldExtractFlags() throws Exception {
        ImapResponse response = createImapResponse("* OK [PERMANENTFLAGS (\\Answered \\Flagged \\Deleted \\Seen " +
                "\\Draft $forwarded NonJunk $label1 \\*)] Flags permitted.");

        PermanentFlagsResponse result = PermanentFlagsResponse.parse(response);

        assertNotNull(result);
        assertEquals(newSet(Flag.ANSWERED, Flag.FLAGGED, Flag.DELETED, Flag.SEEN, Flag.FORWARDED), result.getFlags());
    }

    @Test
    public void parse_withPermanentFlagsResponseContainingSpecialKeyword_shouldSetCanCreateKeywords() throws Exception {
        ImapResponse response = createImapResponse("* OK [PERMANENTFLAGS (\\Deleted \\*)] Flags permitted.");

        PermanentFlagsResponse result = PermanentFlagsResponse.parse(response);

        assertNotNull(result);
        assertEquals(true, result.canCreateKeywords());
    }

    @Test
    public void parse_withPermanentFlagsResponseNotContainingSpecialKeyword_shouldNotSetCanCreateKeywords()
            throws Exception {
        ImapResponse response = createImapResponse("* OK [PERMANENTFLAGS (\\Deleted \\Seen)] Flags permitted.");

        PermanentFlagsResponse result = PermanentFlagsResponse.parse(response);

        assertNotNull(result);
        assertEquals(false, result.canCreateKeywords());
    }

    @Test
    public void parse_withTaggedResponse_shouldReturnNull() throws Exception {
        ImapResponse response = createImapResponse("1 OK [PERMANENTFLAGS (\\Deleted \\Seen)] Flags permitted.");

        PermanentFlagsResponse result = PermanentFlagsResponse.parse(response);

        assertNull(result);
    }

    @Test
    public void parse_withoutOkResponse_shouldReturnNull() throws Exception {
        ImapResponse response = createImapResponse("* BYE See you");

        PermanentFlagsResponse result = PermanentFlagsResponse.parse(response);

        assertNull(result);
    }

    @Test
    public void parse_withoutResponseText_shouldReturnNull() throws Exception {
        ImapResponse response = createImapResponse("* OK Success");

        PermanentFlagsResponse result = PermanentFlagsResponse.parse(response);

        assertNull(result);
    }

    @Test
    public void parse_withTooShortResponseText_shouldReturnNull() throws Exception {
        ImapResponse response = createImapResponse("* OK [PERMANENTFLAGS]");

        PermanentFlagsResponse result = PermanentFlagsResponse.parse(response);

        assertNull(result);
    }

    @Test
    public void parse_withoutPermanentFlagsResponse_shouldReturnNull() throws Exception {
        ImapResponse response = createImapResponse("* OK [UIDNEXT 1]");

        PermanentFlagsResponse result = PermanentFlagsResponse.parse(response);

        assertNull(result);
    }

    @Test
    public void parse_withoutPermanentFlagsList_shouldReturnNull() throws Exception {
        ImapResponse response = createImapResponse("* OK [PERMANENTFLAGS none]");

        PermanentFlagsResponse result = PermanentFlagsResponse.parse(response);

        assertNull(result);
    }

    @Test
    public void parse_withInvalidElementInPermanentFlagsList_shouldReturnNull() throws Exception {
        ImapResponse response = createImapResponse("* OK [PERMANENTFLAGS (\\Seen ())]");

        PermanentFlagsResponse result = PermanentFlagsResponse.parse(response);

        assertNull(result);
    }
}
