package com.fsck.k9.crypto;


import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
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
import static junit.framework.Assert.assertSame;
import static org.mockito.Mockito.mock;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class MessageDecryptVerifierTest {

    private List<MimeBodyPart> createdMultiparts = new ArrayList<>();
    private MessageCryptoAnnotations messageCryptoAnnotations = mock(MessageCryptoAnnotations.class);

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
        MimeMultipart multipartEncrypted = new MimeMultipart();
        multipartEncrypted.setSubType("encrypted");
        MimeMessageHelper.setBody(message, multipartEncrypted);
        addProtocolParameter(message);

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);

        assertEquals(1, encryptedParts.size());
        assertSame(message, encryptedParts.get(0));
    }

    @Test
    public void findEncryptedPartsShouldReturnMultipleEncryptedParts() throws Exception {
        MimeMessage message = new MimeMessage();
        MimeMultipart multipartMixed = new MimeMultipart();
        multipartMixed.setSubType("mixed");
        MimeMessageHelper.setBody(message, multipartMixed);

        MimeMultipart multipartEncryptedOne = new MimeMultipart();
        multipartEncryptedOne.setSubType("encrypted");
        MimeBodyPart bodyPartOne = new MimeBodyPart(multipartEncryptedOne);
        addProtocolParameter(bodyPartOne);
        multipartMixed.addBodyPart(bodyPartOne);

        MimeBodyPart bodyPartTwo = new MimeBodyPart(null, "text/plain");
        multipartMixed.addBodyPart(bodyPartTwo);

        MimeMultipart multipartEncryptedThree = new MimeMultipart();
        multipartEncryptedThree.setSubType("encrypted");
        MimeBodyPart bodyPartThree = new MimeBodyPart(multipartEncryptedThree);
        addProtocolParameter(bodyPartThree);
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
        assertSame(createdMultiparts.get(0), encryptedParts.get(0));
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
        assertSame(createdMultiparts.get(0), encryptedParts.get(0));
        assertSame(createdMultiparts.get(1), encryptedParts.get(1));
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
        assertSame(createdMultiparts.get(0), encryptedParts.get(0));
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
        assertSame(createdMultiparts.get(0), encryptedParts.get(0));
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
        assertSame(createdMultiparts.get(0), signedParts.get(0));
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
        assertSame(createdMultiparts.get(0), signedParts.get(0));
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
        assertSame(createdMultiparts.get(0), signedParts.get(0));
    }

    MimeMessage messageFromBody(BodyPart bodyPart) throws MessagingException {
        MimeMessage message = new MimeMessage();
        MimeMessageHelper.setBody(message, bodyPart.getBody());
        return message;
    }

    MimeBodyPart multipart(String type, BodyPart... subParts) throws MessagingException {
        MimeMultipart multiPart = new MimeMultipart();
        multiPart.setSubType(type);
        for (BodyPart subPart : subParts) {
            multiPart.addBodyPart(subPart);
        }
        MimeBodyPart resultPart = new MimeBodyPart(multiPart);
        createdMultiparts.add(resultPart);
        return resultPart;
    }

    BodyPart bodypart(String type) throws MessagingException {
        return new MimeBodyPart(null, type);
    }

    //TODO: Find a cleaner way to do this
    private static void addProtocolParameter(Part part) throws MessagingException {
        String contentType = part.getContentType();
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType + "; protocol=\"application/pgp-encrypted\"");
    }
}
