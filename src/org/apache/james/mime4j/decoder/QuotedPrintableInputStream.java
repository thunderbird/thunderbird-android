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
 * Performs Quoted-Printable decoding on an underlying stream.
 * 
 * 
 * 
 * @version $Id: QuotedPrintableInputStream.java,v 1.3 2004/11/29 13:15:47 ntherning Exp $
 */
public class QuotedPrintableInputStream extends InputStream {
    private static Log log = LogFactory.getLog(QuotedPrintableInputStream.class);
    
    private InputStream stream;
    ByteQueue byteq = new ByteQueue();
    ByteQueue pushbackq = new ByteQueue();
    private byte state = 0;

    public QuotedPrintableInputStream(InputStream stream) {
        this.stream = stream;
    }
    
    /**
     * Closes the underlying stream.
     * 
     * @throws IOException on I/O errors.
     */
    public void close() throws IOException {
        stream.close();
    }

    public int read() throws IOException {
        fillBuffer();
        if (byteq.count() == 0)
            return -1;
        else {
            byte val = byteq.dequeue();
            if (val >= 0)
                return val;
            else
                return val & 0xFF;
        }
    }

    /**
     * Pulls bytes out of the underlying stream and places them in the
     * pushback queue.  This is necessary (vs. reading from the
     * underlying stream directly) to detect and filter out "transport
     * padding" whitespace, i.e., all whitespace that appears immediately
     * before a CRLF.
     *
     * @throws IOException Underlying stream threw IOException.
     */
    private void populatePushbackQueue() throws IOException {
        //Debug.verify(pushbackq.count() == 0, "PopulatePushbackQueue called when pushback queue was not empty!");

        if (pushbackq.count() != 0)
            return;

        while (true) {
            int i = stream.read();
            switch (i) {
                case -1:
                    // stream is done
                    pushbackq.clear();  // discard any whitespace preceding EOF
                    return;
                case ' ':
                case '\t':
                    pushbackq.enqueue((byte)i);
                    break;
                case '\r':
                case '\n':
                    pushbackq.clear();  // discard any whitespace preceding EOL
                    pushbackq.enqueue((byte)i);
                    return;
                default:
                    pushbackq.enqueue((byte)i);
                    return;
            }
        }
    }

    /**
     * Causes the pushback queue to get populated if it is empty, then
     * consumes and decodes bytes out of it until one or more bytes are
     * in the byte queue.  This decoding step performs the actual QP
     * decoding.
     *
     * @throws IOException Underlying stream threw IOException.
     */
    private void fillBuffer() throws IOException {
        byte msdChar = 0;  // first digit of escaped num
        while (byteq.count() == 0) {
            if (pushbackq.count() == 0) {
                populatePushbackQueue();
                if (pushbackq.count() == 0)
                    return;
            }

            byte b = (byte)pushbackq.dequeue();

            switch (state) {
                case 0:  // start state, no bytes pending
                    if (b != '=') {
                        byteq.enqueue(b);
                        break;  // state remains 0
                    } else {
                        state = 1;
                        break;
                    }
                case 1:  // encountered "=" so far
                    if (b == '\r') {
                        state = 2;
                        break;
                    } else if ((b >= '0' && b <= '9') || (b >= 'A' && b <= 'F') || (b >= 'a' && b <= 'f')) {
                        state = 3;
                        msdChar = b;  // save until next digit encountered
                        break;
                    } else if (b == '=') {
                        /*
                         * Special case when == is encountered.
                         * Emit one = and stay in this state.
                         */
                        if (log.isWarnEnabled()) {
                            log.warn("Malformed MIME; got ==");
                        }
                        byteq.enqueue((byte)'=');
                        break;
                    } else {
                        if (log.isWarnEnabled()) {
                            log.warn("Malformed MIME; expected \\r or "
                                    + "[0-9A-Z], got " + b);
                        }
                        state = 0;
                        byteq.enqueue((byte)'=');
                        byteq.enqueue(b);
                        break;
                    }
                case 2:  // encountered "=\r" so far
                    if (b == '\n') {
                        state = 0;
                        break;
                    } else {
                        if (log.isWarnEnabled()) {
                            log.warn("Malformed MIME; expected " 
                                    + (int)'\n' + ", got " + b);
                        }
                        state = 0;
                        byteq.enqueue((byte)'=');
                        byteq.enqueue((byte)'\r');
                        byteq.enqueue(b);
                        break;
                    }
                case 3:  // encountered =<digit> so far; expecting another <digit> to complete the octet
                    if ((b >= '0' && b <= '9') || (b >= 'A' && b <= 'F') || (b >= 'a' && b <= 'f')) {
                        byte msd = asciiCharToNumericValue(msdChar);
                        byte low = asciiCharToNumericValue(b);
                        state = 0;
                        byteq.enqueue((byte)((msd << 4) | low));
                        break;
                    } else {
                        if (log.isWarnEnabled()) {
                            log.warn("Malformed MIME; expected "
                                     + "[0-9A-Z], got " + b);
                        }
                        state = 0;
                        byteq.enqueue((byte)'=');
                        byteq.enqueue(msdChar);
                        byteq.enqueue(b);
                        break;
                    }
                default:  // should never happen
                    log.error("Illegal state: " + state);
                    state = 0;
                    byteq.enqueue(b);
                    break;
            }
        }
    }

    /**
     * Converts '0' => 0, 'A' => 10, etc.
     * @param c ASCII character value.
     * @return Numeric value of hexadecimal character.
     */
    private byte asciiCharToNumericValue(byte c) {
        if (c >= '0' && c <= '9') {
            return (byte)(c - '0');
        } else if (c >= 'A' && c <= 'Z') {
            return (byte)(0xA + (c - 'A'));
        } else if (c >= 'a' && c <= 'z') {
            return (byte)(0xA + (c - 'a'));
        } else {
            /*
             * This should never happen since all calls to this method
             * are preceded by a check that c is in [0-9A-Za-z]
             */
            throw new IllegalArgumentException((char) c 
                    + " is not a hexadecimal digit");
        }
    }

}
