package com.fsck.k9.crypto;


import java.util.List;

import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.TextBody;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MessageDecryptVerifierTest {

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
        MimeMultipart mulitpartEncrypted = new MimeMultipart();
        mulitpartEncrypted.setSubType("encrypted");
        MimeMessageHelper.setBody(message, mulitpartEncrypted);

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

        MimeMultipart mulitpartEncryptedOne = new MimeMultipart();
        mulitpartEncryptedOne.setSubType("encrypted");
        MimeBodyPart bodyPartOne = new MimeBodyPart(mulitpartEncryptedOne);
        multipartMixed.addBodyPart(bodyPartOne);

        MimeBodyPart bodyPartTwo = new MimeBodyPart(null, "text/plain");
        multipartMixed.addBodyPart(bodyPartTwo);

        MimeMultipart mulitpartEncryptedThree = new MimeMultipart();
        mulitpartEncryptedThree.setSubType("encrypted");
        MimeBodyPart bodyPartThree = new MimeBodyPart(mulitpartEncryptedThree);
        multipartMixed.addBodyPart(bodyPartThree);

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);
        assertEquals(2, encryptedParts.size());
        assertSame(bodyPartOne, encryptedParts.get(0));
        assertSame(bodyPartThree, encryptedParts.get(1));
    }
}
