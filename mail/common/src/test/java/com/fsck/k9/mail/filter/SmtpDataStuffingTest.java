package com.fsck.k9.mail.filter;


import java.io.IOException;

import okio.Buffer;
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;


public class SmtpDataStuffingTest {
    private Buffer buffer;
    private SmtpDataStuffing smtpDataStuffing;

    @Before
    public void setUp() throws Exception {
        buffer = new Buffer();
        smtpDataStuffing = new SmtpDataStuffing(buffer.outputStream());
    }

    @Test
    public void dotAtStartOfLine() throws IOException {
        smtpDataStuffing.write(bytesFor("Hello dot\r\n."));

        assertEquals("Hello dot\r\n..", buffer.readUtf8());
    }

    @Test
    public void dotAtStartOfStream() throws IOException {
        smtpDataStuffing.write(bytesFor(".Hello dots"));

        assertEquals("..Hello dots", buffer.readUtf8());
    }

    @Test
    public void linesNotStartingWithDot() throws IOException {
        smtpDataStuffing.write(bytesFor("Hello\r\nworld\r\n"));

        assertEquals("Hello\r\nworld\r\n", buffer.readUtf8());
    }

    @Test
    public void dotsThatNeedStuffingMixedWithOnesThatDoNot() throws IOException {
        smtpDataStuffing.write(bytesFor("\r\n.Hello . dots.\r\n..\r\n.\r\n..."));

        assertEquals("\r\n..Hello . dots.\r\n...\r\n..\r\n....", buffer.readUtf8());
    }

    private byte[] bytesFor(String input) {
        return ByteString.encodeUtf8(input).toByteArray();
    }
}
