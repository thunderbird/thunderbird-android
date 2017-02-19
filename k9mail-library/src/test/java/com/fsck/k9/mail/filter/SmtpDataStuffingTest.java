package com.fsck.k9.mail.filter;

import java.io.IOException;
import java.io.ByteArrayOutputStream;

import okio.ByteString;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class SmtpDataStuffingTest {
    @Test
    public void smtpDotStuffing_stuffingOnlyDotTest() throws IOException {
        String expectedMessageAfterStuffing = "Hello dot\r\n..";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        SmtpDataStuffing smtpDataStuffingStream = new SmtpDataStuffing(byteArrayOutputStream);
        byte[] data = ByteString.encodeUtf8("Hello dot\r\n.").toByteArray();

        smtpDataStuffingStream.write(data);
        smtpDataStuffingStream.close();
        byteArrayOutputStream.close();
        String actualMessageAfterStuffing = ByteString.of(byteArrayOutputStream.toByteArray()).utf8();

        assertEquals(expectedMessageAfterStuffing, actualMessageAfterStuffing);
    }

    @Test
    public void smtpDotStuffing_noStuffingDotTest() throws IOException {
        String expectedMessageAfterStuffing = "...Hello .. dots.";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        SmtpDataStuffing smtpDataStuffingStream = new SmtpDataStuffing(byteArrayOutputStream);
        byte[] data = ByteString.encodeUtf8("...Hello .. dots.").toByteArray();

        smtpDataStuffingStream.write(data);
        smtpDataStuffingStream.close();
        byteArrayOutputStream.close();
        String actualMessageAfterStuffing = ByteString.of(byteArrayOutputStream.toByteArray()).utf8();

        assertEquals(expectedMessageAfterStuffing, actualMessageAfterStuffing);
    }

    @Test
    public void smtpDotStuffing_stuffingAndNoStuffingDotTest() throws IOException {
        String expectedMessageAfterStuffing = "\r\n..Hello . dots.\r\n...\r\n..\r\n....";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        SmtpDataStuffing smtpDataStuffingStream = new SmtpDataStuffing(byteArrayOutputStream);
        byte[] data = ByteString.encodeUtf8("\r\n.Hello . dots.\r\n..\r\n.\r\n...").toByteArray();

        smtpDataStuffingStream.write(data);
        smtpDataStuffingStream.close();
        byteArrayOutputStream.close();
        String actualMessageAfterStuffing = ByteString.of(byteArrayOutputStream.toByteArray()).utf8();

        assertEquals(expectedMessageAfterStuffing, actualMessageAfterStuffing);
    }
}
