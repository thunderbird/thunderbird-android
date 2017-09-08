package com.fsck.k9.mail;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import android.content.Context;

import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.internet.BinaryTempFileMessageBody;
import com.fsck.k9.mail.internet.CharsetSupport;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.TextBody;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.util.MimeUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@RunWith(K9LibRobolectricTestRunner.class)
public class MessageTest {

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.application;

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        BinaryTempFileBody.setTempDirectory(context.getCacheDir());
    }

    private static final String SEVEN_BIT_RESULT =
              "From: from@example.com\r\n"
            + "To: to@example.com\r\n"
            + "Subject: Test Message\r\n"
            + "Date: Wed, 28 Aug 2013 08:51:09 -0400\r\n"
            + "MIME-Version: 1.0\r\n"
            + "Content-Type: multipart/mixed; boundary=\"----Boundary103\"\r\n"
            + "Content-Transfer-Encoding: 7bit\r\n"
            + "\r\n"
            + "------Boundary103\r\n"
            + "Content-Transfer-Encoding: quoted-printable\r\n"
            + "Content-Type: text/plain;\r\n"
            + " charset=utf-8\r\n"
            + "\r\n"
            + "Testing=2E\r\n"
            + "This is a text body with some greek characters=2E\r\n"
            + "=CE=B1=CE=B2=CE=B3=CE=B4=CE=B5=CE=B6=CE=B7=CE=B8\r\n"
            + "End of test=2E\r\n"
            + "\r\n"
            + "------Boundary103\r\n"
            + "Content-Type: application/octet-stream\r\n"
            + "Content-Transfer-Encoding: base64\r\n"
            + "\r\n"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/\r\n"
            + "\r\n"
            + "------Boundary103\r\n"
            + "Content-Type: message/rfc822\r\n"
            + "Content-Disposition: attachment\r\n"
            + "Content-Transfer-Encoding: 7bit\r\n"
            + "\r\n"
            + "From: from@example.com\r\n"
            + "To: to@example.com\r\n"
            + "Subject: Test Message\r\n"
            + "Date: Wed, 28 Aug 2013 08:51:09 -0400\r\n"
            + "MIME-Version: 1.0\r\n"
            + "Content-Type: multipart/mixed; boundary=\"----Boundary102\"\r\n"
            + "Content-Transfer-Encoding: 7bit\r\n"
            + "\r\n"
            + "------Boundary102\r\n"
            + "Content-Transfer-Encoding: quoted-printable\r\n"
            + "Content-Type: text/plain;\r\n"
            + " charset=utf-8\r\n"
            + "\r\n"
            + "Testing=2E\r\n"
            + "This is a text body with some greek characters=2E\r\n"
            + "=CE=B1=CE=B2=CE=B3=CE=B4=CE=B5=CE=B6=CE=B7=CE=B8\r\n"
            + "End of test=2E\r\n"
            + "\r\n"
            + "------Boundary102\r\n"
            + "Content-Type: application/octet-stream\r\n"
            + "Content-Transfer-Encoding: base64\r\n"
            + "\r\n"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/\r\n"
            + "\r\n"
            + "------Boundary102\r\n"
            + "Content-Type: message/rfc822\r\n"
            + "Content-Disposition: attachment\r\n"
            + "Content-Transfer-Encoding: 7bit\r\n"
            + "\r\n"
            + "From: from@example.com\r\n"
            + "To: to@example.com\r\n"
            + "Subject: Test Message\r\n"
            + "Date: Wed, 28 Aug 2013 08:51:09 -0400\r\n"
            + "MIME-Version: 1.0\r\n"
            + "Content-Type: multipart/mixed; boundary=\"----Boundary101\"\r\n"
            + "Content-Transfer-Encoding: 7bit\r\n"
            + "\r\n"
            + "------Boundary101\r\n"
            + "Content-Transfer-Encoding: quoted-printable\r\n"
            + "Content-Type: text/plain;\r\n"
            + " charset=utf-8\r\n"
            + "\r\n"
            + "Testing=2E\r\n"
            + "This is a text body with some greek characters=2E\r\n"
            + "=CE=B1=CE=B2=CE=B3=CE=B4=CE=B5=CE=B6=CE=B7=CE=B8\r\n"
            + "End of test=2E\r\n"
            + "\r\n"
            + "------Boundary101\r\n"
            + "Content-Type: application/octet-stream\r\n"
            + "Content-Transfer-Encoding: base64\r\n"
            + "\r\n"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/\r\n"
            + "\r\n"
            + "------Boundary101--\r\n"
            + "\r\n"
            + "------Boundary102--\r\n"
            + "\r\n"
            + "------Boundary103--\r\n";

    private static final String TO_BODY_PART_RESULT =
                    "Content-Type: multipart/mixed; boundary=\"----Boundary103\"\r\n"
                    + "Content-Transfer-Encoding: 7bit\r\n"
                    + "\r\n"
                    + "------Boundary103\r\n"
                    + "Content-Transfer-Encoding: quoted-printable\r\n"
                    + "Content-Type: text/plain;\r\n"
                    + " charset=utf-8\r\n"
                    + "\r\n"
                    + "Testing=2E\r\n"
                    + "This is a text body with some greek characters=2E\r\n"
                    + "=CE=B1=CE=B2=CE=B3=CE=B4=CE=B5=CE=B6=CE=B7=CE=B8\r\n"
                    + "End of test=2E\r\n"
                    + "\r\n"
                    + "------Boundary103\r\n"
                    + "Content-Type: application/octet-stream\r\n"
                    + "Content-Transfer-Encoding: base64\r\n"
                    + "\r\n"
                    + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/\r\n"
                    + "\r\n"
                    + "------Boundary103\r\n"
                    + "Content-Type: message/rfc822\r\n"
                    + "Content-Disposition: attachment\r\n"
                    + "Content-Transfer-Encoding: 7bit\r\n"
                    + "\r\n"
                    + "From: from@example.com\r\n"
                    + "To: to@example.com\r\n"
                    + "Subject: Test Message\r\n"
                    + "Date: Wed, 28 Aug 2013 08:51:09 -0400\r\n"
                    + "MIME-Version: 1.0\r\n"
                    + "Content-Type: multipart/mixed; boundary=\"----Boundary102\"\r\n"
                    + "Content-Transfer-Encoding: 7bit\r\n"
                    + "\r\n"
                    + "------Boundary102\r\n"
                    + "Content-Transfer-Encoding: quoted-printable\r\n"
                    + "Content-Type: text/plain;\r\n"
                    + " charset=utf-8\r\n"
                    + "\r\n"
                    + "Testing=2E\r\n"
                    + "This is a text body with some greek characters=2E\r\n"
                    + "=CE=B1=CE=B2=CE=B3=CE=B4=CE=B5=CE=B6=CE=B7=CE=B8\r\n"
                    + "End of test=2E\r\n"
                    + "\r\n"
                    + "------Boundary102\r\n"
                    + "Content-Type: application/octet-stream\r\n"
                    + "Content-Transfer-Encoding: base64\r\n"
                    + "\r\n"
                    + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/\r\n"
                    + "\r\n"
                    + "------Boundary102\r\n"
                    + "Content-Type: message/rfc822\r\n"
                    + "Content-Disposition: attachment\r\n"
                    + "Content-Transfer-Encoding: 7bit\r\n"
                    + "\r\n"
                    + "From: from@example.com\r\n"
                    + "To: to@example.com\r\n"
                    + "Subject: Test Message\r\n"
                    + "Date: Wed, 28 Aug 2013 08:51:09 -0400\r\n"
                    + "MIME-Version: 1.0\r\n"
                    + "Content-Type: multipart/mixed; boundary=\"----Boundary101\"\r\n"
                    + "Content-Transfer-Encoding: 7bit\r\n"
                    + "\r\n"
                    + "------Boundary101\r\n"
                    + "Content-Transfer-Encoding: quoted-printable\r\n"
                    + "Content-Type: text/plain;\r\n"
                    + " charset=utf-8\r\n"
                    + "\r\n"
                    + "Testing=2E\r\n"
                    + "This is a text body with some greek characters=2E\r\n"
                    + "=CE=B1=CE=B2=CE=B3=CE=B4=CE=B5=CE=B6=CE=B7=CE=B8\r\n"
                    + "End of test=2E\r\n"
                    + "\r\n"
                    + "------Boundary101\r\n"
                    + "Content-Type: application/octet-stream\r\n"
                    + "Content-Transfer-Encoding: base64\r\n"
                    + "\r\n"
                    + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/\r\n"
                    + "\r\n"
                    + "------Boundary101--\r\n"
                    + "\r\n"
                    + "------Boundary102--\r\n"
                    + "\r\n"
                    + "------Boundary103--\r\n";

    private int mMimeBoundary;

    @Test
    public void testSetSendDateSetsSentDate() throws Exception {
        Message message = sampleMessage();
        final int milliseconds = 0;
        Date date = new Date(milliseconds);
        message.setSentDate(date, false);
        Date sentDate = message.getSentDate();
        assertNotNull(sentDate);
        assertEquals(milliseconds, sentDate.getTime());
    }

    @Test
    public void testSetSendDateFormatsHeaderCorrectlyWithCurrentTimeZone() throws Exception {
        Message message = sampleMessage();
        message.setSentDate(new Date(0), false);
        assertEquals("Thu, 01 Jan 1970 09:00:00 +0900", message.getHeader("Date")[0]);
    }

    @Test
    public void testSetSendDateFormatsHeaderCorrectlyWithoutTimeZone() throws Exception {
        Message message = sampleMessage();
        message.setSentDate(new Date(0), true);
        assertEquals("Thu, 01 Jan 1970 00:00:00 +0000", message.getHeader("Date")[0]);
    }

    @Test
    public void testMessage() throws MessagingException, IOException {
        MimeMessage message;
        ByteArrayOutputStream out;

        BinaryTempFileBody.setTempDirectory(context.getCacheDir());

        mMimeBoundary = 101;
        message = nestedMessage(nestedMessage(sampleMessage()));
        out = new ByteArrayOutputStream();
        message.writeTo(out);
        assertEquals(SEVEN_BIT_RESULT, out.toString());
    }

    private MimeMessage nestedMessage(MimeMessage subMessage)
            throws MessagingException, IOException {
        BinaryTempFileMessageBody tempMessageBody = new BinaryTempFileMessageBody(MimeUtil.ENC_8BIT);

        OutputStream out = tempMessageBody.getOutputStream();
        try {
            subMessage.writeTo(out);
        } finally {
            out.close();
        }

        MimeBodyPart bodyPart = new MimeBodyPart(tempMessageBody, "message/rfc822");
        bodyPart.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, "attachment");
        bodyPart.setEncoding(MimeUtil.ENC_7BIT);

        MimeMessage parentMessage = sampleMessage();
        ((Multipart) parentMessage.getBody()).addBodyPart(bodyPart);

        return parentMessage;
    }

    private MimeMessage sampleMessage() throws MessagingException, IOException {
        MimeMessage message = new MimeMessage();
        message.setFrom(new Address("from@example.com"));
        message.setRecipient(RecipientType.TO, new Address("to@example.com"));
        message.setSubject("Test Message");
        message.setHeader("Date", "Wed, 28 Aug 2013 08:51:09 -0400");
        message.setEncoding(MimeUtil.ENC_7BIT);

        MimeMultipart multipartBody = new MimeMultipart("multipart/mixed", generateBoundary());
        multipartBody.addBodyPart(textBodyPart());
        multipartBody.addBodyPart(binaryBodyPart());
        MimeMessageHelper.setBody(message, multipartBody);

        return message;
    }

    private MimeBodyPart binaryBodyPart() throws IOException,
            MessagingException {
        String encodedTestString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "abcdefghijklmnopqrstuvwxyz0123456789+/\r\n";

        BinaryTempFileBody tempFileBody = new BinaryTempFileBody(MimeUtil.ENC_BASE64);

        InputStream in = new ByteArrayInputStream(
                encodedTestString.getBytes("UTF-8"));

        OutputStream out = tempFileBody.getOutputStream();
        try {
            IOUtils.copy(in, out);
        } finally {
            out.close();
        }

        MimeBodyPart bodyPart = new MimeBodyPart(tempFileBody,
                "application/octet-stream");
        bodyPart.setEncoding(MimeUtil.ENC_BASE64);

        return bodyPart;
    }

    private MimeBodyPart textBodyPart() throws MessagingException {
        TextBody textBody = new TextBody(
                  "Testing.\r\n"
                + "This is a text body with some greek characters.\r\n"
                + "αβγδεζηθ\r\n"
                + "End of test.\r\n");
        textBody.setCharset("utf-8");

        MimeBodyPart bodyPart = new MimeBodyPart();
        MimeMessageHelper.setBody(bodyPart, textBody);
        CharsetSupport.setCharset("utf-8", bodyPart);
        return bodyPart;
    }

    private String generateBoundary() {
        return "----Boundary" + Integer.toString(mMimeBoundary++);
    }

    @Test
    public void testToBodyPart() throws MessagingException, IOException {
        MimeMessage message;
        ByteArrayOutputStream out;

        BinaryTempFileBody.setTempDirectory(context.getCacheDir());

        mMimeBoundary = 101;
        message = nestedMessage(nestedMessage(sampleMessage()));
        out = new ByteArrayOutputStream();
        MimeBodyPart bodyPart = message.toBodyPart();
        bodyPart.writeTo(out);
        assertEquals(TO_BODY_PART_RESULT, out.toString());
    }

    class StoredMimeMessage extends MimeMessage {
        public StoredMimeMessage(Folder folder, String uid) {
            mFolder = folder;
            mUid = uid;
        }
    }

    class SimpleFolder extends Folder {

        String id;

        @Override
        public void open(int mode) throws MessagingException {}

        @Override
        public void close() {}

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public int getMode() {
            return 0;
        }

        @Override
        public boolean create(FolderType type) throws MessagingException {
            return false;
        }

        @Override
        public boolean exists() throws MessagingException {
            return false;
        }

        @Override
        public int getMessageCount() throws MessagingException {
            return 0;
        }

        @Override
        public int getUnreadMessageCount() throws MessagingException {
            return 0;
        }

        @Override
        public int getFlaggedMessageCount() throws MessagingException {
            return 0;
        }

        @Override
        public Message getMessage(String uid) throws MessagingException {
            return null;
        }

        @Override
        public List getMessages(int start, int end, Date earliestDate, MessageRetrievalListener listener)
                throws MessagingException {
            return null;
        }

        @Override
        public boolean areMoreMessagesAvailable(int indexOfOldestMessage, Date earliestDate)
                throws IOException, MessagingException {
            return false;
        }

        @Override
        public String getUidFromMessageId(Message message) throws MessagingException {
            return null;
        }

        @Override
        public void fetch(List messages, FetchProfile fp, MessageRetrievalListener listener) throws MessagingException {

        }

        @Override
        public void delete(boolean recurse) throws MessagingException {

        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void setFlags(Set set, boolean value) throws MessagingException {

        }

        @Override
        public void setFlags(List list, Set set, boolean value) throws MessagingException {

        }

        @Override
        public Map<String, String> appendMessages(List list) throws MessagingException {
            return null;
        }
    }

    @Test
    public void equals_whenFolderIdDifferent_isFalse() {
        SimpleFolder folder1 = new SimpleFolder();
        folder1.id = "1";
        SimpleFolder folder2 = new SimpleFolder();
        folder2.id = "2";
        String uid = "uid";
        Message m1 = new StoredMimeMessage(folder1, uid);
        Message m2 = new StoredMimeMessage(folder2, uid);

        boolean result = m1.equals(m2);

        assertFalse(result);
    }

    @Test
    public void equals_whenUidDifferent_isFalse() {
        SimpleFolder folder = new SimpleFolder();
        folder.id = "1";
        String uid1 = "uid1";
        String uid2 = "uid2";
        Message m1 = new StoredMimeMessage(folder, uid1);
        Message m2 = new StoredMimeMessage(folder, uid2);

        boolean result = m1.equals(m2);

        assertFalse(result);
    }

    @Test
    public void equals_whenUidAndFolderSame_isTrue() {
        SimpleFolder folder1 = new SimpleFolder();
        folder1.id = "1";
        SimpleFolder folder2 = new SimpleFolder();
        folder2.id = "1";
        String uid = "uid";
        Message m1 = new StoredMimeMessage(folder1, uid);
        Message m2 = new StoredMimeMessage(folder2, uid);

        boolean result = m1.equals(m2);

        assertTrue(result);
    }
}
