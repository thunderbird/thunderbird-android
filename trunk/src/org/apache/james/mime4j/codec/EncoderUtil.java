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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.Locale;

import org.apache.james.mime4j.util.CharsetUtil;

/**
 * ANDROID:  THIS CLASS IS COPIED FROM A NEWER VERSION OF MIME4J
 */

/**
 * Static methods for encoding header field values. This includes encoded-words
 * as defined in <a href='http://www.faqs.org/rfcs/rfc2047.html'>RFC 2047</a>
 * or display-names of an e-mail address, for example.
 * 
 */
public class EncoderUtil {

    // This array is a lookup table that translates 6-bit positive integer index
    // values into their "Base64 Alphabet" equivalents as specified in Table 1
    // of RFC 2045.
    // ANDROID:  THIS TABLE IS COPIED FROM BASE64OUTPUTSTREAM
    static final byte[] BASE64_TABLE = { 'A', 'B', 'C', 'D', 'E', 'F',
            'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
            'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
            't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', '+', '/' };

    // Byte used to pad output.
    private static final byte BASE64_PAD = '=';

    private static final BitSet Q_REGULAR_CHARS = initChars("=_?");

    private static final BitSet Q_RESTRICTED_CHARS = initChars("=_?\"#$%&'(),.:;<>@[\\]^`{|}~");

    private static final int MAX_USED_CHARACTERS = 50;

    private static final String ENC_WORD_PREFIX = "=?";
    private static final String ENC_WORD_SUFFIX = "?=";

    private static final int ENCODED_WORD_MAX_LENGTH = 75; // RFC 2047

    private static final BitSet TOKEN_CHARS = initChars("()<>@,;:\\\"/[]?=");

    private static final BitSet ATEXT_CHARS = initChars("()<>@.,;:\\\"[]");

    private static BitSet initChars(String specials) {
        BitSet bs = new BitSet(128);
        for (char ch = 33; ch < 127; ch++) {
            if (specials.indexOf(ch) == -1) {
                bs.set(ch);
            }
        }
        return bs;
    }

    /**
     * Selects one of the two encodings specified in RFC 2047.
     */
    public enum Encoding {
        /** The B encoding (identical to base64 defined in RFC 2045). */
        B,
        /** The Q encoding (similar to quoted-printable defined in RFC 2045). */
        Q
    }

    /**
     * Indicates the intended usage of an encoded word.
     */
    public enum Usage {
        /**
         * Encoded word is used to replace a 'text' token in any Subject or
         * Comments header field.
         */
        TEXT_TOKEN,
        /**
         * Encoded word is used to replace a 'word' entity within a 'phrase',
         * for example, one that precedes an address in a From, To, or Cc
         * header.
         */
        WORD_ENTITY
    }

    private EncoderUtil() {
    }

    /**
     * Encodes the display-name portion of an address. See <a
     * href='http://www.faqs.org/rfcs/rfc5322.html'>RFC 5322</a> section 3.4
     * and <a href='http://www.faqs.org/rfcs/rfc2047.html'>RFC 2047</a> section
     * 5.3. The specified string should not be folded.
     * 
     * @param displayName
     *            display-name to encode.
     * @return encoded display-name.
     */
    public static String encodeAddressDisplayName(String displayName) {
        // display-name = phrase
        // phrase = 1*( encoded-word / word )
        // word = atom / quoted-string
        // atom = [CFWS] 1*atext [CFWS]
        // CFWS = comment or folding white space

        if (isAtomPhrase(displayName)) {
            return displayName;
        } else if (hasToBeEncoded(displayName, 0)) {
            return encodeEncodedWord(displayName, Usage.WORD_ENTITY);
        } else {
            return quote(displayName);
        }
    }

    /**
     * Encodes the local part of an address specification as described in RFC
     * 5322 section 3.4.1. Leading and trailing CFWS should have been removed
     * before calling this method. The specified string should not contain any
     * illegal (control or non-ASCII) characters.
     * 
     * @param localPart
     *            the local part to encode
     * @return the encoded local part.
     */
    public static String encodeAddressLocalPart(String localPart) {
        // local-part = dot-atom / quoted-string
        // dot-atom = [CFWS] dot-atom-text [CFWS]
        // CFWS = comment or folding white space

        if (isDotAtomText(localPart)) {
            return localPart;
        } else {
            return quote(localPart);
        }
    }

    /**
     * Encodes the specified strings into a header parameter as described in RFC
     * 2045 section 5.1 and RFC 2183 section 2. The specified strings should not
     * contain any illegal (control or non-ASCII) characters.
     * 
     * @param name
     *            parameter name.
     * @param value
     *            parameter value.
     * @return encoded result.
     */
    public static String encodeHeaderParameter(String name, String value) {
        name = name.toLowerCase(Locale.US);

        // value := token / quoted-string
        if (isToken(value)) {
            return name + "=" + value;
        } else {
            return name + "=" + quote(value);
        }
    }

    /**
     * Shortcut method that encodes the specified text into an encoded-word if
     * the text has to be encoded.
     * 
     * @param text
     *            text to encode.
     * @param usage
     *            whether the encoded-word is to be used to replace a text token
     *            or a word entity (see RFC 822).
     * @param usedCharacters
     *            number of characters already used up (<code>0 <= usedCharacters <= 50</code>).
     * @return the specified text if encoding is not necessary or an encoded
     *         word or a sequence of encoded words otherwise.
     */
    public static String encodeIfNecessary(String text, Usage usage,
            int usedCharacters) {
        if (hasToBeEncoded(text, usedCharacters))
            return encodeEncodedWord(text, usage, usedCharacters);
        else
            return text;
    }

    /**
     * Determines if the specified string has to encoded into an encoded-word.
     * Returns <code>true</code> if the text contains characters that don't
     * fall into the printable ASCII character set or if the text contains a
     * 'word' (sequence of non-whitespace characters) longer than 77 characters
     * (including characters already used up in the line).
     * 
     * @param text
     *            text to analyze.
     * @param usedCharacters
     *            number of characters already used up (<code>0 <= usedCharacters <= 50</code>).
     * @return <code>true</code> if the specified text has to be encoded into
     *         an encoded-word, <code>false</code> otherwise.
     */
    public static boolean hasToBeEncoded(String text, int usedCharacters) {
        if (text == null)
            throw new IllegalArgumentException();
        if (usedCharacters < 0 || usedCharacters > MAX_USED_CHARACTERS)
            throw new IllegalArgumentException();

        int nonWhiteSpaceCount = usedCharacters;

        for (int idx = 0; idx < text.length(); idx++) {
            char ch = text.charAt(idx);
            if (ch == '\t' || ch == ' ') {
                nonWhiteSpaceCount = 0;
            } else {
                nonWhiteSpaceCount++;
                if (nonWhiteSpaceCount > 77) {
                    // Line cannot be folded into multiple lines with no more
                    // than 78 characters each. Encoding as encoded-words makes
                    // that possible. One character has to be reserved for
                    // folding white space; that leaves 77 characters.
                    return true;
                }

                if (ch < 32 || ch >= 127) {
                    // non-printable ascii character has to be encoded
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Encodes the specified text into an encoded word or a sequence of encoded
     * words separated by space. The text is separated into a sequence of
     * encoded words if it does not fit in a single one.
     * <p>
     * The charset to encode the specified text into a byte array and the
     * encoding to use for the encoded-word are detected automatically.
     * <p>
     * This method assumes that zero characters have already been used up in the
     * current line.
     * 
     * @param text
     *            text to encode.
     * @param usage
     *            whether the encoded-word is to be used to replace a text token
     *            or a word entity (see RFC 822).
     * @return the encoded word (or sequence of encoded words if the given text
     *         does not fit in a single encoded word).
     * @see #hasToBeEncoded(String, int)
     */
    public static String encodeEncodedWord(String text, Usage usage) {
        return encodeEncodedWord(text, usage, 0, null, null);
    }

    /**
     * Encodes the specified text into an encoded word or a sequence of encoded
     * words separated by space. The text is separated into a sequence of
     * encoded words if it does not fit in a single one.
     * <p>
     * The charset to encode the specified text into a byte array and the
     * encoding to use for the encoded-word are detected automatically.
     * 
     * @param text
     *            text to encode.
     * @param usage
     *            whether the encoded-word is to be used to replace a text token
     *            or a word entity (see RFC 822).
     * @param usedCharacters
     *            number of characters already used up (<code>0 <= usedCharacters <= 50</code>).
     * @return the encoded word (or sequence of encoded words if the given text
     *         does not fit in a single encoded word).
     * @see #hasToBeEncoded(String, int)
     */
    public static String encodeEncodedWord(String text, Usage usage,
            int usedCharacters) {
        return encodeEncodedWord(text, usage, usedCharacters, null, null);
    }

    /**
     * Encodes the specified text into an encoded word or a sequence of encoded
     * words separated by space. The text is separated into a sequence of
     * encoded words if it does not fit in a single one.
     * 
     * @param text
     *            text to encode.
     * @param usage
     *            whether the encoded-word is to be used to replace a text token
     *            or a word entity (see RFC 822).
     * @param usedCharacters
     *            number of characters already used up (<code>0 <= usedCharacters <= 50</code>).
     * @param charset
     *            the Java charset that should be used to encode the specified
     *            string into a byte array. A suitable charset is detected
     *            automatically if this parameter is <code>null</code>.
     * @param encoding
     *            the encoding to use for the encoded-word (either B or Q). A
     *            suitable encoding is automatically chosen if this parameter is
     *            <code>null</code>.
     * @return the encoded word (or sequence of encoded words if the given text
     *         does not fit in a single encoded word).
     * @see #hasToBeEncoded(String, int)
     */
    public static String encodeEncodedWord(String text, Usage usage,
            int usedCharacters, Charset charset, Encoding encoding) {
        if (text == null)
            throw new IllegalArgumentException();
        if (usedCharacters < 0 || usedCharacters > MAX_USED_CHARACTERS)
            throw new IllegalArgumentException();

        if (charset == null)
            charset = determineCharset(text);

        String mimeCharset = CharsetUtil.toMimeCharset(charset.name());
        if (mimeCharset == null) {
            // cannot happen if charset was originally null
            throw new IllegalArgumentException("Unsupported charset");
        }

        byte[] bytes = encode(text, charset);

        if (encoding == null)
            encoding = determineEncoding(bytes, usage);

        if (encoding == Encoding.B) {
            String prefix = ENC_WORD_PREFIX + mimeCharset + "?B?";
            return encodeB(prefix, text, usedCharacters, charset, bytes);
        } else {
            String prefix = ENC_WORD_PREFIX + mimeCharset + "?Q?";
            return encodeQ(prefix, text, usage, usedCharacters, charset, bytes);
        }
    }

    /**
     * Encodes the specified byte array using the B encoding defined in RFC
     * 2047.
     * 
     * @param bytes
     *            byte array to encode.
     * @return encoded string.
     */
    public static String encodeB(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        int idx = 0;
        final int end = bytes.length;
        for (; idx < end - 2; idx += 3) {
            int data = (bytes[idx] & 0xff) << 16 | (bytes[idx + 1] & 0xff) << 8
                    | bytes[idx + 2] & 0xff;
            sb.append((char) BASE64_TABLE[data >> 18 & 0x3f]);
            sb.append((char) BASE64_TABLE[data >> 12 & 0x3f]);
            sb.append((char) BASE64_TABLE[data >> 6 & 0x3f]);
            sb.append((char) BASE64_TABLE[data & 0x3f]);
        }

        if (idx == end - 2) {
            int data = (bytes[idx] & 0xff) << 16 | (bytes[idx + 1] & 0xff) << 8;
            sb.append((char) BASE64_TABLE[data >> 18 & 0x3f]);
            sb.append((char) BASE64_TABLE[data >> 12 & 0x3f]);
            sb.append((char) BASE64_TABLE[data >> 6 & 0x3f]);
            sb.append((char) BASE64_PAD);

        } else if (idx == end - 1) {
            int data = (bytes[idx] & 0xff) << 16;
            sb.append((char) BASE64_TABLE[data >> 18 & 0x3f]);
            sb.append((char) BASE64_TABLE[data >> 12 & 0x3f]);
            sb.append((char) BASE64_PAD);
            sb.append((char) BASE64_PAD);
        }

        return sb.toString();
    }

    /**
     * Encodes the specified byte array using the Q encoding defined in RFC
     * 2047.
     * 
     * @param bytes
     *            byte array to encode.
     * @param usage
     *            whether the encoded-word is to be used to replace a text token
     *            or a word entity (see RFC 822).
     * @return encoded string.
     */
    public static String encodeQ(byte[] bytes, Usage usage) {
        BitSet qChars = usage == Usage.TEXT_TOKEN ? Q_REGULAR_CHARS
                : Q_RESTRICTED_CHARS;

        StringBuilder sb = new StringBuilder();

        final int end = bytes.length;
        for (int idx = 0; idx < end; idx++) {
            int v = bytes[idx] & 0xff;
            if (v == 32) {
                sb.append('_');
            } else if (!qChars.get(v)) {
                sb.append('=');
                sb.append(hexDigit(v >>> 4));
                sb.append(hexDigit(v & 0xf));
            } else {
                sb.append((char) v);
            }
        }

        return sb.toString();
    }

    /**
     * Tests whether the specified string is a token as defined in RFC 2045
     * section 5.1.
     * 
     * @param str
     *            string to test.
     * @return <code>true</code> if the specified string is a RFC 2045 token,
     *         <code>false</code> otherwise.
     */
    public static boolean isToken(String str) {
        // token := 1*<any (US-ASCII) CHAR except SPACE, CTLs, or tspecials>
        // tspecials := "(" / ")" / "<" / ">" / "@" / "," / ";" / ":" / "\" /
        // <"> / "/" / "[" / "]" / "?" / "="
        // CTL := 0.- 31., 127.

        final int length = str.length();
        if (length == 0)
            return false;

        for (int idx = 0; idx < length; idx++) {
            char ch = str.charAt(idx);
            if (!TOKEN_CHARS.get(ch))
                return false;
        }

        return true;
    }

    private static boolean isAtomPhrase(String str) {
        // atom = [CFWS] 1*atext [CFWS]

        boolean containsAText = false;

        final int length = str.length();
        for (int idx = 0; idx < length; idx++) {
            char ch = str.charAt(idx);
            if (ATEXT_CHARS.get(ch)) {
                containsAText = true;
            } else if (!CharsetUtil.isWhitespace(ch)) {
                return false;
            }
        }

        return containsAText;
    }

    // RFC 5322 section 3.2.3
    private static boolean isDotAtomText(String str) {
        // dot-atom-text = 1*atext *("." 1*atext)
        // atext = ALPHA / DIGIT / "!" / "#" / "$" / "%" / "&" / "'" / "*" /
        // "+" / "-" / "/" / "=" / "?" / "^" / "_" / "`" / "{" / "|" / "}" / "~"

        char prev = '.';

        final int length = str.length();
        if (length == 0)
            return false;

        for (int idx = 0; idx < length; idx++) {
            char ch = str.charAt(idx);

            if (ch == '.') {
                if (prev == '.' || idx == length - 1)
                    return false;
            } else {
                if (!ATEXT_CHARS.get(ch))
                    return false;
            }

            prev = ch;
        }

        return true;
    }

    // RFC 5322 section 3.2.4
    private static String quote(String str) {
        // quoted-string = [CFWS] DQUOTE *([FWS] qcontent) [FWS] DQUOTE [CFWS]
        // qcontent = qtext / quoted-pair
        // qtext = %d33 / %d35-91 / %d93-126
        // quoted-pair = ("\" (VCHAR / WSP))
        // VCHAR = %x21-7E
        // DQUOTE = %x22

        String escaped = str.replaceAll("[\\\\\"]", "\\\\$0");
        return "\"" + escaped + "\"";
    }

    private static String encodeB(String prefix, String text,
            int usedCharacters, Charset charset, byte[] bytes) {
        int encodedLength = bEncodedLength(bytes);

        int totalLength = prefix.length() + encodedLength
                + ENC_WORD_SUFFIX.length();
        if (totalLength <= ENCODED_WORD_MAX_LENGTH - usedCharacters) {
            return prefix + encodeB(bytes) + ENC_WORD_SUFFIX;
        } else {
            int splitOffset = text.offsetByCodePoints(text.length() / 2, -1);
                                                         
            String part1 = text.substring(0, splitOffset);
            byte[] bytes1 = encode(part1, charset);
            String word1 = encodeB(prefix, part1, usedCharacters, charset,
                    bytes1);

            String part2 = text.substring(splitOffset);
            byte[] bytes2 = encode(part2, charset);
            String word2 = encodeB(prefix, part2, 0, charset, bytes2);

            return word1 + " " + word2;
        }
    }

    private static int bEncodedLength(byte[] bytes) {
        return (bytes.length + 2) / 3 * 4;
    }

    private static String encodeQ(String prefix, String text, Usage usage,
            int usedCharacters, Charset charset, byte[] bytes) {
        int encodedLength = qEncodedLength(bytes, usage);

        int totalLength = prefix.length() + encodedLength
                + ENC_WORD_SUFFIX.length();
        if (totalLength <= ENCODED_WORD_MAX_LENGTH - usedCharacters) {
            return prefix + encodeQ(bytes, usage) + ENC_WORD_SUFFIX;
        } else {
            int splitOffset = text.offsetByCodePoints(text.length() / 2, -1);

            String part1 = text.substring(0, splitOffset);
            byte[] bytes1 = encode(part1, charset);
            String word1 = encodeQ(prefix, part1, usage, usedCharacters,
                    charset, bytes1);

            String part2 = text.substring(splitOffset);
            byte[] bytes2 = encode(part2, charset);
            String word2 = encodeQ(prefix, part2, usage, 0, charset, bytes2);

            return word1 + " " + word2;
        }
    }

    private static int qEncodedLength(byte[] bytes, Usage usage) {
        BitSet qChars = usage == Usage.TEXT_TOKEN ? Q_REGULAR_CHARS
                : Q_RESTRICTED_CHARS;

        int count = 0;

        for (int idx = 0; idx < bytes.length; idx++) {
            int v = bytes[idx] & 0xff;
            if (v == 32) {
                count++;
            } else if (!qChars.get(v)) {
                count += 3;
            } else {
                count++;
            }
        }

        return count;
    }

    private static byte[] encode(String text, Charset charset) {
        ByteBuffer buffer = charset.encode(text);
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return bytes;
    }

    private static Charset determineCharset(String text) {
        // it is an important property of iso-8859-1 that it directly maps
        // unicode code points 0000 to 00ff to byte values 00 to ff.
        boolean ascii = true;
        final int len = text.length();
        for (int index = 0; index < len; index++) {
            char ch = text.charAt(index);
            if (ch > 0xff) {
                return CharsetUtil.UTF_8;
            }
            if (ch > 0x7f) {
                ascii = false;
            }
        }
        return ascii ? CharsetUtil.US_ASCII : CharsetUtil.ISO_8859_1;
    }

    private static Encoding determineEncoding(byte[] bytes, Usage usage) {
        if (bytes.length == 0)
            return Encoding.Q;

        BitSet qChars = usage == Usage.TEXT_TOKEN ? Q_REGULAR_CHARS
                : Q_RESTRICTED_CHARS;

        int qEncoded = 0;
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            if (v != 32 && !qChars.get(v)) {
                qEncoded++;
            }
        }

        int percentage = qEncoded * 100 / bytes.length;
        return percentage > 30 ? Encoding.B : Encoding.Q;
    }

    private static char hexDigit(int i) {
        return i < 10 ? (char) (i + '0') : (char) (i - 10 + 'A');
    }
}
