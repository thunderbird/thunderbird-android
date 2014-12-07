package com.fsck.k9.mail;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.test.AndroidTestCase;

import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.internet.MimeMessage;


public class ReconstructMessageTest extends AndroidTestCase {

    public void testMessage() throws IOException, MessagingException {
        String messageSource =
                "From: from@example.com\r\n" +
                "To: to@example.com\r\n" +
                "Subject: Test Message \r\n" +
                "Date: Thu, 13 Nov 2014 17:09:38 +0100\r\n" +
                "Content-Type: multipart/mixed;\r\n" +
                " boundary=\"----Boundary\"\r\n" +
                "Content-Transfer-Encoding: 8bit\r\n" +
                "MIME-Version: 1.0\r\n" +
                "\r\n" +
                "This is a multipart MIME message.\r\n" +
                "------Boundary\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Transfer-Encoding: 8bit\r\n" +
                "\r\n" +
                "Testing.\r\n" +
                "This is a text body with some greek characters.\r\n" +
                "αβγδεζηθ\r\n" +
                "End of test.\r\n" +
                "\r\n" +
                "------Boundary\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Transfer-Encoding: base64\r\n" +
                "\r\n" +
                "VGhpcyBpcyBhIHRl\r\n" +
                "c3QgbWVzc2FnZQ==\r\n" +
                "\r\n" +
                "------Boundary--\r\n" +
                "Hi, I'm the epilogue";

        BinaryTempFileBody.setTempDirectory(getContext().getCacheDir());

        InputStream messageInputStream = new ByteArrayInputStream(messageSource.getBytes());
        MimeMessage message;
        try {
            message = new MimeMessage(messageInputStream, true);
        } finally {
            messageInputStream.close();
        }

        ByteArrayOutputStream messageOutputStream = new ByteArrayOutputStream();
        try {
            message.writeTo(messageOutputStream);
        } finally {
            messageOutputStream.close();
        }

        String reconstructedMessage = new String(messageOutputStream.toByteArray());

        assertEquals(messageSource, reconstructedMessage);
    }
}
