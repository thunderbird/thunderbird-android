package com.fsck.k9.mailstore;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.fsck.k9.activity.K9ActivityCommon;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.TextBody;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.fsck.k9.mailstore.LocalMessageExtractor.extractTextAndAttachments;
import static junit.framework.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
public class LocalMessageExtractorTest {

    @Test
    public void testSimplePlainTextMessage() throws MessagingException {
        String bodyText = "K-9 Mail rocks :>";

        // Create text/plain body
        TextBody body = new TextBody(bodyText);

        // Create message
        MimeMessage message = new MimeMessage();
        MimeMessageHelper.setBody(message, body);

        // Extract text
        ViewableContainer container = extractTextAndAttachments(InstrumentationRegistry.getTargetContext(), message);

        String expectedText = bodyText;
        String expectedHtml =
                "<pre class=\"k9mail\">" +
                "K-9 Mail rocks :&gt;" +
                "</pre>";

        assertEquals(expectedText, container.text);
        assertEquals(expectedHtml, container.html);
    }

    @Test
    public void testSimpleHtmlMessage() throws MessagingException {
        String bodyText = "<strong>K-9 Mail</strong> rocks :&gt;";

        // Create text/plain body
        TextBody body = new TextBody(bodyText);

        // Create message
        MimeMessage message = new MimeMessage();
        message.setHeader("Content-Type", "text/html");
        MimeMessageHelper.setBody(message, body);

        // Extract text
        ViewableContainer container = extractTextAndAttachments(InstrumentationRegistry.getTargetContext(), message);

        String expectedText = "K-9 Mail rocks :>";
        String expectedHtml =
                bodyText;

        assertEquals(expectedText, container.text);
        assertEquals(expectedHtml, container.html);
    }

    @Test
    public void testMultipartPlainTextMessage() throws MessagingException {
        String bodyText1 = "text body 1";
        String bodyText2 = "text body 2";

        // Create text/plain bodies
        TextBody body1 = new TextBody(bodyText1);
        TextBody body2 = new TextBody(bodyText2);

        // Create multipart/mixed part
        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart bodyPart1 = new MimeBodyPart(body1, "text/plain");
        MimeBodyPart bodyPart2 = new MimeBodyPart(body2, "text/plain");
        multipart.addBodyPart(bodyPart1);
        multipart.addBodyPart(bodyPart2);

        // Create message
        MimeMessage message = new MimeMessage();
        MimeMessageHelper.setBody(message, multipart);

        // Extract text
        ViewableContainer container = extractTextAndAttachments(InstrumentationRegistry.getTargetContext(), message);

        String expectedText =
                bodyText1 + "\r\n\r\n" +
                "------------------------------------------------------------------------\r\n\r\n" +
                bodyText2;
        String expectedHtml =
                "<pre class=\"k9mail\">" +
                bodyText1 +
                "</pre>" +
                "<p style=\"margin-top: 2.5em; margin-bottom: 1em; " +
                        "border-bottom: 1px solid #000\"></p>" +
                "<pre class=\"k9mail\">" +
                bodyText2 +
                "</pre>";


        assertEquals(expectedText, container.text);
        assertEquals(expectedHtml, container.html);
    }

    @Test
    public void testTextPlusRfc822Message() throws MessagingException {
        K9ActivityCommon.setLanguage(InstrumentationRegistry.getTargetContext(), "en");
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+01:00"));

        String bodyText = "Some text here";
        String innerBodyText = "Hey there. I'm inside a message/rfc822 (inline) attachment.";

        // Create text/plain body
        TextBody textBody = new TextBody(bodyText);

        // Create inner text/plain body
        TextBody innerBody = new TextBody(innerBodyText);

        // Create message/rfc822 body
        MimeMessage innerMessage = new MimeMessage();
        innerMessage.addSentDate(new Date(112, 02, 17), false);
        innerMessage.setRecipients(RecipientType.TO, new Address[] { new Address("to@example.com") });
        innerMessage.setSubject("Subject");
        innerMessage.setFrom(new Address("from@example.com"));
        MimeMessageHelper.setBody(innerMessage, innerBody);

        // Create multipart/mixed part
        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart bodyPart1 = new MimeBodyPart(textBody, "text/plain");
        MimeBodyPart bodyPart2 = new MimeBodyPart(innerMessage, "message/rfc822");
        bodyPart2.setHeader("Content-Disposition", "inline; filename=\"message.eml\"");
        multipart.addBodyPart(bodyPart1);
        multipart.addBodyPart(bodyPart2);

        // Create message
        MimeMessage message = new MimeMessage();
        MimeMessageHelper.setBody(message, multipart);

        // Extract text
        ViewableContainer container = extractTextAndAttachments(InstrumentationRegistry.getTargetContext(), message);

        String expectedText =
                bodyText +
                "\r\n\r\n" +
                "----- message.eml ------------------------------------------------------" +
                "\r\n\r\n" +
                "From: from@example.com" + "\r\n" +
                "To: to@example.com" + "\r\n" +
                "Sent: Sat Mar 17 00:00:00 GMT+01:00 2012" + "\r\n" +
                "Subject: Subject" + "\r\n" +
                "\r\n" +
                innerBodyText;
        String expectedHtml =
                "<pre class=\"k9mail\">" +
                bodyText +
                "</pre>" +
                "<p style=\"margin-top: 2.5em; margin-bottom: 1em; border-bottom: " +
                        "1px solid #000\">message.eml</p>" +
                "<table style=\"border: 0\">" +
                "<tr>" +
                "<th style=\"text-align: left; vertical-align: top;\">From:</th>" +
                "<td>from@example.com</td>" +
                "</tr><tr>" +
                "<th style=\"text-align: left; vertical-align: top;\">To:</th>" +
                "<td>to@example.com</td>" +
                "</tr><tr>" +
                "<th style=\"text-align: left; vertical-align: top;\">Sent:</th>" +
                "<td>Sat Mar 17 00:00:00 GMT+01:00 2012</td>" +
                "</tr><tr>" +
                "<th style=\"text-align: left; vertical-align: top;\">Subject:</th>" +
                "<td>Subject</td>" +
                "</tr>" +
                "</table>" +
                "<pre class=\"k9mail\">" +
                innerBodyText +
                "</pre>";

        assertEquals(expectedText, container.text);
        assertEquals(expectedHtml, container.html);
    }
}
