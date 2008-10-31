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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.mime4j.util.CharsetUtil;

/**
 * Static methods for decoding strings, byte arrays and encoded words.
 *
 * 
 * @version $Id: DecoderUtil.java,v 1.3 2005/02/07 15:33:59 ntherning Exp $
 */
public class DecoderUtil {
    private static Log log = LogFactory.getLog(DecoderUtil.class);
    
    /**
     * Decodes a string containing quoted-printable encoded data. 
     * 
     * @param s the string to decode.
     * @return the decoded bytes.
     */
    public static byte[] decodeBaseQuotedPrintable(String s) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            byte[] bytes = s.getBytes("US-ASCII");
            
            QuotedPrintableInputStream is = new QuotedPrintableInputStream(
                                               new ByteArrayInputStream(bytes));
            
            int b = 0;
            while ((b = is.read()) != -1) {
                baos.write(b);
            }
        } catch (IOException e) {
            /*
             * This should never happen!
             */
            log.error(e);
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Decodes a string containing base64 encoded data. 
     * 
     * @param s the string to decode.
     * @return the decoded bytes.
     */
    public static byte[] decodeBase64(String s) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            byte[] bytes = s.getBytes("US-ASCII");
            
            Base64InputStream is = new Base64InputStream(
                                        new ByteArrayInputStream(bytes));
            
            int b = 0;
            while ((b = is.read()) != -1) {
                baos.write(b);
            }
        } catch (IOException e) {
            /*
             * This should never happen!
             */
            log.error(e);
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Decodes an encoded word encoded with the 'B' encoding (described in 
     * RFC 2047) found in a header field body.
     * 
     * @param encodedWord the encoded word to decode.
     * @param charset the Java charset to use.
     * @return the decoded string.
     * @throws UnsupportedEncodingException if the given Java charset isn't 
     *         supported.
     */
    public static String decodeB(String encodedWord, String charset) 
            throws UnsupportedEncodingException {
        
        return new String(decodeBase64(encodedWord), charset);
    }
    
    /**
     * Decodes an encoded word encoded with the 'Q' encoding (described in 
     * RFC 2047) found in a header field body.
     * 
     * @param encodedWord the encoded word to decode.
     * @param charset the Java charset to use.
     * @return the decoded string.
     * @throws UnsupportedEncodingException if the given Java charset isn't 
     *         supported.
     */
    public static String decodeQ(String encodedWord, String charset)
            throws UnsupportedEncodingException {
           
        /*
         * Replace _ with =20
         */
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < encodedWord.length(); i++) {
            char c = encodedWord.charAt(i);
            if (c == '_') {
                sb.append("=20");
            } else {
                sb.append(c);
            }
        }
        
        return new String(decodeBaseQuotedPrintable(sb.toString()), charset);
    }
    
    /**
     * Decodes a string containing encoded words as defined by RFC 2047.
     * Encoded words in have the form 
     * =?charset?enc?Encoded word?= where enc is either 'Q' or 'q' for 
     * quoted-printable and 'B' or 'b' for Base64.
     * 
     * @param body the string to decode.
     * @return the decoded string.
     */
    public static String decodeEncodedWords(String body) {
        StringBuffer sb = new StringBuffer();
        
        int p1 = 0;
        int p2 = 0;
        
        try {
            
            /*
             * Encoded words in headers have the form 
             * =?charset?enc?Encoded word?= where enc is either 'Q' or 'q' for 
             * quoted printable and 'B' and 'b' for Base64
             */
            
            while (p2 < body.length()) {
                /*
                 * Find beginning of first encoded word
                 */
                p1 = body.indexOf("=?", p2);
                if (p1 == -1) {
                    /*
                     * None found. Emit the rest of the header and exit.
                     */
                    sb.append(body.substring(p2));
                    break;
                }
                
                /*
                 * p2 points to the previously found end marker or the start
                 * of the entire header text. Append the text between that
                 * marker and the one pointed to by p1.
                 */
                if (p1 - p2 > 0) {
                    sb.append(body.substring(p2, p1));
                }

                /*
                 * Find the first and second '?':s after the marker pointed to
                 * by p1.
                 */
                int t1 = body.indexOf('?', p1 + 2);
                int t2 = t1 != -1 ? body.indexOf('?', t1 + 1) : -1;

                /*
                 * Find this words end marker.
                 */
                p2 = t2 != -1 ? body.indexOf("?=", t2 + 1) : -1;
                if (p2 == -1) {
                    if (t2 != -1 && (body.length() - 1 == t2 || body.charAt(t2 + 1) == '=')) {
                        /*
                         * Treat "=?charset?enc?" and "=?charset?enc?=" as
                         * empty strings.
                         */
                        p2 = t2;
                    } else {
                        /*
                         * No end marker was found. Append the rest of the 
                         * header and exit.
                         */
                        sb.append(body.substring(p1));
                        break;
                    }
                }

                /*
                 * [p1+2, t1] -> charset
                 * [t1+1, t2] -> encoding
                 * [t2+1, p2] -> encoded word
                 */
                
                String decodedWord = null;
                if (t2 == p2) {
                    /*
                     * The text is empty
                     */
                    decodedWord = "";
                } else {

                    String mimeCharset = body.substring(p1 + 2, t1);
                    String enc = body.substring(t1 + 1, t2);
                    String encodedWord = body.substring(t2 + 1, p2);

                    /*
                     * Convert the MIME charset to a corresponding Java one.
                     */
                    String charset = CharsetUtil.toJavaCharset(mimeCharset);
                    if (charset == null) {
                        decodedWord = body.substring(p1, p2 + 2);
                        if (log.isWarnEnabled()) {
                            log.warn("MIME charset '" + mimeCharset 
                                    + "' in header field doesn't have a "
                                    +"corresponding Java charset");
                        }
                    } else if (!CharsetUtil.isDecodingSupported(charset)) {
                        decodedWord = body.substring(p1, p2 + 2);
                        if (log.isWarnEnabled()) {
                            log.warn("Current JDK doesn't support decoding "
                                   + "of charset '" + charset 
                                   + "' (MIME charset '" 
                                   + mimeCharset + "')");
                        }
                    } else {
                        if (enc.equalsIgnoreCase("Q")) {
                            decodedWord = DecoderUtil.decodeQ(encodedWord, charset);
                        } else if (enc.equalsIgnoreCase("B")) {
                            decodedWord = DecoderUtil.decodeB(encodedWord, charset);
                        } else {
                            decodedWord = encodedWord;
                            if (log.isWarnEnabled()) {
                                log.warn("Warning: Unknown encoding in "
                                        + "header field '" + enc + "'");
                            }
                        }
                    }
                }
                p2 += 2;
                sb.append(decodedWord);
            }
        } catch (Throwable t) {
            log.error("Decoding header field body '" + body + "'", t);
        }
        
        return sb.toString();
    }
}
