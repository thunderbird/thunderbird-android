package com.fsck.k9.mail.internet

/**
 * Check unstructured header field syntax.
 *
 * This does not allow the obsolete syntax. Only use this for messages constructed by K-9 Mail, not incoming messages.
 *
 * See RFC 5322
 * ```
 * optional-field =  field-name ":" unstructured CRLF
 * field-name     =  1*ftext
 * ftext          =  %d33-57 / %d59-126  ; Printable US-ASCII characters not including ":".
 *
 * unstructured   =  (*([FWS] VCHAR) *WSP) / obs-unstruct
 * FWS            =  ([*WSP CRLF] 1*WSP) / obs-FWS  ; Folding white space
 * ```
 */
object MimeHeaderChecker {
    fun checkHeader(name: String, value: String) {
        if (!name.isValidFieldName()) {
            throw MimeHeaderParserException("Header name contains characters not allowed: $name")
        }

        val initialLineLength = name.length + 2 // name + colon + space
        UnstructuredHeaderChecker(value, initialLineLength).checkHeaderValue()
    }

    private fun String.isValidFieldName() = all { it.isFieldText() }

    private fun Char.isFieldText() = isVChar() && this != ':'
}

private class UnstructuredHeaderChecker(val input: String, initialLineLength: Int) {
    private val endIndex = input.length
    private var currentIndex = 0
    private var lineLength = initialLineLength

    fun checkHeaderValue() {
        while (!endReached()) {
            val char = peek()
            when {
                char == CR -> {
                    expectCr()
                    expectLf()

                    if (lineLength > 1000) {
                        throw MimeHeaderParserException("Line exceeds 998 characters", currentIndex - 1)
                    }
                    lineLength = 0

                    expectWsp()
                    skipWsp()
                    expectVChar()
                }
                char.isVChar() || char.isWsp() -> {
                    skipVCharAndWsp()
                }
                else -> {
                    throw MimeHeaderParserException("Unexpected character (${char.code})", currentIndex)
                }
            }
        }

        if (lineLength > 998) {
            throw MimeHeaderParserException("Line exceeds 998 characters", currentIndex)
        }
    }

    private fun expectCr() = expect("CR", CR)

    private fun expectLf() = expect("LF", LF)

    private fun expectVChar() = expect("VCHAR") { it.isVChar() }

    private fun expectWsp() = expect("WSP") { it.isWsp() }

    private fun skipWsp() {
        while (!endReached() && peek().isWsp()) {
            skip()
        }
    }

    private fun skipVCharAndWsp() {
        while (!endReached() && peek().let { it.isVChar() || it.isWsp() }) {
            skip()
        }
    }

    private fun endReached() = currentIndex >= endIndex

    private fun peek(): Char {
        if (currentIndex >= input.length) {
            throw MimeHeaderParserException("End of input reached unexpectedly", currentIndex)
        }

        return input[currentIndex]
    }

    private fun skip() {
        currentIndex++
        lineLength++
    }

    private fun expect(displayInError: String, character: Char) {
        expect(displayInError) { it == character }
    }

    private inline fun expect(displayInError: String, predicate: (Char) -> Boolean) {
        if (!endReached() && predicate(peek())) {
            skip()
        } else {
            throw MimeHeaderParserException("Expected $displayInError", currentIndex)
        }
    }
}
