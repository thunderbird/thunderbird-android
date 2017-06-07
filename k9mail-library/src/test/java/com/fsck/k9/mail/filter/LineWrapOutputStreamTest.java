package com.fsck.k9.mail.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class LineWrapOutputStreamTest {
    private static final String INPUT_STRING =
            "This is going to be a long test string with some longer words."
                    + " This allows testing for splitting words when needed."
                    + " This will also mean a shortened maxLineLength.\r\n"
                    + "ParticularlyLongWordToBeSplit.\r\n"
                    + "This will also test for some end of lines.";

    private static final String EXPECTED_OUTPUT_28 =
            "This is going to be a long\r\ntest string with some longer\r\nwords."
                    + " This allows testing\r\nfor splitting words when\r\nneeded."
                    + " This will also mean\r\na shortened maxLineLength.\r\n"
                    + "ParticularlyLongWordToBeSpli\r\nt.\r\n"
                    + "This will also test for some\r\nend of lines.";

    private static final String EXPECTED_OUTPUT_30 =
            "This is going to be a long\r\ntest string with some longer\r\nwords."
                    + " This allows testing for\r\nsplitting words when needed."
                    + "\r\nThis will also mean a\r\nshortened maxLineLength.\r\n"
                    + "ParticularlyLongWordToBeSplit.\r\n"
                    + "This will also test for some\r\nend of lines.";

    @Test
    public void testLineWrapLength28() throws IOException{
        OutputStream outputStream = new ByteArrayOutputStream();
        LineWrapOutputStream lineWrapOutputStream = new LineWrapOutputStream(outputStream, 28+2);
        lineWrapOutputStream.write(INPUT_STRING.getBytes("US-ASCII"));
        lineWrapOutputStream.flush();
        assertEquals("Failed on length 28",EXPECTED_OUTPUT_28,outputStream.toString());
    }

    @Test
    public void testLineWrapLength30() throws IOException{
        OutputStream outputStream = new ByteArrayOutputStream();
        LineWrapOutputStream lineWrapOutputStream = new LineWrapOutputStream(outputStream, 30+2);
        lineWrapOutputStream.write(INPUT_STRING.getBytes("US-ASCII"));
        lineWrapOutputStream.flush();
        assertEquals("Failed on length 30",EXPECTED_OUTPUT_30,outputStream.toString());
    }
}
