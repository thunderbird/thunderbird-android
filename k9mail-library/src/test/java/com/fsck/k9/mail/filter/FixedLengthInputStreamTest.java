package com.fsck.k9.mail.filter;


import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import okio.Buffer;

import static org.junit.Assert.assertEquals;


@SuppressWarnings("ResultOfMethodCallIgnored")
public class FixedLengthInputStreamTest {
    private static final int UNDERSIZED_LIMIT = 200;
    private static final int LARGE_LIMIT = 1000;
    private static int strLength;
    private static String string;
    private InputStream inputStream;

    @BeforeClass
    public static void setUp() {
        string =
                "MEnASSKHcqghDICuZtxZPtVHuIFHNtNFBFBfrDWJhnzVoPdZuNXvuXgPAhOJLKEoGkzWiMlCZKKMJfbWcwzgSWEzHlIpSxFoMALb" +
                        "bYEmStiGGKBhiIPfQikVsOnBkdfXuMJVOmMYeIBrpMMhExSndzYQcbczqCnhFJanfnTbsyFrIYLdpEcyYQBzirKRYrWvBqzjJJXN" +
                        "mgEWthlQjdUvaxrhmKcsyQyMxTUNOgGBhWjyGQwtsxsLzcSHvtJbXXYHYDsnHEFPDRVpTtHdbahaoKFgZPLYiiNOmYqxzNcXmJTQ" +
                        "AbPjqeTumnrStJcWmnexWhouoyaVwVnGmiGpIvAyuHNomOaPUTxxfYeoGfGWCxjGiEorNQpCESRxzrGrFlsWQzSIIVBFPSLHZwhz" +
                        "nGLzFPszsoPWHAfMUpsqcqqCFeWyOlfkFElGXiKUQeaAWibIFczowqJThbqEmOZdAugggzlJwnbRzEVRtkKCSoMyppiMsTGYqbZV";
        strLength = string.length();
    }

    @Before
    public void initInputStream() {
        inputStream = new Buffer().writeUtf8(string).inputStream();
    }

    @Test
    public void testLongInputWithUnderSizedLimit() throws IOException {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream, UNDERSIZED_LIMIT);
        byte[] bytes = new byte[UNDERSIZED_LIMIT];

        fixedLengthInputStream.read(bytes, 0, 500);

        assertEquals(new String(bytes), string.substring(0, UNDERSIZED_LIMIT));
    }

    @Test
    public void testLengthLimitWithUndersizedLimit() throws IOException {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream, UNDERSIZED_LIMIT);

        fixedLengthInputStream.read(new byte[UNDERSIZED_LIMIT], 0, UNDERSIZED_LIMIT);

        int b = fixedLengthInputStream.read();
        assertEquals(-1, b);
    }

    @Test
    public void testMultiReadWithUndersizedLimit() throws IOException {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream, UNDERSIZED_LIMIT);
        byte[] bytes = new byte[UNDERSIZED_LIMIT];

        fixedLengthInputStream.read(bytes, 0, 99);
        fixedLengthInputStream.read(bytes, 99, 301);

        final String expected = string.substring(0, UNDERSIZED_LIMIT);
        assertEquals(expected, new String(bytes));
    }


    @Test
    public void testLongInputWithLargeLimit() throws IOException {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream, LARGE_LIMIT);
        byte[] bytes = new byte[LARGE_LIMIT];

        fixedLengthInputStream.read(bytes, 0, 700);

        assertEquals(string, new String(bytes).substring(0, strLength));
    }

    @Test
    public void testLengthLimitWithLargeLimit() throws IOException {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream, LARGE_LIMIT);

        fixedLengthInputStream.read(new byte[LARGE_LIMIT], 0, LARGE_LIMIT);

        int b = fixedLengthInputStream.read();
        assertEquals(-1, b);
    }

    @Test
    public void testMultiReadWithLargeLimit() throws IOException {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream, LARGE_LIMIT);
        byte[] bytes = new byte[700];

        fixedLengthInputStream.read(bytes, 0, 399);
        fixedLengthInputStream.read(bytes, 399, 301);

        final String actual = new String(bytes).substring(0, strLength);
        assertEquals(string, actual);
    }

    @Test
    public void testAvailable() throws IOException {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream, UNDERSIZED_LIMIT);

        fixedLengthInputStream.read();

        int actual = fixedLengthInputStream.available();
        assertEquals(UNDERSIZED_LIMIT - 1, actual);
    }

    @Test
    public void testSkip() throws IOException {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream, LARGE_LIMIT);

        fixedLengthInputStream.skip(250);

        int actual = fixedLengthInputStream.available();
        assertEquals(LARGE_LIMIT - 250, actual);
    }

    @Test
    public void testSkipRemaining() throws IOException {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream, UNDERSIZED_LIMIT);

        fixedLengthInputStream.skipRemaining();

        assertEquals(-1, fixedLengthInputStream.read());
    }

    @Test
    public void testRead() throws IOException {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream, UNDERSIZED_LIMIT);
        fixedLengthInputStream.skip(100);

        int b = fixedLengthInputStream.read();

        assertEquals(string.charAt(100), b);
    }

    @Test
    public void testReadBytes() throws IOException {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream, UNDERSIZED_LIMIT);
        final byte[] bytes = new byte[150];

        fixedLengthInputStream.read(bytes);

        assertEquals(string.substring(0, 150), new String(bytes));
    }

}
