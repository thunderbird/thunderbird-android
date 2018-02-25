package com.fsck.k9.crypto;


import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.ui.crypto.MessageCryptoAnnotations;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.fsck.k9.message.TestMessageConstructionUtils.bodypart;
import static com.fsck.k9.message.TestMessageConstructionUtils.messageFromBody;
import static com.fsck.k9.message.TestMessageConstructionUtils.multipart;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


@SuppressWarnings("WeakerAccess")
@RunWith(RobolectricTestRunner.class)
public class MessageCryptoStructureDetectorTest {
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

        Part cryptoPart = MessageCryptoStructureDetector.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

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

        Part cryptoPart = MessageCryptoStructureDetector.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

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

        Part cryptoPart = MessageCryptoStructureDetector.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

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

        Part cryptoPart = MessageCryptoStructureDetector.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

        assertSame(pgpInlinePart, cryptoPart);
    }

    @Test
    public void findPrimaryCryptoPart_withEmptyMultipartAlternative_shouldReturnNull() throws Exception {
        List<Part> outputExtraParts = new ArrayList<>();
        Message message = messageFromBody(
                multipart("alternative")
        );

        Part cryptoPart = MessageCryptoStructureDetector.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

        assertNull(cryptoPart);
    }

    @Test
    public void findPrimaryCryptoPart_withEmptyMultipartMixed_shouldReturnNull() throws Exception {
        List<Part> outputExtraParts = new ArrayList<>();
        Message message = messageFromBody(
                multipart("mixed")
        );

        Part cryptoPart = MessageCryptoStructureDetector.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

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

        Part cryptoPart = MessageCryptoStructureDetector.findPrimaryEncryptedOrSignedPart(message, outputExtraParts);

        assertNull(cryptoPart);
    }

    @Test
    public void findEncryptedPartsShouldReturnEmptyListForEmptyMessage() throws Exception {
        MimeMessage emptyMessage = new MimeMessage();

        List<Part> encryptedParts = MessageCryptoStructureDetector.findMultipartEncryptedParts(emptyMessage);

        assertEquals(0, encryptedParts.size());
    }

    @Test
    public void findEncryptedPartsShouldReturnEmptyListForSimpleMessage() throws Exception {
        MimeMessage message = new MimeMessage();
        message.setBody(new TextBody("message text"));

        List<Part> encryptedParts = MessageCryptoStructureDetector.findMultipartEncryptedParts(message);

        assertEquals(0, encryptedParts.size());
    }

    @Test
    public void findEncrypted__withMultipartEncrypted__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("encrypted", "protocol=\"application/pgp-encrypted\"",
                        bodypart("application/pgp-encrypted"),
                        bodypart("application/octet-stream")
                )
        );

        List<Part> encryptedParts = MessageCryptoStructureDetector.findMultipartEncryptedParts(message);

        assertEquals(1, encryptedParts.size());
        assertSame(message, encryptedParts.get(0));
    }

    @Test
    public void findEncrypted__withBadProtocol__shouldReturnEmpty() throws Exception {
        Message message = messageFromBody(
                multipart("encrypted", "protocol=\"application/not-pgp-encrypted\"",
                        bodypart("application/pgp-encrypted"),
                        bodypart("application/octet-stream", "content")
                )
        );

        List<Part> encryptedParts = MessageCryptoStructureDetector.findMultipartEncryptedParts(message);

        assertTrue(encryptedParts.isEmpty());
    }

    @Test
    public void findEncrypted__withBadProtocolAndNoBody__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("encrypted",
                        bodypart("application/pgp-encrypted"),
                        bodypart("application/octet-stream")
                )
        );

        List<Part> encryptedParts = MessageCryptoStructureDetector.findMultipartEncryptedParts(message);

        assertEquals(1, encryptedParts.size());
        assertSame(message, encryptedParts.get(0));
    }

    @Test
    public void findEncrypted__withEmptyProtocol__shouldReturnEmpty() throws Exception {
        Message message = messageFromBody(
                multipart("encrypted",
                        bodypart("application/pgp-encrypted"),
                        bodypart("application/octet-stream", "content")
                )
        );

        List<Part> encryptedParts = MessageCryptoStructureDetector.findMultipartEncryptedParts(message);

        assertTrue(encryptedParts.isEmpty());
    }

    @Test
    public void findEncrypted__withMissingEncryptedBody__shouldReturnEmpty() throws Exception {
        Message message = messageFromBody(
                multipart("encrypted", "protocol=\"application/pgp-encrypted\"",
                        bodypart("application/pgp-encrypted")
                )
        );

        List<Part> encryptedParts = MessageCryptoStructureDetector.findMultipartEncryptedParts(message);

        assertTrue(encryptedParts.isEmpty());
    }

    @Test
    public void findEncrypted__withBadStructure__shouldReturnEmpty() throws Exception {
        Message message = messageFromBody(
                multipart("encrypted", "protocol=\"application/pgp-encrypted\"",
                        bodypart("application/octet-stream")
                )
        );

        List<Part> encryptedParts = MessageCryptoStructureDetector.findMultipartEncryptedParts(message);

        assertTrue(encryptedParts.isEmpty());
    }

    @Test
    public void findEncrypted__withMultipartMixedSubEncrypted__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("mixed",
                        multipart("encrypted", "protocol=\"application/pgp-encrypted\"",
                            bodypart("application/pgp-encrypted"),
                            bodypart("application/octet-stream")
                        )
                )
        );

        List<Part> encryptedParts = MessageCryptoStructureDetector.findMultipartEncryptedParts(message);

        assertEquals(1, encryptedParts.size());
        assertSame(getPart(message, 0), encryptedParts.get(0));
    }

    @Test
    public void findEncrypted__withMultipartMixedSubEncryptedAndEncrypted__shouldReturnBoth()
            throws Exception {
        Message message = messageFromBody(
                multipart("mixed",
                        multipart("encrypted", "protocol=\"application/pgp-encrypted\"",
                                bodypart("application/pgp-encrypted"),
                                bodypart("application/octet-stream")
                        ),
                        multipart("encrypted", "protocol=\"application/pgp-encrypted\"",
                                bodypart("application/pgp-encrypted"),
                                bodypart("application/octet-stream")
                        )
                )
        );

        List<Part> encryptedParts = MessageCryptoStructureDetector.findMultipartEncryptedParts(message);

        assertEquals(2, encryptedParts.size());
        assertSame(getPart(message, 0), encryptedParts.get(0));
        assertSame(getPart(message, 1), encryptedParts.get(1));
    }

    @Test
    public void findEncrypted__withMultipartMixedSubTextAndEncrypted__shouldReturnEncrypted() throws Exception {
        Message message = messageFromBody(
                multipart("mixed",
                        bodypart("text/plain"),
                        multipart("encrypted", "protocol=\"application/pgp-encrypted\"",
                                bodypart("application/pgp-encrypted"),
                                bodypart("application/octet-stream")
                        )
                )
        );

        List<Part> encryptedParts = MessageCryptoStructureDetector.findMultipartEncryptedParts(message);

        assertEquals(1, encryptedParts.size());
        assertSame(getPart(message, 1), encryptedParts.get(0));
    }

    @Test
    public void findEncrypted__withMultipartMixedSubEncryptedAndText__shouldReturnEncrypted() throws Exception {
        Message message = messageFromBody(
                multipart("mixed",
                        multipart("encrypted", "protocol=\"application/pgp-encrypted\"",
                                bodypart("application/pgp-encrypted"),
                                bodypart("application/octet-stream")
                        ),
                        bodypart("text/plain")
                )
        );

        List<Part> encryptedParts = MessageCryptoStructureDetector.findMultipartEncryptedParts(message);

        assertEquals(1, encryptedParts.size());
        assertSame(getPart(message, 0), encryptedParts.get(0));
    }

    @Test
    public void findSigned__withSimpleMultipartSigned__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("signed", "protocol=\"application/pgp-signature\"",
                        bodypart("text/plain"),
                        bodypart("application/pgp-signature")
                )
        );

        List<Part> signedParts = MessageCryptoStructureDetector
                .findMultipartSignedParts(message, messageCryptoAnnotations);

        assertEquals(1, signedParts.size());
        assertSame(message, signedParts.get(0));
    }

    @Test
    public void findSigned__withNoProtocolAndNoBody__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("signed",
                        bodypart("text/plain"),
                        bodypart("application/pgp-signature")
                )
        );

        List<Part> signedParts = MessageCryptoStructureDetector
                .findMultipartSignedParts(message, messageCryptoAnnotations);

        assertEquals(1, signedParts.size());
        assertSame(message, signedParts.get(0));
    }

    @Test
    public void findSigned__withBadProtocol__shouldReturnNothing() throws Exception {
        Message message = messageFromBody(
                multipart("signed", "protocol=\"application/not-pgp-signature\"",
                        bodypart("text/plain", "content"),
                        bodypart("application/pgp-signature")
                )
        );

        List<Part> signedParts = MessageCryptoStructureDetector
                .findMultipartSignedParts(message, messageCryptoAnnotations);

        assertTrue(signedParts.isEmpty());
    }

    @Test
    public void findSigned__withEmptyProtocol__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("signed",
                        bodypart("text/plain", "content"),
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
                multipart("signed", "protocol=\"application/pgp-signature\"",
                        bodypart("text/plain")
                )
        );

        List<Part> signedParts = MessageCryptoStructureDetector
                .findMultipartSignedParts(message, messageCryptoAnnotations);

        assertTrue(signedParts.isEmpty());
    }

    @Test
    public void findSigned__withComplexMultipartSigned__shouldReturnRoot() throws Exception {
        Message message = messageFromBody(
                multipart("signed", "protocol=\"application/pgp-signature\"",
                        multipart("mixed",
                                bodypart("text/plain"),
                                bodypart("application/pdf")
                        ),
                        bodypart("application/pgp-signature")
                )
        );

        List<Part> signedParts = MessageCryptoStructureDetector
                .findMultipartSignedParts(message, messageCryptoAnnotations);

        assertEquals(1, signedParts.size());
        assertSame(message, signedParts.get(0));
    }

    @Test
    public void findEncrypted__withMultipartMixedSubSigned__shouldReturnSigned() throws Exception {
        Message message = messageFromBody(
                multipart("mixed",
                        multipart("signed", "protocol=\"application/pgp-signature\"",
                                bodypart("text/plain"),
                                bodypart("application/pgp-signature")
                    )
                )
        );

        List<Part> signedParts = MessageCryptoStructureDetector
                .findMultipartSignedParts(message, messageCryptoAnnotations);

        assertEquals(1, signedParts.size());
        assertSame(getPart(message, 0), signedParts.get(0));
    }

    @Test
    public void findEncrypted__withMultipartMixedSubSignedAndText__shouldReturnSigned() throws Exception {
        Message message = messageFromBody(
                multipart("mixed",
                        multipart("signed", "application/pgp-signature",
                                bodypart("text/plain"),
                                bodypart("application/pgp-signature")
                        ),
                        bodypart("text/plain")
                )
        );

        List<Part> signedParts = MessageCryptoStructureDetector
                .findMultipartSignedParts(message, messageCryptoAnnotations);

        assertEquals(1, signedParts.size());
        assertSame(getPart(message, 0), signedParts.get(0));
    }

    @Test
    public void findEncrypted__withMultipartMixedSubTextAndSigned__shouldReturnSigned() throws Exception {
        Message message = messageFromBody(
                multipart("mixed",
                        bodypart("text/plain"),
                        multipart("signed", "application/pgp-signature",
                                bodypart("text/plain"),
                                bodypart("application/pgp-signature")
                        )
                )
        );

        List<Part> signedParts = MessageCryptoStructureDetector
                .findMultipartSignedParts(message, messageCryptoAnnotations);

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

        assertTrue(MessageCryptoStructureDetector.isPartPgpInlineEncrypted(message));
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

        assertTrue(MessageCryptoStructureDetector.isPartPgpInlineEncryptedOrSigned(message));
        assertTrue(MessageCryptoStructureDetector.isPartPgpInlineEncrypted(message));
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

        assertFalse(MessageCryptoStructureDetector.isPartPgpInlineEncryptedOrSigned(message));
        assertFalse(MessageCryptoStructureDetector.isPartPgpInlineEncrypted(message));
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

        assertTrue(MessageCryptoStructureDetector.isPartPgpInlineEncryptedOrSigned(message));
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

        assertFalse(MessageCryptoStructureDetector.isPartPgpInlineEncrypted(message));
    }

    static Part getPart(Part searchRootPart, int... indexes) {
        Part part = searchRootPart;
        for (int index : indexes) {
            part = ((Multipart) part.getBody()).getBodyPart(index);
        }
        return part;
    }
}
