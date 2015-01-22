package com.fsck.k9.mailstore;


import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.TextBody;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
public class MessageInfoExtractorTest {

    @Test
    public void shouldExtractPreviewFromSinglePlainTextPart() throws MessagingException {
        MimeMessage message = new MimeMessage();
        message.addHeader("Content-Type", "text/plain");
        TextBody body = new TextBody("Message text ");
        message.setBody(body);

        String preview = new MessageInfoExtractor(getContext(), message).getMessageTextPreview();

        assertEquals("Message text", preview);
    }

    @Test
    public void shouldLimitPreviewTo512Characters() throws MessagingException {
        MimeMessage message = new MimeMessage();
        message.addHeader("Content-Type", "text/plain");
        TextBody body = new TextBody("10--------20--------30--------40--------50--------" +
                "60--------70--------80--------90--------100-------" +
                "110-------120-------130-------140-------150-------" +
                "160-------170-------180-------190-------200-------" +
                "210-------220-------230-------240-------250-------" +
                "260-------270-------280-------290-------300-------" +
                "310-------320-------330-------340-------350-------" +
                "360-------370-------380-------390-------400-------" +
                "410-------420-------430-------440-------450-------" +
                "460-------470-------480-------490-------500-------" +
                "510-------520-------530-------540-------550-------" +
                "560-------570-------580-------590-------600-------");
        message.setBody(body);

        String preview = new MessageInfoExtractor(getContext(), message).getMessageTextPreview();

        assertEquals(512, preview.length());
        assertEquals('…', preview.charAt(511));
    }

    @Test
    public void shouldExtractPreviewFromSingleHtmlPart() throws MessagingException {
        MimeMessage message = new MimeMessage();
        message.addHeader("Content-Type", "text/html");
        TextBody body = new TextBody("<html><body><pre>Message text</pre></body></html>");
        message.setBody(body);

        String preview = new MessageInfoExtractor(getContext(), message).getMessageTextPreview();

        assertEquals("Message text", preview);
    }

    @Test
    public void shouldExtractPreviewFromMultipartAlternative() throws MessagingException {
        MimeMessage message = new MimeMessage();
        message.addHeader("Content-Type", "multipart/alternative");
        MimeMultipart multipart = new MimeMultipart();
        multipart.setSubType("alternative");
        message.setBody(multipart);

        TextBody textBody = new TextBody("text");
        MimeBodyPart textPart = new MimeBodyPart(textBody, "text/plain");
        multipart.addBodyPart(textPart);

        TextBody htmlBody = new TextBody("<html><body>html</body></html>");
        MimeBodyPart htmlPart = new MimeBodyPart(htmlBody, "text/html");
        multipart.addBodyPart(htmlPart);

        String preview = new MessageInfoExtractor(getContext(), message).getMessageTextPreview();

        assertEquals("text", preview);
    }

    @Test
    public void shouldExtractPreviewFromMultipartMixed() throws MessagingException {
        MimeMessage message = new MimeMessage();
        message.addHeader("Content-Type", "multipart/mixed");
        MimeMultipart multipart = new MimeMultipart();
        multipart.setSubType("mixed");
        message.setBody(multipart);

        TextBody textBody = new TextBody("text");
        MimeBodyPart textPart = new MimeBodyPart(textBody, "text/plain");
        multipart.addBodyPart(textPart);

        TextBody htmlBody = new TextBody("<html><body>html</body></html>");
        MimeBodyPart htmlPart = new MimeBodyPart(htmlBody, "text/html");
        multipart.addBodyPart(htmlPart);

        String preview = new MessageInfoExtractor(getContext(), message).getMessageTextPreview();

        assertEquals("text / html", preview);
    }

    @Test
    public void shouldExtractPreviewFromMultipartMixedWithInnerMesssage() throws MessagingException {
        MimeMessage message = new MimeMessage();
        message.addHeader("Content-Type", "multipart/mixed");
        MimeMultipart multipart = new MimeMultipart();
        multipart.setSubType("mixed");
        message.setBody(multipart);

        TextBody textBody = new TextBody("text");
        MimeBodyPart textPart = new MimeBodyPart(textBody, "text/plain");
        multipart.addBodyPart(textPart);

        MimeMessage innerMessage = new MimeMessage();
        innerMessage.addHeader("Content-Type", "text/html");
        innerMessage.addHeader("Subject", "inner message");
        TextBody htmlBody = new TextBody("<html><body>ht&#109;l</body></html>");
        innerMessage.setBody(htmlBody);

        MimeBodyPart messagePart = new MimeBodyPart(innerMessage, "message/rfc822");
        multipart.addBodyPart(messagePart);

        String preview = new MessageInfoExtractor(getContext(), message).getMessageTextPreview();

        assertEquals("text / Includes message titled \"inner message\" containing: html", preview);
    }

    private Context getContext() {
        return InstrumentationRegistry.getTargetContext();
    }
}
