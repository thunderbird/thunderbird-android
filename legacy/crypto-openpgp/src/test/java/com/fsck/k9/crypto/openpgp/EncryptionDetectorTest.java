package com.fsck.k9.crypto.openpgp;


import com.fsck.k9.mail.Message;
import com.fsck.k9.message.extractors.TextPartFinder;
import org.junit.Before;
import org.junit.Test;

import static com.fsck.k9.crypto.openpgp.MessageCreationHelper.createMessage;
import static com.fsck.k9.crypto.openpgp.MessageCreationHelper.createMultipartMessage;
import static com.fsck.k9.crypto.openpgp.MessageCreationHelper.createPart;
import static com.fsck.k9.crypto.openpgp.MessageCreationHelper.createTextMessage;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class EncryptionDetectorTest {
    private static final String CRLF = "\r\n";


    private EncryptionDetector encryptionDetector;


    @Before
    public void setUp() {
        encryptionDetector = new EncryptionDetector(new TextPartFinder());
    }

    @Test
    public void isEncrypted_withTextPlain_shouldReturnFalse() {
        Message message = createTextMessage("text/plain", "plain text");

        boolean encrypted = encryptionDetector.isEncrypted(message);

        assertFalse(encrypted);
    }

    @Test
    public void isEncrypted_withMultipartEncrypted_shouldReturnTrue() throws Exception {
        Message message = createMultipartMessage("multipart/encrypted",
                createPart("application/octet-stream"), createPart("application/octet-stream"));

        boolean encrypted = encryptionDetector.isEncrypted(message);

        assertTrue(encrypted);
    }

    @Test
    public void isEncrypted_withSMimePart_shouldReturnTrue() {
        Message message = createMessage("application/pkcs7-mime");

        boolean encrypted = encryptionDetector.isEncrypted(message);

        assertTrue(encrypted);
    }

    @Test
    public void isEncrypted_withMultipartMixedContainingSMimePart_shouldReturnTrue() throws Exception {
        Message message = createMultipartMessage("multipart/mixed",
                createPart("application/pkcs7-mime"), createPart("text/plain"));

        boolean encrypted = encryptionDetector.isEncrypted(message);

        assertTrue(encrypted);
    }

    @Test
    public void isEncrypted_withInlinePgp_shouldReturnTrue() {
        Message message = createTextMessage("text/plain", "" +
                "-----BEGIN PGP MESSAGE-----" + CRLF +
                "some encrypted stuff here" + CRLF +
                "-----END PGP MESSAGE-----");

        boolean encrypted = encryptionDetector.isEncrypted(message);

        assertTrue(encrypted);
    }

    @Test
    public void isEncrypted_withPlainTextAndPreambleWithInlinePgp_shouldReturnFalse() {
        Message message = createTextMessage("text/plain", "" +
                "preamble" + CRLF +
                "-----BEGIN PGP MESSAGE-----" + CRLF +
                "some encrypted stuff here" + CRLF +
                "-----END PGP MESSAGE-----" + CRLF +
                "epilogue");

        boolean encrypted = encryptionDetector.isEncrypted(message);

        assertFalse(encrypted);
    }

    @Test
    public void isEncrypted_withQuotedInlinePgp_shouldReturnFalse() {
        Message message = createTextMessage("text/plain", "" +
                "good talk!" + CRLF +
                CRLF +
                "> -----BEGIN PGP MESSAGE-----" + CRLF +
                "> some encrypted stuff here" + CRLF +
                "> -----END PGP MESSAGE-----" + CRLF +
                CRLF +
                "-- " + CRLF +
                "my signature");

        boolean encrypted = encryptionDetector.isEncrypted(message);

        assertFalse(encrypted);
    }
}
