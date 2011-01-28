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

/**
 * Utility methods related to codecs.
 */
public class CodecUtil {

    static final int DEFAULT_ENCODING_BUFFER_SIZE = 1024;

    /**
     * Copies the contents of one stream to the other.
     * @param in not null
     * @param out not null
     * @throws IOException
     */
    public static void copy(final InputStream in, final OutputStream out) throws IOException {
        final byte[] buffer = new byte[DEFAULT_ENCODING_BUFFER_SIZE];
        int inputLength;
        while (-1 != (inputLength = in.read(buffer))) {
            out.write(buffer, 0, inputLength);
        }
    }

    /**
     * Encodes the given stream using Quoted-Printable.
     * This assumes that stream is binary and therefore escapes
     * all line endings.
     * @param in not null
     * @param out not null
     * @throws IOException
     */
    public static void encodeQuotedPrintableBinary(final InputStream in, final OutputStream out) throws IOException {
        QuotedPrintableOutputStream qpOut = new QuotedPrintableOutputStream(out, true);
        copy(in, qpOut);
        qpOut.close();
    }

    /**
     * Encodes the given stream using Quoted-Printable.
     * This assumes that stream is text and therefore does not escape
     * all line endings.
     * @param in not null
     * @param out not null
     * @throws IOException
     */
    public static void encodeQuotedPrintable(final InputStream in, final OutputStream out) throws IOException {
        QuotedPrintableOutputStream qpOut = new QuotedPrintableOutputStream(out, false);
        copy(in, qpOut);
        qpOut.close();
    }

    /**
     * Encodes the given stream using base64.
     *
     * @param in not null
     * @param out not null
     * @throws IOException if an I/O error occurs
     */
    public static void encodeBase64(final InputStream in, final OutputStream out) throws IOException {
        Base64OutputStream b64Out = new Base64OutputStream(out);
        copy(in, b64Out);
        b64Out.close();
    }

    /**
     * Wraps the given stream in a Quoted-Printable encoder.
     * @param out not null
     * @return encoding outputstream
     * @throws IOException
     */
    public static OutputStream wrapQuotedPrintable(final OutputStream out, boolean binary) throws IOException {
        return new QuotedPrintableOutputStream(out, binary);
    }

    /**
     * Wraps the given stream in a Base64 encoder.
     * @param out not null
     * @return encoding outputstream
     * @throws IOException
     */
    public static OutputStream wrapBase64(final OutputStream out) throws IOException {
        return new Base64OutputStream(out);
    }

}
