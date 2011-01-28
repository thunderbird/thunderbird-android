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

import java.io.IOException;
import java.io.InputStream;

import org.apache.james.mime4j.util.ByteArrayBuffer;

/**
 * Performs Base-64 decoding on an underlying stream.
 */
public class Base64InputStream extends InputStream {
    private static final int ENCODED_BUFFER_SIZE = 1536;

    private static final int[] BASE64_DECODE = new int[256];

    static {
        for (int i = 0; i < 256; i++)
            BASE64_DECODE[i] = -1;
        for (int i = 0; i < Base64OutputStream.BASE64_TABLE.length; i++)
            BASE64_DECODE[Base64OutputStream.BASE64_TABLE[i] & 0xff] = i;
    }

    private static final byte BASE64_PAD = '=';

    private static final int EOF = -1;

    private final byte[] singleByte = new byte[1];

    private final InputStream in;
    private final byte[] encoded;
    private final ByteArrayBuffer decodedBuf;

    private int position = 0; // current index into encoded buffer
    private int size = 0; // current size of encoded buffer

    private boolean closed = false;
    private boolean eof; // end of file or pad character reached

    private final DecodeMonitor monitor;

    public Base64InputStream(InputStream in, DecodeMonitor monitor) {
        this(ENCODED_BUFFER_SIZE, in, monitor);
    }

    protected Base64InputStream(int bufsize, InputStream in, DecodeMonitor monitor) {
        if (in == null)
            throw new IllegalArgumentException();
        this.encoded = new byte[bufsize];
        this.decodedBuf = new ByteArrayBuffer(512);
        this.in = in;
        this.monitor = monitor;
    }

    public Base64InputStream(InputStream in) {
        this(in, false);
    }

    public Base64InputStream(InputStream in, boolean strict) {
        this(ENCODED_BUFFER_SIZE, in, strict ? DecodeMonitor.STRICT : DecodeMonitor.SILENT);
    }

    @Override
    public int read() throws IOException {
        if (closed)
            throw new IOException("Stream has been closed");

        while (true) {
            int bytes = read0(singleByte, 0, 1);
            if (bytes == EOF)
                return EOF;

            if (bytes == 1)
                return singleByte[0] & 0xff;
        }
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        if (closed)
            throw new IOException("Stream has been closed");

        if (buffer == null)
            throw new NullPointerException();

        if (buffer.length == 0)
            return 0;

        return read0(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (closed)
            throw new IOException("Stream has been closed");

        if (buffer == null)
            throw new NullPointerException();

        if (offset < 0 || length < 0 || offset + length > buffer.length)
            throw new IndexOutOfBoundsException();

        if (length == 0)
            return 0;

        return read0(buffer, offset, length);
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;

        closed = true;
    }

    private int read0(final byte[] buffer, final int off, final int len) throws IOException {
        int from = off;
        int to = off + len;
        int index = off;

        // check if a previous invocation left decoded content
        if (decodedBuf.length() > 0) {
            int chunk = Math.min(decodedBuf.length(), len);
            System.arraycopy(decodedBuf.buffer(), 0, buffer, index, chunk);
            decodedBuf.remove(0, chunk);
            index += chunk;
        }

        // eof or pad reached?

        if (eof)
            return index == from ? EOF : index - from;

        // decode into given buffer

        int data = 0; // holds decoded data; up to four sextets
        int sextets = 0; // number of sextets

        while (index < to) {
            // make sure buffer not empty

            while (position == size) {
                int n = in.read(encoded, 0, encoded.length);
                if (n == EOF) {
                    eof = true;

                    if (sextets != 0) {
                        // error in encoded data
                        handleUnexpectedEof(sextets);
                    }

                    return index == from ? EOF : index - from;
                } else if (n > 0) {
                    position = 0;
                    size = n;
                } else {
                    assert n == 0;
                }
            }

            // decode buffer

            while (position < size && index < to) {
                int value = encoded[position++] & 0xff;

                if (value == BASE64_PAD) {
                    index = decodePad(data, sextets, buffer, index, to);
                    return index - from;
                }

                int decoded = BASE64_DECODE[value];
                if (decoded < 0) { // -1: not a base64 char
                    if (value != 0x0D && value != 0x0A && value != 0x20) {
                        if (monitor.warn("Unexpected base64 byte: "+(byte) value, "ignoring."))
                            throw new IOException("Unexpected base64 byte");
                    }
                    continue;
                }

                data = (data << 6) | decoded;
                sextets++;

                if (sextets == 4) {
                    sextets = 0;

                    byte b1 = (byte) (data >>> 16);
                    byte b2 = (byte) (data >>> 8);
                    byte b3 = (byte) data;

                    if (index < to - 2) {
                        buffer[index++] = b1;
                        buffer[index++] = b2;
                        buffer[index++] = b3;
                    } else {
                        if (index < to - 1) {
                            buffer[index++] = b1;
                            buffer[index++] = b2;
                            decodedBuf.append(b3);
                        } else if (index < to) {
                            buffer[index++] = b1;
                            decodedBuf.append(b2);
                            decodedBuf.append(b3);
                        } else {
                            decodedBuf.append(b1);
                            decodedBuf.append(b2);
                            decodedBuf.append(b3);
                        }

                        assert index == to;
                        return to - from;
                    }
                }
            }
        }

        assert sextets == 0;
        assert index == to;
        return to - from;
    }

    private int decodePad(int data, int sextets, final byte[] buffer,
            int index, final int end) throws IOException {
        eof = true;

        if (sextets == 2) {
            // one byte encoded as "XY=="

            byte b = (byte) (data >>> 4);
            if (index < end) {
                buffer[index++] = b;
            } else {
                decodedBuf.append(b);
            }
        } else if (sextets == 3) {
            // two bytes encoded as "XYZ="

            byte b1 = (byte) (data >>> 10);
            byte b2 = (byte) ((data >>> 2) & 0xFF);

            if (index < end - 1) {
                buffer[index++] = b1;
                buffer[index++] = b2;
            } else if (index < end) {
                buffer[index++] = b1;
                decodedBuf.append(b2);
            } else {
                decodedBuf.append(b1);
                decodedBuf.append(b2);
            }
        } else {
            // error in encoded data
            handleUnexpecedPad(sextets);
        }

        return index;
    }

    private void handleUnexpectedEof(int sextets) throws IOException {
        if (monitor.warn("Unexpected end of BASE64 stream", "dropping " + sextets + " sextet(s)"))
            throw new IOException("Unexpected end of BASE64 stream");
    }

    private void handleUnexpecedPad(int sextets) throws IOException {
        if (monitor.warn("Unexpected padding character", "dropping " + sextets + " sextet(s)"))
            throw new IOException("Unexpected padding character");
    }
}
