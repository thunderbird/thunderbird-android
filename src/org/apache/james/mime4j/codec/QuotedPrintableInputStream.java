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
 * Performs Quoted-Printable decoding on an underlying stream.
 */
public class QuotedPrintableInputStream extends InputStream {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 2;

    private static final byte EQ = 0x3D;
    private static final byte CR = 0x0D;
    private static final byte LF = 0x0A;

    private final byte[] singleByte = new byte[1];

    private final InputStream in;
    private final ByteArrayBuffer decodedBuf;
    private final ByteArrayBuffer blanks;

    private final byte[] encoded;
    private int pos = 0; // current index into encoded buffer
    private int limit = 0; // current size of encoded buffer

    private boolean closed;

    private final DecodeMonitor monitor;

    public QuotedPrintableInputStream(final InputStream in, DecodeMonitor monitor) {
        this(DEFAULT_BUFFER_SIZE, in, monitor);
    }

    protected QuotedPrintableInputStream(final int bufsize, final InputStream in, DecodeMonitor monitor) {
        super();
        this.in = in;
        this.encoded = new byte[bufsize];
        this.decodedBuf = new ByteArrayBuffer(512);
        this.blanks = new ByteArrayBuffer(512);
        this.closed = false;
        this.monitor = monitor;
    }

    protected QuotedPrintableInputStream(final int bufsize, final InputStream in, boolean strict) {
        this(bufsize, in, strict ? DecodeMonitor.STRICT : DecodeMonitor.SILENT);
    }

    public QuotedPrintableInputStream(final InputStream in, boolean strict) {
        this(DEFAULT_BUFFER_SIZE, in, strict);
    }

    public QuotedPrintableInputStream(final InputStream in) {
        this(in, false);
    }

    /**
     * Terminates Quoted-Printable coded content. This method does NOT close
     * the underlying input stream.
     *
     * @throws IOException on I/O errors.
     */
    @Override
    public void close() throws IOException {
        closed = true;
    }

    private int fillBuffer() throws IOException {
        // Compact buffer if needed
        if (pos < limit) {
            System.arraycopy(encoded, pos, encoded, 0, limit - pos);
            limit -= pos;
            pos = 0;
        } else {
            limit = 0;
            pos = 0;
        }

        int capacity = encoded.length - limit;
        if (capacity > 0) {
            int bytesRead = in.read(encoded, limit, capacity);
            if (bytesRead > 0) {
                limit += bytesRead;
            }
            return bytesRead;
        } else {
            return 0;
        }
    }

    private int getnext() {
        if (pos < limit) {
            byte b =  encoded[pos];
            pos++;
            return b & 0xFF;
        } else {
            return -1;
        }
    }

    private int peek(int i) {
        if (pos + i < limit) {
            return encoded[pos + i] & 0xFF;
        } else {
            return -1;
        }
    }

    private int transfer(
            final int b, final byte[] buffer, final int from, final int to, boolean keepblanks) throws IOException {
        int index = from;
        if (keepblanks && blanks.length() > 0) {
            int chunk = Math.min(blanks.length(), to - index);
            System.arraycopy(blanks.buffer(), 0, buffer, index, chunk);
            index += chunk;
            int remaining = blanks.length() - chunk;
            if (remaining > 0) {
                decodedBuf.append(blanks.buffer(), chunk, remaining);
            }
            blanks.clear();
        } else if (blanks.length() > 0 && !keepblanks) {
            StringBuilder sb = new StringBuilder(blanks.length() * 3);
            for (int i = 0; i < blanks.length(); i++) sb.append(" "+blanks.byteAt(i));
            if (monitor.warn("ignored blanks", sb.toString()))
                throw new IOException("ignored blanks");
        }
        if (b != -1) {
            if (index < to) {
                buffer[index++] = (byte) b;
            } else {
                decodedBuf.append(b);
            }
        }
        return index;
    }

    private int read0(final byte[] buffer, final int off, final int len) throws IOException {
        boolean eof = false;
        int from = off;
        int to = off + len;
        int index = off;

        // check if a previous invocation left decoded content
        if (decodedBuf.length() > 0) {
            int chunk = Math.min(decodedBuf.length(), to - index);
            System.arraycopy(decodedBuf.buffer(), 0, buffer, index, chunk);
            decodedBuf.remove(0, chunk);
            index += chunk;
        }

        while (index < to) {

            if (limit - pos < 3) {
                int bytesRead = fillBuffer();
                eof = bytesRead == -1;
            }

            // end of stream?
            if (limit - pos == 0 && eof) {
                return index == from ? -1 : index - from;
            }

            boolean lastWasCR = false;
            while (pos < limit && index < to) {
                int b = encoded[pos++] & 0xFF;

                if (lastWasCR && b != LF) {
                    if (monitor.warn("Found CR without LF", "Leaving it as is"))
                        throw new IOException("Found CR without LF");
                    index = transfer(CR, buffer, index, to, false);
                } else if (!lastWasCR && b == LF) {
                    if (monitor.warn("Found LF without CR", "Translating to CRLF"))
                        throw new IOException("Found LF without CR");
                }

                if (b == CR) {
                    lastWasCR = true;
                    continue;
                } else {
                    lastWasCR = false;
                }

                if (b == LF) {
                    // at end of line
                    if (blanks.length() == 0) {
                        index = transfer(CR, buffer, index, to, false);
                        index = transfer(LF, buffer, index, to, false);
                    } else {
                        if (blanks.byteAt(0) != EQ) {
                            // hard line break
                            index = transfer(CR, buffer, index, to, false);
                            index = transfer(LF, buffer, index, to, false);
                        }
                    }
                    blanks.clear();
                } else if (b == EQ) {
                    if (limit - pos < 2 && !eof) {
                        // not enough buffered data
                        pos--;
                        break;
                    }

                    // found special char '='
                    int b2 = getnext();
                    if (b2 == EQ) {
                        index = transfer(b2, buffer, index, to, true);
                        // deal with '==\r\n' brokenness
                        int bb1 = peek(0);
                        int bb2 = peek(1);
                        if (bb1 == LF || (bb1 == CR && bb2 == LF)) {
                            monitor.warn("Unexpected ==EOL encountered", "== 0x"+bb1+" 0x"+bb2);
                            blanks.append(b2);
                        } else {
                            monitor.warn("Unexpected == encountered", "==");
                        }
                    } else if (Character.isWhitespace((char) b2)) {
                        // soft line break
                        index = transfer(-1, buffer, index, to, true);
                        if (b2 != LF) {
                            blanks.append(b);
                            blanks.append(b2);
                        }
                    } else {
                        int b3 = getnext();
                        int upper = convert(b2);
                        int lower = convert(b3);
                        if (upper < 0 || lower < 0) {
                            monitor.warn("Malformed encoded value encountered", "leaving "+((char) EQ)+((char) b2)+((char) b3)+" as is");
                            // TODO see MIME4J-160
                            index = transfer(EQ, buffer, index, to, true);
                            index = transfer(b2, buffer, index, to, false);
                            index = transfer(b3, buffer, index, to, false);
                        } else {
                            index = transfer((upper << 4) | lower, buffer, index, to, true);
                        }
                    }
                } else if (Character.isWhitespace(b)) {
                    blanks.append(b);
                } else {
                    index = transfer((int) b & 0xFF, buffer, index, to, true);
                }
            }
        }
        return to - from;
    }

    /**
     * Converts '0' => 0, 'A' => 10, etc.
     * @param c ASCII character value.
     * @return Numeric value of hexadecimal character.
     */
    private int convert(int c) {
        if (c >= '0' && c <= '9') {
            return (c - '0');
        } else if (c >= 'A' && c <= 'F') {
            return (0xA + (c - 'A'));
        } else if (c >= 'a' && c <= 'f') {
            return (0xA + (c - 'a'));
        } else {
            return -1;
        }
    }

    @Override
    public int read() throws IOException {
        if (closed) {
            throw new IOException("Stream has been closed");
        }
        for (;;) {
            int bytes = read(singleByte, 0, 1);
            if (bytes == -1) {
                return -1;
            }
            if (bytes == 1) {
                return singleByte[0] & 0xff;
            }
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Stream has been closed");
        }
        return read0(b, off, len);
    }

}
