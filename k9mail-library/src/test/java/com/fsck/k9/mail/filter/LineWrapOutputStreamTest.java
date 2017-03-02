package com.fsck.k9.mail.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class LineWrapOutputStreamTest {
    private static final int maxLineLength = 28;
    private static final String INPUT_STRING =
            "This is going to be a long test string with some longer words."
                    + " This allows testing for splitting words when needed."
                    + " This will also mean a shortened maxLineLength.\r\n"
                    + "ParticularlyLongWordToBeSplit.\r\n"
                    + "This will also test for some end of lines.";

    private static final String EXPECTED_OUTPUT =
            "This is going to be a long\r\ntest string with some longer\r\nwords."
                    + " This allows testing\r\nfor splitting words when\r\nneeded."
                    + " This will also mean\r\na shortened maxLineLength.\r\n"
                    + "ParticularlyLongWordToBeSpli\r\nt.\r\n"
                    + "This will also test for some\r\nend of lines.";

    @Test
    public void testLineWrap() throws IOException{
        OutputStream outputStream = new ByteArrayOutputStream();
        LineWrapOutputStream lineWrapOutputStream = new LineWrapOutputStream(outputStream, maxLineLength+2);
        lineWrapOutputStream.write(INPUT_STRING.getBytes("US-ASCII"));
        lineWrapOutputStream.flush();
        assertEquals(EXPECTED_OUTPUT,outputStream.toString());
    }
}
