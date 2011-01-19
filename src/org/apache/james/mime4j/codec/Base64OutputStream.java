/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mime4j.codec;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * This class implements section <cite>6.8. Base64 Content-Transfer-Encoding</cite>
 * from RFC 2045 <cite>Multipurpose Internet Mail Extensions (MIME) Part One:
 * Format of Internet Message Bodies</cite> by Freed and Borenstein.
 * <p>
 * Code is based on Base64 and Base64OutputStream code from Commons-Codec 1.4.
 *
 * @see <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045</a>
 */
public class Base64OutputStream extends FilterOutputStream {

    // Default line length per RFC 2045 section 6.8.
    private static final int DEFAULT_LINE_LENGTH = 76;

    // CRLF line separator per RFC 2045 section 2.1.
    private static final byte[] CRLF_SEPARATOR = { '\r', '\n' };

    // This array is a lookup table that translates 6-bit positive integer index
    // values into their "Base64 Alphabet" equivalents as specified in Table 1
    // of RFC 2045.
    static final byte[] BASE64_TABLE = { 'A', 'B', 'C', 'D', 'E', 'F',
            'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
            'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
            't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', '+', '/' };

    // Byte used to pad output.
    private static final byte BASE64_PAD = '=';

    // This set contains all base64 characters including the pad character. Used
    // solely to check if a line separator contains any of these characters.
    private static final Set<Byte> BASE64_CHARS = new HashSet<Byte>();

    static {
        for (byte b : BASE64_TABLE) {
            BASE64_CHARS.add(b);
        }
        BASE64_CHARS.add(BASE64_PAD);
    }

    // Mask used to extract 6 bits
    private static final int MASK_6BITS = 0x3f;

    private static final int ENCODED_BUFFER_SIZE = 2048;

    private final byte[] singleByte = new byte[1];

    private final int lineLength;
    private final byte[] lineSeparator;

    private boolean closed = false;

    private final byte[] encoded;
    private int position = 0;

    private int data = 0;
    private int modulus = 0;

    private int linePosition = 0;

    /**
     * Creates a <code>Base64OutputStream</code> that writes the encoded data
     * to the given output stream using the default line length (76) and line
     * separator (CRLF).
     *
     * @param out
     *            underlying output stream.
     */
    public Base64OutputStream(OutputStream out) {
        this(out, DEFAULT_LINE_LENGTH, CRLF_SEPARATOR);
    }

    /**
     * Creates a <code>Base64OutputStream</code> that writes the encoded data
     * to the given output stream using the given line length and the default
     * line separator (CRLF).
     * <p>
     * The given line length will be rounded up to the nearest multiple of 4. If
     * the line length is zero then the output will not be split into lines.
     *
     * @param out
     *            underlying output stream.
     * @param lineLength
     *            desired line length.
     */
    public Base64OutputStream(OutputStream out, int lineLength) {
        this(out, lineLength, CRLF_SEPARATOR);
    }

    /**
     * Creates a <code>Base64OutputStream</code> that writes the encoded data
     * to the given output stream using the given line length and line
     * separator.
     * <p>
     * The given line length will be rounded up to the nearest multiple of 4. If
     * the line length is zero then the output will not be split into lines and
     * the line separator is ignored.
     * <p>
     * The line separator must not include characters from the BASE64 alphabet
     * (including the padding character <code>=</code>).
     *
     * @param out
     *            underlying output stream.
     * @param lineLength
     *            desired line length.
     * @param lineSeparator
     *            line separator to use.
     */
    public Base64OutputStream(OutputStream out, int lineLength,
            byte[] lineSeparator) {
        super(out);

        if (out == null)
            throw new IllegalArgumentException();
        if (lineLength < 0)
            throw new IllegalArgumentException();
        checkLineSeparator(lineSeparator);

        this.lineLength = lineLength;
        this.lineSeparator = new byte[lineSeparator.length];
        System.arraycopy(lineSeparator, 0, this.lineSeparator, 0,
                lineSeparator.length);

        this.encoded = new byte[ENCODED_BUFFER_SIZE];
    }

    @Override
    public final void write(final int b) throws IOException {
        if (closed)
            throw new IOException("Base64OutputStream has been closed");

        singleByte[0] = (byte) b;
        write0(singleByte, 0, 1);
    }

    @Override
    public final void write(final byte[] buffer) throws IOException {
        if (closed)
            throw new IOException("Base64OutputStream has been closed");

        if (buffer == null)
            throw new NullPointerException();

        if (buffer.length == 0)
            return;

        write0(buffer, 0, buffer.length);
    }

    @Override
    public final void write(final byte[] buffer, final int offset,
            final int length) throws IOException {
        if (closed)
            throw new IOException("Base64OutputStream has been closed");

        if (buffer == null)
            throw new NullPointerException();

        if (offset < 0 || length < 0 || offset + length > buffer.length)
            throw new IndexOutOfBoundsException();

        if (length == 0)
            return;

        write0(buffer, offset, offset + length);
    }

    @Override
    public void flush() throws IOException {
        if (closed)
            throw new IOException("Base64OutputStream has been closed");

        flush0();
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;

        closed = true;
        close0();
    }

    private void write0(final byte[] buffer, final int from, final int to)
            throws IOException {
        for (int i = from; i < to; i++) {
            data = (data << 8) | (buffer[i] & 0xff);

            if (++modulus == 3) {
                modulus = 0;

                // write line separator if necessary

                if (lineLength > 0 && linePosition >= lineLength) {
                    // writeLineSeparator() inlined for performance reasons

                    linePosition = 0;

                    if (encoded.length - position < lineSeparator.length)
                        flush0();

                    for (byte ls : lineSeparator)
                        encoded[position++] = ls;
                }

                // encode data into 4 bytes

                if (encoded.length - position < 4)
                    flush0();

                encoded[position++] = BASE64_TABLE[(data >> 18) & MASK_6BITS];
                encoded[position++] = BASE64_TABLE[(data >> 12) & MASK_6BITS];
                encoded[position++] = BASE64_TABLE[(data >> 6) & MASK_6BITS];
                encoded[position++] = BASE64_TABLE[data & MASK_6BITS];

                linePosition += 4;
            }
        }
    }

    private void flush0() throws IOException {
        if (position > 0) {
            out.write(encoded, 0, position);
            position = 0;
        }
    }

    private void close0() throws IOException {
        if (modulus != 0)
            writePad();

        // write line separator at the end of the encoded data

        if (lineLength > 0 && linePosition > 0) {
            writeLineSeparator();
        }

        flush0();
    }

    private void writePad() throws IOException {
        // write line separator if necessary

        if (lineLength > 0 && linePosition >= lineLength) {
            writeLineSeparator();
        }

        // encode data into 4 bytes

        if (encoded.length - position < 4)
            flush0();

        if (modulus == 1) {
            encoded[position++] = BASE64_TABLE[(data >> 2) & MASK_6BITS];
            encoded[position++] = BASE64_TABLE[(data << 4) & MASK_6BITS];
            encoded[position++] = BASE64_PAD;
            encoded[position++] = BASE64_PAD;
        } else {
            assert modulus == 2;
            encoded[position++] = BASE64_TABLE[(data >> 10) & MASK_6BITS];
            encoded[position++] = BASE64_TABLE[(data >> 4) & MASK_6BITS];
            encoded[position++] = BASE64_TABLE[(data << 2) & MASK_6BITS];
            encoded[position++] = BASE64_PAD;
        }

        linePosition += 4;
    }

    private void writeLineSeparator() throws IOException {
        linePosition = 0;

        if (encoded.length - position < lineSeparator.length)
            flush0();

        for (byte ls : lineSeparator)
            encoded[position++] = ls;
    }

    private void checkLineSeparator(byte[] lineSeparator) {
        if (lineSeparator.length > ENCODED_BUFFER_SIZE)
            throw new IllegalArgumentException("line separator length exceeds "
                    + ENCODED_BUFFER_SIZE);

        for (byte b : lineSeparator) {
            if (BASE64_CHARS.contains(b)) {
                throw new IllegalArgumentException(
                        "line separator must not contain base64 character '"
                                + (char) (b & 0xff) + "'");
            }
        }
    }
}
