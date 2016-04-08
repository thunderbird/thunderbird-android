package com.fsck.k9.crypto;


import java.util.List;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.TextBody;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class MessageDecryptVerifierTest {

    @Test
    public void findPgpEncryptedPartsShouldReturnEmptyListForEmptyMessage() throws Exception {
        MimeMessage emptyMessage = new MimeMessage();

        List<Part> encryptedParts = MessageDecryptVerifier.findPgpEncryptedParts(emptyMessage);

        assertEquals(0, encryptedParts.size());
    }

    @Test
    public void findPgpEncryptedPartsShouldReturnEmptyListForSimpleMessage() throws Exception {
        MimeMessage message = new MimeMessage();
        message.setBody(new TextBody("message text"));

        List<Part> encryptedParts = MessageDecryptVerifier.findPgpEncryptedParts(message);

        assertEquals(0, encryptedParts.size());
    }

    @Test
    public void findPgpEncryptedPartsShouldReturnEmptyEncryptedPart() throws Exception {
        MimeMessage message = new MimeMessage();
        MimeMultipart multipartEncrypted = new MimeMultipart();
        multipartEncrypted.setSubType("encrypted");
        MimeMessageHelper.setBody(message, multipartEncrypted);
        addPgpProtocolParameter(message);

        List<Part> encryptedParts = MessageDecryptVerifier.findPgpEncryptedParts(message);

        assertEquals(1, encryptedParts.size());
        assertSame(message, encryptedParts.get(0));
    }

    @Test
    public void findPgpEncryptedPartsShouldReturnMultipleEncryptedParts() throws Exception {
        MimeMessage message = new MimeMessage();
        MimeMultipart multipartMixed = new MimeMultipart();
        multipartMixed.setSubType("mixed");
        MimeMessageHelper.setBody(message, multipartMixed);

        MimeMultipart multipartEncryptedOne = new MimeMultipart();
        multipartEncryptedOne.setSubType("encrypted");
        MimeBodyPart bodyPartOne = new MimeBodyPart(multipartEncryptedOne);
        addPgpProtocolParameter(bodyPartOne);
        multipartMixed.addBodyPart(bodyPartOne);

        MimeBodyPart bodyPartTwo = new MimeBodyPart(null, "text/plain");
        multipartMixed.addBodyPart(bodyPartTwo);

        MimeMultipart multipartEncryptedThree = new MimeMultipart();
        multipartEncryptedThree.setSubType("encrypted");
        MimeBodyPart bodyPartThree = new MimeBodyPart(multipartEncryptedThree);
        addPgpProtocolParameter(bodyPartThree);
        multipartMixed.addBodyPart(bodyPartThree);

        List<Part> encryptedParts = MessageDecryptVerifier.findPgpEncryptedParts(message);

        assertEquals(2, encryptedParts.size());
        assertSame(bodyPartOne, encryptedParts.get(0));
        assertSame(bodyPartThree, encryptedParts.get(1));
    }

    //TODO: Find a cleaner way to do this
    private void addPgpProtocolParameter(Part part) throws MessagingException {
        String contentType = part.getContentType();
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType + "; protocol=\"application/pgp-encrypted\"");
    }

    @Test
    public void findSmimeEncryptedPartsShouldReturnEmptyListForEmptyMessage() throws Exception {
        MimeMessage emptyMessage = new MimeMessage();
        List<Part> encryptedParts = MessageDecryptVerifier.findSmimeEncryptedParts(emptyMessage);
        assertEquals(0, encryptedParts.size());
    }

    //TODO: Test finding SMIME encrypted part
}
