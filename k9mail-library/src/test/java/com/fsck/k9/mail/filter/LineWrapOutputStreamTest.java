package com.fsck.k9.mail.filter;

import org.junit.Test;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by ployt0 on 2/15/17.
 * Java quotes by Bruce Eckel, Thinking In Java
 */
public class LineWrapOutputStreamTest {
    private ByteArrayOutputStream baos = new ByteArrayOutputStream(64000);
    private Random rand = new Random();

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
            "It's generally a good idea to encode\r\n"+
            "lines that begin with\r\n"+
            "From because some mail transport\r\n"+
            "agents will insert a greater-\r\n"+
            "than (>) sign, thus invalidating the\r\n"+
            "signature.\r\n"+
            "\r\n"+
            "Also, in some cases it might be\r\n"+
            "desirable to encode any    \r\n"+
            "trailing whitespace that occurs on\r\n"+
            "lines in order to ensure   \r\n"+
            "that the message signature is not\r\n"+
            "invalidated when passing  \r\n"+
            "a gateway that modifies such\r\n"+
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


    static final String arrayOfStrings[] = {//964 characters in 1st paragraph.
            "Encapsulation creates new data types by combining characteristics and behaviors. " +
                    "Implementation hiding separates the interface from the implementation by " +
                    "making the details private. This sort of mechanical organization makes " +
                    "ready sense to someone with a procedural programming background. But " +
                    "polymorphism deals with decoupling in terms of types. In the last " +
                    "chapter, you saw how inheritance allows the treatment of an object " +
                    "as its own type or its base type. This ability is critical because " +
                    "it allows many types (derived from the same base type) to be treated " +
                    "as if they were one type, and a single piece of code to work on all " +
                    "those different types equally. The polymorphic method call allows one " +
                    "type to express its distinction from another, similar type, as long as " +
                    "they’re both derived from the same base type. This distinction is " +
                    "expressed through differences in behavior of the methods that you " +
                    "can call through the base class.",


            "Both inheritance and composition allow you to create a new type from existing types. " +
                    "Composition reuses existing types as part of the underlying implementation of the new type, " +
                    "and inheritance reuses the interface." +
                    "With Inheritance, the derived class has the base-class interface, so it can be upcast to the " +
                    "base, which is critical for polymorphism, as you’ll see in the next chapter. " +
                    "Despite the strong emphasis on inheritance in object-oriented programming, when you start " +
                    "a design you should generally prefer composition (or possibly delegation) during the first cut " +
                    "and use inheritance only when it is clearly necessary. Composition tends to be more flexible. " +
                    "In addition, by using the added artifice of inheritance with your member type, you can " +
                    "change the exact type, and thus the behavior, of those member objects at run time. Therefore, " +
                    "you can change the behavior of the composed object at run time." +
                    "When designing a system, your goal is to find or create a set of classes in which each class has " +
                    "a specific use and is neither too big (encompassing so much functionality that it’s unwieldy to " +
                    "reuse) nor annoyingly small (you can’t use it by itself or without adding functionality). If your " +
                    "designs become too complex, it’s often helpful to add more objects by breaking down existing " +
                    "ones into smaller parts." +
                    "When you set out to design a system, it’s important to realize that program development is an " +
                    "incremental process, just like human learning. It relies on experimentation; you can do as " +
                    "much analysis as you want, but you still won’t know all the answers when you set out on a " +
                    "project. You’ll have much more success-and more immediate feedback-if you start out to " +
                    "“grow” your project as an organic, evolutionary creature, rather than constructing it all at " +
                    "once like a glass-box skyscraper. Inheritance and composition are two of the most " +
                    "fundamental tools in object-oriented programming that allow you to perform such " +
                    "experiments.",
    };

    @Test
    public void testWithForTrailingSpacePreservation() throws IOException {
        writeToLineWrapStream(20, baos);
        assertEquals("Mismatch with wrap length of 20", EXPECTED_WRAPPED_AT20, new String(
                baos.toByteArray(), "UTF-8"));
        writeToLineWrapStream(40, baos);
        assertEquals("Mismatch with wrap length of 40", EXPECTED_WRAPPED_AT40, new String(
                baos.toByteArray(), "UTF-8"));
        writeToLineWrapStream(60, baos);
        assertEquals("Mismatch with wrap length of 60", EXPECTED_WRAPPED_AT60, new String(
                baos.toByteArray(), "UTF-8"));
        writeToLineWrapStream(80, baos);
        assertEquals("Mismatch with wrap length of 80", EXPECTED_WRAPPED_AT80, new String(
                baos.toByteArray(), "UTF-8"));
    }

    private static void writeToLineWrapStream(int MAX_BYTES_PER_LINE, ByteArrayOutputStream baos)
            throws IOException {
        baos.reset();
        LineWrapOutputStream lwops = new LineWrapOutputStream(baos, MAX_BYTES_PER_LINE);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(lwops), 100);
        writer.write(INPUT_STRING);
        writer.flush();
    }

    /**
     * Feeds in some predefined texts without CRLF characters since we will be
     * assessing optimal, non-overflowing, wrapping and empty, excessive, breaks
     * would interfere.
     * @throws Exception
     */
    @Test
    public void testTightWrappingWithLongPresets() throws Exception {
        testTightWrapLongStatic(20, baos);
        testTightWrapLongStatic(40, baos);
        testTightWrapLongStatic(60, baos);
        testTightWrapLongStatic(80, baos);
        testTightWrapLongStatic(100, baos);
        testTightWrapLongStatic(120, baos);
    }

    private static void testTightWrapLongStatic(final int MAX_BYTES_PER_LINE
            , ByteArrayOutputStream baos) throws IOException {
        baos.reset();
        LineWrapOutputStream lwops = new LineWrapOutputStream(baos, MAX_BYTES_PER_LINE);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(lwops), 100);
        fillWithStaticData(writer);
        checkWrapsTight(MAX_BYTES_PER_LINE, baos);
    }

    private static void fillWithStaticData(BufferedWriter writer) throws IOException {
        for (String field : arrayOfStrings) {
            writer.write(field);
        }
        writer.flush();
    }

    private static void testTightWrapRandom(final int MAX_BYTES_PER_LINE
            , ByteArrayOutputStream baos, Random rand) throws IOException {
        baos.reset();
        LineWrapOutputStream lwops = new LineWrapOutputStream(baos, MAX_BYTES_PER_LINE);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(lwops), 100);
        fillWithRandomData(writer, MAX_BYTES_PER_LINE, rand);
        checkWrapsTight(MAX_BYTES_PER_LINE, baos);
    }

    private static void fillWithRandomData(BufferedWriter writer, int max_bytes_per_line
            , Random rand) throws IOException {
        final int NUM_LINES = 100 + rand.nextInt(400);
        for (int i = 0; i < NUM_LINES; ++i) {
            writeRandomLine(writer, max_bytes_per_line, rand);
        }
        writer.flush();
    }

    private static void writeRandomLine(BufferedWriter writer, int MAX_BYTES_PER_LINE
            , Random rand) throws IOException {
        final int MAX_WORD_LEN =  2 + rand.nextInt(MAX_BYTES_PER_LINE - 5);
        int nextWordPos = 2 + rand.nextInt(MAX_WORD_LEN);
        for (int i = 0; i < MAX_BYTES_PER_LINE; ++i) {
            if (nextWordPos <= i) {
                writer.write(' ');
                nextWordPos += 2 + rand.nextInt(MAX_WORD_LEN);
            } else {
                int nextByte = rand.nextInt(64000);
                while (nextByte == '\n' || nextByte == '\r') nextByte = rand.nextInt(64000);
                writer.write(nextByte);
            }
        }
    }

    /**
     * Random bytes because it is hard to count the bytes in preset strings
     * which are the datums on which wrapping occurs.
     * CRLF characters are filtered from the input since we will be
     * assessing optimal, non-overflowing, wrapping and empty, excessive, breaks
     * would interfere.
     * @throws Exception
     */
    @Test
    public void testTightWrappingWithRandBytes() throws Exception {
        for (int i = 0; i < 10; ++i) {
            testTightWrapRandom(20, baos, rand);
            testTightWrapRandom(40, baos, rand);
            testTightWrapRandom(60, baos, rand);
            testTightWrapRandom(80, baos, rand);
            testTightWrapRandom(100, baos, rand);
            testTightWrapRandom(120, baos, rand);
            testTightWrapRandom(240, baos, rand);
            testTightWrapRandom(480, baos, rand);
            testTightWrapRandom(1024, baos, rand);
            testTightWrapRandom(2048, baos, rand);
        }
    }

    private static void checkWrapsTight(int MAX_BYTES_PER_LINE, ByteArrayOutputStream baos) {
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
                        if (lastLinesLen >= 0) {
                            //Configuration for a 20 byte line will only attempt 17 + CRLF.
                            final int WASTED_LEN = 1;
                            if (MAX_BYTES_PER_LINE - 3 - WASTED_LEN - lastLinesLen >= firstSpace) {
                                String lastString = new String(bArray, lastLineStartPos
                                        , lineStartPos - lastLineStartPos - 2);
                                String curString = new String(bArray, lineStartPos
                                        , i);
                                try {
                                    System.out.println(new String(bArray, "UTF-8"));
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                fail(String.format("bytePos:%d. %d bytes spare on last line, this line starts" +
                                        " with a word of just %d bytes.\n1:\"%s\"\n2:\"%s\"",
                                        bytesRead - 1, MAX_BYTES_PER_LINE - 3 - lastLinesLen
                                        , firstSpace, lastString, curString));
                            }
                        }
                    }
                }
            }
        } while (bytesRead < baos.size());
    }
}