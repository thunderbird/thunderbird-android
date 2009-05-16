/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package org.apache.commons.codec.net;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import org.apache.commons.codec.BinaryDecoder;
import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringDecoder;
import org.apache.commons.codec.StringEncoder;

/**
 * <p>
 * Codec for the Quoted-Printable section of <a href="http://www.ietf.org/rfc/rfc1521.txt">RFC 1521 </a>.
 * </p>
 * <p>
 * The Quoted-Printable encoding is intended to represent data that largely consists of octets that correspond to
 * printable characters in the ASCII character set. It encodes the data in such a way that the resulting octets are
 * unlikely to be modified by mail transport. If the data being encoded are mostly ASCII text, the encoded form of the
 * data remains largely recognizable by humans. A body which is entirely ASCII may also be encoded in Quoted-Printable
 * to ensure the integrity of the data should the message pass through a character- translating, and/or line-wrapping
 * gateway.
 * </p>
 * 
 * <p>
 * Note:
 * </p>
 * <p>
 * Rules #3, #4, and #5 of the quoted-printable spec are not implemented yet because the complete quoted-printable spec
 * does not lend itself well into the byte[] oriented codec framework. Complete the codec once the steamable codec
 * framework is ready. The motivation behind providing the codec in a partial form is that it can already come in handy
 * for those applications that do not require quoted-printable line formatting (rules #3, #4, #5), for instance Q codec.
 * </p>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1521.txt"> RFC 1521 MIME (Multipurpose Internet Mail Extensions) Part One:
 *          Mechanisms for Specifying and Describing the Format of Internet Message Bodies </a>
 * 
 * @author Apache Software Foundation
 * @since 1.3
 * @version $Id: QuotedPrintableCodec.java,v 1.7 2004/04/09 22:21:07 ggregory Exp $
 */
public class QuotedPrintableCodec implements BinaryEncoder, BinaryDecoder, StringEncoder, StringDecoder {
    /**
     * The default charset used for string decoding and encoding.
     */
    private String charset = StringEncodings.UTF8;

    /**
     * BitSet of printable characters as defined in RFC 1521.
     */
    private static final BitSet PRINTABLE_CHARS = new BitSet(256);

    private static byte ESCAPE_CHAR = '=';

    private static byte TAB = 9;

    private static byte SPACE = 32;
    // Static initializer for printable chars collection
    static {
        // alpha characters
        for (int i = 33; i <= 60; i++) {
            PRINTABLE_CHARS.set(i);
        }
        for (int i = 62; i <= 126; i++) {
            PRINTABLE_CHARS.set(i);
        }
        PRINTABLE_CHARS.set(TAB);
        PRINTABLE_CHARS.set(SPACE);
    }

    /**
     * Default constructor.
     */
    public QuotedPrintableCodec() {
        super();
    }

    /**
     * Constructor which allows for the selection of a default charset
     * 
     * @param charset
     *                  the default string charset to use.
     */
    public QuotedPrintableCodec(String charset) {
        super();
        this.charset = charset;
    }

    /**
     * Encodes byte into its quoted-printable representation.
     * 
     * @param b
     *                  byte to encode
     * @param buffer
     *                  the buffer to write to
     */
    private static final void encodeQuotedPrintable(int b, ByteArrayOutputStream buffer) {
        buffer.write(ESCAPE_CHAR);
        char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
        char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
        buffer.write(hex1);
        buffer.write(hex2);
    }

    /**
     * Encodes an array of bytes into an array of quoted-printable 7-bit characters. Unsafe characters are escaped.
     * 
     * <p>
     * This function implements a subset of quoted-printable encoding specification (rule #1 and rule #2) as defined in
     * RFC 1521 and is suitable for encoding binary data and unformatted text.
     * </p>
     * 
     * @param printable
     *                  bitset of characters deemed quoted-printable
     * @param bytes
     *                  array of bytes to be encoded
     * @return array of bytes containing quoted-printable data
     */
    public static final byte[] encodeQuotedPrintable(BitSet printable, byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        if (printable == null) {
            printable = PRINTABLE_CHARS;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i];
            if (b < 0) {
                b = 256 + b;
            }
            if (printable.get(b)) {
                buffer.write(b);
            } else {
                encodeQuotedPrintable(b, buffer);
            }
        }
        return buffer.toByteArray();
    }

    /**
     * Decodes an array quoted-printable characters into an array of original bytes. Escaped characters are converted
     * back to their original representation.
     * 
     * <p>
     * This function implements a subset of quoted-printable encoding specification (rule #1 and rule #2) as defined in
     * RFC 1521.
     * </p>
     * 
     * @param bytes
     *                  array of quoted-printable characters
     * @return array of original bytes
     * @throws DecoderException
     *                  Thrown if quoted-printable decoding is unsuccessful
     */
    public static final byte[] decodeQuotedPrintable(byte[] bytes) throws DecoderException {
        if (bytes == null) {
            return null;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i];
            if (b == ESCAPE_CHAR) {
                try {
                    int u = Character.digit((char) bytes[++i], 16);
                    int l = Character.digit((char) bytes[++i], 16);
                    if (u == -1 || l == -1) {
                        throw new DecoderException("Invalid quoted-printable encoding");
                    }
                    buffer.write((char) ((u << 4) + l));
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new DecoderException("Invalid quoted-printable encoding");
                }
            } else {
                buffer.write(b);
            }
        }
        return buffer.toByteArray();
    }

    /**
     * Encodes an array of bytes into an array of quoted-printable 7-bit characters. Unsafe characters are escaped.
     * 
     * <p>
     * This function implements a subset of quoted-printable encoding specification (rule #1 and rule #2) as defined in
     * RFC 1521 and is suitable for encoding binary data and unformatted text.
     * </p>
     * 
     * @param bytes
     *                  array of bytes to be encoded
     * @return array of bytes containing quoted-printable data
     */
    public byte[] encode(byte[] bytes) {
        return encodeQuotedPrintable(PRINTABLE_CHARS, bytes);
    }

    /**
     * Decodes an array of quoted-printable characters into an array of original bytes. Escaped characters are converted
     * back to their original representation.
     * 
     * <p>
     * This function implements a subset of quoted-printable encoding specification (rule #1 and rule #2) as defined in
     * RFC 1521.
     * </p>
     * 
     * @param bytes
     *                  array of quoted-printable characters
     * @return array of original bytes
     * @throws DecoderException
     *                  Thrown if quoted-printable decoding is unsuccessful
     */
    public byte[] decode(byte[] bytes) throws DecoderException {
        return decodeQuotedPrintable(bytes);
    }

    /**
     * Encodes a string into its quoted-printable form using the default string charset. Unsafe characters are escaped.
     * 
     * <p>
     * This function implements a subset of quoted-printable encoding specification (rule #1 and rule #2) as defined in
     * RFC 1521 and is suitable for encoding binary data.
     * </p>
     * 
     * @param pString
     *                  string to convert to quoted-printable form
     * @return quoted-printable string
     * 
     * @throws EncoderException
     *                  Thrown if quoted-printable encoding is unsuccessful
     * 
     * @see #getDefaultCharset()
     */
    public String encode(String pString) throws EncoderException {
        if (pString == null) {
            return null;
        }
        try {
            return encode(pString, getDefaultCharset());
        } catch (UnsupportedEncodingException e) {
            throw new EncoderException(e.getMessage());
        }
    }

    /**
     * Decodes a quoted-printable string into its original form using the specified string charset. Escaped characters
     * are converted back to their original representation.
     * 
     * @param pString
     *                  quoted-printable string to convert into its original form
     * @param charset
     *                  the original string charset
     * @return original string
     * @throws DecoderException
     *                  Thrown if quoted-printable decoding is unsuccessful
     * @throws UnsupportedEncodingException
     *                  Thrown if charset is not supported
     */
    public String decode(String pString, String charset) throws DecoderException, UnsupportedEncodingException {
        if (pString == null) {
            return null;
        }
        return new String(decode(pString.getBytes(StringEncodings.US_ASCII)), charset);
    }

    /**
     * Decodes a quoted-printable string into its original form using the default string charset. Escaped characters are
     * converted back to their original representation.
     * 
     * @param pString
     *                  quoted-printable string to convert into its original form
     * @return original string
     * @throws DecoderException
     *                  Thrown if quoted-printable decoding is unsuccessful
     * @throws UnsupportedEncodingException
     *                  Thrown if charset is not supported
     * @see #getDefaultCharset()
     */
    public String decode(String pString) throws DecoderException {
        if (pString == null) {
            return null;
        }
        try {
            return decode(pString, getDefaultCharset());
        } catch (UnsupportedEncodingException e) {
            throw new DecoderException(e.getMessage());
        }
    }

    /**
     * Encodes an object into its quoted-printable safe form. Unsafe characters are escaped.
     * 
     * @param pObject
     *                  string to convert to a quoted-printable form
     * @return quoted-printable object
     * @throws EncoderException
     *                  Thrown if quoted-printable encoding is not applicable to objects of this type or if encoding is
     *                  unsuccessful
     */
    public Object encode(Object pObject) throws EncoderException {
        if (pObject == null) {
            return null;
        } else if (pObject instanceof byte[]) {
            return encode((byte[]) pObject);
        } else if (pObject instanceof String) {
            return encode((String) pObject);
        } else {
            throw new EncoderException("Objects of type "
                + pObject.getClass().getName()
                + " cannot be quoted-printable encoded");
        }
    }

    /**
     * Decodes a quoted-printable object into its original form. Escaped characters are converted back to their original
     * representation.
     * 
     * @param pObject
     *                  quoted-printable object to convert into its original form
     * @return original object
     * @throws DecoderException
     *                  Thrown if quoted-printable decoding is not applicable to objects of this type if decoding is
     *                  unsuccessful
     */
    public Object decode(Object pObject) throws DecoderException {
        if (pObject == null) {
            return null;
        } else if (pObject instanceof byte[]) {
            return decode((byte[]) pObject);
        } else if (pObject instanceof String) {
            return decode((String) pObject);
        } else {
            throw new DecoderException("Objects of type "
                + pObject.getClass().getName()
                + " cannot be quoted-printable decoded");
        }
    }

    /**
     * Returns the default charset used for string decoding and encoding.
     * 
     * @return the default string charset.
     */
    public String getDefaultCharset() {
        return this.charset;
    }

    /**
     * Encodes a string into its quoted-printable form using the specified charset. Unsafe characters are escaped.
     * 
     * <p>
     * This function implements a subset of quoted-printable encoding specification (rule #1 and rule #2) as defined in
     * RFC 1521 and is suitable for encoding binary data and unformatted text.
     * </p>
     * 
     * @param pString
     *                  string to convert to quoted-printable form
     * @param charset
     *                  the charset for pString
     * @return quoted-printable string
     * 
     * @throws UnsupportedEncodingException
     *                  Thrown if the charset is not supported
     */
    public String encode(String pString, String charset) throws UnsupportedEncodingException {
        if (pString == null) {
            return null;
        }
        return new String(encode(pString.getBytes(charset)), StringEncodings.US_ASCII);
    }
}