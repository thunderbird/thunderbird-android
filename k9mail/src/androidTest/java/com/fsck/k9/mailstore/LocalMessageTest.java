package com.fsck.k9.mailstore;


import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMultipart;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
public class LocalMessageTest {
    private LocalMessage message;
    private Account account;
    private Preferences preferences;

    @Before
    public void setUp() throws Exception {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        preferences = Preferences.getPreferences(targetContext);
        account = preferences.newAccount();
        LocalStore store = LocalStore.getInstance(account, targetContext);
        message = new LocalMessage(store, "uid", new LocalFolder(store, "test"));
    }

    @After
    public void tearDown() throws Exception {
        preferences.deleteAccount(account);
    }

    @Test
    public void testGetDisplayTextWithPlainTextPart() throws Exception {
        String textBodyText = "text body";

        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart bodyPart1 = new MimeBodyPart(new LocalTextBody(textBodyText, textBodyText), "text/plain");
        multipart.addBodyPart(bodyPart1);
        message.setBody(multipart);
        assertEquals("text body", message.getTextForDisplay());
    }

    @Test
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
