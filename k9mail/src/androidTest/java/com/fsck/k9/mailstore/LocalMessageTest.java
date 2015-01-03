package com.fsck.k9.mailstore;


import android.test.AndroidTestCase;

import com.fsck.k9.Preferences;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMultipart;


public class LocalMessageTest extends AndroidTestCase {
    private LocalMessage message;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Preferences preferences = Preferences.getPreferences(getContext());
        LocalStore store = LocalStore.getInstance(preferences.newAccount(), getContext());
        message = new LocalMessage(store, "uid", new LocalFolder(store, "test"));
    }

    public void testGetDisplayTextWithPlainTextPart() throws Exception {
        String textBodyText = "text body";

        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart bodyPart1 = new MimeBodyPart(new LocalTextBody(textBodyText, textBodyText), "text/plain");
        multipart.addBodyPart(bodyPart1);
        message.setBody(multipart);
        assertEquals("text body", message.getTextForDisplay());
    }

    public void testGetDisplayTextWithHtmlPart() throws Exception {
        String htmlBodyText = "html body";
        String textBodyText = "text body";

        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart bodyPart1 = new MimeBodyPart(new LocalTextBody(htmlBodyText, htmlBodyText), "text/html");
        MimeBodyPart bodyPart2 = new MimeBodyPart(new LocalTextBody(textBodyText, textBodyText), "text/plain");
        multipart.addBodyPart(bodyPart1);
        multipart.addBodyPart(bodyPart2);
        message.setBody(multipart);
        assertEquals("html body", message.getTextForDisplay());
    }
}
