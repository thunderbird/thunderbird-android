package com.fsck.k9.mail.store.imap.selectedstate.response;


import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fsck.k9.mail.K9LibRobolectricTestRunner;
import com.fsck.k9.mail.store.imap.ImapResponse;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createMultipleImapResponses;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


@RunWith(K9LibRobolectricTestRunner.class)
public class UidCopyResponseTest {
    @Test
    public void parse_withCopyUidResponse_shouldCreateUidMapping() throws Exception {
        List<List<ImapResponse>> imapResponse = createImapResponse("x OK [COPYUID 1 1,3:5 7:10] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponse);

        assertNotNull(result);
        assertEquals(createUidMapping("1=7", "3=8", "4=9", "5=10"), result.getUidMapping());
    }

    @Test
    public void parse_withUntaggedResponse_shouldReturnNull() throws Exception {
        List<List<ImapResponse>> imapResponse = createImapResponse("* OK [COPYUID 1 1,3:5 7:10] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withTooShortResponse_shouldReturnNull() throws Exception {
        List<List<ImapResponse>> imapResponse = createImapResponse("x OK");

        UidCopyResponse result = UidCopyResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withoutOkResponse_shouldReturnNull() throws Exception {
        List<List<ImapResponse>> imapResponse = createImapResponse("x BYE Logout");

        UidCopyResponse result = UidCopyResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withoutResponseTextList_shouldReturnNull() throws Exception {
        List<List<ImapResponse>> imapResponse = createImapResponse("x OK Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withResponseTextListTooShort_shouldReturnNull() throws Exception {
        List<List<ImapResponse>> imapResponse = createImapResponse("x OK [A B C] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withoutCopyUidResponse_shouldReturnNull() throws Exception {
        List<List<ImapResponse>> imapResponse = createImapResponse("x OK [A B C D] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withNonStringCopyUidArgumentOne_shouldReturnNull() throws Exception {
        List<List<ImapResponse>> imapResponse = createImapResponse("x OK [COPYUID () C D] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withNonStringCopyUidArgumentTwo_shouldReturnNull() throws Exception {
        List<List<ImapResponse>> imapResponse = createImapResponse("x OK [COPYUID B () D] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withNonStringCopyUidArgumentThree_shouldReturnNull() throws Exception {
        List<List<ImapResponse>> imapResponse = createImapResponse("x OK [COPYUID B C ()] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withNonNumberCopyUidArguments_shouldReturnNull() throws Exception {
        List<List<ImapResponse>> imapResponse = createImapResponse("x OK [COPYUID B C D] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponse);

        assertNull(result);
    }

    @Test
    public void parse_withUnbalancedCopyUidArguments_shouldReturnNull() throws Exception {
        List<List<ImapResponse>> imapResponse = createImapResponse("x OK [COPYUID B 1 1,2] Success");

        UidCopyResponse result = UidCopyResponse.parse(imapResponse);

        assertNull(result);
    }

    private List<List<ImapResponse>> createImapResponse(String response) throws IOException {
        return createMultipleImapResponses(Collections.singletonList(response));
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
