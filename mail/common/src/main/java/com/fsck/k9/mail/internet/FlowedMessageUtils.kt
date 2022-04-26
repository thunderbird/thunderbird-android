package com.fsck.k9.mail.internet

/**
 * Decodes text encoded as `text/plain; format=flowed` (RFC 3676).
 */
object FlowedMessageUtils {
    private const val QUOTE = '>'
    private const val SPACE = ' '
    private const val CR = '\r'
    private const val LF = '\n'
    private const val SIGNATURE = "-- "
    private const val CRLF = "\r\n"

    @JvmStatic
    fun deflow(text: String, delSp: Boolean): String {
        var lineStartIndex = 0
        var lastLineQuoteDepth = 0
        var lastLineFlowed = false

        return buildString {
            while (lineStartIndex <= text.lastIndex) {
                var quoteDepth = 0
                while (lineStartIndex <= text.lastIndex && text[lineStartIndex] == QUOTE) {
                    quoteDepth++
                    lineStartIndex++
                }

                // Remove space stuffing
                if (lineStartIndex <= text.lastIndex && text[lineStartIndex] == SPACE) {
                    lineStartIndex++
                }

                // We support both LF and CRLF line endings. To cover both cases we search for LF.
                val lineFeedIndex = text.indexOf(LF, lineStartIndex)
                val lineBreakFound = lineFeedIndex != -1

                var lineEndIndex = if (lineBreakFound) lineFeedIndex else text.length
                if (lineEndIndex > 0 && text[lineEndIndex - 1] == CR) {
                    lineEndIndex--
                }

                if (lastLineFlowed && quoteDepth != lastLineQuoteDepth) {
                    append(CRLF)
                    lastLineFlowed = false
                }

                val lineIsSignatureMarker = lineEndIndex - lineStartIndex == SIGNATURE.length &&
                    text.regionMatches(lineStartIndex, SIGNATURE, 0, SIGNATURE.length)

                var lineFlowed = false
                if (lineIsSignatureMarker) {
                    if (lastLineFlowed) {
                        append(CRLF)
                        lastLineFlowed = false
                    }
                } else if (lineEndIndex > lineStartIndex && text[lineEndIndex - 1] == SPACE) {
                    lineFlowed = true
                    if (delSp) {
                        lineEndIndex--
                    }
                }

                if (!lastLineFlowed && quoteDepth > 0) {
                    // This is not a continuation line, so prefix the text with quote characters.
                    repeat(quoteDepth) {
                        append(QUOTE)
                    }
                    append(SPACE)
                }

                append(text, lineStartIndex, lineEndIndex)

                if (!lineFlowed && lineBreakFound) {
                    append(CRLF)
                }

                lineStartIndex = if (lineBreakFound) lineFeedIndex + 1 else text.length
                lastLineQuoteDepth = quoteDepth
                lastLineFlowed = lineFlowed
            }
        }
    }
}
