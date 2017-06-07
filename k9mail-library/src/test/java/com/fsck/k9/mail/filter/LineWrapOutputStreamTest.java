package com.fsck.k9.mail.filter;

import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import okio.Buffer;
import okio.ByteString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by ployt0 on 2/15/17.
 */
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


    //Approx 2000 characters.
    static final String someLongString = "Python is an easy to learn, powerful programming " +
            "language. It has efficient high-level data structures and a simple but effective " +
            "approach to object-oriented programming. Python’s elegant syntax and dynamic typing," +
            " together with its interpreted nature, make it an ideal language for scripting and " +
            "rapid application development in many areas on most platforms.   The Python " +
            "interpreter and the extensive standard library are freely available in source or " +
            "binary form for all major platforms from the Python Web site, https://www.python" +
            ".org/, and may be freely distributed. The same site also contains distributions of " +
            "and pointers to many free third party Python modules, programs and tools, and " +
            "additional documentation.   The Python interpreter is easily extended with new " +
            "functions and data types implemented in C or C++ (or other languages callable from " +
            "C). Python is also suitable as an extension language for customizable applications. " +
            "  This tutorial introduces the reader informally to the basic concepts and features " +
            "of the Python language and system. It helps to have a Python interpreter handy for " +
            "hands-on experience, but all examples are self-contained, so the tutorial can be " +
            "read off-line as well.   For a description of standard objects and modules, see The " +
            "Python Standard Library. The Python Language Reference gives a more formal " +
            "definition of the language. To write extensions in C or C++, read Extending and " +
            "Embedding the Python Interpreter and Python/C API Reference Manual. There are also " +
            "several books covering Python in depth.   This tutorial does not attempt to be " +
            "comprehensive and cover every single feature, or even every commonly used feature. " +
            "Instead, it introduces many of Python’s most noteworthy features, and will give you " +
            "a good idea of the language’s flavor and style. After reading it, you will be able " +
            "to read and write Python modules and programs, and you will be ready to learn more " +
            "about the various Python library modules described in The Python Standard Library.  " +
            " The Glossary is also worth going through.";

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

    /**
     * Feeds in some predefined texts without CRLF characters since we will be
     * assessing optimal, non-overflowing, wrapping: empty, excessive, breaks
     * would interfere.
     *
     * @throws Exception
     */
    @Test
    public void testThatWrappingIsTight() throws Exception {
        Buffer baos = new Buffer();
        testTightWrapLongStatic(20, baos);
        testTightWrapLongStatic(40, baos);
        testTightWrapLongStatic(60, baos);
        testTightWrapLongStatic(80, baos);
        testTightWrapLongStatic(100, baos);
        testTightWrapLongStatic(120, baos);
    }

    private static void testTightWrapLongStatic(final int MAX_BYTES_PER_LINE
            , Buffer baos) throws IOException {
        baos.clear();
        LineWrapOutputStream lwops = new LineWrapOutputStream(baos.outputStream(), MAX_BYTES_PER_LINE);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(lwops), 100);
        fillWithStaticData(writer);
        checkWrapsTight(MAX_BYTES_PER_LINE, baos.readByteString());
    }

    private static void fillWithStaticData(BufferedWriter writer) throws IOException {
        writer.write(someLongString);
        writer.flush();
    }

    private static void checkWrapsTight(int MAX_BYTES_PER_LINE, ByteString baos) {
        byte bArray[] = baos.toByteArray();
        int bytesRead = 0;
        int lastLinesLen = -1;
        int lineStartPos = 0;
        int lastLineStartPos = -1;
        do {
            int firstSpace = -1;
            for (int i = 0; i < MAX_BYTES_PER_LINE && bytesRead < bArray.length; ++i) {
                byte curByte = bArray[bytesRead++];
                if (curByte == '\r') {
                    lastLinesLen = i;
                    lastLineStartPos = lineStartPos;
                    lineStartPos = ++bytesRead;
                    break;
                }
                if (curByte == ' ') {
                    if (firstSpace < 0) {
                        firstSpace = i;
                        firstSpace = assert1stWordCouldntHaveBeenLast(MAX_BYTES_PER_LINE, bArray
                                , bytesRead, lastLinesLen, lineStartPos, lastLineStartPos
                                , firstSpace, i);
                    }
                }
            }
        } while (bytesRead < baos.size());
    }

    private static int assert1stWordCouldntHaveBeenLast(int MAX_BYTES_PER_LINE, byte[] bArray
            , int bytesRead, int lastLinesLen, int lineStartPos, int lastLineStartPos
            , int firstSpace, int i) {
        if (lastLinesLen >= 0) {
            //Configuration for a 20 byte line will only attempt 17 + CRLF.
            final int TRAILING_SPACE = 1;
            if (MAX_BYTES_PER_LINE - 3 - TRAILING_SPACE - lastLinesLen >= firstSpace) {
                String lastString = new String(bArray, lastLineStartPos
                        , lineStartPos - lastLineStartPos - 2);
                String curString = new String(bArray, lineStartPos
                        , i);
                fail(String.format("bytePos:%d. %d bytes spare on last line, this line starts" +
                                " with a word of just %d bytes.\n1:\"%s\"\n2:\"%s\"",
                        bytesRead - 1, MAX_BYTES_PER_LINE - 3 - lastLinesLen
                        , firstSpace, lastString, curString));
            }
        }
        return firstSpace;
    }
}