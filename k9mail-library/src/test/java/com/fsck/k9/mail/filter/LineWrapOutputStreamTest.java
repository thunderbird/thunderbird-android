package com.fsck.k9.mail.filter;

import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import okio.Buffer;

import static org.junit.Assert.assertEquals;


public class LineWrapOutputStreamTest {

    private static final String INPUT_STRING =
            "It's generally a good idea to encode lines that begin with\r\n"
                    + "From because some mail transport agents will insert a greater-\r\n"
                    + "than (>) sign, thus invalidating the signature.\r\n\r\n"
                    + "Also, in some cases it might be desirable to encode any    \r\n"
                    + "trailing whitespace that occurs on lines in order to ensure   \r\n"
                    + "that the message signature is not invalidated when passing  \r\n"
                    + "a gateway that modifies such whitespace (like BITNET).  \r\n\r\n";


    private static final String EXPECTED_WRAPPED_AT20 =
            "It's generally a\r\n" +
                    "good idea to\r\n" +
                    "encode lines that\r\n" +
                    "begin with\r\n" +
                    "From because some\r\n" +
                    "mail transport\r\n" +
                    "agents will\r\n" +
                    "insert a greater-\r\n" +
                    "than (>) sign,\r\n" +
                    "thus invalidating\r\n" +
                    "the signature.\r\n" +
                    "\r\n" +
                    "Also, in some\r\n" +
                    "cases it might be\r\n" +
                    "desirable to\r\n" +
                    "encode any    \r\n" +
                    "trailing\r\n" +
                    "whitespace that\r\n" +
                    "occurs on lines\r\n" +
                    "in order to\r\n" +
                    "ensure   \r\n" +
                    "that the message\r\n" +
                    "signature is not\r\n" +
                    "invalidated when\r\n" +
                    "passing  \r\n" +
                    "a gateway that\r\n" +
                    "modifies such\r\n" +
                    "whitespace (like\r\n" +
                    "BITNET).  \r\n\r\n";

    private static final String EXPECTED_WRAPPED_AT40 =
            "It's generally a good idea to encode\r\n" +
                    "lines that begin with\r\n" +
                    "From because some mail transport\r\n" +
                    "agents will insert a greater-\r\n" +
                    "than (>) sign, thus invalidating the\r\n" +
                    "signature.\r\n" +
                    "\r\n" +
                    "Also, in some cases it might be\r\n" +
                    "desirable to encode any    \r\n" +
                    "trailing whitespace that occurs on\r\n" +
                    "lines in order to ensure   \r\n" +
                    "that the message signature is not\r\n" +
                    "invalidated when passing  \r\n" +
                    "a gateway that modifies such\r\n" +
                    "whitespace (like BITNET).  \r\n\r\n";

    private static final String EXPECTED_WRAPPED_AT60 =
            "It's generally a good idea to encode lines that begin\r\n" +
                    "with\r\n" +
                    "From because some mail transport agents will insert a\r\n" +
                    "greater-\r\n" +
                    "than (>) sign, thus invalidating the signature.\r\n" +
                    "\r\n" +
                    "Also, in some cases it might be desirable to encode any  \r\n" +
                    " \r\n" +
                    "trailing whitespace that occurs on lines in order to\r\n" +
                    "ensure   \r\n" +
                    "that the message signature is not invalidated when\r\n" +
                    "passing  \r\n" +
                    "a gateway that modifies such whitespace (like BITNET).  \r\n\r\n";

    private static final String EXPECTED_WRAPPED_AT80 =
            "It's generally a good idea to encode lines that begin with\r\n" +
                    "From because some mail transport agents will insert a greater-\r\n" +
                    "than (>) sign, thus invalidating the signature.\r\n" +
                    "\r\n" +
                    "Also, in some cases it might be desirable to encode any    \r\n" +
                    "trailing whitespace that occurs on lines in order to ensure   \r\n" +
                    "that the message signature is not invalidated when passing  \r\n" +
                    "a gateway that modifies such whitespace (like BITNET).  \r\n\r\n";

    @Test
    public void testWithForTrailingSpacePreservation() throws IOException {
        Buffer baos = new Buffer();
        writeToLineWrapStream(20, baos);
        assertEquals("Mismatch with wrap length of 20", EXPECTED_WRAPPED_AT20, baos.readUtf8());
        writeToLineWrapStream(40, baos);
        assertEquals("Mismatch with wrap length of 40", EXPECTED_WRAPPED_AT40, baos.readUtf8());
        writeToLineWrapStream(60, baos);
        assertEquals("Mismatch with wrap length of 60", EXPECTED_WRAPPED_AT60, baos.readUtf8());
        writeToLineWrapStream(80, baos);
        assertEquals("Mismatch with wrap length of 80", EXPECTED_WRAPPED_AT80, baos.readUtf8());
    }

    private static void writeToLineWrapStream(int MAX_BYTES_PER_LINE, Buffer baos)
            throws IOException {
        baos.clear();
        LineWrapOutputStream lwops = new LineWrapOutputStream(baos.outputStream(), MAX_BYTES_PER_LINE);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(lwops), 100);
        writer.write(INPUT_STRING);
        writer.flush();
    }
}