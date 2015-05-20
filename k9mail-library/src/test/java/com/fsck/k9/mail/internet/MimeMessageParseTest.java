package com.fsck.k9.mail.internet;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.Multipart;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MimeMessageParseTest {
    @Before
    public void setup() {
        BinaryTempFileBody.setTempDirectory(new File(System.getProperty("java.io.tmpdir")));
    }

    @Test
    public void testSinglePart7BitNoRecurse() throws Exception {
        MimeMessage msg = parseWithoutRecurse(toStream(
                "From: <adam@example.org>\r\n" +
                        "To: <eva@example.org>\r\n" +
                        "Subject: Testmail\r\n" +
                        "MIME-Version: 1.0\r\n" +
                        "Content-type: text/plain\r\n" +
                        "Content-Transfer-Encoding: 7bit\r\n" +
                        "\r\n" +
                        "this is some test text."));

        checkAddresses(msg.getFrom(), "adam@example.org");
        checkAddresses(msg.getRecipients(RecipientType.TO), "eva@example.org");
        assertEquals("Testmail", msg.getSubject());
        assertEquals("text/plain", msg.getContentType());
        assertEquals("this is some test text.", streamToString(MimeUtility.decodeBody(msg.getBody())));
    }

    @Test
    public void testSinglePart8BitRecurse() throws Exception {
        MimeMessage msg = parseWithRecurse(toStream(
                "From: <adam@example.org>\r\n" +
                        "To: <eva@example.org>\r\n" +
                        "Subject: Testmail\r\n" +
                        "MIME-Version: 1.0\r\n" +
                        "Content-type: text/plain; encoding=ISO-8859-1\r\n" +
                        "Content-Transfer-Encoding: 8bit\r\n" +
                        "\r\n" +
                        "gefährliche Umlaute"));

        checkAddresses(msg.getFrom(), "adam@example.org");
        checkAddresses(msg.getRecipients(RecipientType.TO), "eva@example.org");
        assertEquals("Testmail", msg.getSubject());
        assertEquals("text/plain; encoding=ISO-8859-1", msg.getContentType());
        assertEquals("gefährliche Umlaute", streamToString(MimeUtility.decodeBody(msg.getBody())));
    }

    @Test
    public void testSinglePartBase64NoRecurse() throws Exception {
        MimeMessage msg = parseWithoutRecurse(toStream(
                "From: <adam@example.org>\r\n" +
                        "To: <eva@example.org>\r\n" +
                        "Subject: Testmail\r\n" +
                        "MIME-Version: 1.0\r\n" +
                        "Content-type: text/plain\r\n" +
                        "Content-Transfer-Encoding: base64\r\n" +
                        "\r\n" +
                        "dGhpcyBpcyBzb21lIG1vcmUgdGVzdCB0ZXh0Lg==\r\n"));

        checkAddresses(msg.getFrom(), "adam@example.org");
        checkAddresses(msg.getRecipients(RecipientType.TO), "eva@example.org");
        assertEquals("Testmail", msg.getSubject());
        assertEquals("text/plain", msg.getContentType());
        assertEquals("this is some more test text.", streamToString(MimeUtility.decodeBody(msg.getBody())));
    }

    @Test
    public void testMultipartSingleLayerNoRecurse() throws Exception {
        MimeMessage msg = parseWithoutRecurse(toStream(
                "From: <x@example.org>\r\n" +
                        "To: <y@example.org>\r\n" +
                        "Subject: Testmail 2\r\n" +
                        "MIME-Version: 1.0\n" +
                        "Content-Type: multipart/mixed; boundary=frontier\n" +
                        "\n" +
                        "This is a message with multiple parts in MIME format.\n" +
                        "--frontier\n" +
                        "Content-Type: text/plain\n" +
                        "\n" +
                        "This is the body of the message.\n" +
                        "--frontier\n" +
                        "Content-Type: application/octet-stream\n" +
                        "Content-Transfer-Encoding: base64\n" +
                        "\n" +
                        "PGh0bWw+CiAgPGhlYWQ+CiAgPC9oZWFkPgogIDxib2R5PgogICAgPHA+VGhpcyBpcyB0aGUg\n" +
                        "Ym9keSBvZiB0aGUgbWVzc2FnZS48L3A+CiAgPC9ib2R5Pgo8L2h0bWw+Cg=\n" +
                        "--frontier--"));

        checkAddresses(msg.getFrom(), "x@example.org");
        checkAddresses(msg.getRecipients(RecipientType.TO), "y@example.org");
        assertEquals("Testmail 2", msg.getSubject());
        assertEquals("multipart/mixed; boundary=frontier", msg.getContentType());
        checkLeafParts(msg,
                "This is the body of the message.",
                "<html>\n" +
                        "  <head>\n" +
                        "  </head>\n" +
                        "  <body>\n" +
                        "    <p>This is the body of the message.</p>\n" +
                        "  </body>\n" +
                        "</html>\n" +
                        "");
    }

    @Test
    public void testMultipartSingleLayerRecurse() throws Exception {
        MimeMessage msg = parseWithRecurse(toStream(
                "From: <x@example.org>\r\n" +
                        "To: <y@example.org>\r\n" +
                        "Subject: Testmail 2\r\n" +
                        "MIME-Version: 1.0\n" +
                        "Content-Type: multipart/mixed; boundary=frontier\n" +
                        "\n" +
                        "This is a message with multiple parts in MIME format.\n" +
                        "--frontier\n" +
                        "Content-Type: text/plain\n" +
                        "\n" +
                        "This is the body of the message.\n" +
                        "--frontier\n" +
                        "Content-Type: application/octet-stream\n" +
                        "Content-Transfer-Encoding: base64\n" +
                        "\n" +
                        "PGh0bWw+CiAgPGhlYWQ+CiAgPC9oZWFkPgogIDxib2R5PgogICAgPHA+VGhpcyBpcyB0aGUg\n" +
                        "Ym9keSBvZiB0aGUgbWVzc2FnZS48L3A+CiAgPC9ib2R5Pgo8L2h0bWw+Cg=\n" +
                        "--frontier--"));

        checkAddresses(msg.getFrom(), "x@example.org");
        checkAddresses(msg.getRecipients(RecipientType.TO), "y@example.org");
        assertEquals("Testmail 2", msg.getSubject());
        assertEquals("multipart/mixed; boundary=frontier", msg.getContentType());
        checkLeafParts(msg,
                "This is the body of the message.",
                "<html>\n" +
                        "  <head>\n" +
                        "  </head>\n" +
                        "  <body>\n" +
                        "    <p>This is the body of the message.</p>\n" +
                        "  </body>\n" +
                        "</html>\n" +
                        "");
    }

    @Test
    public void testMultipartTwoLayersRecurse() throws Exception {
        MimeMessage msg = parseWithRecurse(toStream(
                "From: <x@example.org>\r\n" +
                        "To: <y@example.org>\r\n" +
                        "Subject: Testmail 2\r\n" +
                        "MIME-Version: 1.0\n" +
                        "Content-Type: multipart/mixed; boundary=1\n" +
                        "\n" +
                        "This is a message with multiple parts in MIME format.\n" +
                        "--1\n" +
                        "Content-Type: text/plain\n" +
                        "\n" +
                        "some text in the first part\n" +
                        "--1\n" +
                        "Content-Type: multipart/alternative; boundary=2\n" +
                        "\n" +
                        "--2\n" +
                        "Content-Type: text/plain\n" +
                        "\n" +
                        "alternative 1\n" +
                        "--2\n" +
                        "Content-Type: text/plain\n" +
                        "\n" +
                        "alternative 2\n" +
                        "--2--\n" +
                        "--1--"));

        checkAddresses(msg.getFrom(), "x@example.org");
        checkAddresses(msg.getRecipients(RecipientType.TO), "y@example.org");
        assertEquals("Testmail 2", msg.getSubject());
        assertEquals("multipart/mixed; boundary=1", msg.getContentType());
        checkLeafParts(msg,
                "some text in the first part",
                "alternative 1",
                "alternative 2");
    }


    private static ByteArrayInputStream toStream(String rawMailData) throws Exception {
        return new ByteArrayInputStream(rawMailData.getBytes("ISO-8859-1"));
    }

    private static MimeMessage parseWithoutRecurse(InputStream data) throws Exception {
        return new MimeMessage(data, false);
    }

    private static MimeMessage parseWithRecurse(InputStream data) throws Exception {
        return new MimeMessage(data, true);
    }

    private static void checkAddresses(Address[] actual, String... expected) {
        for (int i = 0; i < actual.length; i++) {
            assertEquals(actual[i].toEncodedString(), expected[i]);
        }
        assertEquals(expected.length, actual.length);
    }

    private static String streamToString(InputStream stream) throws Exception {
        return IOUtils.toString(stream, "ISO-8859-1");
    }

    private static List<Body> getLeafParts(Body body) {
        if (body instanceof Multipart) {
            List<Body> ret = new ArrayList<Body>();
            for (BodyPart child : ((Multipart) body).getBodyParts()) {
                ret.addAll(getLeafParts(child.getBody()));
            }
            return ret;
        } else {
            return Collections.singletonList(body);
        }
    }

    private static void checkLeafParts(MimeMessage msg, String... expectedParts) throws Exception {
        List<String> actual = new ArrayList<String>();
        for (Body leaf : getLeafParts(msg.getBody())) {
            actual.add(streamToString(MimeUtility.decodeBody(leaf)));
        }
        assertEquals(Arrays.asList(expectedParts), actual);
    }
}
