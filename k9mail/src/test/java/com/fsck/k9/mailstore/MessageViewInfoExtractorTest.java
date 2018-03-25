package com.fsck.k9.mailstore;


import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Application;
import android.support.annotation.NonNull;

import com.fsck.k9.GlobalsHelper;
import com.fsck.k9.activity.K9ActivityCommon;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mail.internet.Viewable;
import com.fsck.k9.mail.internet.Viewable.MessageHeader;
import com.fsck.k9.mailstore.CryptoResultAnnotation.CryptoError;
import com.fsck.k9.mailstore.MessageViewInfoExtractor.ViewableExtractedText;
import com.fsck.k9.message.extractors.AttachmentInfoExtractor;
import com.fsck.k9.message.html.HtmlProcessor;
import com.fsck.k9.ui.crypto.MessageCryptoAnnotations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.fsck.k9.message.TestMessageConstructionUtils.bodypart;
import static com.fsck.k9.message.TestMessageConstructionUtils.messageFromBody;
import static com.fsck.k9.message.TestMessageConstructionUtils.multipart;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


@SuppressWarnings("WeakerAccess")
@RunWith(RobolectricTestRunner.class)
public class MessageViewInfoExtractorTest {
    public static final String BODY_TEXT = "K-9 Mail rocks :>";
    public static final String BODY_TEXT_HTML = "K-9 Mail rocks :&gt;";
    public static final String BODY_TEXT_FLOWED = "K-9 Mail rocks :> \r\nflowed line\r\nnot flowed line";


    private MessageViewInfoExtractor messageViewInfoExtractor;
    private Application context;
    private AttachmentInfoExtractor attachmentInfoExtractor;


    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.application;

        GlobalsHelper.setContext(context);

        HtmlProcessor htmlProcessor = createFakeHtmlProcessor();
        attachmentInfoExtractor = spy(AttachmentInfoExtractor.getInstance());
        messageViewInfoExtractor = new MessageViewInfoExtractor(context, attachmentInfoExtractor, htmlProcessor);
    }

    @Test
    public void testShouldSanitizeOutputHtml() throws MessagingException {
        // Create text/plain body
        TextBody body = new TextBody(BODY_TEXT);

        // Create message
        MimeMessage message = new MimeMessage();
        MimeMessageHelper.setBody(message, body);
        message.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/plain; format=flowed");

        // Prepare fixture
        HtmlProcessor htmlProcessor = mock(HtmlProcessor.class);
        MessageViewInfoExtractor messageViewInfoExtractor =
                new MessageViewInfoExtractor(context, null, htmlProcessor);
        String value = "--sanitized html--";
        when(htmlProcessor.processForDisplay(anyString())).thenReturn(value);

        // Extract text
        List<Part> outputNonViewableParts = new ArrayList<>();
        ArrayList<Viewable> outputViewableParts = new ArrayList<>();
        MessageExtractor.findViewablesAndAttachments(message, outputViewableParts, outputNonViewableParts);
        ViewableExtractedText viewableExtractedText =
                messageViewInfoExtractor.extractTextFromViewables(outputViewableParts);

        assertSame(value, viewableExtractedText.html);
    }

    @Test
    public void testSimplePlainTextMessage() throws MessagingException {
        // Create text/plain body
        TextBody body = new TextBody(BODY_TEXT);

        // Create message
        MimeMessage message = new MimeMessage();
        message.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/plain");
        MimeMessageHelper.setBody(message, body);

        // Extract text
        List<Part> outputNonViewableParts = new ArrayList<>();
        ArrayList<Viewable> outputViewableParts = new ArrayList<>();
        MessageExtractor.findViewablesAndAttachments(message, outputViewableParts, outputNonViewableParts);
        ViewableExtractedText container = messageViewInfoExtractor.extractTextFromViewables(outputViewableParts);

        String expectedHtml =
                "<pre class=\"k9mail\">" +
                "K-9 Mail rocks :&gt;" +
                "</pre>";

        assertEquals(BODY_TEXT, container.text);
        assertEquals(expectedHtml, container.html);
    }

    @Test
    public void testTextPlainFormatFlowed() throws MessagingException {
        // Create text/plain body
        TextBody body = new TextBody(BODY_TEXT_FLOWED);

        // Create message
        MimeMessage message = new MimeMessage();
        MimeMessageHelper.setBody(message, body);
        message.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/plain; format=flowed");

        // Extract text
        List<Part> outputNonViewableParts = new ArrayList<>();
        ArrayList<Viewable> outputViewableParts = new ArrayList<>();
        MessageExtractor.findViewablesAndAttachments(message, outputViewableParts, outputNonViewableParts);
        ViewableExtractedText container = messageViewInfoExtractor.extractTextFromViewables(outputViewableParts);

        String expectedText = "K-9 Mail rocks :> flowed line\r\n" +
                "not flowed line";
        String expectedHtml =
                "<pre class=\"k9mail\">" +
                        "K-9 Mail rocks :&gt; flowed line<br>not flowed line" +
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
        message.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/html");
        MimeMessageHelper.setBody(message, body);

        // Extract text
        ArrayList<Viewable> outputViewableParts = new ArrayList<>();
        MessageExtractor.findViewablesAndAttachments(message, outputViewableParts, null);
        assertEquals(outputViewableParts.size(), 1);
        ViewableExtractedText container = messageViewInfoExtractor.extractTextFromViewables(outputViewableParts);

        assertEquals(BODY_TEXT, container.text);
        assertEquals(bodyText, container.html);
    }

    @Test
    public void testMultipartPlainTextMessage() throws MessagingException {
        String bodyText1 = "text body 1";
        String bodyText2 = "text body 2";

        // Create text/plain bodies
        TextBody body1 = new TextBody(bodyText1);
        TextBody body2 = new TextBody(bodyText2);

        // Create multipart/mixed part
        MimeMultipart multipart = MimeMultipart.newInstance();
        MimeBodyPart bodyPart1 = new MimeBodyPart(body1, "text/plain");
        MimeBodyPart bodyPart2 = new MimeBodyPart(body2, "text/plain");
        multipart.addBodyPart(bodyPart1);
        multipart.addBodyPart(bodyPart2);

        // Create message
        MimeMessage message = new MimeMessage();
        MimeMessageHelper.setBody(message, multipart);

        // Extract text
        List<Part> outputNonViewableParts = new ArrayList<>();
        ArrayList<Viewable> outputViewableParts = new ArrayList<>();
        MessageExtractor.findViewablesAndAttachments(message, outputViewableParts, outputNonViewableParts);
        ViewableExtractedText container = messageViewInfoExtractor.extractTextFromViewables(outputViewableParts);

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
        K9ActivityCommon.setLanguage(context, "en");
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+01:00"));

        String innerBodyText = "Hey there. I'm inside a message/rfc822 (inline) attachment.";

        // Create text/plain body
        TextBody textBody = new TextBody(BODY_TEXT);

        // Create inner text/plain body
        TextBody innerBody = new TextBody(innerBodyText);

        // Create message/rfc822 body
        MimeMessage innerMessage = new MimeMessage();
        innerMessage.addSentDate(new Date(112, 2, 17), false);
        innerMessage.setRecipients(RecipientType.TO, new Address[] { new Address("to@example.com") });
        innerMessage.setSubject("Subject");
        innerMessage.setFrom(new Address("from@example.com"));
        MimeMessageHelper.setBody(innerMessage, innerBody);

        // Create multipart/mixed part
        MimeMultipart multipart = MimeMultipart.newInstance();
        MimeBodyPart bodyPart1 = new MimeBodyPart(textBody, "text/plain");
        MimeBodyPart bodyPart2 = new MimeBodyPart(innerMessage, "message/rfc822");
        bodyPart2.setHeader("Content-Disposition", "inline; filename=\"message.eml\"");
        multipart.addBodyPart(bodyPart1);
        multipart.addBodyPart(bodyPart2);

        // Create message
        MimeMessage message = new MimeMessage();
        MimeMessageHelper.setBody(message, multipart);

        // Extract text
        List<Part> outputNonViewableParts = new ArrayList<Part>();
        ArrayList<Viewable> outputViewableParts = new ArrayList<>();
        MessageExtractor.findViewablesAndAttachments(message, outputViewableParts, outputNonViewableParts);
        ViewableExtractedText container = messageViewInfoExtractor.extractTextFromViewables(outputViewableParts);

        String expectedText =
                BODY_TEXT +
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
                        BODY_TEXT_HTML +
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

    @Test
    public void testMultipartDigestWithMessages() throws Exception {
        String data = "Content-Type: multipart/digest; boundary=\"bndry\"\r\n" +
                "\r\n" +
                "--bndry\r\n" +
                "\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                "text body of first message\r\n" +
                "\r\n" +
                "--bndry\r\n" +
                "\r\n" +
                "Subject: subject of second message\r\n" +
                "Content-Type: multipart/alternative; boundary=\"bndry2\"\r\n" +
                "\r\n" +
                "--bndry2\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                "text part of second message\r\n" +
                "\r\n" +
                "--bndry2\r\n" +
                "Content-Type: text/html\"\r\n" +
                "\r\n" +
                "html part of second message\r\n" +
                "\r\n" +
                "--bndry2--\r\n" +
                "\r\n" +
                "--bndry--\r\n";
        MimeMessage message = MimeMessage.parseMimeMessage(new ByteArrayInputStream(data.getBytes()), false);

        // Extract text
        List<Part> outputNonViewableParts = new ArrayList<>();
        ArrayList<Viewable> outputViewableParts = new ArrayList<>();
        MessageExtractor.findViewablesAndAttachments(message, outputViewableParts, outputNonViewableParts);

        String expectedExtractedText = "Subject: (No subject)\r\n" +
                "\r\n" +
                "text body of first message\r\n" +
                "\r\n" +
                "\r\n" +
                "------------------------------------------------------------------------\r\n" +
                "\r\n" +
                "Subject: subject of second message\r\n" +
                "\r\n" +
                "text part of second message\r\n";
        String expectedHtmlText = "<table style=\"border: 0\">" +
                "<tr><th style=\"text-align: left; vertical-align: top;\">Subject:</th><td>(No subject)</td></tr>" +
                "</table>" +
                "<pre class=\"k9mail\">text body of first message<br></pre>" +
                "<p style=\"margin-top: 2.5em; margin-bottom: 1em; border-bottom: 1px solid #000\"></p>" +
                "<table style=\"border: 0\">" +
                "<tr><th style=\"text-align: left; vertical-align: top;\">Subject:</th><td>subject of second message</td></tr>" +
                "</table>" +
                "<pre class=\"k9mail\">text part of second message<br></pre>";


        assertEquals(4, outputViewableParts.size());
        assertEquals("subject of second message", ((MessageHeader) outputViewableParts.get(2)).getMessage().getSubject());

        ViewableExtractedText firstMessageExtractedText =
                messageViewInfoExtractor.extractTextFromViewables(outputViewableParts);
        assertEquals(expectedExtractedText, firstMessageExtractedText.text);
        assertEquals(expectedHtmlText, firstMessageExtractedText.html);
    }

    @Test
    public void extractMessage_withAttachment() throws Exception {
        BodyPart attachmentPart = bodypart("application/octet-stream");
        Message message = messageFromBody(multipart("mixed",
                bodypart("text/plain", "text"),
                attachmentPart
        ));
        AttachmentViewInfo attachmentViewInfo = mock(AttachmentViewInfo.class);
        setupAttachmentInfoForPart(attachmentPart, attachmentViewInfo);


        MessageViewInfo messageViewInfo = messageViewInfoExtractor.extractMessageForView(message, null);


        assertEquals("<pre class=\"k9mail\">text</pre>", messageViewInfo.text);
        assertSame(attachmentViewInfo, messageViewInfo.attachments.get(0));
        assertNull(messageViewInfo.cryptoResultAnnotation);
        assertTrue(messageViewInfo.extraAttachments.isEmpty());
    }

    @Test
    public void extractMessage_withCryptoAnnotation() throws Exception {
        Message message = messageFromBody(multipart("signed", "protocol=\"application/pgp-signature\"",
                bodypart("text/plain", "text"),
                bodypart("application/pgp-signature")
        ));
        CryptoResultAnnotation annotation = CryptoResultAnnotation.createOpenPgpResultAnnotation(
                null, null, null, null, null, false);
        MessageCryptoAnnotations messageCryptoAnnotations = createAnnotations(message, annotation);


        MessageViewInfo messageViewInfo = messageViewInfoExtractor.extractMessageForView(message, messageCryptoAnnotations);


        assertEquals("<pre class=\"k9mail\">text</pre>", messageViewInfo.text);
        assertSame(annotation, messageViewInfo.cryptoResultAnnotation);
        assertSame(message, messageViewInfo.message);
        assertSame(message, messageViewInfo.rootPart);
        assertTrue(messageViewInfo.attachments.isEmpty());
        assertTrue(messageViewInfo.extraAttachments.isEmpty());
    }

    @Test
    public void extractMessage_withCryptoAnnotation_andReplacementPart() throws Exception {
        Message message = messageFromBody(multipart("signed", "protocol=\"application/pgp-signature\"",
                bodypart("text/plain", "text"),
                bodypart("application/pgp-signature")
        ));
        MimeBodyPart replacementPart = bodypart("text/plain", "replacement text");
        CryptoResultAnnotation annotation = CryptoResultAnnotation.createOpenPgpResultAnnotation(
                null, null, null, null, replacementPart, false);
        MessageCryptoAnnotations messageCryptoAnnotations = createAnnotations(message, annotation);


        MessageViewInfo messageViewInfo = messageViewInfoExtractor.extractMessageForView(message, messageCryptoAnnotations);


        assertEquals("<pre class=\"k9mail\">replacement text</pre>", messageViewInfo.text);
        assertSame(annotation, messageViewInfo.cryptoResultAnnotation);
        assertSame(message, messageViewInfo.message);
        assertSame(replacementPart, messageViewInfo.rootPart);
        assertTrue(messageViewInfo.attachments.isEmpty());
        assertTrue(messageViewInfo.extraAttachments.isEmpty());
    }

    @Test
    public void extractMessage_withCryptoAnnotation_andExtraText() throws Exception {
        MimeBodyPart signedPart = multipart("signed", "protocol=\"application/pgp-signature\"",
                bodypart("text/plain", "text"),
                bodypart("application/pgp-signature")
        );
        BodyPart extraText = bodypart("text/plain", "extra text");
        Message message = messageFromBody(multipart("mixed",
                signedPart,
                extraText
        ));
        CryptoResultAnnotation annotation = CryptoResultAnnotation.createOpenPgpResultAnnotation(
                null, null, null, null, null, false);
        MessageCryptoAnnotations messageCryptoAnnotations = createAnnotations(signedPart, annotation);


        MessageViewInfo messageViewInfo = messageViewInfoExtractor.extractMessageForView(message, messageCryptoAnnotations);


        assertEquals("<pre class=\"k9mail\">text</pre>", messageViewInfo.text);
        assertSame(annotation, messageViewInfo.cryptoResultAnnotation);
        assertEquals("extra text", messageViewInfo.extraText);
        assertTrue(messageViewInfo.attachments.isEmpty());
        assertTrue(messageViewInfo.extraAttachments.isEmpty());
    }

    @Test
    public void extractMessage_withCryptoAnnotation_andExtraAttachment() throws Exception {
        MimeBodyPart signedPart = multipart("signed", "protocol=\"application/pgp-signature\"",
                bodypart("text/plain", "text"),
                bodypart("application/pgp-signature")
        );
        BodyPart extraAttachment = bodypart("application/octet-stream");
        Message message = messageFromBody(multipart("mixed",
                signedPart,
                extraAttachment
        ));
        CryptoResultAnnotation annotation = CryptoResultAnnotation.createOpenPgpResultAnnotation(
                null, null, null, null, null, false);
        MessageCryptoAnnotations messageCryptoAnnotations = createAnnotations(signedPart, annotation);

        AttachmentViewInfo attachmentViewInfo = mock(AttachmentViewInfo.class);
        setupAttachmentInfoForPart(extraAttachment, attachmentViewInfo);


        MessageViewInfo messageViewInfo = messageViewInfoExtractor.extractMessageForView(message, messageCryptoAnnotations);


        assertEquals("<pre class=\"k9mail\">text</pre>", messageViewInfo.text);
        assertSame(annotation, messageViewInfo.cryptoResultAnnotation);
        assertSame(attachmentViewInfo, messageViewInfo.extraAttachments.get(0));
        assertTrue(messageViewInfo.attachments.isEmpty());
    }

    @Test
    public void extractMessage_openPgpEncrypted_withoutAnnotations() throws Exception {
        Message message = messageFromBody(
                multipart("encrypted", "protocol=\"application/pgp-encrypted\"",
                        bodypart("application/pgp-encrypted"),
                        bodypart("application/octet-stream")
                )
        );

        MessageViewInfo messageViewInfo = messageViewInfoExtractor.extractMessageForView(message, null);

        assertEquals(CryptoError.OPENPGP_ENCRYPTED_NO_PROVIDER, messageViewInfo.cryptoResultAnnotation.getErrorType());
        assertNull(messageViewInfo.text);
        assertNull(messageViewInfo.attachments);
        assertNull(messageViewInfo.extraAttachments);
    }

    @Test
    public void extractMessage_multipartSigned_UnknownProtocol() throws Exception {
        Message message = messageFromBody(
                multipart("signed", "protocol=\"application/pkcs7-signature\"",
                    bodypart("text/plain", "text"),
                    bodypart("application/pkcs7-signature", "signature")
                )
        );

        MessageViewInfo messageViewInfo = messageViewInfoExtractor.extractMessageForView(message, null);

        assertEquals("<pre class=\"k9mail\">text</pre>", messageViewInfo.text);
        assertNull(messageViewInfo.cryptoResultAnnotation);
        assertTrue(messageViewInfo.attachments.isEmpty());
        assertTrue(messageViewInfo.extraAttachments.isEmpty());
    }

    @Test
    public void extractMessage_multipartSigned_UnknownProtocol_withExtraAttachments() throws Exception {
        BodyPart extraAttachment = bodypart("application/octet-stream");
        Message message = messageFromBody(
                multipart("mixed",
                        multipart("signed", "protocol=\"application/pkcs7-signature\"",
                                bodypart("text/plain", "text"),
                                bodypart("application/pkcs7-signature", "signature")
                        ),
                        extraAttachment
                )
        );
        AttachmentViewInfo mock = mock(AttachmentViewInfo.class);
        setupAttachmentInfoForPart(extraAttachment, mock);

        MessageViewInfo messageViewInfo = messageViewInfoExtractor.extractMessageForView(message, null);

        assertEquals("<pre class=\"k9mail\">text</pre>", messageViewInfo.text);
        assertNull(messageViewInfo.cryptoResultAnnotation);
        assertSame(mock, messageViewInfo.attachments.get(0));
        assertTrue(messageViewInfo.extraAttachments.isEmpty());
    }

    void setupAttachmentInfoForPart(BodyPart extraAttachment, AttachmentViewInfo attachmentViewInfo)
            throws MessagingException {
        doReturn(attachmentViewInfo).when(attachmentInfoExtractor).extractAttachmentInfo(extraAttachment);
    }

    @NonNull
    MessageCryptoAnnotations createAnnotations(Part part, CryptoResultAnnotation annotation) {
        MessageCryptoAnnotations messageCryptoAnnotations = new MessageCryptoAnnotations();
        messageCryptoAnnotations.put(part, annotation);
        return messageCryptoAnnotations;
    }

    HtmlProcessor createFakeHtmlProcessor() {
        HtmlProcessor htmlProcessor = mock(HtmlProcessor.class);

        when(htmlProcessor.processForDisplay(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return (String) invocation.getArguments()[0];
            }
        });

        return htmlProcessor;
    }
}
