package com.fsck.k9.mail.internet;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MimeHeaderTest {
    private static final String TEST_WRITE_WITH_CONTENT_PARAMETERS =
            "Content-Type: multipart/signed; protocol=\"application-pgp\"; micalg=md5\r\n"
            + "  ; boundary=\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"\r\n";

    private static final String TEST_WRITE_WITH_CONTENT_PARAMETERS2 =
            "Content-Type: multipart/signed; protocol=\"application-pgp\"; micalg=md5\r\n"
                    + "  ; boundary=\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"\r\n"
            + "  ; param=\"sssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss\"\r\n";

    @Test
    public void testWriteToWithContentTypeParameters() throws IOException {
        MimeHeader header = new MimeHeader();
        header.setHeader("Content-Type", "multipart/signed");
        header.addContentTypeParameter("protocol", "\"application-pgp\"");
        header.addContentTypeParameter("micalg", "md5");
        header.addContentTypeParameter("boundary", "\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        header.writeTo(output);
        String strOutput = new String(output.toByteArray(), "US-ASCII");
        assertEquals(TEST_WRITE_WITH_CONTENT_PARAMETERS, strOutput);

        header.addContentTypeParameter("param", "\"sssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss\"");
        output = new ByteArrayOutputStream();
        header.writeTo(output);
        strOutput = new String(output.toByteArray(), "US-ASCII");
        assertEquals(TEST_WRITE_WITH_CONTENT_PARAMETERS2, strOutput);

        for (int i = 0; i < 20 ; i++){
            header.addContentTypeParameter("anotherparam" + i, "value");
        }
        output = new ByteArrayOutputStream();
        header.writeTo(output);
        strOutput = new String(output.toByteArray(), "US-ASCII");
        String[] splited = strOutput.split("\r\n");
        for (String line : splited) {
            assertTrue(line.length() <= 128);
        }
    }
}
