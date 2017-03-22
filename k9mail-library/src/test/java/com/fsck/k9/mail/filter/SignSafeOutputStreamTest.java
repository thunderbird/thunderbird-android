package com.fsck.k9.mail.filter;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.james.mime4j.codec.QuotedPrintableOutputStream;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SignSafeOutputStreamTest {
    private static final String INPUT_STRING =
            "It's generally a good idea to encode lines that begin with\r\n"
                    + "From because some mail transport agents will insert a greater-\r\n"
                    + "than (>) sign, thus invalidating the signature.\r\n\r\n"
                    + "Also, in some cases it might be desirable to encode any    \r\n"
                    + "trailing whitespace that occurs on lines in order to ensure   \r\n"
                    + "that the message signature is not invalidated when passing  \r\n"
                    + "a gateway that modifies such whitespace (like BITNET).  \r\n\r\n";

    private static final String EXPECTED_QUOTED_PRINTABLE =
            "It's generally a good idea to encode lines that begin with\r\n"
                    + "From because some mail transport agents will insert a greater-\r\n"
                    + "than (>) sign, thus invalidating the signature=2E\r\n\r\n"
                    + "Also, in some cases it might be desirable to encode any   =20\r\n"
                    + "trailing whitespace that occurs on lines in order to ensure  =20\r\n"
                    + "that the message signature is not invalidated when passing =20\r\n"
                    + "a gateway that modifies such whitespace (like BITNET)=2E =20\r\n\r\n";

    private static final String EXPECTED_QUOTED_PRINTABLE_SIGNSAFE =
            "It's generally a good idea to encode lines that begin with\r\n"
                    + "From=20because some mail transport agents will insert a greater-\r\n"
                    + "than (>) sign, thus invalidating the signature=2E\r\n\r\n"
                    + "Also, in some cases it might be desirable to encode any   =20\r\n"
                    + "trailing whitespace that occurs on lines in order to ensure  =20\r\n"
                    + "that the message signature is not invalidated when passing =20\r\n"
                    + "a gateway that modifies such whitespace (like BITNET)=2E =20\r\n\r\n";

    private static final String EXPECTED_SIGNSAFE =
            "It's generally a good idea to encode lines that begin with\r\n"
                    + "From=20because some mail transport agents will insert a greater-\r\n"
                    + "than (>) sign, thus invalidating the signature.\r\n\r\n"
                    + "Also, in some cases it might be desirable to encode any    \r\n"
                    + "trailing whitespace that occurs on lines in order to ensure   \r\n"
                    + "that the message signature is not invalidated when passing  \r\n"
                    + "a gateway that modifies such whitespace (like BITNET).  \r\n\r\n";

    @Test
    public void testSignSafeOutputStream() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        OutputStream output = new SignSafeOutputStream(byteArrayOutputStream);
        output.write(INPUT_STRING.getBytes("US-ASCII"));
        output.close();

        assertEquals(EXPECTED_SIGNSAFE, new String(byteArrayOutputStream.toByteArray(), "US-ASCII"));
    }

    @Test
    public void testSignSafeQuotedPrintableOutputStream() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        OutputStream signSafeOutputStream = new SignSafeOutputStream(byteArrayOutputStream);
        OutputStream quotedPrintableOutputStream = new QuotedPrintableOutputStream(signSafeOutputStream, false);
        quotedPrintableOutputStream.write(INPUT_STRING.getBytes("US-ASCII"));
        quotedPrintableOutputStream.close();
        signSafeOutputStream.close();

        assertEquals(EXPECTED_QUOTED_PRINTABLE_SIGNSAFE,
                new String(byteArrayOutputStream.toByteArray(), "US-ASCII"));
    }

    @Test
    public void testQuotedPrintableOutputStream() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        OutputStream output = new QuotedPrintableOutputStream(byteArrayOutputStream, false);
        output.write(INPUT_STRING.getBytes("US-ASCII"));
        output.close();

        assertEquals(EXPECTED_QUOTED_PRINTABLE,
                new String(byteArrayOutputStream.toByteArray(), "US-ASCII"));
    }
}
