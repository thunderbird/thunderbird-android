package com.fsck.k9.mail.internet

/**
 * Read Message identifier(s).
 *
 * Used in the `Message-ID`, `In-Reply-To`, and `References` header fields.
 * This does not support the obsolete syntax.
 *
 * See RFC 5322
 * ```
 * msg-id          =   [CFWS] "<" id-left "@" id-right ">" [CFWS]
 * id-left         =   dot-atom-text / obs-id-left
 * id-right        =   dot-atom-text / no-fold-literal / obs-id-right
 *
 * dot-atom-text   =   1*atext *("." 1*atext)
 * no-fold-literal =   "[" *dtext "]"
 * CFWS            =   (1*([FWS] comment) [FWS]) / FWS
 * FWS             =   ([*WSP CRLF] 1*WSP) / obs-FWS ; Folding white space
 * comment         =   "(" *([FWS] ccontent) [FWS] ")"
 * ccontent        =   ctext / quoted-pair / comment
 * quoted-pair     =   ("\" (VCHAR / WSP)) / obs-qp
 * ```
 */
class MessageIdParser private constructor(private val input: String) {
    private val endIndex = input.length
    private var currentIndex = 0

    fun parse(): String {
        val messageId = readMessageId()

        if (!endReached()) {
            throw MimeHeaderParserException("Expected end of input", currentIndex)
        }

        return messageId
    }

    fun parseList(): List<String> {
        if (input.isEmpty()) {
            throw MimeHeaderParserException("Expected message identifier", errorIndex = 0)
        }

        val messageIds = mutableListOf<String>()
        while (!endReached()) {
            messageIds.add(readMessageId())
        }

        return messageIds
    }

    private fun readMessageId(): String {
        skipCfws()
        expect('<')
        val idLeft = readIdLeft()
        expect('@')
        val idRight = readIdRight()
        expect('>')
        skipCfws()

        return "<$idLeft@$idRight>"
    }

    private fun readIdLeft(): String {
        return readDotAtom()
    }

    private fun readIdRight(): String {
        return if (peek() == '[') {
            readDText()
        } else {
            readDotAtom()
        }
    }

    private fun readDotAtom(): String {
        val startIndex = currentIndex

        do {
            expect("atext") { it.isAText() }
            if (peek() == '.') {
                expect('.')
                expect("atext") { it.isAText() }
            }
        } while (peek().isAText())

        return input.substring(startIndex, currentIndex)
    }

    private fun readDText(): String {
        val startIndex = currentIndex

        expect('[')

        while (peek().isDText()) {
            skip()
        }

        expect(']')

        return input.substring(startIndex, currentIndex)
    }

    private fun skipCfws() {
        do {
            val lastIndex = currentIndex

            skipFws()

            if (!endReached() && peek() == '(') {
                expectComment()
            }
        } while (currentIndex != lastIndex && !endReached())
    }

    private fun skipFws() {
        skipWsp()
        if (!endReached() && peek() == CR) {
            expectCr()
            expectLf()
            expectWsp()
            skipWsp()
        }
    }

    private fun expectComment() {
        expect('(')
        var level = 1

        do {
            skipFws()

            val char = peek()
            when {
                char == '(' -> {
                    expect('(')
                    level++
                }
                char == '\\' -> {
                    expectQuotedPair()
                }
                char.isCText() -> {
                    skip()
                }
                else -> {
                    expect(')')
                    level--
                }
            }
        } while (level > 0)
    }

    private fun expectQuotedPair() {
        expect('\\')
        expect("VCHAR or WSP") { it.isVChar() || it.isWsp() }
    }

    private fun expectCr() = expect("CR", CR)

    private fun expectLf() = expect("LF", LF)

    private fun expectWsp() = expect("WSP") { it.isWsp() }

    private fun skipWsp() {
        while (!endReached() && peek().isWsp()) {
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
    }

    private fun expect(character: Char) {
        expect("'$character'") { it == character }
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

    companion object {
        fun parse(input: String): String = MessageIdParser(input).parse()

        @JvmStatic
        fun parseList(input: String): List<String> = MessageIdParser(input).parseList()
    }
}
