package com.fsck.k9.mail.internet

import com.fsck.k9.mail.filter.Hex.appendHex
import com.fsck.k9.mail.helper.encodeUtf8
import com.fsck.k9.mail.helper.utf8Size

/**
 * Encode MIME parameter values as specified in RFC 2045 and RFC 2231.
 */
object MimeParameterEncoder {
    // RFC 5322, section 2.1.1
    private const val MAX_LINE_LENGTH = 78

    private const val ENCODED_VALUE_PREFIX = "UTF-8''"

    private const val FOLDING_SPACE_LENGTH = 1
    private const val EQUAL_SIGN_LENGTH = 1
    private const val SEMICOLON_LENGTH = 1
    private const val QUOTES_LENGTH = 2
    private const val ASTERISK_LENGTH = 1

    /**
     * Create header field value with parameters encoded if necessary.
     */
    @JvmStatic
    fun encode(value: String, parameters: Map<String, String>): String {
        return if (parameters.isEmpty()) {
            value
        } else {
            buildString {
                append(value)
                encodeAndAppendParameters(parameters)
            }
        }
    }

    private fun StringBuilder.encodeAndAppendParameters(parameters: Map<String, String>) {
        for ((name, value) in parameters) {
            encodeAndAppendParameter(name, value)
        }
    }

    private fun StringBuilder.encodeAndAppendParameter(name: String, value: String) {
        val fixedCostLength = FOLDING_SPACE_LENGTH + name.length + EQUAL_SIGN_LENGTH + SEMICOLON_LENGTH
        val unencodedValueFitsOnSingleLine = fixedCostLength + value.length <= MAX_LINE_LENGTH
        val quotedValueMightFitOnSingleLine = fixedCostLength + value.length + QUOTES_LENGTH <= MAX_LINE_LENGTH

        if (unencodedValueFitsOnSingleLine && value.isToken()) {
            appendParameter(name, value)
        } else if (quotedValueMightFitOnSingleLine && value.isQuotable() &&
            fixedCostLength + value.quotedLength() <= MAX_LINE_LENGTH
        ) {
            appendParameter(name, value.quoted())
        } else {
            rfc2231EncodeAndAppendParameter(name, value)
        }
    }

    private fun StringBuilder.appendParameter(name: String, value: String) {
        append(";$CRLF ")
        append(name).append('=').append(value)
    }

    private fun StringBuilder.rfc2231EncodeAndAppendParameter(name: String, value: String) {
        val encodedValueLength = FOLDING_SPACE_LENGTH + name.length + ASTERISK_LENGTH + EQUAL_SIGN_LENGTH +
            ENCODED_VALUE_PREFIX.length + value.rfc2231EncodedLength() + SEMICOLON_LENGTH

        if (encodedValueLength <= MAX_LINE_LENGTH) {
            appendRfc2231SingleLineParameter(name, value.rfc2231Encoded())
        } else {
            encodeAndAppendRfc2231MultiLineParameter(name, value)
        }
    }

    private fun StringBuilder.appendRfc2231SingleLineParameter(name: String, encodedValue: String) {
        append(";$CRLF ")
        append(name)
        append("*=$ENCODED_VALUE_PREFIX")
        append(encodedValue)
    }

    private fun StringBuilder.encodeAndAppendRfc2231MultiLineParameter(name: String, value: String) {
        var index = 0
        var line = 0
        var startOfLine = true
        var remainingSpaceInLine = 0
        val endIndex = value.length
        while (index < endIndex) {
            if (startOfLine) {
                append(";$CRLF ")
                val lineStartIndex = length - 1
                append(name).append('*').append(line).append("*=")
                if (line == 0) {
                    append(ENCODED_VALUE_PREFIX)
                }

                remainingSpaceInLine = MAX_LINE_LENGTH - (length - lineStartIndex) - SEMICOLON_LENGTH
                if (remainingSpaceInLine < 3) {
                    throw UnsupportedOperationException("Parameter name too long")
                }

                startOfLine = false
                line++
            }

            val codePoint = value.codePointAt(index)

            // Keep all characters encoding a single code point on the same line
            val utf8Size = codePoint.utf8Size()
            if (utf8Size == 1 && codePoint.toChar().isAttributeChar() && remainingSpaceInLine >= 1) {
                append(codePoint.toChar())
                index++
                remainingSpaceInLine--
            } else if (remainingSpaceInLine >= utf8Size * 3) {
                codePoint.encodeUtf8 {
                    append('%')
                    appendHex(it, lowerCase = false)
                    remainingSpaceInLine -= 3
                }
                index += Character.charCount(codePoint)
            } else {
                startOfLine = true
            }
        }
    }

    private fun String.rfc2231Encoded() = buildString {
        this@rfc2231Encoded.encodeUtf8 { byte ->
            val c = byte.toInt().toChar()
            if (c.isAttributeChar()) {
                append(c)
            } else {
                append('%')
                appendHex(byte, lowerCase = false)
            }
        }
    }

    private fun String.rfc2231EncodedLength(): Int {
        var length = 0
        encodeUtf8 { byte ->
            length += if (byte.toInt().toChar().isAttributeChar()) 1 else 3
        }
        return length
    }

    fun String.isToken() = when {
        isEmpty() -> false
        else -> all { it.isTokenChar() }
    }

    private fun String.isQuotable() = all { it.isQuotable() }

    private fun String.quoted(): String {
        // quoted-string = [CFWS] DQUOTE *([FWS] qcontent) [FWS] DQUOTE [CFWS]
        // qcontent      = qtext / quoted-pair
        // quoted-pair   = ("\" (VCHAR / WSP))

        return buildString(capacity = length + 16) {
            append(DQUOTE)
            for (c in this@quoted) {
                if (c.isQText() || c.isWsp()) {
                    append(c)
                } else if (c.isVChar()) {
                    append('\\').append(c)
                } else {
                    throw IllegalArgumentException("Unsupported character: $c")
                }
            }
            append(DQUOTE)
        }
    }

    // RFC 6532-style header values
    // Right now we only create such values for internal use (see IMAP BODYSTRUCTURE response parsing code)
    fun String.quotedUtf8(): String {
        return buildString(capacity = length + 16) {
            append(DQUOTE)
            for (c in this@quotedUtf8) {
                if (c == DQUOTE || c == BACKSLASH) {
                    append('\\').append(c)
                } else {
                    append(c)
                }
            }
            append(DQUOTE)
        }
    }

    private fun String.quotedLength(): Int {
        var length = QUOTES_LENGTH
        for (c in this) {
            if (c.isQText() || c.isWsp()) {
                length++
            } else if (c.isVChar()) {
                length += 2
            } else {
                throw IllegalArgumentException("Unsupported character: $c")
            }
        }
        return length
    }

    private fun Char.isQuotable() = when {
        isWsp() -> true
        isVChar() -> true
        else -> false
    }

    // RFC 5322: qtext = %d33 / %d35-91 / %d93-126 / obs-qtext
    private fun Char.isQText() = when (code) {
        33 -> true
        in 35..91 -> true
        in 93..126 -> true
        else -> false
    }
}
