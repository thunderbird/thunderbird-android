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
 * <p>Implements the 'www-form-urlencoded' encoding scheme, 
 * also misleadingly known as URL encoding.</p>
 *  
 * <p>For more detailed information please refer to 
 * <a href="http://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.1">
 * Chapter 17.13.4 'Form content types'</a> of the 
 * <a href="http://www.w3.org/TR/html4/">HTML 4.01 Specification<a></p>
 * 
 * <p> 
 * This codec is meant to be a replacement for standard Java classes
 * {@link java.net.URLEncoder} and {@link java.net.URLDecoder} 
 * on older Java platforms, as these classes in Java versions below 
 * 1.4 rely on the platform's default charset encoding.
 * </p>
 * 
 * @author Apache Software Foundation
 * @since 1.2
 * @version $Id: URLCodec.java,v 1.19 2004/03/29 07:59:00 ggregory Exp $
 */
public class URLCodec implements BinaryEncoder, BinaryDecoder, StringEncoder, StringDecoder {
    
    /**
     * The default charset used for string decoding and encoding.
     */
    protected String charset = StringEncodings.UTF8;
    
    protected static byte ESCAPE_CHAR = '%';
    /**
     * BitSet of www-form-url safe characters.
     */
    protected static final BitSet WWW_FORM_URL = new BitSet(256);
    
    // Static initializer for www_form_url
    static {
        // alpha characters
        for (int i = 'a'; i <= 'z'; i++) {
            WWW_FORM_URL.set(i);
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            WWW_FORM_URL.set(i);
        }
        // numeric characters
        for (int i = '0'; i <= '9'; i++) {
            WWW_FORM_URL.set(i);
        }
        // special chars
        WWW_FORM_URL.set('-');
        WWW_FORM_URL.set('_');
        WWW_FORM_URL.set('.');
        WWW_FORM_URL.set('*');
        // blank to be replaced with +
        WWW_FORM_URL.set(' ');
    }


    /**
     * Default constructor.
     */
    public URLCodec() {
        super();
    }

    /**
     * Constructor which allows for the selection of a default charset
     * 
     * @param charset the default string charset to use.
     */
    public URLCodec(String charset) {
        super();
        this.charset = charset;
    }

    /**
     * Encodes an array of bytes into an array of URL safe 7-bit 
     * characters. Unsafe characters are escaped.
     *
     * @param urlsafe bitset of characters deemed URL safe
     * @param bytes array of bytes to convert to URL safe characters
     * @return array of bytes containing URL safe characters
     */
    public static final byte[] encodeUrl(BitSet urlsafe, byte[] bytes) 
    {
        if (bytes == null) {
            return null;
        }
        if (urlsafe == null) {
            urlsafe = WWW_FORM_URL;
        }
        
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(); 
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i];
            if (b < 0) {
                b = 256 + b;
            }
            if (urlsafe.get(b)) {
                if (b == ' ') {
                    b = '+';
                }
                buffer.write(b);
            } else {
                buffer.write('%');
                char hex1 = Character.toUpperCase(
                  Character.forDigit((b >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(
                  Character.forDigit(b & 0xF, 16));
                buffer.write(hex1);
                buffer.write(hex2);
            }
        }
        return buffer.toByteArray(); 
    }


    /**
     * Decodes an array of URL safe 7-bit characters into an array of 
     * original bytes. Escaped characters are converted back to their 
     * original representation.
     *
     * @param bytes array of URL safe characters
     * @return array of original bytes 
     * @throws DecoderException Thrown if URL decoding is unsuccessful
     */
    public static final byte[] decodeUrl(byte[] bytes) 
         throws DecoderException
    {
        if (bytes == null) {
            return null;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(); 
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i];
            if (b == '+') {
                buffer.write(' ');
            } else if (b == '%') {
                try {
                    int u = Character.digit((char)bytes[++i], 16);
                    int l = Character.digit((char)bytes[++i], 16);
                    if (u == -1 || l == -1) {
                        throw new DecoderException("Invalid URL encoding");
                    }
                    buffer.write((char)((u << 4) + l));
                } catch(ArrayIndexOutOfBoundsException e) {
                    throw new DecoderException("Invalid URL encoding");
                }
            } else {
                buffer.write(b);
            }
        }
        return buffer.toByteArray(); 
    }


    /**
     * Encodes an array of bytes into an array of URL safe 7-bit 
     * characters. Unsafe characters are escaped.
     *
     * @param bytes array of bytes to convert to URL safe characters
     * @return array of bytes containing URL safe characters
     */
    public byte[] encode(byte[] bytes) {
        return encodeUrl(WWW_FORM_URL, bytes);
    }


    /**
     * Decodes an array of URL safe 7-bit characters into an array of 
     * original bytes. Escaped characters are converted back to their 
     * original representation.
     *
     * @param bytes array of URL safe characters
     * @return array of original bytes 
     * @throws DecoderException Thrown if URL decoding is unsuccessful
     */
    public byte[] decode(byte[] bytes) throws DecoderException {
        return decodeUrl(bytes);
    }


    /**
     * Encodes a string into its URL safe form using the specified
     * string charset. Unsafe characters are escaped.
     *
     * @param pString string to convert to a URL safe form
     * @param charset the charset for pString
     * @return URL safe string
     * @throws UnsupportedEncodingException Thrown if charset is not
     *                                      supported 
     */
    public String encode(String pString, String charset) 
        throws UnsupportedEncodingException  
    {
        if (pString == null) {
            return null;
        }
        return new String(encode(pString.getBytes(charset)), StringEncodings.US_ASCII);
    }


    /**
     * Encodes a string into its URL safe form using the default string 
     * charset. Unsafe characters are escaped.
     *
     * @param pString string to convert to a URL safe form
     * @return URL safe string
     * @throws EncoderException Thrown if URL encoding is unsuccessful
     * 
     * @see #getDefaultCharset()
     */
    public String encode(String pString) throws EncoderException {
        if (pString == null) {
            return null;
        }
        try {
            return encode(pString, getDefaultCharset());
        } catch(UnsupportedEncodingException e) {
            throw new EncoderException(e.getMessage());
        }
    }


    /**
     * Decodes a URL safe string into its original form using the 
     * specified encoding. Escaped characters are converted back 
     * to their original representation.
     *
     * @param pString URL safe string to convert into its original form
     * @param charset the original string charset
     * @return original string 
     * @throws DecoderException Thrown if URL decoding is unsuccessful
     * @throws UnsupportedEncodingException Thrown if charset is not
     *                                      supported 
     */
    public String decode(String pString, String charset) 
        throws DecoderException, UnsupportedEncodingException 
    {
        if (pString == null) {
            return null;
        }
        return new String(decode(pString.getBytes(StringEncodings.US_ASCII)), charset);
    }


    /**
     * Decodes a URL safe string into its original form using the default
     * string charset. Escaped characters are converted back to their 
     * original representation.
     *
     * @param pString URL safe string to convert into its original form
     * @return original string 
     * @throws DecoderException Thrown if URL decoding is unsuccessful
     * 
     * @see #getDefaultCharset()
     */
    public String decode(String pString) throws DecoderException {
        if (pString == null) {
            return null;
        }
        try {
            return decode(pString, getDefaultCharset());
        } catch(UnsupportedEncodingException e) {
            throw new DecoderException(e.getMessage());
        }
    }

    /**
     * Encodes an object into its URL safe form. Unsafe characters are 
     * escaped.
     *
     * @param pObject string to convert to a URL safe form
     * @return URL safe object
     * @throws EncoderException Thrown if URL encoding is not 
     *                          applicable to objects of this type or
     *                          if encoding is unsuccessful
     */
    public Object encode(Object pObject) throws EncoderException {
        if (pObject == null) {
            return null;
        } else if (pObject instanceof byte[]) {
            return encode((byte[])pObject);
        } else if (pObject instanceof String) {
            return encode((String)pObject);
        } else {
            throw new EncoderException("Objects of type " +
                pObject.getClass().getName() + " cannot be URL encoded"); 
              
        }
    }

    /**
     * Decodes a URL safe object into its original form. Escaped 
     * characters are converted back to their original representation.
     *
     * @param pObject URL safe object to convert into its original form
     * @return original object 
     * @throws DecoderException Thrown if URL decoding is not 
     *                          applicable to objects of this type
     *                          if decoding is unsuccessful
     */
    public Object decode(Object pObject) throws DecoderException {
        if (pObject == null) {
            return null;
        } else if (pObject instanceof byte[]) {
            return decode((byte[])pObject);
        } else if (pObject instanceof String) {
            return decode((String)pObject);
        } else {
            throw new DecoderException("Objects of type " +
                pObject.getClass().getName() + " cannot be URL decoded"); 
              
        }
    }

    /**
     * The <code>String</code> encoding used for decoding and encoding.
     *
     * @return Returns the encoding.
     * 
     * @deprecated use #getDefaultCharset()
     */
    public String getEncoding() {
        return this.charset;
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
