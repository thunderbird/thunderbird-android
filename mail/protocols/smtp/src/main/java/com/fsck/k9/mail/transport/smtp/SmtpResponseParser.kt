package com.fsck.k9.mail.transport.smtp

import com.fsck.k9.mail.filter.PeekableInputStream
import okio.Buffer
import okio.BufferedSource

private const val CR = '\r'
private const val LF = '\n'
private const val SPACE = ' '
private const val DASH = '-'
private const val HTAB = '\t'
private const val DOT = '.'
private const val END_OF_STREAM = -1

/**
 * Parser for SMTP response lines.
 *
 * Supports enhanced status codes as defined in RFC 2034.
 *
 * Unfortunately at least one popular implementation doesn't always send an enhanced status code even though its EHLO
 * response contains the ENHANCEDSTATUSCODES keyword. Begrudgingly, we allow this and other minor standard violations.
 * However, we output a log message when such a case is encountered.
 */
internal class SmtpResponseParser(
    private val logger: SmtpLogger,
    private val input: PeekableInputStream,
) {
    private val logBuffer = Buffer()

    fun readGreeting(): SmtpResponse {
        // We're not interested in the domain or address literal in the greeting. So we use the standard parser.
        return readResponse(enhancedStatusCodes = false)
    }

    fun readHelloResponse(): SmtpHelloResponse {
        logBuffer.clear()

        val replyCode = readReplyCode()

        if (replyCode != 250) {
            val response = readResponseAfterReplyCode(replyCode, enhancedStatusCodes = false)
            return SmtpHelloResponse.Error(response)
        }

        val texts = mutableListOf<String>()

        // Read first line containing 'domain' and maybe 'ehlo-greet' (we don't check the syntax and allow any text)
        when (val char = peekChar()) {
            SPACE -> {
                expect(SPACE)

                val text = readUntilEndOfLine().readUtf8()

                expect(CR)
                expect(LF)

                return SmtpHelloResponse.Hello(
                    response = SmtpResponse(replyCode, enhancedStatusCode = null, texts = listOf(text)),
                    keywords = emptyMap(),
                )
            }
            DASH -> {
                expect(DASH)

                val text = readUntilEndOfLine().readUtf8()
                texts.add(text)

                expect(CR)
                expect(LF)
            }
            else -> unexpectedCharacterError(char)
        }

        val keywords = mutableMapOf<String, List<String>>()

        // Read EHLO keywords and parameters
        while (true) {
            val currentReplyCode = readReplyCode()
            if (currentReplyCode != replyCode) {
                parserError("Multi-line response with reply codes not matching: $replyCode != $currentReplyCode")
            }

            when (val char = peekChar()) {
                SPACE -> {
                    expect(SPACE)

                    val bufferedSource = readUntilEndOfLine()
                    val ehloLine = bufferedSource.readEhloLine()
                    texts.add(ehloLine)

                    parseEhloLine(ehloLine, keywords)

                    expect(CR)
                    expect(LF)

                    return SmtpHelloResponse.Hello(
                        response = SmtpResponse(replyCode, enhancedStatusCode = null, texts),
                        keywords = keywords,
                    )
                }
                DASH -> {
                    expect(DASH)

                    val bufferedSource = readUntilEndOfLine()
                    val ehloLine = bufferedSource.readEhloLine()
                    texts.add(ehloLine)

                    parseEhloLine(ehloLine, keywords)

                    expect(CR)
                    expect(LF)
                }
                else -> unexpectedCharacterError(char)
            }
        }
    }

    private fun parseEhloLine(ehloLine: String, keywords: MutableMap<String, List<String>>) {
        val parts = ehloLine.split(" ")

        try {
            val keyword = checkAndNormalizeEhloKeyword(parts[0])
            val parameters = checkEhloParameters(parts)

            if (keywords.containsKey(keyword)) {
                parserError("Same EHLO keyword present in more than one response line", logging = false)
            }

            keywords[keyword] = parameters
        } catch (e: SmtpResponseParserException) {
            logger.log(e, "Ignoring EHLO keyword line: %s", ehloLine)
        }
    }

    private fun checkAndNormalizeEhloKeyword(text: String): String {
        val keyword = text.uppercase()
        if (!keyword[0].isCapitalAlphaDigit() || keyword.any { !it.isCapitalAlphaDigit() && it != DASH }) {
            parserError("EHLO keyword contains invalid character", logging = false)
        }

        return keyword
    }

    private fun checkEhloParameters(parts: List<String>): List<String> {
        for (i in 1..parts.lastIndex) {
            val parameter = parts[i]
            if (parameter.isEmpty()) {
                parserError("EHLO parameter must not be empty", logging = false)
            } else if (parameter.any { it.code !in 33..126 }) {
                parserError("EHLO parameter contains invalid character", logging = false)
            }
        }

        return parts.drop(1)
    }

    fun readResponse(enhancedStatusCodes: Boolean): SmtpResponse {
        logBuffer.clear()

        val replyCode = readReplyCode()
        return readResponseAfterReplyCode(replyCode, enhancedStatusCodes)
    }

    private fun readResponseAfterReplyCode(replyCode: Int, enhancedStatusCodes: Boolean): SmtpResponse {
        val texts = mutableListOf<String>()
        var enhancedStatusCode: EnhancedStatusCode? = null
        var isFirstLine = true

        fun BufferedSource.maybeReadAndCompareEnhancedStatusCode(replyCode: Int): EnhancedStatusCode? {
            val currentStatusCode = maybeReadEnhancedStatusCode(replyCode)
            if (!isFirstLine && enhancedStatusCode != currentStatusCode) {
                parserError(
                    "Multi-line response with enhanced status codes not matching: " +
                        "$enhancedStatusCode != $currentStatusCode",
                )
            }
            isFirstLine = false

            return currentStatusCode
        }

        while (true) {
            when (val char = peekChar()) {
                CR -> {
                    expect(CR)
                    expect(LF)

                    return SmtpResponse(replyCode, enhancedStatusCode, texts)
                }
                SPACE -> {
                    expect(SPACE)

                    val bufferedSource = readUntilEndOfLine()

                    if (enhancedStatusCodes) {
                        enhancedStatusCode = bufferedSource.maybeReadAndCompareEnhancedStatusCode(replyCode)
                    }

                    val textString = bufferedSource.readTextString()
                    if (textString.isNotEmpty()) {
                        texts.add(textString)
                    }

                    expect(CR)
                    expect(LF)

                    return SmtpResponse(replyCode, enhancedStatusCode, texts)
                }
                DASH -> {
                    expect(DASH)

                    val bufferedSource = readUntilEndOfLine()

                    if (enhancedStatusCodes) {
                        enhancedStatusCode = bufferedSource.maybeReadAndCompareEnhancedStatusCode(replyCode)
                    }

                    val textString = bufferedSource.readTextString()
                    texts.add(textString)

                    expect(CR)
                    expect(LF)

                    val currentReplyCode = readReplyCode()
                    if (currentReplyCode != replyCode) {
                        parserError(
                            "Multi-line response with reply codes not matching: $replyCode != $currentReplyCode",
                        )
                    }
                }
                else -> unexpectedCharacterError(char)
            }
        }
    }

    private fun readReplyCode(): Int {
        return readReplyCode1() * 100 + readReplyCode2() * 10 + readReplyCode3()
    }

    private fun readReplyCode1(): Int {
        val replyCode1 = readDigit()
        if (replyCode1 !in 2..5) parserError("Unsupported 1st reply code digit: $replyCode1")

        return replyCode1
    }

    private fun readReplyCode2(): Int {
        val replyCode2 = readDigit()
        if (replyCode2 !in 0..5) {
            logger.log("2nd digit of reply code outside of specified range (0..5): %d", replyCode2)
        }

        return replyCode2
    }

    private fun readReplyCode3(): Int {
        return readDigit()
    }

    private fun readDigit(): Int {
        val char = readChar()
        if (char !in '0'..'9') unexpectedCharacterError(char)

        return char - '0'
    }

    private fun expect(expectedChar: Char) {
        val char = readChar()
        if (char != expectedChar) unexpectedCharacterError(char)
    }

    private fun readByte(): Int {
        return input.read()
            .also {
                throwIfEndOfStreamReached(it)
                logBuffer.writeByte(it)
            }
    }

    private fun readChar(): Char {
        return readByte().toChar()
    }

    private fun peekChar(): Char {
        return input.peek()
            .also { throwIfEndOfStreamReached(it) }
            .toChar()
    }

    private fun throwIfEndOfStreamReached(data: Int) {
        if (data == END_OF_STREAM) parserError("Unexpected end of stream")
    }

    private fun readUntilEndOfLine(): BufferedSource {
        val buffer = Buffer()

        while (peekChar() != CR) {
            val byte = readByte()
            buffer.writeByte(byte)
        }

        return buffer
    }

    private fun BufferedSource.readEhloLine(): String {
        val text = readUtf8()
        if (text.isEmpty()) {
            parserError("EHLO line must not be empty")
        }

        return text
    }

    private fun BufferedSource.readTextString(): String {
        val text = readUtf8()
        if (text.isEmpty()) {
            logger.log("'textstring' expected, but CR found instead")
        } else if (text.any { it != HTAB && it.code !in 32..126 }) {
            logger.log("Text contains characters not allowed in 'textstring'")
        }

        return text
    }

    private fun BufferedSource.maybeReadEnhancedStatusCode(replyCode: Int): EnhancedStatusCode? {
        val replyCode1 = replyCode / 100
        if (replyCode1 != 2 && replyCode1 != 4 && replyCode1 != 5) return null

        return try {
            val peekBufferedSource = peek()
            val statusCode = peekBufferedSource.readEnhancedStatusCode(replyCode1)

            val statusCodeLength = buffer.size - peekBufferedSource.buffer.size
            skip(statusCodeLength)

            statusCode
        } catch (e: SmtpResponseParserException) {
            logger.log(e, "Error parsing enhanced status code")
            null
        }
    }

    private fun BufferedSource.readEnhancedStatusCode(replyCode1: Int): EnhancedStatusCode {
        val statusClass = readStatusCodeClass(replyCode1)
        expect(DOT)
        val subject = readOneToThreeDigitNumber()
        expect(DOT)
        val detail = readOneToThreeDigitNumber()

        expect(SPACE)

        return EnhancedStatusCode(statusClass, subject, detail)
    }

    private fun BufferedSource.readStatusCodeClass(replyCode1: Int): StatusCodeClass {
        val char = readChar()
        val statusClass = when (char) {
            '2' -> StatusCodeClass.SUCCESS
            '4' -> StatusCodeClass.PERSISTENT_TRANSIENT_FAILURE
            '5' -> StatusCodeClass.PERMANENT_FAILURE
            else -> unexpectedCharacterError(char, logging = false)
        }

        if (char != replyCode1.digitToChar()) {
            parserError("Reply code doesn't match status code class: $replyCode1 != $char", logging = false)
        }

        return statusClass
    }

    private fun BufferedSource.readOneToThreeDigitNumber(): Int {
        var number = readDigit()
        repeat(2) {
            if (peek().readChar() in '0'..'9') {
                number *= 10
                number += readDigit()
            }
        }

        return number
    }

    private fun BufferedSource.readDigit(): Int {
        val char = readChar()
        if (char !in '0'..'9') unexpectedCharacterError(char, logging = false)

        return char - '0'
    }

    private fun BufferedSource.readChar(): Char {
        if (exhausted()) parserError("Unexpected end of stream", logging = false)

        return readByte().toInt().toChar()
    }

    private fun BufferedSource.expect(expectedChar: Char) {
        val char = readChar()
        if (char != expectedChar) unexpectedCharacterError(char, logging = false)
    }

    private fun unexpectedCharacterError(char: Char, logging: Boolean = true): Nothing {
        if (char.code in 33..126) {
            parserError("Unexpected character: $char (${char.code})", logging)
        } else {
            parserError("Unexpected character: (${char.code})", logging)
        }
    }

    private fun parserError(message: String, logging: Boolean = true): Nothing {
        if (logging && logger.isRawProtocolLoggingEnabled) {
            logger.log("SMTP response data on parser error:\n%s", logBuffer.readUtf8().replace("\r\n", "\n"))
        }

        throw SmtpResponseParserException(message)
    }

    private fun Char.isCapitalAlphaDigit(): Boolean = this in '0'..'9' || this in 'A'..'Z'
}
