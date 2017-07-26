package com.fsck.k9.mail.store.imap;


import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fsck.k9.mail.Flag;
import org.junit.Before;
import org.junit.Test;

import static com.fsck.k9.mail.Folder.OPEN_MODE_RO;
import static com.fsck.k9.mail.Folder.OPEN_MODE_RW;
import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponse;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class SelectOrExamineResponseTest {

    private static final long TEST_UIDVALIDITY = 1125022061L;
    private static final long TEST_HIGHESTMODSEQ = 345678912L;
    private static final Set<Flag> TEST_PERMANENT_FLAGS = new HashSet<>(0);

    private ImapFolder folder;

    @Before
    public void setup() throws Exception {
        ImapStore store = mock(ImapStore.class);
        when(store.getPermanentFlagsIndex()).thenReturn(TEST_PERMANENT_FLAGS);
        folder = mock(ImapFolder.class);
        when(folder.getStore()).thenReturn(store);
        when(folder.doesConnectionSupportQresync()).thenReturn(true);
    }

    @Test
    public void newInstance_withSelectResponse_shouldParseAllFieldsCorrectly() throws Exception {
        List<ImapResponse> imapResponses = createMockImapResponses("1 OK [READ-WRITE] mailbox selected");

        SelectOrExamineResponse result = SelectOrExamineResponse.newInstance(imapResponses, folder);

        assertNotNull(result);
        assertEquals(result.getUidValidity(), TEST_UIDVALIDITY);
        assertEquals(result.getHighestModSeq(), TEST_HIGHESTMODSEQ);
        assertEquals(TEST_PERMANENT_FLAGS.size(), 4);
        assertEquals(result.canCreateKeywords(), true);
        assertNotNull(result.getQresyncParamResponse());
        assertEquals(result.getQresyncParamResponse().getExpungedUids().size(), 6);
        assertEquals(result.getQresyncParamResponse().getModifiedMessages().size(), 2);
        assertEquals(true, result.hasOpenMode());
        assertEquals(OPEN_MODE_RW, result.getOpenMode());
    }

    @Test
    public void newInstance_withSelectResponse_shouldReturnOpenModeReadWrite() throws Exception {
        List<ImapResponse> imapResponses = createMockImapResponses("x OK [READ-WRITE] Select completed.");

        SelectOrExamineResponse result = SelectOrExamineResponse.newInstance(imapResponses, folder);

        assertNotNull(result);
        assertEquals(true, result.hasOpenMode());
        assertEquals(OPEN_MODE_RW, result.getOpenMode());
    }

    @Test
    public void newInstance_withExamineResponse_shouldReturnOpenModeReadOnly() throws Exception {
        List<ImapResponse> imapResponses = createMockImapResponses("x OK [READ-ONLY] Examine completed.");

        SelectOrExamineResponse result = SelectOrExamineResponse.newInstance(imapResponses, folder);

        assertNotNull(result);
        assertEquals(true, result.hasOpenMode());
        assertEquals(OPEN_MODE_RO, result.getOpenMode());
    }

    @Test
    public void newInstance_withoutResponseCode_shouldReturnHasOpenModeFalse() throws Exception {
        List<ImapResponse> imapResponses = createMockImapResponses("x OK Select completed.");

        SelectOrExamineResponse result = SelectOrExamineResponse.newInstance(imapResponses, folder);

        assertNotNull(result);
        assertEquals(false, result.hasOpenMode());
    }

    @Test
    public void getOpenMode_withoutResponseCode_shouldThrow() throws Exception {
        List<ImapResponse> imapResponses = createMockImapResponses("x OK Select completed.");

        SelectOrExamineResponse result = SelectOrExamineResponse.newInstance(imapResponses, folder);

        assertNotNull(result);
        try {
            result.getOpenMode();
            fail("Expected exception");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void newInstance_withInvalidResponseText_shouldReturnHasOpenModeFalse() throws Exception {
        List<ImapResponse> imapResponses = createMockImapResponses("x OK [()] Examine completed.");

        SelectOrExamineResponse result = SelectOrExamineResponse.newInstance(imapResponses, folder);

        assertNotNull(result);
        assertEquals(false, result.hasOpenMode());
    }

    @Test
    public void newInstance_withUnknownResponseText_shouldReturnHasOpenModeFalse() throws Exception {
        List<ImapResponse> imapResponses = createMockImapResponses("x OK [FUNKY] Examine completed.");

        SelectOrExamineResponse result = SelectOrExamineResponse.newInstance(imapResponses, folder);

        assertNotNull(result);
        assertEquals(false, result.hasOpenMode());
    }

    @Test
    public void newInstance_withUntaggedResponse_shouldReturnNull() throws Exception {
        List<ImapResponse> imapResponses = createMockImapResponses("* OK [READ-WRITE] Select completed.");

        SelectOrExamineResponse result = SelectOrExamineResponse.newInstance(imapResponses, folder);

        assertNull(result);
    }

    @Test
    public void newInstance_withoutOkResponse_shouldReturnNull() throws Exception {
        List<ImapResponse> imapResponses = createMockImapResponses("x BYE");

        SelectOrExamineResponse result = SelectOrExamineResponse.newInstance(imapResponses, folder);

        assertNull(result);
    }

    private static List<ImapResponse> createMockImapResponses(String lastLine) throws IOException {
        return asList(
                createImapResponse("* 23 EXISTS"),
                createImapResponse("* 0 RECENT"),
                createImapResponse("* OK [UIDVALIDITY " + TEST_UIDVALIDITY + "] UIDs valid"),
                createImapResponse("* OK [UIDNEXT 57576] Predicted next UID"),
                createImapResponse("* OK [HIGHESTMODSEQ " + TEST_HIGHESTMODSEQ + "] Highest mailbox mod-sequence"),
                createImapResponse("* FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft NonJunk $MDNSent)"),
                createImapResponse("* OK [PERMANENTFLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft NonJunk " +
                        "$MDNSent \\*)] Flags permitted."),
                createImapResponse("* VANISHED (EARLIER) 41,200,230:233"),
                createImapResponse("* 49 FETCH (UID 117 FLAGS (\\Seen \\Answered) MODSEQ (90060115194045001))"),
                createImapResponse("* 50 FETCH (UID 119 FLAGS (\\Draft $MDNSent) MODSEQ (90060115194045308))"),
                createImapResponse(lastLine)
        );
    }
}
