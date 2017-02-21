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
        smtpDataStuffing.write(bytesFor("...Hello .. dots."));

        //FIXME: The first line is a line, too. So This should be dot stuffed.
        // See https://tools.ietf.org/html/rfc5321#section-4.5.2
        assertEquals("...Hello .. dots.", buffer.readUtf8());
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
