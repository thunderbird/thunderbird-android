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

/**
 * <p>
 * Implements methods common to all codecs defined in RFC 1522.
 * </p>
 * 
 * <p>
 * <a href="http://www.ietf.org/rfc/rfc1522.txt">RFC 1522</a> 
 * describes techniques to allow the encoding of non-ASCII text in 
 * various portions of a RFC 822 [2] message header, in a manner which
 * is unlikely to confuse existing message handling software.
 * </p>

 * @see <a href="http://www.ietf.org/rfc/rfc1522.txt">
 * MIME (Multipurpose Internet Mail Extensions) Part Two:
 * Message Header Extensions for Non-ASCII Text</a>
 * </p>
 * 
 * @author Apache Software Foundation
 * @since 1.3
 * @version $Id: RFC1522Codec.java,v 1.2 2004/04/09 22:21:43 ggregory Exp $
 */
abstract class RFC1522Codec {
    
    /**
     * Applies an RFC 1522 compliant encoding scheme to the given string of text with the 
     * given charset. This method constructs the "encoded-word" header common to all the 
     * RFC 1522 codecs and then invokes {@link #doEncoding(byte [])} method of a concrete 
     * class to perform the specific enconding.
     * 
     * @param text a string to encode
     * @param charset a charset to be used
     * 
     * @return RFC 1522 compliant "encoded-word"
     * 
     * @throws EncoderException thrown if there is an error conidition during the Encoding 
     *  process.
     * @throws UnsupportedEncodingException thrown if charset is not supported 
     * 
     * @see <a href="http://java.sun.com/j2se/1.3/docs/api/java/lang/package-summary.html#charenc">JRE character
     *          encoding names</a>
     */
    protected String encodeText(final String text, final String charset)
     throws EncoderException, UnsupportedEncodingException  
    {
        if (text == null) {
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append("=?"); 
        buffer.append(charset); 
        buffer.append('?'); 
        buffer.append(getEncoding()); 
        buffer.append('?');
        byte [] rawdata = doEncoding(text.getBytes(charset)); 
        buffer.append(new String(rawdata, StringEncodings.US_ASCII));
        buffer.append("?="); 
        return buffer.toString();
    }
    
    /**
     * Applies an RFC 1522 compliant decoding scheme to the given string of text. This method 
     * processes the "encoded-word" header common to all the RFC 1522 codecs and then invokes 
     * {@link #doEncoding(byte [])} method of a concrete class to perform the specific deconding.
     * 
     * @param text a string to decode
     * 
     * @throws DecoderException thrown if there is an error conidition during the Decoding 
     *  process.
     * @throws UnsupportedEncodingException thrown if charset specified in the "encoded-word" 
     *  header is not supported 
     */
    protected String decodeText(final String text)
     throws DecoderException, UnsupportedEncodingException  
    {
        if (text == null) {
            return null;
        }
        if ((!text.startsWith("=?")) || (!text.endsWith("?="))) {
            throw new DecoderException("RFC 1522 violation: malformed encoded content");
        }
        int termnator = text.length() - 2;
        int from = 2;
        int to = text.indexOf("?", from);
        if ((to == -1) || (to == termnator)) {
            throw new DecoderException("RFC 1522 violation: charset token not found");
        }
        String charset = text.substring(from, to);
        if (charset.equals("")) {
            throw new DecoderException("RFC 1522 violation: charset not specified");
        }
        from = to + 1;
        to = text.indexOf("?", from);
        if ((to == -1) || (to == termnator)) {
            throw new DecoderException("RFC 1522 violation: encoding token not found");
        }
        String encoding = text.substring(from, to);
        if (!getEncoding().equalsIgnoreCase(encoding)) {
            throw new DecoderException("This codec cannot decode " + 
                encoding + " encoded content");
        }
        from = to + 1;
        to = text.indexOf("?", from);
        byte[] data = text.substring(from, to).getBytes(StringEncodings.US_ASCII);
        data = doDecoding(data); 
        return new String(data, charset);
    }

    /**
     * Returns the codec name (referred to as encoding in the RFC 1522)
     * 
     * @return name of the codec
     */    
    protected abstract String getEncoding();

    /**
     * Encodes an array of bytes using the defined encoding scheme
     * 
     * @param bytes Data to be encoded
     *
     * @return A byte array containing the encoded data
     * 
     * @throws EncoderException thrown if the Encoder encounters a failure condition 
     *  during the encoding process.
     */    
    protected abstract byte[] doEncoding(byte[] bytes) throws EncoderException;

    /**
     * Decodes an array of bytes using the defined encoding scheme
     * 
     * @param bytes Data to be decoded
     *
     * @return a byte array that contains decoded data
     * 
     * @throws DecoderException A decoder exception is thrown if a Decoder encounters a 
     *  failure condition during the decode process.
     */    
    protected abstract byte[] doDecoding(byte[] bytes) throws DecoderException;
}
