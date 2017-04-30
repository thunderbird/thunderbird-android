package com.fsck.k9.mail.store.imap;


import com.fsck.k9.mail.K9LibRobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


@RunWith(K9LibRobolectricTestRunner.class)
public class CopyUidResponseTest {
    @Test
    public void parse_withCopyUidResponse_shouldCreateUidMapping() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK [COPYUID 1 1,3:5 7:10] Success");

        CopyUidResponse result = CopyUidResponse.parse(imapResponse);

        assertNotNull(result);
        assertEquals(createUidMapping("1=7", "3=8", "4=9", "5=10"), result.getUidMapping());
    }

    @Test
    public void parse_withUntaggedResponse_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("* OK [COPYUID 1 1,3:5 7:10] Success");

        CopyUidResponse result = CopyUidResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withTooShortResponse_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK");

        CopyUidResponse result = CopyUidResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withoutOkResponse_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x BYE Logout");

        CopyUidResponse result = CopyUidResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withoutResponseTextList_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK Success");

        CopyUidResponse result = CopyUidResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withResponseTextListTooShort_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK [A B C] Success");

        CopyUidResponse result = CopyUidResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withoutCopyUidResponse_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK [A B C D] Success");

        CopyUidResponse result = CopyUidResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withNonStringCopyUidArgumentOne_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK [COPYUID () C D] Success");

        CopyUidResponse result = CopyUidResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withNonStringCopyUidArgumentTwo_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK [COPYUID B () D] Success");

        CopyUidResponse result = CopyUidResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withNonStringCopyUidArgumentThree_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK [COPYUID B C ()] Success");

        CopyUidResponse result = CopyUidResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withNonNumberCopyUidArguments_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK [COPYUID B C D] Success");

        CopyUidResponse result = CopyUidResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withUnbalancedCopyUidArguments_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x OK [COPYUID B 1 1,2] Success");

        CopyUidResponse result = CopyUidResponse.parse(imapResponse);

        assertNull(result);
    }

    private Map<String, String> createUidMapping(String... values) {
        Map<String, String> mapping = new HashMap<>(values.length);

        for (String value : values) {
            String[] parts = value.split("=");
            String oldUid = parts[0];
            String newUid = parts[1];
            mapping.put(oldUid, newUid);
        }

        return mapping;
    }
}
