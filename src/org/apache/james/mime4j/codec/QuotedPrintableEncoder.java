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
import java.io.OutputStream;

// Taken from Apache Mime4j 0.6

final class QuotedPrintableEncoder
{
    private static final byte TAB = 0x09;
    private static final byte SPACE = 0x20;
    private static final byte EQUALS = 0x3D;
    private static final byte CR = 0x0D;
    private static final byte LF = 0x0A;
    private static final byte QUOTED_PRINTABLE_LAST_PLAIN = 0x7E;
    private static final int QUOTED_PRINTABLE_MAX_LINE_LENGTH = 76;
    private static final int QUOTED_PRINTABLE_OCTETS_PER_ESCAPE = 3;
    private static final byte[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private final byte[] inBuffer;
    private final byte[] outBuffer;
    private final boolean binary;

    private boolean pendingSpace;
    private boolean pendingTab;
    private boolean pendingCR;
    private int nextSoftBreak;
    private int outputIndex;
    private OutputStream out;

    public QuotedPrintableEncoder(int bufferSize, boolean binary)
    {
        inBuffer = new byte[bufferSize];
        outBuffer = new byte[3 * bufferSize];
        outputIndex = 0;
        nextSoftBreak = QUOTED_PRINTABLE_MAX_LINE_LENGTH + 1;
        out = null;
        this.binary = binary;
        pendingSpace = false;
        pendingTab = false;
        pendingCR = false;
    }

    void initEncoding(final OutputStream out)
    {
        this.out = out;
        pendingSpace = false;
        pendingTab = false;
        pendingCR = false;
        nextSoftBreak = QUOTED_PRINTABLE_MAX_LINE_LENGTH + 1;
    }

    void encodeChunk(byte[] buffer, int off, int len) throws IOException
    {
        for (int inputIndex = off; inputIndex < len + off; inputIndex++)
        {
            encode(buffer[inputIndex]);
        }
    }

    void completeEncoding() throws IOException
    {
        writePending();
        flushOutput();
    }

    public void encode(final InputStream in, final OutputStream out)
            throws IOException
    {
        initEncoding(out);
        int inputLength;
        while ((inputLength = in.read(inBuffer)) > -1)
        {
            encodeChunk(inBuffer, 0, inputLength);
        }
        completeEncoding();
    }

    private void writePending() throws IOException
    {
        if (pendingSpace)
        {
            plain(SPACE);
        }
        else if (pendingTab)
        {
            plain(TAB);
        }
        else if (pendingCR)
        {
            plain(CR);
        }
        clearPending();
    }

    private void clearPending() throws IOException
    {
        pendingSpace = false;
        pendingTab = false;
        pendingCR = false;
    }

    private void encode(byte next) throws IOException
    {
        if (next == LF)
        {
            if (binary)
            {
                writePending();
                escape(next);
            }
            else
            {
                if (pendingCR)
                {
                    // Expect either space or tab pending
                    // but not both
                    if (pendingSpace)
                    {
                        escape(SPACE);
                    }
                    else if (pendingTab)
                    {
                        escape(TAB);
                    }
                    lineBreak();
                    clearPending();
                }
                else
                {
                    writePending();
                    plain(next);
                }
            }
        }
        else if (next == CR)
        {
            if (binary)
            {
                escape(next);
            }
            else
            {
                pendingCR = true;
            }
        }
        else
        {
            writePending();
            if (next == SPACE)
            {
                if (binary)
                {
                    escape(next);
                }
                else
                {
                    pendingSpace = true;
                }
            }
            else if (next == TAB)
            {
                if (binary)
                {
                    escape(next);
                }
                else
                {
                    pendingTab = true;
                }
            }
            else if (next < SPACE)
            {
                escape(next);
            }
            else if (next > QUOTED_PRINTABLE_LAST_PLAIN)
            {
                escape(next);
            }
            else if (next == EQUALS)
            {
                escape(next);
            }
            else
            {
                plain(next);
            }
        }
    }

    private void plain(byte next) throws IOException
    {
        if (--nextSoftBreak <= 1)
        {
            softBreak();
        }
        write(next);
    }

    private void escape(byte next) throws IOException
    {
        if (--nextSoftBreak <= QUOTED_PRINTABLE_OCTETS_PER_ESCAPE)
        {
            softBreak();
        }

        int nextUnsigned = next & 0xff;

        write(EQUALS);
        --nextSoftBreak;
        write(HEX_DIGITS[nextUnsigned >> 4]);
        --nextSoftBreak;
        write(HEX_DIGITS[nextUnsigned % 0x10]);
    }

    private void write(byte next) throws IOException
    {
        outBuffer[outputIndex++] = next;
        if (outputIndex >= outBuffer.length)
        {
            flushOutput();
        }
    }

    private void softBreak() throws IOException
    {
        write(EQUALS);
        lineBreak();
    }

    private void lineBreak() throws IOException
    {
        write(CR);
        write(LF);
        nextSoftBreak = QUOTED_PRINTABLE_MAX_LINE_LENGTH;
    }

    void flushOutput() throws IOException
    {
        if (outputIndex < outBuffer.length)
        {
            out.write(outBuffer, 0, outputIndex);
        }
        else
        {
            out.write(outBuffer);
        }
        outputIndex = 0;
    }
}
