package com.fsck.k9.mail.internet

import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import java.io.ByteArrayInputStream
import java.io.IOException
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.buffer
import okio.source
import org.apache.james.mime4j.codec.QuotedPrintableInputStream
import org.apache.james.mime4j.util.CharsetUtil
import timber.log.Timber

/**
 * Decoder for encoded words (RFC 2047).
 *
 * This class is based on `org.apache.james.mime4j.decoder.DecoderUtil`. It was modified in order to support early
 * non-Unicode emoji variants.
 */
internal object DecoderUtil {
    /**
     * Decodes a string containing encoded words as defined by RFC 2047.
     *
     * Encoded words have the form `=?charset?enc?Encoded word?=` where `enc` is either 'Q' or 'q' for
     * quoted-printable and 'B' or 'b' for Base64.
     *
     * @param body The string to decode.
     * @param message The message containing the string. It will be used to figure out which JIS variant to use for
     *     charset decoding. May be `null`.
     * @return The decoded string.
     */
    @JvmStatic
    fun decodeEncodedWords(body: String, message: Message?): String {
        // Most strings will not include "=?". So a quick test can prevent unneeded work.
        if (!body.contains("=?")) return body

        var previousWord: EncodedWord? = null
        var previousEnd = 0
        val output = StringBuilder()

        while (true) {
            val begin = body.indexOf("=?", previousEnd)
            if (begin == -1) {
                decodePreviousAndAppendSuffix(output, previousWord, body, previousEnd)
                return output.toString()
            }

            val qm1 = body.indexOf('?', begin + 2)
            if (qm1 == -1) {
                decodePreviousAndAppendSuffix(output, previousWord, body, previousEnd)
                return output.toString()
            }

            val qm2 = body.indexOf('?', qm1 + 1)
            if (qm2 == -1) {
                decodePreviousAndAppendSuffix(output, previousWord, body, previousEnd)
                return output.toString()
            }

            var end = body.indexOf("?=", qm2 + 1)
            if (end == -1) {
                decodePreviousAndAppendSuffix(output, previousWord, body, previousEnd)
                return output.toString()
            }
            end += 2

            val sep = body.substring(previousEnd, begin)
            val word = extractEncodedWord(body, begin, end, message)

            if (previousWord == null) {
                output.append(sep)
                if (word == null) {
                    output.append(body, begin, end)
                }
            } else if (word == null) {
                output.append(charsetDecode(previousWord))
                output.append(sep)
                output.append(body, begin, end)
            } else if (!CharsetUtil.isWhitespace(sep)) {
                output.append(charsetDecode(previousWord))
                output.append(sep)
            } else if (previousWord.canBeCombinedWith(word)) {
                word.data = previousWord.data + word.data
            } else {
                output.append(charsetDecode(previousWord))
            }

            previousWord = word
            previousEnd = end
        }
    }

    private fun decodePreviousAndAppendSuffix(
        output: StringBuilder,
        previousWord: EncodedWord?,
        body: String,
        previousEnd: Int
    ) {
        if (previousWord != null) {
            output.append(charsetDecode(previousWord))
        }
        output.append(body, previousEnd, body.length)
    }

    private fun charsetDecode(word: EncodedWord): String? {
        return try {
            val inputStream = Buffer().write(word.data).inputStream()
            CharsetSupport.readToString(inputStream, word.charset)
        } catch (e: IOException) {
            null
        }
    }

    private fun extractEncodedWord(body: String, begin: Int, end: Int, message: Message?): EncodedWord? {
        val qm1 = body.indexOf('?', begin + 2)
        if (qm1 == end - 2) return null

        val qm2 = body.indexOf('?', qm1 + 1)
        if (qm2 == end - 2) return null

        // Extract charset, skipping language information if present (example: =?utf-8*en?Q?Text?=)
        val charsetPart = body.substring(begin + 2, qm1)
        val languageSuffixStart = charsetPart.indexOf('*')
        val languageSuffixFound = languageSuffixStart != -1
        val mimeCharset = if (languageSuffixFound) charsetPart.substring(0, languageSuffixStart) else charsetPart

        val encoding = body.substring(qm1 + 1, qm2)
        val encodedText = body.substring(qm2 + 1, end - 2)

        val charset = try {
            CharsetSupport.fixupCharset(mimeCharset, message)
        } catch (e: MessagingException) {
            return null
        }

        if (encodedText.isEmpty()) {
            Timber.w("Missing encoded text in encoded word: '%s'", body.substring(begin, end))
            return null
        }

        return if (encoding.equals("Q", ignoreCase = true)) {
            EncodedWord(charset, Encoding.Q, decodeQ(encodedText))
        } else if (encoding.equals("B", ignoreCase = true)) {
            EncodedWord(charset, Encoding.B, decodeB(encodedText))
        } else {
            Timber.w("Warning: Unknown encoding in encoded word '%s'", body.substring(begin, end))
            null
        }
    }

    private fun decodeQ(encodedWord: String): ByteString {
        // Replace _ with =20
        val bytes = buildString {
            for (character in encodedWord) {
                if (character == '_') {
                    append("=20")
                } else {
                    append(character)
                }
            }
        }.toByteArray(Charsets.US_ASCII)

        return QuotedPrintableInputStream(ByteArrayInputStream(bytes)).use { inputStream ->
            try {
                inputStream.source().buffer().readByteString()
            } catch (e: IOException) {
                ByteString.EMPTY
            }
        }
    }

    private fun decodeB(encodedText: String): ByteString {
        return encodedText.decodeBase64() ?: ByteString.EMPTY
    }

    private operator fun ByteString.plus(second: ByteString): ByteString {
        return Buffer().write(this).write(second).readByteString()
    }

    private val ASCII_ESCAPE_SEQUENCE = byteArrayOf(0x1B, 0x28, 0x42)

    private class EncodedWord(
        val charset: String,
        val encoding: Encoding,
        var data: ByteString
    ) {
        fun canBeCombinedWith(other: EncodedWord): Boolean {
            return encoding == other.encoding && charset == other.charset && !isAsciiEscapeSequence()
        }

        private fun isAsciiEscapeSequence(): Boolean {
            return charset.startsWith("ISO-2022-JP", ignoreCase = true) && data.endsWith(ASCII_ESCAPE_SEQUENCE)
        }
    }

    private enum class Encoding {
        Q, B
    }
}
