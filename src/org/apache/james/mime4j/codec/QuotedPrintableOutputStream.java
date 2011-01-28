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

/**
 * Performs Quoted-Printable encoding on an underlying stream.
 */
public class QuotedPrintableOutputStream extends FilterOutputStream {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 3;

    private static final byte TB = 0x09;
    private static final byte SP = 0x20;
    private static final byte EQ = 0x3D;
    private static final byte CR = 0x0D;
    private static final byte LF = 0x0A;
    private static final byte QUOTED_PRINTABLE_LAST_PLAIN = 0x7E;
    private static final int QUOTED_PRINTABLE_MAX_LINE_LENGTH = 76;
    private static final int QUOTED_PRINTABLE_OCTETS_PER_ESCAPE = 3;
    private static final byte[] HEX_DIGITS = {
        '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    private final byte[] outBuffer;
    private final boolean binary;

    private boolean pendingSpace;
    private boolean pendingTab;
    private boolean pendingCR;
    private int nextSoftBreak;
    private int outputIndex;

    private boolean closed = false;

    private byte[] singleByte = new byte[1];

    public QuotedPrintableOutputStream(int bufsize, OutputStream out, boolean binary) {
        super(out);
        this.outBuffer = new byte[bufsize];
        this.binary = binary;
        this.pendingSpace = false;
        this.pendingTab = false;
        this.pendingCR = false;
        this.outputIndex = 0;
        this.nextSoftBreak = QUOTED_PRINTABLE_MAX_LINE_LENGTH + 1;
    }

    public QuotedPrintableOutputStream(OutputStream out, boolean binary) {
        this(DEFAULT_BUFFER_SIZE, out, binary);
    }

    private void encodeChunk(byte[] buffer, int off, int len) throws IOException {
        for (int inputIndex = off; inputIndex < len + off; inputIndex++) {
            encode(buffer[inputIndex]);
        }
    }

    private void completeEncoding() throws IOException {
        writePending();
        flushOutput();
    }

    private void writePending() throws IOException {
        if (pendingSpace) {
            plain(SP);
        } else if (pendingTab) {
            plain(TB);
        } else if (pendingCR) {
            plain(CR);
        }
        clearPending();
    }

    private void clearPending() throws IOException {
        pendingSpace  = false;
        pendingTab = false;
        pendingCR = false;
    }

    private void encode(byte next) throws IOException {
        if (next == LF) {
            if (binary) {
                writePending();
                escape(next);
            } else {
                if (pendingCR) {
                    // Expect either space or tab pending
                    // but not both
                    if (pendingSpace) {
                        escape(SP);
                    } else if (pendingTab) {
                        escape(TB);
                    }
                    lineBreak();
                    clearPending();
                } else {
                    writePending();
                    plain(next);
                }
            }
        } else if (next == CR) {
            if (binary)  {
                escape(next);
            } else {
                pendingCR = true;
            }
        } else {
            writePending();
            if (next == SP) {
                if (binary)  {
                    escape(next);
                } else {
                    pendingSpace = true;
                }
            } else if (next == TB) {
                if (binary)  {
                    escape(next);
                } else {
                    pendingTab = true;
                }
            } else if (next < SP) {
                escape(next);
            } else if (next > QUOTED_PRINTABLE_LAST_PLAIN) {
                escape(next);
            } else if (next == EQ) {
                escape(next);
            } else {
                plain(next);
            }
        }
    }

    private void plain(byte next) throws IOException {
        if (--nextSoftBreak <= 1) {
            softBreak();
        }
        write(next);
    }

    private void escape(byte next) throws IOException {
        if (--nextSoftBreak <= QUOTED_PRINTABLE_OCTETS_PER_ESCAPE) {
            softBreak();
        }

        int nextUnsigned = next & 0xff;

        write(EQ);
        --nextSoftBreak;
        write(HEX_DIGITS[nextUnsigned >> 4]);
        --nextSoftBreak;
        write(HEX_DIGITS[nextUnsigned % 0x10]);
    }

    private void write(byte next) throws IOException {
        outBuffer[outputIndex++] = next;
        if (outputIndex >= outBuffer.length) {
            flushOutput();
        }
    }

    private void softBreak() throws IOException {
        write(EQ);
        lineBreak();
    }

    private void lineBreak() throws IOException {
        write(CR);
        write(LF);
        nextSoftBreak = QUOTED_PRINTABLE_MAX_LINE_LENGTH;
    }

    void flushOutput() throws IOException {
        if (outputIndex < outBuffer.length) {
            out.write(outBuffer, 0, outputIndex);
        } else {
            out.write(outBuffer);
        }
        outputIndex = 0;
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;

        try {
            completeEncoding();
            // do not close the wrapped stream
        } finally {
            closed = true;
        }
    }

    @Override
    public void flush() throws IOException {
        flushOutput();
    }

    @Override
    public void write(int b) throws IOException {
        singleByte[0] = (byte) b;
        this.write(singleByte, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Stream has been closed");
        }
        encodeChunk(b, off, len);
    }

}