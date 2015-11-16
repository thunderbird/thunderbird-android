package com.fsck.k9.mail.filter;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by alexandre on 11/10/15.
 */
public class SignSafeOutputStreamTest {
    private static final String INPUT_STRING = "It's generally a good idea to encode lines that begin with\r\n"
            + "From because some mail transport agents will insert a greater-\r\n"
            + "than (>) sign, thus invalidating the signature.\r\n\r\n"
            + "Also, in some cases it might be desirable to encode any    \r\n"
            + "trailing whitespace that occurs on lines in order to ensure   \r\n"
            + "that the message signature is not invalidated when passing  \r\n"
            + "a gateway that modifies such whitespace (like BITNET).  \r\n\r\n";

    private static final String EXPECTED = "It's generally a good idea to encode lines that begin with\r\n"
            + "From=20because some mail transport agents will insert a greater-\r\n"
            + "than (>) sign, thus invalidating the signature.\r\n\r\n"
            + "Also, in some cases it might be desirable to encode any   =20\r\n"
            + "trailing whitespace that occurs on lines in order to ensure  =20\r\n"
            + "that the message signature is not invalidated when passing =20\r\n"
            + "a gateway that modifies such whitespace (like BITNET). =20\r\n\r\n";

    @Test
    public void testWrite() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new SignSafeOutputStream(output).write(INPUT_STRING.getBytes("US-ASCII"));
        assertEquals(EXPECTED, new String(output.toByteArray(), "US-ASCII"));
    }
}
