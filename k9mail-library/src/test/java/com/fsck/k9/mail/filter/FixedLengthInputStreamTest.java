package com.fsck.k9.mail.filter;


import java.io.IOException;
import java.io.InputStream;

import okio.Buffer;
import okio.ByteString;
import okio.Okio;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class FixedLengthInputStreamTest {
    @Test
    public void readingStream_shouldReturnDataUpToLimit() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello world"), 5);

        String readString = readStreamAsUtf8String(fixedLengthInputStream);

        assertEquals("Hello", readString);
    }

    @Test
    public void readingStream_shouldNotConsumeMoreThanLimitFromUnderlyingInputStream() throws Exception {
        InputStream inputStream = inputStream("Hello world");
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream, 5);

        exhaustStream(fixedLengthInputStream);

        assertRemainingInputStreamEquals(" world", inputStream);
    }

    @Test
    //TODO: Maybe this should throw. The underlying stream delivering less bytes than expected is most likely an error.
    public void readingStream_withLimitGreaterThanNumberOfBytesInUnderlyingInputStream() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello World"), 100);

        String readString = readStreamAsUtf8String(fixedLengthInputStream);

        assertEquals("Hello World", readString);
    }

    @Test
    public void read_withOverSizedByteArray_shouldReturnDataUpToLimit() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello World"), 6);

        byte[] data = new byte[100];
        int numberOfBytesRead = fixedLengthInputStream.read(data);

        assertEquals(6, numberOfBytesRead);
        assertEquals("Hello ", ByteString.of(data, 0, numberOfBytesRead).utf8());
    }

    @Test
    public void read_withOverSizedByteArray_shouldNotConsumeMoreThanLimitFromUnderlyingStream() throws Exception {
        InputStream inputStream = inputStream("Hello World");
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream, 6);

        //noinspection ResultOfMethodCallIgnored
        fixedLengthInputStream.read(new byte[100]);

        assertRemainingInputStreamEquals("World", inputStream);
    }

    @Test
    public void read_withByteArraySmallerThanLimit_shouldConsumeSizeOfByteArray() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello World"), 6);

        byte[] data = new byte[5];
        int numberOfBytesRead = fixedLengthInputStream.read(data);

        assertEquals(5, numberOfBytesRead);
        assertEquals("Hello", ByteString.of(data).utf8());
    }

    @Test
    public void read_withOverSizedByteArrayInMiddleOfStream_shouldReturnDataUpToLimit() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello World"), 6);
        consumeBytes(fixedLengthInputStream, 5);

        byte[] data = new byte[10];
        int numberOfBytesRead = fixedLengthInputStream.read(data);

        assertEquals(1, numberOfBytesRead);
        assertEquals(" ", ByteString.of(data, 0, numberOfBytesRead).utf8());
    }

    @Test
    public void read_withOverSizedByteArrayInMiddleOfStream_shouldNotConsumeMoreThanLimitFromUnderlyingStream()
            throws Exception {
        InputStream inputStream = inputStream("Hello World");
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream, 6);
        consumeBytes(fixedLengthInputStream, 5);

        //noinspection ResultOfMethodCallIgnored
        fixedLengthInputStream.read(new byte[10]);

        assertRemainingInputStreamEquals("World", inputStream);
    }

    @Test
    public void read_atStartOfStream() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Word"), 2);

        int readByte = fixedLengthInputStream.read();

        assertEquals('W', (char) readByte);
    }

    @Test
    public void read_inMiddleOfStream() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Word"), 2);
        consumeBytes(fixedLengthInputStream, 1);

        int readByte = fixedLengthInputStream.read();

        assertEquals('o', (char) readByte);
    }

    @Test
    public void read_atEndOfStream_shouldReturnMinusOne() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello world"), 5);
        exhaustStream(fixedLengthInputStream);

        int readByte = fixedLengthInputStream.read();

        assertEquals(-1, readByte);
    }

    @Test
    public void readArray_atEndOfStream_shouldReturnMinusOne() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello world"), 5);
        exhaustStream(fixedLengthInputStream);

        int numberOfBytesRead = fixedLengthInputStream.read(new byte[2]);

        assertEquals(-1, numberOfBytesRead);
    }

    @Test
    public void readArrayWithOffset_atEndOfStream_shouldReturnMinusOne() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello world"), 5);
        exhaustStream(fixedLengthInputStream);

        int numberOfBytesRead = fixedLengthInputStream.read(new byte[2], 0, 2);

        assertEquals(-1, numberOfBytesRead);
    }

    @Test
    public void available_atStartOfStream() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello World"), 5);

        int available = fixedLengthInputStream.available();

        assertEquals(5, available);
    }

    @Test
    public void available_afterPartialRead() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello World"), 5);
        //noinspection ResultOfMethodCallIgnored
        fixedLengthInputStream.read();

        int available = fixedLengthInputStream.available();

        assertEquals(4, available);
    }

    @Test
    public void available_afterPartialReadArray() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello World"), 5);
        consumeBytes(fixedLengthInputStream, 2);

        int available = fixedLengthInputStream.available();

        assertEquals(3, available);
    }

    @Test
    public void available_afterStreamHasBeenExhausted() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello World"), 5);
        exhaustStream(fixedLengthInputStream);

        int available = fixedLengthInputStream.available();

        assertEquals(0, available);
    }

    @Test
    public void available_afterSkip() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello World"), 5);
        guaranteedSkip(fixedLengthInputStream, 2);

        int available = fixedLengthInputStream.available();

        assertEquals(3, available);
    }

    @Test
    public void available_afterSkipRemaining() throws Exception {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello World"), 5);
        fixedLengthInputStream.skipRemaining();

        int available = fixedLengthInputStream.available();

        assertEquals(0, available);
    }

    @Test
    public void skip_shouldConsumeBytes() throws IOException {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello World"), 5);

        guaranteedSkip(fixedLengthInputStream, 2);

        assertRemainingInputStreamEquals("llo", fixedLengthInputStream);
    }

    @Test
    public void skipRemaining_shouldExhaustStream() throws IOException {
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream("Hello World"), 5);

        fixedLengthInputStream.skipRemaining();

        assertInputStreamExhausted(fixedLengthInputStream);
    }

    @Test
    public void skipRemaining_shouldNotConsumeMoreThanLimitFromUnderlyingInputStream() throws IOException {
        InputStream inputStream = inputStream("Hello World");
        FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(inputStream, 6);

        fixedLengthInputStream.skipRemaining();

        assertRemainingInputStreamEquals("World", inputStream);
    }


    private String readStreamAsUtf8String(InputStream inputStream) throws IOException {
        return Okio.buffer(Okio.source(inputStream)).readUtf8();
    }

    private void exhaustStream(InputStream inputStream) throws IOException {
        Okio.buffer(Okio.source(inputStream)).readAll(Okio.blackhole());
    }

    private void consumeBytes(InputStream inputStream, int numberOfBytes) throws IOException {
        int read = inputStream.read(new byte[numberOfBytes]);
        assertEquals(numberOfBytes, read);
    }

    private void guaranteedSkip(InputStream inputStream, int numberOfBytesToSkip) throws IOException {
        int remaining = numberOfBytesToSkip;
        while (remaining > 0) {
            remaining -= inputStream.skip(remaining);
        }
        assertEquals(0, remaining);
    }

    private void assertRemainingInputStreamEquals(String expected, InputStream inputStream) throws IOException {
        assertEquals(expected, readStreamAsUtf8String(inputStream));
    }

    private void assertInputStreamExhausted(InputStream inputStream) throws IOException {
        assertEquals(-1, inputStream.read());
    }

    private InputStream inputStream(String data) {
        return new Buffer().writeUtf8(data).inputStream();
    }
}
