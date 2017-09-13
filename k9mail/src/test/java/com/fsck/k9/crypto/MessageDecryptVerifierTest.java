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


@SuppressWarnings("WeakerAccess")
@RunWith(K9RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MessageDecryptVerifierTest {
    MessageCryptoAnnotations messageCryptoAnnotations = mock(MessageCryptoAnnotations.class);
    static final String PGP_INLINE_DATA = "" +
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
                multipart("alternative", null,
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
                multipart("mixed", null,
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
                multipart("mixed", null,
                        multipart("alternative", null,
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
                multipart("alternative", null)
        );

        Part cryptoPart = MessageDecryptVerifier.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

        assertNull(cryptoPart);
    }

    @Test
    public void findPrimaryCryptoPart_withEmptyMultipartMixed_shouldReturnNull() throws Exception {
        List<Part> outputExtraParts = new ArrayList<>();
        Message message = messageFromBody(
                multipart("mixed", null)
        );

        Part cryptoPart = MessageDecryptVerifier.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

        assertNull(cryptoPart);
    }

    @Test
    public void findPrimaryCryptoPart_withEmptyMultipartAlternativeInsideMultipartMixed_shouldReturnNull()
            throws Exception {
        List<Part> outputExtraParts = new ArrayList<>();
        Message message = messageFromBody(
                multipart("mixed", null,
                        multipart("alternative", null)
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
    public void findEncrypted__withMultipartEncrypted__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("encrypted", "application/pgp-encrypted",
                        bodypart("application/pgp-encrypted"),
                        bodypart("application/octet-stream")
                )
        );

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);

        assertEquals(1, encryptedParts.size());
        assertSame(message, encryptedParts.get(0));
    }

    @Test
    public void findEncrypted__withBadProtocol__shouldReturnEmpty() throws Exception {
        Message message = messageFromBody(
                multipart("encrypted", "application/not-pgp-encrypted",
                        bodypart("application/pgp-encrypted"),
                        bodypart("application/octet-stream")
                )
        );

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);

        assertTrue(encryptedParts.isEmpty());
    }

    @Test
    public void findEncrypted__withEmptyProtocol__shouldReturnEmpty() throws Exception {
        Message message = messageFromBody(
                multipart("encrypted", null,
                        bodypart("application/pgp-encrypted"),
                        bodypart("application/octet-stream")
                )
        );

        List<Part> encryptedParts = MessageCryptoStructureDetector.findMultipartEncryptedParts(message);

        assertTrue(encryptedParts.isEmpty());
    }

    @Test
    public void findEncrypted__withMissingEncryptedBody__shouldReturnEmpty() throws Exception {
        Message message = messageFromBody(
                multipart("encrypted", "application/pgp-encrypted",
                        bodypart("application/pgp-encrypted")
                )
        );

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);

        assertTrue(encryptedParts.isEmpty());
    }

    @Test
    public void findEncrypted__withBadStructure__shouldReturnEmpty() throws Exception {
        Message message = messageFromBody(
                multipart("encrypted", "application/pgp-encrypted",
                        bodypart("application/octet-stream")
                )
        );

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);

        assertTrue(encryptedParts.isEmpty());
    }

    @Test
    public void findEncrypted__withMultipartMixedSubEncrypted__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("mixed", null,
                        multipart("encrypted", "application/pgp-encrypted",
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
                multipart("mixed", null,
                        multipart("encrypted", "application/pgp-encrypted",
                                bodypart("application/pgp-encrypted"),
                                bodypart("application/octet-stream")
                        ),
                        multipart("encrypted", "application/pgp-encrypted",
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
                multipart("mixed", null,
                        bodypart("text/plain"),
                        multipart("encrypted", "application/pgp-encrypted",
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
                multipart("mixed", null,
                        multipart("encrypted", "application/pgp-encrypted",
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
                multipart("signed", "application/pgp-signature",
                        bodypart("text/plain"),
                        bodypart("application/pgp-signature")
                )
        );

        List<Part> signedParts = MessageDecryptVerifier.findSignedParts(message, messageCryptoAnnotations);

        assertEquals(1, signedParts.size());
        assertSame(message, signedParts.get(0));
    }

    @Test
    public void findSigned__withBadProtocol__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("signed", "application/not-pgp-signature",
                        bodypart("text/plain"),
                        bodypart("application/pgp-signature")
                )
        );

        List<Part> signedParts = MessageDecryptVerifier.findSignedParts(message, messageCryptoAnnotations);

        assertTrue(signedParts.isEmpty());
    }

    @Test
    public void findSigned__withEmptyProtocol__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("signed", null,
                        bodypart("text/plain"),
                        bodypart("application/pgp-signature")
                )
        );

        List<Part> signedParts = MessageCryptoStructureDetector
                .findMultipartSignedParts(message, messageCryptoAnnotations);

        assertTrue(signedParts.isEmpty());
    }

    @Test
    public void findSigned__withMissingSignature__shouldReturnEmpty() throws Exception {
        Message message = messageFromBody(
                multipart("signed", "application/pgp-signature",
                        bodypart("text/plain")
                )
        );

        List<Part> signedParts = MessageDecryptVerifier.findSignedParts(message, messageCryptoAnnotations);

        assertTrue(signedParts.isEmpty());
    }

    @Test
    public void findSigned__withComplexMultipartSigned__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("signed", "application/pgp-signature",
                        multipart("mixed", null,
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
                multipart("mixed", null,
                        multipart("signed", "application/pgp-signature",
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
                multipart("mixed", null,
                        multipart("signed", "application/pgp-signature",
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
                multipart("mixed", null,
                        bodypart("text/plain"),
                        multipart("signed", "application/pgp-signature",
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
        if (bodyPart.getContentType() != null) {
            message.setHeader("Content-Type", bodyPart.getContentType());
        }
        return message;
    }

    MimeBodyPart multipart(String type, String protocol, BodyPart... subParts) throws MessagingException {
        MimeMultipart multiPart = MimeMultipart.newInstance();
        multiPart.setSubType(type);
        for (BodyPart subPart : subParts) {
            multiPart.addBodyPart(subPart);
        }
        MimeBodyPart mimeBodyPart = new MimeBodyPart(multiPart);
        if (protocol != null) {
            mimeBodyPart.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                    mimeBodyPart.getContentType() + "; protocol=\"" + protocol + "\"");
        }
        return mimeBodyPart;
    }

    BodyPart bodypart(String type) throws MessagingException {
        return new MimeBodyPart(null, type);
    }

    BodyPart bodypart(String type, String text) throws MessagingException {
        TextBody textBody = new TextBody(text);
        return new MimeBodyPart(textBody, type);
    }

    static Part getPart(Part searchRootPart, int... indexes) {
        Part part = searchRootPart;
        for (int index : indexes) {
            part = ((Multipart) part.getBody()).getBodyPart(index);
        }
        return part;
    }
}
