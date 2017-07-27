package com.fsck.k9.mail.store.imap;


import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    }

    @Test
    public void newInstance_withSelectResponse_shouldParseAllFieldsCorrectly() throws Exception {
        List<ImapResponse> imapResponses = createMockImapResponses("1 OK [READ-WRITE] mailbox selected");
        when(folder.doesConnectionSupportQresync()).thenReturn(true);

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

    private List<ImapResponse> createMockImapResponses(String lastLine) throws IOException {
        List<String> expungedUids = asList("41", "200", "230", "252", "255", "257");
        Map<String, Set<Flag>> modifiedMessages = new HashMap<>(2);
        modifiedMessages.put("117", new HashSet<>(asList(Flag.SEEN, Flag.ANSWERED)));
        modifiedMessages.put("119", Collections.singleton(Flag.DRAFT));
        List<ImapResponse> responses = ImapResponseHelper.createFolderOpenedResponse(OPEN_MODE_RW, TEST_UIDVALIDITY,
                TEST_HIGHESTMODSEQ, expungedUids, modifiedMessages);
        responses.remove(responses.size() - 1);
        responses.add(createImapResponse(lastLine));
        return responses;
    }
}
