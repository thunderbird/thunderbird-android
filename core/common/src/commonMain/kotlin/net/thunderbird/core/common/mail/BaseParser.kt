package net.thunderbird.core.common.mail

import net.thunderbird.core.common.mail.EmailAddressParserError.UnexpectedCharacter
import net.thunderbird.core.common.mail.EmailAddressParserError.UnexpectedEndOfInput

@Suppress("UnnecessaryAbstractClass")
/**
 * Base class for string parsers.
 *
 * Provides basic navigation and reading operations on an input string.
 *
 * @property input The string to be parsed.
 * @property startIndex The index to start parsing from (inclusive).
 * @property endIndex The index to stop parsing at (exclusive).
 */
internal abstract class BaseParser(val input: String, startIndex: Int = 0, val endIndex: Int = input.length) {
    protected var currentIndex = startIndex

    val position: Int
        get() = currentIndex

    fun endReached() = currentIndex >= endIndex

    fun peek(): Char {
        if (currentIndex >= endIndex) {
            parserError(UnexpectedEndOfInput)
        }

        return input[currentIndex]
    }

    fun read(): Char {
        if (currentIndex >= endIndex) {
            parserError(UnexpectedEndOfInput)
        }

        return input[currentIndex].also { currentIndex++ }
    }

    fun expect(character: Char) {
        if (!endReached() && peek() == character) {
            currentIndex++
        } else {
            parserError(UnexpectedCharacter, message = "Expected '$character' (${character.code})")
        }
    }

    @Suppress("SameParameterValue")
    protected inline fun expect(displayInError: String, predicate: (Char) -> Boolean) {
        if (!endReached() && predicate(peek())) {
            skip()
        } else {
            parserError(UnexpectedCharacter, message = "Expected $displayInError")
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun skip() {
        currentIndex++
    }

    protected inline fun skipWhile(crossinline predicate: (Char) -> Boolean) {
        while (!endReached() && predicate(input[currentIndex])) {
            currentIndex++
        }
    }

    protected inline fun readString(block: () -> Unit): String {
        val startIndex = currentIndex
        block()
        return input.substring(startIndex, currentIndex)
    }

    protected inline fun <P : BaseParser, T> withParser(parser: P, block: P.() -> T): T {
        try {
            return block(parser)
        } finally {
            currentIndex = parser.position
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun parserError(
        error: EmailAddressParserError,
        position: Int = currentIndex,
        message: String = error.message,
    ): Nothing {
        throw EmailAddressParserException(message, error, input, position)
    }
}
