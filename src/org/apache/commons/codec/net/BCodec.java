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

import java.io.UnsupportedEncodingException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringDecoder;
import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.binary.Base64;

/**
 * <p>
 * Identical to the Base64 encoding defined by <a href="http://www.ietf.org/rfc/rfc1521.txt">RFC
 * 1521</a> and allows a character set to be specified.
 * </p>
 * 
 * <p>
 * <a href="http://www.ietf.org/rfc/rfc1522.txt">RFC 1522</a> describes techniques to allow the encoding of non-ASCII
 * text in various portions of a RFC 822 [2] message header, in a manner which is unlikely to confuse existing message
 * handling software.
 * </p>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1522.txt">MIME (Multipurpose Internet Mail Extensions) Part Two: Message
 *          Header Extensions for Non-ASCII Text</a>
 * 
 * @author Apache Software Foundation
 * @since 1.3
 * @version $Id: BCodec.java,v 1.5 2004/04/13 22:46:37 ggregory Exp $
 */
public class BCodec extends RFC1522Codec implements StringEncoder, StringDecoder {
    /**
     * The default charset used for string decoding and encoding.
     */
    private String charset = StringEncodings.UTF8;

    /**
     * Default constructor.
     */
    public BCodec() {
        super();
    }

    /**
     * Constructor which allows for the selection of a default charset
     * 
     * @param charset
     *                  the default string charset to use.
     * 
     * @see <a href="http://java.sun.com/j2se/1.3/docs/api/java/lang/package-summary.html#charenc">JRE character
     *          encoding names</a>
     */
    public BCodec(final String charset) {
        super();
        this.charset = charset;
    }

    protected String getEncoding() {
        return "B";
    }

    protected byte[] doEncoding(byte[] bytes) throws EncoderException {
        if (bytes == null) {
            return null;
        }
        return Base64.encodeBase64(bytes);
    }

    protected byte[] doDecoding(byte[] bytes) throws DecoderException {
        if (bytes == null) {
            return null;
        }
        return Base64.decodeBase64(bytes);
    }

    /**
     * Encodes a string into its Base64 form using the specified charset. Unsafe characters are escaped.
     * 
     * @param value
     *                  string to convert to Base64 form
     * @param charset
     *                  the charset for pString
     * @return Base64 string
     * 
     * @throws EncoderException
     *                  thrown if a failure condition is encountered during the encoding process.
     */
    public String encode(final String value, final String charset) throws EncoderException {
        if (value == null) {
            return null;
        }
        try {
            return encodeText(value, charset);
        } catch (UnsupportedEncodingException e) {
            throw new EncoderException(e.getMessage());
        }
    }

    /**
     * Encodes a string into its Base64 form using the default charset. Unsafe characters are escaped.
     * 
     * @param value
     *                  string to convert to Base64 form
     * @return Base64 string
     * 
     * @throws EncoderException
     *                  thrown if a failure condition is encountered during the encoding process.
     */
    public String encode(String value) throws EncoderException {
        if (value == null) {
            return null;
        }
        return encode(value, getDefaultCharset());
    }

    /**
     * Decodes a Base64 string into its original form. Escaped characters are converted back to their original
     * representation.
     * 
     * @param value
     *                  Base64 string to convert into its original form
     * 
     * @return original string
     * 
     * @throws DecoderException
     *                  A decoder exception is thrown if a failure condition is encountered during the decode process.
     */
    public String decode(String value) throws DecoderException {
        if (value == null) {
            return null;
        }
        try {
            return decodeText(value);
        } catch (UnsupportedEncodingException e) {
            throw new DecoderException(e.getMessage());
        }
    }

    /**
     * Encodes an object into its Base64 form using the default charset. Unsafe characters are escaped.
     * 
     * @param value
     *                  object to convert to Base64 form
     * @return Base64 object
     * 
     * @throws EncoderException
     *                  thrown if a failure condition is encountered during the encoding process.
     */
    public Object encode(Object value) throws EncoderException {
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return encode((String) value);
        } else {
            throw new EncoderException("Objects of type "
                + value.getClass().getName()
                + " cannot be encoded using BCodec");
        }
    }

    /**
     * Decodes a Base64 object into its original form. Escaped characters are converted back to their original
     * representation.
     * 
     * @param value
     *                  Base64 object to convert into its original form
     * 
     * @return original object
     * 
     * @throws DecoderException
     *                  A decoder exception is thrown if a failure condition is encountered during the decode process.
     */
    public Object decode(Object value) throws DecoderException {
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return decode((String) value);
        } else {
            throw new DecoderException("Objects of type "
                + value.getClass().getName()
                + " cannot be decoded using BCodec");
        }
    }

    /**
     * The default charset used for string decoding and encoding.
     * 
     * @return the default string charset.
     */
    public String getDefaultCharset() {
        return this.charset;
    }
}