
package com.fsck.k9.mail.internet;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import okio.Buffer;
import okio.ByteString;
import okio.Okio;
import org.apache.james.mime4j.codec.QuotedPrintableInputStream;
import org.apache.james.mime4j.util.CharsetUtil;
import timber.log.Timber;


/**
 * Static methods for decoding strings, byte arrays and encoded words.
 *
 * This class is copied from the org.apache.james.mime4j.decoder.DecoderUtil class.  It's modified here in order to
 * decode emoji characters in the Subject headers.  The method to decode emoji depends on the MimeMessage class because
 * it has to be determined with the sender address, the mailer and so on.
 */
class DecoderUtil {
    /**
     * Decodes a string containing encoded words as defined by RFC 2047.
     * Encoded words in have the form
     * =?charset?enc?Encoded word?= where enc is either 'Q' or 'q' for
     * quoted-printable and 'B' or 'b' for Base64.
     *
     * ANDROID:  COPIED FROM A NEWER VERSION OF MIME4J
     *
     * @param body the string to decode.
     * @param message the message which has the string.
     * @return the decoded string.
     */
    public static String decodeEncodedWords(String body, Message message) {

        // ANDROID:  Most strings will not include "=?" so a quick test can prevent unneeded
        // object creation.  This could also be handled via lazy creation of the StringBuilder.
        if (!body.contains("=?")) {
            return body;
        }

        EncodedWord previousWord = null;
        int previousEnd = 0;

        StringBuilder sb = new StringBuilder();

        while (true) {
            int begin = body.indexOf("=?", previousEnd);
            if (begin == -1) {
                decodePreviousAndAppendSuffix(sb, previousWord, body, previousEnd);
                return sb.toString();
            }

            // ANDROID:  The mime4j original version has an error here.  It gets confused if
            // the encoded string begins with an '=' (just after "?Q?").  This patch seeks forward
            // to find the two '?' in the "header", before looking for the final "?=".
            int qm1 = body.indexOf('?', begin + 2);
            if (qm1 == -1) {
                decodePreviousAndAppendSuffix(sb, previousWord, body, previousEnd);
                return sb.toString();
            }

            int qm2 = body.indexOf('?', qm1 + 1);
            if (qm2 == -1) {
                decodePreviousAndAppendSuffix(sb, previousWord, body, previousEnd);
                return sb.toString();
            }

            int end = body.indexOf("?=", qm2 + 1);
            if (end == -1) {
                decodePreviousAndAppendSuffix(sb, previousWord, body, previousEnd);
                return sb.toString();
            }
            end += 2;

            String sep = body.substring(previousEnd, begin);

            EncodedWord word = extractEncodedWord(body, begin, end, message);

            if (previousWord == null) {
                sb.append(sep);
                if (word == null) {
                    sb.append(body.substring(begin, end));
                }
            } else {
                if (word == null) {
                    sb.append(charsetDecode(previousWord));
                    sb.append(sep);
                    sb.append(body.substring(begin, end));
                } else {
                    if (!CharsetUtil.isWhitespace(sep)) {
                        sb.append(charsetDecode(previousWord));
                        sb.append(sep);
                    } else if (previousWord.encoding.equals(word.encoding) &&
                            previousWord.charset.equals(word.charset)) {
                        word.data = concat(previousWord.data, word.data);
                    } else {
                        sb.append(charsetDecode(previousWord));
                    }
                }
            }

            previousWord = word;
            previousEnd = end;
        }
    }

    private static void decodePreviousAndAppendSuffix(StringBuilder sb, EncodedWord previousWord, String body,
            int previousEnd) {

        if (previousWord != null) {
            sb.append(charsetDecode(previousWord));
        }

        sb.append(body.substring(previousEnd));
    }

    private static String charsetDecode(EncodedWord word) {
        try {
            InputStream inputStream = new Buffer().write(word.data).inputStream();
            return CharsetSupport.readToString(inputStream, word.charset);
        } catch (IOException e) {
            return null;
        }
    }

    private static EncodedWord extractEncodedWord(String body, int begin, int end, Message message) {
        int qm1 = body.indexOf('?', begin + 2);
        if (qm1 == end - 2)
            return null;

        int qm2 = body.indexOf('?', qm1 + 1);
        if (qm2 == end - 2)
            return null;

        String mimeCharset = body.substring(begin + 2, qm1);
        String encoding = body.substring(qm1 + 1, qm2);
        String encodedText = body.substring(qm2 + 1, end - 2);

        String charset;
        try {
            charset = CharsetSupport.fixupCharset(mimeCharset, message);
        } catch (MessagingException e) {
            return null;
        }

        if (encodedText.isEmpty()) {
            Timber.w("Missing encoded text in encoded word: '%s'", body.substring(begin, end));
            return null;
        }

        EncodedWord encodedWord = new EncodedWord();
        encodedWord.charset = charset;
        if (encoding.equalsIgnoreCase("Q")) {
            encodedWord.encoding = "Q";
            encodedWord.data = decodeQ(encodedText);
        } else if (encoding.equalsIgnoreCase("B")) {
            encodedWord.encoding = "B";
            encodedWord.data = decodeB(encodedText);
        } else {
            Timber.w("Warning: Unknown encoding in encoded word '%s'", body.substring(begin, end));
            return null;
        }
        return encodedWord;
    }

    private static ByteString decodeQ(String encodedWord) {
        /*
         * Replace _ with =20
         */
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < encodedWord.length(); i++) {
            char c = encodedWord.charAt(i);
            if (c == '_') {
                sb.append("=20");
            } else {
                sb.append(c);
            }
        }

        byte[] bytes = sb.toString().getBytes(Charset.forName("US-ASCII"));

        QuotedPrintableInputStream is = new QuotedPrintableInputStream(new ByteArrayInputStream(bytes));
        try {
            return Okio.buffer(Okio.source(is)).readByteString();
        } catch (IOException e) {
            return null;
        }
    }

    private static ByteString decodeB(String encodedText) {
        ByteString decoded = ByteString.decodeBase64(encodedText);
        return decoded == null ? ByteString.EMPTY : decoded;
    }

    private static ByteString concat(ByteString first, ByteString second) {
        return new Buffer().write(first).write(second).readByteString();
    }


    private static class EncodedWord {
        private String charset;
        private String encoding;
        private ByteString data;
    }
}
