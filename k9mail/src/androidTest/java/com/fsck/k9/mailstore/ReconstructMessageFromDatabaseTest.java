package com.fsck.k9.mailstore;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import android.content.Context;
import android.test.ApplicationTestCase;
import android.test.RenamingDelegatingContext;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import org.apache.james.mime4j.util.MimeUtil;


public class ReconstructMessageFromDatabaseTest extends ApplicationTestCase<K9> {

    public static final String MESSAGE_SOURCE = "From: from@example.com\r\n" +
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

    private Account account;

    public ReconstructMessageFromDatabaseTest() {
        super(K9.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        RenamingDelegatingContext context = new RenamingDelegatingContext(getContext(), "db-test-");
        setContext(context);

        BinaryTempFileBody.setTempDirectory(context.getCacheDir());

        createApplication();

        createDummyAccount(context);
    }

    private void createDummyAccount(Context context) {
        account = new DummyAccount(context);
    }

    public void testThatByteIdenticalCopyOfMessageIsReconstructed() throws IOException, MessagingException {

        LocalFolder folder = createFolderInDatabase();

        MimeMessage message = parseMessage();

        saveMessageToDatabase(folder, message);

        LocalMessage localMessage = readMessageFromDatabase(folder, message);

        String reconstructedMessage = writeMessageToString(localMessage);

        assertEquals(MESSAGE_SOURCE, reconstructedMessage);
    }

    public void testAddMissingPart() throws MessagingException, IOException {
        LocalFolder folder = createFolderInDatabase();

        MimeMessage message = new MimeMessage();
        message.addHeader("To", "to@example.com");
        message.addHeader("MIME-Version", "1.0");
        message.addHeader("Content-Type", "text/plain");
        message.setServerExtra("text");

        saveMessageToDatabase(folder, message);

        LocalMessage localMessage = readMessageFromDatabase(folder, message);

        assertEquals("to@example.com", localMessage.getHeader("To")[0]);
        assertEquals("text/plain", localMessage.getHeader(MimeHeader.HEADER_CONTENT_TYPE)[0]);
        assertEquals("text", localMessage.getServerExtra());
        assertNull(localMessage.getBody());

        Body body = new BinaryMemoryBody("Test message body".getBytes(), MimeUtil.ENC_7BIT);
        localMessage.setBody(body);
        folder.addPartToMessage(localMessage, localMessage);

        LocalMessage completeLocalMessage = readMessageFromDatabase(folder, message);
        String reconstructedMessage = writeMessageToString(completeLocalMessage);

        assertEquals("To: to@example.com\r\n" +
                "MIME-Version: 1.0\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                "Test message body",
                reconstructedMessage);
    }

    protected MimeMessage parseMessage() throws IOException, MessagingException {
        InputStream messageInputStream = new ByteArrayInputStream(MESSAGE_SOURCE.getBytes());
        try {
            return MimeMessage.parseMimeMessage(messageInputStream, true);
        } finally {
            messageInputStream.close();
        }
    }

    protected LocalFolder createFolderInDatabase() throws MessagingException {
        LocalStore localStore = LocalStore.getInstance(account, getApplication());
        LocalFolder inbox = localStore.getFolder("INBOX");
        localStore.createFolders(Collections.singletonList(inbox), 10);
        return inbox;
    }

    protected void saveMessageToDatabase(LocalFolder folder, MimeMessage message) throws MessagingException {
        folder.appendMessages(Collections.singletonList(message));
    }

    protected LocalMessage readMessageFromDatabase(LocalFolder folder, MimeMessage message) throws MessagingException {
        LocalMessage localMessage = folder.getMessage(message.getUid());

        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.ENVELOPE);
        fp.add(FetchProfile.Item.BODY);
        folder.fetch(Collections.singletonList(localMessage), fp, null);
        folder.close();

        return localMessage;
    }

    protected String writeMessageToString(LocalMessage localMessage) throws IOException, MessagingException {
        ByteArrayOutputStream messageOutputStream = new ByteArrayOutputStream();
        try {
            localMessage.writeTo(messageOutputStream);
        } finally {
            messageOutputStream.close();
        }

        return new String(messageOutputStream.toByteArray());
    }

    static class DummyAccount extends Account {

        protected DummyAccount(Context context) {
            super(context);
        }
    }
}
