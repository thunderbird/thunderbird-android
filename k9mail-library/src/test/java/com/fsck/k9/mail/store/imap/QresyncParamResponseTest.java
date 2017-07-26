package com.fsck.k9.mail.store.imap;


import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fsck.k9.mail.Flag;
import org.junit.Test;

import static com.fsck.k9.mail.Folder.OPEN_MODE_RW;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;


public class QresyncParamResponseTest {

    @Test
    public void fromSelectOrExamineResponse_shouldParseResponseCorrectly() throws Exception {
        List<String> expungedUids = asList("3", "4");
        Map<String, Set<Flag>> modifiedMessages = singletonMap("15", singleton(Flag.SEEN));
        List<ImapResponse> selectOrExamineResponse = ImapResponseHelper.createFolderOpenedResponse(OPEN_MODE_RW, 1L, 2L,
                expungedUids, modifiedMessages);

        QresyncParamResponse response = QresyncParamResponse.fromSelectOrExamineResponse(selectOrExamineResponse,
                mock(ImapFolder.class));

        assertNotNull(response);
        assertEquals(response.getExpungedUids(), expungedUids);
        ImapMessage modifiedMessage = response.getModifiedMessages().get(0);
        assertEquals(modifiedMessage.getUid(), "15");
        assertEquals(modifiedMessage.getFlags(), singleton(Flag.SEEN));
    }
}
