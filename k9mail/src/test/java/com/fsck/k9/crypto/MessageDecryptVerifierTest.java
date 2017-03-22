package com.fsck.k9.crypto;


import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.ui.crypto.MessageCryptoAnnotations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


@RunWith(K9RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MessageDecryptVerifierTest {
    private static final String MIME_TYPE_MULTIPART_ENCRYPTED = "multipart/encrypted";
    private MessageCryptoAnnotations messageCryptoAnnotations = mock(MessageCryptoAnnotations.class);
    private static final String PROTCOL_PGP_ENCRYPTED = "application/pgp-encrypted";
    private static final String PGP_INLINE_DATA = "" +
            "-----BEGIN PGP MESSAGE-----\n" +
            "Header: Value\n" +
            "\n" +
            "base64base64base64base64\n" +
            "-----END PGP MESSAGE-----\n";


    @Test
    public void findPrimaryCryptoPart_withSimplePgpInline() throws Exception {
        List<Part> outputExtraParts = new ArrayList<>();
        Message message = new MimeMessage();
        MimeMessageHelper.setBody(message, new TextBody(PGP_INLINE_DATA));

        Part cryptoPart = MessageDecryptVerifier.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

        assertSame(message, cryptoPart);
    }

    @Test
    public void findPrimaryCryptoPart_withMultipartAlternativeContainingPgpInline() throws Exception {
        List<Part> outputExtraParts = new ArrayList<>();
        BodyPart pgpInlinePart = bodypart("text/plain", PGP_INLINE_DATA);
        Message message = messageFromBody(
                multipart("alternative",
                        pgpInlinePart,
                        bodypart("text/html")
                )
        );

        Part cryptoPart = MessageDecryptVerifier.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

        assertSame(pgpInlinePart, cryptoPart);
    }

    @Test
    public void findPrimaryCryptoPart_withMultipartMixedContainingPgpInline() throws Exception {
        List<Part> outputExtraParts = new ArrayList<>();
        BodyPart pgpInlinePart = bodypart("text/plain", PGP_INLINE_DATA);
        Message message = messageFromBody(
                multipart("mixed",
                        pgpInlinePart,
                        bodypart("application/octet-stream")
                )
        );

        Part cryptoPart = MessageDecryptVerifier.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

        assertSame(pgpInlinePart, cryptoPart);
    }

    @Test
    public void findPrimaryCryptoPart_withMultipartMixedContainingMultipartAlternativeContainingPgpInline()
            throws Exception {
        List<Part> outputExtraParts = new ArrayList<>();
        BodyPart pgpInlinePart = bodypart("text/plain", PGP_INLINE_DATA);
        Message message = messageFromBody(
                multipart("mixed",
                        multipart("alternative",
                            pgpInlinePart,
                            bodypart("text/html")
                        ),
                        bodypart("application/octet-stream")
                )
        );

        Part cryptoPart = MessageDecryptVerifier.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

        assertSame(pgpInlinePart, cryptoPart);
    }

    @Test
    public void findPrimaryCryptoPart_withEmptyMultipartAlternative_shouldReturnNull() throws Exception {
        List<Part> outputExtraParts = new ArrayList<>();
        Message message = messageFromBody(
                multipart("alternative")
        );

        Part cryptoPart = MessageDecryptVerifier.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

        assertNull(cryptoPart);
    }

    @Test
    public void findPrimaryCryptoPart_withEmptyMultipartMixed_shouldReturnNull() throws Exception {
        List<Part> outputExtraParts = new ArrayList<>();
        Message message = messageFromBody(
                multipart("mixed")
        );

        Part cryptoPart = MessageDecryptVerifier.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

        assertNull(cryptoPart);
    }

    @Test
    public void findPrimaryCryptoPart_withEmptyMultipartAlternativeInsideMultipartMixed_shouldReturnNull()
            throws Exception {
        List<Part> outputExtraParts = new ArrayList<>();
        Message message = messageFromBody(
                multipart("mixed",
                        multipart("alternative")
                )
        );

        Part cryptoPart = MessageDecryptVerifier.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

        assertNull(cryptoPart);
    }

    @Test
    public void findEncryptedPartsShouldReturnEmptyListForEmptyMessage() throws Exception {
        MimeMessage emptyMessage = new MimeMessage();

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(emptyMessage);

        assertEquals(0, encryptedParts.size());
    }

    @Test
    public void findEncryptedPartsShouldReturnEmptyListForSimpleMessage() throws Exception {
        MimeMessage message = new MimeMessage();
        message.setBody(new TextBody("message text"));

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);

        assertEquals(0, encryptedParts.size());
    }

    @Test
    public void findEncryptedPartsShouldReturnEmptyEncryptedPart() throws Exception {
        MimeMessage message = new MimeMessage();
        MimeMultipart multipartEncrypted = MimeMultipart.newInstance();
        multipartEncrypted.setSubType("encrypted");
        MimeMessageHelper.setBody(message, multipartEncrypted);
        setContentTypeWithProtocol(message, MIME_TYPE_MULTIPART_ENCRYPTED, PROTCOL_PGP_ENCRYPTED);

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);

        assertEquals(1, encryptedParts.size());
        assertSame(message, encryptedParts.get(0));
    }

    @Test
    public void findEncryptedPartsShouldReturnMultipleEncryptedParts() throws Exception {
        MimeMessage message = new MimeMessage();
        MimeMultipart multipartMixed = MimeMultipart.newInstance();
        multipartMixed.setSubType("mixed");
        MimeMessageHelper.setBody(message, multipartMixed);

        MimeMultipart multipartEncryptedOne = MimeMultipart.newInstance();
        multipartEncryptedOne.setSubType("encrypted");
        MimeBodyPart bodyPartOne = new MimeBodyPart(multipartEncryptedOne);
        setContentTypeWithProtocol(bodyPartOne, MIME_TYPE_MULTIPART_ENCRYPTED, PROTCOL_PGP_ENCRYPTED);
        multipartMixed.addBodyPart(bodyPartOne);

        MimeBodyPart bodyPartTwo = new MimeBodyPart(null, "text/plain");
        multipartMixed.addBodyPart(bodyPartTwo);

        MimeMultipart multipartEncryptedThree = MimeMultipart.newInstance();
        multipartEncryptedThree.setSubType("encrypted");
        MimeBodyPart bodyPartThree = new MimeBodyPart(multipartEncryptedThree);
        setContentTypeWithProtocol(bodyPartThree, MIME_TYPE_MULTIPART_ENCRYPTED, PROTCOL_PGP_ENCRYPTED);
        multipartMixed.addBodyPart(bodyPartThree);

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);

        assertEquals(2, encryptedParts.size());
        assertSame(bodyPartOne, encryptedParts.get(0));
        assertSame(bodyPartThree, encryptedParts.get(1));
    }

    @Test
    public void findEncrypted__withMultipartEncrypted__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("encrypted",
                        bodypart("application/pgp-encrypted"),
                        bodypart("application/octet-stream")
                )
        );

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);

        assertEquals(1, encryptedParts.size());
        assertSame(message, encryptedParts.get(0));
    }

    @Test
    public void findEncrypted__withMultipartMixedSubEncrypted__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("mixed",
                        multipart("encrypted",
                            bodypart("application/pgp-encrypted"),
                            bodypart("application/octet-stream")
                        )
                )
        );

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);

        assertEquals(1, encryptedParts.size());
        assertSame(getPart(message, 0), encryptedParts.get(0));
    }

    @Test
    public void findEncrypted__withMultipartMixedSubEncryptedAndEncrypted__shouldReturnBoth()
            throws Exception {
        Message message = messageFromBody(
                multipart("mixed",
                        multipart("encrypted",
                                bodypart("application/pgp-encrypted"),
                                bodypart("application/octet-stream")
                        ),
                        multipart("encrypted",
                                bodypart("application/pgp-encrypted"),
                                bodypart("application/octet-stream")
                        )
                )
        );

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);

        assertEquals(2, encryptedParts.size());
        assertSame(getPart(message, 0), encryptedParts.get(0));
        assertSame(getPart(message, 1), encryptedParts.get(1));
    }

    @Test
    public void findEncrypted__withMultipartMixedSubTextAndEncrypted__shouldReturnEncrypted() throws Exception {
        Message message = messageFromBody(
                multipart("mixed",
                        bodypart("text/plain"),
                        multipart("encrypted",
                                bodypart("application/pgp-encrypted"),
                                bodypart("application/octet-stream")
                        )
                )
        );

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);

        assertEquals(1, encryptedParts.size());
        assertSame(getPart(message, 1), encryptedParts.get(0));
    }

    @Test
    public void findEncrypted__withMultipartMixedSubEncryptedAndText__shouldReturnEncrypted() throws Exception {
        Message message = messageFromBody(
                multipart("mixed",
                        multipart("encrypted",
                                bodypart("application/pgp-encrypted"),
                                bodypart("application/octet-stream")
                        ),
                        bodypart("text/plain")
                )
        );

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);

        assertEquals(1, encryptedParts.size());
        assertSame(getPart(message, 0), encryptedParts.get(0));
    }

    @Test
    public void findSigned__withSimpleMultipartSigned__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("signed",
                        bodypart("text/plain"),
                        bodypart("application/pgp-signature")
                )
        );

        List<Part> signedParts = MessageDecryptVerifier.findSignedParts(message, messageCryptoAnnotations);

        assertEquals(1, signedParts.size());
        assertSame(message, signedParts.get(0));
    }

    @Test
    public void findSigned__withComplexMultipartSigned__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("signed",
                        multipart("mixed",
                                bodypart("text/plain"),
                                bodypart("application/pdf")
                        ),
                        bodypart("application/pgp-signature")
                )
        );

        List<Part> signedParts = MessageDecryptVerifier.findSignedParts(message, messageCryptoAnnotations);

        assertEquals(1, signedParts.size());
        assertSame(message, signedParts.get(0));
    }

    @Test
    public void findEncrypted__withMultipartMixedSubSigned__shouldReturnSigned() throws Exception {
        Message message = messageFromBody(
                multipart("mixed",
                    multipart("signed",
                            bodypart("text/plain"),
                            bodypart("application/pgp-signature")
                    )
                )
        );

        List<Part> signedParts = MessageDecryptVerifier.findSignedParts(message, messageCryptoAnnotations);

        assertEquals(1, signedParts.size());
        assertSame(getPart(message, 0), signedParts.get(0));
    }

    @Test
    public void findEncrypted__withMultipartMixedSubSignedAndText__shouldReturnSigned() throws Exception {
        Message message = messageFromBody(
                multipart("mixed",
                        multipart("signed",
                                bodypart("text/plain"),
                                bodypart("application/pgp-signature")
                        ),
                        bodypart("text/plain")
                )
        );

        List<Part> signedParts = MessageDecryptVerifier.findSignedParts(message, messageCryptoAnnotations);

        assertEquals(1, signedParts.size());
        assertSame(getPart(message, 0), signedParts.get(0));
    }

    @Test
    public void findEncrypted__withMultipartMixedSubTextAndSigned__shouldReturnSigned() throws Exception {
        Message message = messageFromBody(
                multipart("mixed",
                        bodypart("text/plain"),
                        multipart("signed",
                                bodypart("text/plain"),
                                bodypart("application/pgp-signature")
                        )
                )
        );

        List<Part> signedParts = MessageDecryptVerifier.findSignedParts(message, messageCryptoAnnotations);

        assertEquals(1, signedParts.size());
        assertSame(getPart(message, 1), signedParts.get(0));
    }

    @Test
    public void isPgpInlineMethods__withPgpInlineData__shouldReturnTrue() throws Exception {
        String pgpInlineData = "-----BEGIN PGP MESSAGE-----\n" +
                "Header: Value\n" +
                "\n" +
                "base64base64base64base64\n" +
                "-----END PGP MESSAGE-----\n";

        MimeMessage message = new MimeMessage();
        message.setBody(new TextBody(pgpInlineData));

        assertTrue(MessageDecryptVerifier.isPartPgpInlineEncrypted(message));
    }

    @Test
    public void isPgpInlineMethods__withEncryptedDataAndLeadingWhitespace__shouldReturnTrue() throws Exception {
        String pgpInlineData = "\n   \n \n" +
                "-----BEGIN PGP MESSAGE-----\n" +
                "Header: Value\n" +
                "\n" +
                "base64base64base64base64\n" +
                "-----END PGP MESSAGE-----\n";

        MimeMessage message = new MimeMessage();
        message.setBody(new TextBody(pgpInlineData));

        assertTrue(MessageDecryptVerifier.isPartPgpInlineEncryptedOrSigned(message));
        assertTrue(MessageDecryptVerifier.isPartPgpInlineEncrypted(message));
    }

    @Test
    public void isPgpInlineMethods__withEncryptedDataAndLeadingGarbage__shouldReturnFalse() throws Exception {
        String pgpInlineData = "garbage!" +
                "-----BEGIN PGP MESSAGE-----\n" +
                "Header: Value\n" +
                "\n" +
                "base64base64base64base64\n" +
                "-----END PGP MESSAGE-----\n";

        MimeMessage message = new MimeMessage();
        message.setBody(new TextBody(pgpInlineData));

        assertFalse(MessageDecryptVerifier.isPartPgpInlineEncryptedOrSigned(message));
        assertFalse(MessageDecryptVerifier.isPartPgpInlineEncrypted(message));
    }

    @Test
    public void isPartPgpInlineEncryptedOrSigned__withSignedData__shouldReturnTrue() throws Exception {
        String pgpInlineData = "-----BEGIN PGP SIGNED MESSAGE-----\n" +
                "Header: Value\n" +
                "\n" +
                "-----BEGIN PGP SIGNATURE-----\n" +
                "Header: Value\n" +
                "\n" +
                "base64base64base64base64\n" +
                "-----END PGP SIGNED MESSAGE-----\n";

        MimeMessage message = new MimeMessage();
        message.setBody(new TextBody(pgpInlineData));

        assertTrue(MessageDecryptVerifier.isPartPgpInlineEncryptedOrSigned(message));
    }

    @Test
    public void isPartPgpInlineEncrypted__withSignedData__shouldReturnFalse() throws Exception {
        String pgpInlineData = "-----BEGIN PGP SIGNED MESSAGE-----\n" +
                "Header: Value\n" +
                "\n" +
                "-----BEGIN PGP SIGNATURE-----\n" +
                "Header: Value\n" +
                "\n" +
                "base64base64base64base64\n" +
                "-----END PGP SIGNED MESSAGE-----\n";

        MimeMessage message = new MimeMessage();
        message.setBody(new TextBody(pgpInlineData));

        assertFalse(MessageDecryptVerifier.isPartPgpInlineEncrypted(message));
    }

    MimeMessage messageFromBody(BodyPart bodyPart) throws MessagingException {
        MimeMessage message = new MimeMessage();
        MimeMessageHelper.setBody(message, bodyPart.getBody());
        return message;
    }

    MimeBodyPart multipart(String type, BodyPart... subParts) throws MessagingException {
        MimeMultipart multiPart = MimeMultipart.newInstance();
        multiPart.setSubType(type);
        for (BodyPart subPart : subParts) {
            multiPart.addBodyPart(subPart);
        }
        return new MimeBodyPart(multiPart);
    }

    BodyPart bodypart(String type) throws MessagingException {
        return new MimeBodyPart(null, type);
    }

    BodyPart bodypart(String type, String text) throws MessagingException {
        TextBody textBody = new TextBody(text);
        return new MimeBodyPart(textBody, type);
    }

    public static Part getPart(Part searchRootPart, int... indexes) {
        Part part = searchRootPart;
        for (int index : indexes) {
            part = ((Multipart) part.getBody()).getBodyPart(index);
        }
        return part;
    }

    //TODO: Find a cleaner way to do this
    private static void setContentTypeWithProtocol(Part part, String mimeType, String protocol)
            throws MessagingException {
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, mimeType + "; protocol=\"" + protocol + "\"");
    }
}
