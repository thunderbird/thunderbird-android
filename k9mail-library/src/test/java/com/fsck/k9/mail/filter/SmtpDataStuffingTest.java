package com.fsck.k9.mail.filter;

import java.io.IOException;
import java.io.ByteArrayOutputStream;

import okio.ByteString;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class SmtpDataStuffingTest {
    private String expectedMessageAfterStuffing;
    private String actualMessageAfterStuffing;

    @Before
    public void setUp(){
        expectedMessageAfterStuffing = "";
        actualMessageAfterStuffing = "";
    }

    @Test
    public void smtpDotStuffing_OnlyStuffingDotTest() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        SmtpDataStuffing smtpDataStuffingStream = new SmtpDataStuffing(byteArrayOutputStream);
        byte[] data = ByteString.encodeUtf8("Hello dot\r\n.").toByteArray();

        expectedMessageAfterStuffing = "Hello dot\r\n..";
        try {
            smtpDataStuffingStream.write(data);
        }
        finally {
            smtpDataStuffingStream.close();
            byteArrayOutputStream.close();
        }
        actualMessageAfterStuffing = ByteString.of(byteArrayOutputStream.toByteArray()).utf8();

        assertEquals(expectedMessageAfterStuffing, actualMessageAfterStuffing);
    }

    @Test
    public void smtpDotStuffing_NoStuffingDotTest() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        SmtpDataStuffing smtpDataStuffingStream = new SmtpDataStuffing(byteArrayOutputStream);
        byte[] data = ByteString.encodeUtf8("...Hello .. dots.").toByteArray();

        expectedMessageAfterStuffing = "...Hello .. dots.";
        try {
            smtpDataStuffingStream.write(data);
        }
        finally {
            smtpDataStuffingStream.close();
            byteArrayOutputStream.close();
        }
        actualMessageAfterStuffing = ByteString.of(byteArrayOutputStream.toByteArray()).utf8();

        assertEquals(expectedMessageAfterStuffing, actualMessageAfterStuffing);
    }

    @Test
    public void smtpDotStuffing_StuffingAndNoStuffingDotsTest() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        SmtpDataStuffing smtpDataStuffingStream = new SmtpDataStuffing(byteArrayOutputStream);
        byte[] data = ByteString.encodeUtf8("\r\n.Hello . dots.\r\n..\r\n.\r\n...").toByteArray();

        expectedMessageAfterStuffing = "\r\n..Hello . dots.\r\n...\r\n..\r\n....";
        try {
            smtpDataStuffingStream.write(data);
        }
        finally {
            smtpDataStuffingStream.close();
            byteArrayOutputStream.close();
        }
        actualMessageAfterStuffing = ByteString.of(byteArrayOutputStream.toByteArray()).utf8();

        assertEquals(expectedMessageAfterStuffing, actualMessageAfterStuffing);
    }
}