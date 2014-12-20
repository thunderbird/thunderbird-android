
package com.fsck.k9.mail.internet;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.BitSet;

import org.apache.james.mime4j.util.CharsetUtil;

/**
 * Static methods for encoding header field values. This includes encoded-words
 * as defined in <a href='http://www.faqs.org/rfcs/rfc2047.html'>RFC 2047</a>
 * or display-names of an e-mail address, for example.
 *
 * This class is copied from the org.apache.james.mime4j.decoder.EncoderUtil class.  It's modified here in order to
 * encode emoji characters in the Subject headers.  The method to decode emoji depends on the MimeMessage class because
 * it has to be determined with the sender address.
 */
class EncoderUtil {
    private static final BitSet Q_RESTRICTED_CHARS = initChars("=_?\"#$%&'(),.:;<>@[\\]^`{|}~");

    private static final String ENC_WORD_PREFIX = "=?";
    private static final String ENC_WORD_SUFFIX = "?=";

    private static final int ENCODED_WORD_MAX_LENGTH = 75; // RFC 2047

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

    private EncoderUtil() {
    }

    /**
     * Encodes the specified text into an encoded word or a sequence of encoded
     * words separated by space. The text is separated into a sequence of
     * encoded words if it does not fit in a single one.
     *
     * @param text
     *            text to encode.
     * @param charset
     *            the Java charset that should be used to encode the specified
     *            string into a byte array. A suitable charset is detected
     *            automatically if this parameter is <code>null</code>.
     * @return the encoded word (or sequence of encoded words if the given text
     *         does not fit in a single encoded word).
     */
    public static String encodeEncodedWord(String text, Charset charset) {
        if (text == null)
            throw new IllegalArgumentException();

        if (charset == null)
            charset = determineCharset(text);

        String mimeCharset = CharsetSupport.getExternalCharset(charset.name());

        byte[] bytes = encode(text, charset);

        Encoding encoding = determineEncoding(bytes);

        if (encoding == Encoding.B) {
            String prefix = ENC_WORD_PREFIX + mimeCharset + "?B?";
            return encodeB(prefix, text, charset, bytes);
        } else {
            String prefix = ENC_WORD_PREFIX + mimeCharset + "?Q?";
            return encodeQ(prefix, text, charset, bytes);
        }
    }

    private static String encodeB(String prefix, String text, Charset charset, byte[] bytes) {
        int encodedLength = bEncodedLength(bytes);

        int totalLength = prefix.length() + encodedLength
                          + ENC_WORD_SUFFIX.length();
        if (totalLength <= ENCODED_WORD_MAX_LENGTH) {
            return prefix + org.apache.james.mime4j.codec.EncoderUtil.encodeB(bytes) + ENC_WORD_SUFFIX;
        } else {
            String part1 = text.substring(0, text.length() / 2);
            byte[] bytes1 = encode(part1, charset);
            String word1 = encodeB(prefix, part1, charset, bytes1);

            String part2 = text.substring(text.length() / 2);
            byte[] bytes2 = encode(part2, charset);
            String word2 = encodeB(prefix, part2, charset, bytes2);

            return word1 + " " + word2;
        }
    }

    private static int bEncodedLength(byte[] bytes) {
        return (bytes.length + 2) / 3 * 4;
    }

    private static String encodeQ(String prefix, String text,  Charset charset, byte[] bytes) {
        int encodedLength = qEncodedLength(bytes);

        int totalLength = prefix.length() + encodedLength
                          + ENC_WORD_SUFFIX.length();
        if (totalLength <= ENCODED_WORD_MAX_LENGTH) {
            return prefix + org.apache.james.mime4j.codec.EncoderUtil.encodeQ(bytes, org.apache.james.mime4j.codec.EncoderUtil.Usage.WORD_ENTITY) + ENC_WORD_SUFFIX;
        } else {
            String part1 = text.substring(0, text.length() / 2);
            byte[] bytes1 = encode(part1, charset);
            String word1 = encodeQ(prefix, part1, charset, bytes1);

            String part2 = text.substring(text.length() / 2);
            byte[] bytes2 = encode(part2, charset);
            String word2 = encodeQ(prefix, part2, charset, bytes2);

            return word1 + " " + word2;
        }
    }

    private static int qEncodedLength(byte[] bytes) {
        int count = 0;

        for (byte b : bytes) {
            int v = b & 0xff;
            if (v == 32) {
                count++;
            } else if (!Q_RESTRICTED_CHARS.get(v)) {
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

    private static Encoding determineEncoding(byte[] bytes) {
        if (bytes.length == 0)
            return Encoding.Q;

        int qEncoded = 0;
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            if (v != 32 && !Q_RESTRICTED_CHARS.get(v)) {
                qEncoded++;
            }
        }

        int percentage = qEncoded * 100 / bytes.length;
        return percentage > 30 ? Encoding.B : Encoding.Q;
    }
}
