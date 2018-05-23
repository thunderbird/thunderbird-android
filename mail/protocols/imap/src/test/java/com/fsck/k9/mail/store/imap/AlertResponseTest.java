package com.fsck.k9.mail.store.imap;


import org.junit.Test;

import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class AlertResponseTest {

    @Test
    public void getAlertText_withProperAlertResponse() throws Exception {
        ImapResponse imapResponse = createImapResponse("x NO [ALERT] Please don't do that");

        String result = AlertResponse.getAlertText(imapResponse);

        assertEquals("Please don't do that", result);
    }

    @Test
    public void getAlertText_withoutResponseCodeText_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x NO");

        String result = AlertResponse.getAlertText(imapResponse);

        assertNull(result);
    }

    @Test
    public void getAlertText_withoutAlertText_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x NO [ALERT]");

        String result = AlertResponse.getAlertText(imapResponse);

        assertNull(result);
    }

    @Test
    public void getAlertText_withoutResponseCodeTextList_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x NO ALERT ALARM!");

        String result = AlertResponse.getAlertText(imapResponse);

        assertNull(result);
    }

    @Test
    public void getAlertText_withResponseCodeTextContainingTooManyItems_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x NO [ALERT SOMETHING] ALARM!");

        String result = AlertResponse.getAlertText(imapResponse);

        assertNull(result);
    }

    @Test
    public void getAlertText_withWrongResponseCodeText_shouldReturnNull() throws Exception {
        ImapResponse imapResponse = createImapResponse("x NO [ALARM] ALERT!");

        String result = AlertResponse.getAlertText(imapResponse);

        assertNull(result);
    }
}
