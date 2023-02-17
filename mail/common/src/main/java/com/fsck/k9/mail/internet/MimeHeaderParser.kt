package com.fsck.k9.mail.internet

import okio.Buffer

class MimeHeaderParser(private val input: String) {
    private val endIndex = input.length
    private var currentIndex = 0

    fun readHeaderValue(): String {
        return buildString {
            var whitespace = false
            loop@ while (!endReached()) {
                val character = peek()
                when {
                    character == ';' -> break@loop
                    character.isWsp() || character == CR || character == LF -> {
                        skipWhitespace()
                        whitespace = true
                    }
                    character == '(' -> skipComment()
                    else -> {
                        if (isNotEmpty() && whitespace) {
                            append(' ')
                        }
                        append(character)
                        currentIndex++
                        whitespace = false
                    }
                }
            }
        }
    }

    fun readUntil(character: Char) = readWhile { peek() != character }

    fun readExtendedParameterValueInto(output: Buffer) {
        while (!endReached() && peek() != ';') {
            val c = read()
            when {
                c == '%' -> output.writeByte(readPercentEncoded())
                c.isAttributeChar() -> output.writeByte(c.code)
                else -> return
            }
        }
    }

    private fun readPercentEncoded(): Int {
        val value1 = readHexDigit()
        val value2 = readHexDigit()

        return (value1 shl 4) + value2
    }

    private fun readHexDigit(): Int {
        return when (val character = read()) {
            in '0'..'9' -> character - '0'
            in 'a'..'f' -> character - 'a' + 10
            in 'A'..'F' -> character - 'A' + 10
            else -> throw MimeHeaderParserException("Expected hex character", currentIndex - 1)
        }
    }

    fun readQuotedString(): String {
        expect('"')

        val text = buildString {
            while (!endReached() && peek() != '\"') {
                val c = read()
                when (c) {
                    CR -> Unit
                    LF -> Unit
                    '\\' -> append(read())
                    else -> append(c)
                }
            }
        }

        expect('"')

        return text
    }

    fun readToken(): String {
        skipCFWS()
        val startIndex = currentIndex
        while (!endReached() && peek().isTokenChar()) {
            currentIndex++
        }

        if (startIndex == currentIndex) {
            throw MimeHeaderParserException("At least one character expected in token", currentIndex)
        }

        return input.substring(startIndex, currentIndex)
    }

    fun optional(character: Char): Boolean {
        if (peek() == character) {
            currentIndex++
            return true
        }

        return false
    }

    fun endReached() = currentIndex >= endIndex

    fun position() = currentIndex

    fun expect(character: Char) {
        if (!endReached() && peek() == character) {
            currentIndex++
        } else {
            throw MimeHeaderParserException("Expected '$character' (${character.code})", currentIndex)
        }
    }

    private fun skipWhitespace() {
        while (!endReached() && peek().let { it.isWsp() || it == CR || it == LF }) {
            currentIndex++
        }
    }

    fun skipCFWS() {
        while (!endReached()) {
            val character = peek()
            when {
                character.isWsp() || character == CR || character == LF -> currentIndex++
                character == '(' -> skipComment()
                else -> return
            }
        }
    }

    private fun skipComment() {
        expect('(')
        var depth = 1
        while (!endReached() && depth > 0) {
            val character = read()
            when {
                character == '(' -> depth++
                character == ')' -> depth--
                character == '\\' -> currentIndex++
                character == CR -> Unit
                character == LF -> Unit
                character.isWsp() -> Unit
                character.isVChar() -> Unit
                else -> {
                    currentIndex--
                    throw MimeHeaderParserException(
                        "Unexpected '$character' (${character.code}) in comment",
                        errorIndex = currentIndex,
                    )
                }
            }
        }
    }

    fun peek(): Char {
        if (currentIndex >= input.length) {
            throw MimeHeaderParserException("End of input reached unexpectedly", currentIndex)
        }

        return input[currentIndex]
    }

    fun read(): Char {
        if (currentIndex >= input.length) {
            throw MimeHeaderParserException("End of input reached unexpectedly", currentIndex)
        }

        val value = input[currentIndex]
        currentIndex++
        return value
    }

    private inline fun readWhile(crossinline predicate: () -> Boolean): String {
        val startIndex = currentIndex
        while (!endReached() && predicate()) {
            currentIndex++
        }

        return input.substring(startIndex, currentIndex)
    }
}

class MimeHeaderParserException(message: String, val errorIndex: Int = -1) : RuntimeException(message)
