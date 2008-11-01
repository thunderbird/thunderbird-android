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

package org.apache.james.mime4j.decoder;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Performs Base-64 decoding on an underlying stream.
 * 
 * 
 * @version $Id: Base64InputStream.java,v 1.3 2004/11/29 13:15:47 ntherning Exp $
 */
public class Base64InputStream extends InputStream {
    private static Log log = LogFactory.getLog(Base64InputStream.class);

    private final InputStream s;
    private final ByteQueue byteq = new ByteQueue(3);
    private boolean done = false;

    public Base64InputStream(InputStream s) {
        this.s = s;
    }

    /**
     * Closes the underlying stream.
     * 
     * @throws IOException on I/O errors.
     */
    public void close() throws IOException {
        s.close();
    }
    
    public int read() throws IOException {
        if (byteq.count() == 0) {
            fillBuffer();
            if (byteq.count() == 0) {
                return -1;
            }
        }

        byte val = byteq.dequeue();
        if (val >= 0)
            return val;
        else
            return val & 0xFF;
    }

    /**
     * Retrieve data from the underlying stream, decode it,
     * and put the results in the byteq.
     * @throws IOException
     */
    private void fillBuffer() throws IOException {
        byte[] data = new byte[4];
        int pos = 0;

        int i;
        while (!done) {
            switch (i = s.read()) {
                case -1:
                    if (pos > 0) {
                        log.warn("Unexpected EOF in MIME parser, dropping " 
                                + pos + " sextets");
                    }
                    return;
                case '=':
                    decodeAndEnqueue(data, pos);
                    done = true;
                    break;
                default:
                    byte sX = TRANSLATION[i];
                    if (sX < 0)
                        continue;
                    data[pos++] = sX;
                    if (pos == data.length) {
                        decodeAndEnqueue(data, pos);
                        return;
                    }
                    break;
            }
        }
    }

    private void decodeAndEnqueue(byte[] data, int len) {
        int accum = 0;
        accum |= data[0] << 18;
        accum |= data[1] << 12;
        accum |= data[2] << 6;
        accum |= data[3];

        byte b1 = (byte)(accum >>> 16);
        byteq.enqueue(b1);

        if (len > 2) {
            byte b2 = (byte)((accum >>> 8) & 0xFF);
            byteq.enqueue(b2);

            if (len > 3) {
                byte b3 = (byte)(accum & 0xFF);
                byteq.enqueue(b3);
            }
        }
    }

    private static byte[] TRANSLATION = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 0x00 */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 0x10 */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, /* 0x20 */
        52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, /* 0x30 */
        -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, /* 0x40 */
        15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, /* 0x50 */
        -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, /* 0x60 */
        41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1, /* 0x70 */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 0x80 */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 0x90 */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 0xA0 */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 0xB0 */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 0xC0 */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 0xD0 */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, /* 0xE0 */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1     /* 0xF0 */
    };


}
