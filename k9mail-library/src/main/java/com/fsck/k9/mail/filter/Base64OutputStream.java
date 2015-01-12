/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fsck.k9.mail.filter;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Provides Base64 encoding and decoding in a streaming fashion (unlimited size).
 * When encoding the default lineLength is 76 characters and the default
 * lineEnding is CRLF, but these can be overridden by using the appropriate
 * constructor.
 * <p>
 * The default behaviour of the Base64OutputStream is to ENCODE, whereas the
 * default behaviour of the Base64InputStream is to DECODE.  But this behaviour
 * can be overridden by using a different constructor.
 * </p><p>
 * This class implements section <cite>6.8. Base64 Content-Transfer-Encoding</cite> from RFC 2045 <cite>Multipurpose
 * Internet Mail Extensions (MIME) Part One: Format of Internet Message Bodies</cite> by Freed and Borenstein.
 * </p>
 *
 * @author Apache Software Foundation
 * @version $Id $
 * @see <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045</a>
 * @since 1.0-dev
 */
public class Base64OutputStream extends FilterOutputStream {
    private final boolean doEncode;
    private final Base64 base64;
    private final byte[] singleByte = new byte[1];

    /**
     * Creates a Base64OutputStream such that all data written is Base64-encoded
     * to the original provided OutputStream.
     *
     * @param out OutputStream to wrap.
     */
    public Base64OutputStream(OutputStream out) {
        this(out, true);
    }

    /**
     * Creates a Base64OutputStream such that all data written is either
     * Base64-encoded or Base64-decoded to the original provided OutputStream.
     *
     * @param out      OutputStream to wrap.
     * @param doEncode true if we should encode all data written to us,
     *                 false if we should decode.
     */
    public Base64OutputStream(OutputStream out, boolean doEncode) {
        super(out);
        this.doEncode = doEncode;
        this.base64 = new Base64();
    }

    /**
     * Creates a Base64OutputStream such that all data written is either
     * Base64-encoded or Base64-decoded to the original provided OutputStream.
     *
     * @param out           OutputStream to wrap.
     * @param doEncode      true if we should encode all data written to us,
     *                      false if we should decode.
     * @param lineLength    If doEncode is true, each line of encoded
     *                      data will contain lineLength characters.
     *                      If lineLength <=0, the encoded data is not divided into lines.
     *                      If doEncode is false, lineLength is ignored.
     * @param lineSeparator If doEncode is true, each line of encoded
     *                      data will be terminated with this byte sequence (e.g. \r\n).
     *                      If lineLength <= 0, the lineSeparator is not used.
     *                      If doEncode is false lineSeparator is ignored.
     */
    public Base64OutputStream(OutputStream out, boolean doEncode, int lineLength, byte[] lineSeparator) {
        super(out);
        this.doEncode = doEncode;
        this.base64 = new Base64(lineLength, lineSeparator);
    }

    /**
     * Writes the specified <code>byte</code> to this output stream.
     */
    @Override
    public void write(int i) throws IOException {
        singleByte[0] = (byte) i;
        write(singleByte, 0, 1);
    }

    /**
     * Writes <code>len</code> bytes from the specified
     * <code>b</code> array starting at <code>offset</code> to
     * this output stream.
     *
     * @param b source byte array
     * @param offset where to start reading the bytes
     * @param len maximum number of bytes to write
     *
     * @throws IOException if an I/O error occurs.
     * @throws NullPointerException if the byte array parameter is null
     * @throws IndexOutOfBoundsException if offset, len or buffer size are invalid
     */
    @Override
    public void write(byte b[], int offset, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (offset < 0 || len < 0 || offset + len < 0) {
            throw new IndexOutOfBoundsException();
        } else if (offset > b.length || offset + len > b.length) {
            throw new IndexOutOfBoundsException();
        } else if (len > 0) {
            if (doEncode) {
                base64.encode(b, offset, len);
            } else {
                base64.decode(b, offset, len);
            }
            flush(false);
        }
    }

    /**
     * Flushes this output stream and forces any buffered output bytes
     * to be written out to the stream.  If propogate is true, the wrapped
     * stream will also be flushed.
     *
     * @param propogate boolean flag to indicate whether the wrapped
     *                  OutputStream should also be flushed.
     * @throws IOException if an I/O error occurs.
     */
    private void flush(boolean propogate) throws IOException {
        int avail = base64.avail();
        if (avail > 0) {
            byte[] buf = new byte[avail];
            int c = base64.readResults(buf, 0, avail);
            if (c > 0) {
                out.write(buf, 0, c);
            }
        }
        if (propogate) {
            out.flush();
        }
    }

    /**
     * Flushes this output stream and forces any buffered output bytes
     * to be written out to the stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void flush() throws IOException {
        flush(true);
    }

    /**
     * Closes this output stream, flushing any remaining bytes that must be encoded. The
     * underlying stream is flushed but not closed.
     */
    @Override
    public void close() throws IOException {
        // Notify encoder of EOF (-1).
        if (doEncode) {
            base64.encode(singleByte, 0, -1);
        } else {
            base64.decode(singleByte, 0, -1);
        }
        flush();
    }

}
