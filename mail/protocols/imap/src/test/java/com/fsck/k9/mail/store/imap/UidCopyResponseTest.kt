package com.fsck.k9.mail.store.imap;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponseList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class UidCopyResponseTest {
    @Test
    public void parse_withCopyUidResponse_shouldCreateUidMapping() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList("x OK [COPYUID 1 1,3:5 7:10] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponses);

        assertNotNull(result);
        assertEquals(createUidMapping("1=7", "3=8", "4=9", "5=10"), result.getUidMapping());
    }

    @Test
    public void parse_withUntaggedResponse_shouldReturnNull() throws Exception {
        List<ImapResponse> imapResponse = createImapResponseList("* OK [COPYUID 1 1,3:5 7:10] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withTooShortResponse_shouldReturnNull() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList("x OK");

        UidCopyResponse result = UidCopyResponse.parse(imapResponses);

        assertNull(result);
    }

    @Test
    public void parse_withoutOkResponse_shouldReturnNull() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList("x BYE Logout");

        UidCopyResponse result = UidCopyResponse.parse(imapResponses);

        assertNull(result);
    }

    @Test
    public void parse_withoutResponseTextList_shouldReturnNull() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList("x OK Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponses);

        assertNull(result);
    }

    @Test
    public void parse_withResponseTextListTooShort_shouldReturnNull() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList("x OK [A B C] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponses);

        assertNull(result);
    }

    @Test
    public void parse_withoutCopyUidResponse_shouldReturnNull() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList("x OK [A B C D] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponses);

        assertNull(result);
    }

    @Test
    public void parse_withNonStringCopyUidArgumentOne_shouldReturnNull() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList("x OK [COPYUID () C D] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponses);

        assertNull(result);
    }

    @Test
    public void parse_withNonStringCopyUidArgumentTwo_shouldReturnNull() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList("x OK [COPYUID B () D] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponses);

        assertNull(result);
    }

    @Test
    public void parse_withNonStringCopyUidArgumentThree_shouldReturnNull() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList("x OK [COPYUID B C ()] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponses);

        assertNull(result);
    }

    @Test
    public void parse_withNonNumberCopyUidArguments_shouldReturnNull() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList("x OK [COPYUID B C D] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponses);

        assertNull(result);
    }

    @Test
    public void parse_withUnbalancedCopyUidArguments_shouldReturnNull() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList("x OK [COPYUID B 1 1,2] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponses);

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
