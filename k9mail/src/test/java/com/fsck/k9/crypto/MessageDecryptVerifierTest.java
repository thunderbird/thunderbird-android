package com.fsck.k9.crypto;


import java.util.List;

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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class MessageDecryptVerifierTest {

    public static final String MIME_TYPE_MULTIPART_ENCRYPTED = "multipart/encrypted";
    private MessageCryptoAnnotations messageCryptoAnnotations = mock(MessageCryptoAnnotations.class);
    public static final String PROTCOL_PGP_ENCRYPTED = "application/pgp-encrypted";

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
