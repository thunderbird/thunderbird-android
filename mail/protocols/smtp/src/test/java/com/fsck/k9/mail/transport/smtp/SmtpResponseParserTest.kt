package com.fsck.k9.mail.transport.smtp

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasMessage
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.key
import assertk.assertions.prop
import com.fsck.k9.mail.crlf
import com.fsck.k9.mail.filter.PeekableInputStream
import org.junit.Test

class SmtpResponseParserTest {
    private val logger = TestSmtpLogger()

    @Test
    fun `read greeting`() {
        val input = "220 smtp.domain.example ESMTP ready".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readGreeting()

        assertThat(response.replyCode).isEqualTo(220)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).containsExactly("smtp.domain.example ESMTP ready")
        assertInputExhausted(input)
    }

    @Test
    fun `read multi-line greeting`() {
        val input = """
            220-Greetings, stranger
            220 smtp.domain.example ESMTP ready
        """.toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readGreeting()

        assertThat(response.replyCode).isEqualTo(220)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).containsExactly("Greetings, stranger", "smtp.domain.example ESMTP ready")
        assertInputExhausted(input)
    }

    @Test
    fun `read EHLO response`() {
        val input = """
            250-smtp.domain.example greets 127.0.0.1
            250-PIPELINING
            250-ENHANCEDSTATUSCODES
            250-8BITMIME
            250-SIZE 104857600
            250-DELIVERBY
            250-AUTH PLAIN LOGIN CRAM-MD5 DIGEST-MD5
            250 help
        """.trimIndent()
        val inputStream = input.toPeekableInputStream()
        val parser = SmtpResponseParser(logger, inputStream)

        val response = parser.readHelloResponse()

        assertThat(response).isInstanceOf<SmtpHelloResponse.Hello>().all {
            prop(SmtpHelloResponse.Hello::response)
                .transform { it.toLogString(false, "") }.isEqualTo(input)
            prop(SmtpHelloResponse.Hello::keywords).all {
                transform { it.keys }.containsExactlyInAnyOrder(
                    "PIPELINING",
                    "ENHANCEDSTATUSCODES",
                    "8BITMIME",
                    "SIZE",
                    "DELIVERBY",
                    "AUTH",
                    "HELP",
                )
                key("PIPELINING").isNotNull().isEmpty()
                key("SIZE").isNotNull().containsExactly("104857600")
                key("AUTH").isNotNull().containsExactly("PLAIN", "LOGIN", "CRAM-MD5", "DIGEST-MD5")
            }
        }

        assertInputExhausted(inputStream)
    }

    @Test
    fun `read EHLO response with only one line`() {
        val input = "250 smtp.domain.example".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readHelloResponse()

        assertThat(response).isInstanceOf<SmtpHelloResponse.Hello>().all {
            prop(SmtpHelloResponse.Hello::response).all {
                prop(SmtpResponse::replyCode).isEqualTo(250)
                prop(SmtpResponse::texts).containsExactly("smtp.domain.example")
            }
            prop(SmtpHelloResponse.Hello::keywords).isEmpty()
        }
    }

    @Test
    fun `read EHLO error response`() {
        val input = "421 Service not available".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readHelloResponse()

        assertThat(response).isInstanceOf<SmtpHelloResponse.Error>()
            .prop(SmtpHelloResponse.Error::response).all {
                prop(SmtpResponse::replyCode).isEqualTo(421)
                prop(SmtpResponse::texts).containsExactly("Service not available")
            }
    }

    @Test
    fun `read EHLO response with only reply code`() {
        val input = "250".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        assertFailure {
            parser.readHelloResponse()
        }.isInstanceOf<SmtpResponseParserException>()
            .hasMessage("Unexpected character: (13)")

        assertThat(logger.logEntries).containsExactly(
            LogEntry(
                throwable = null,
                message = """
                    SMTP response data on parser error:
                    250
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `read EHLO response with reply code not matching`() {
        val input = """
            250-smtp.domain.example
            220
        """.toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        assertFailure {
            parser.readHelloResponse()
        }.isInstanceOf<SmtpResponseParserException>()
            .hasMessage("Multi-line response with reply codes not matching: 250 != 220")

        assertThat(logger.logEntries).containsExactly(
            LogEntry(
                throwable = null,
                message = """
                    SMTP response data on parser error:
                    250-smtp.domain.example
                    220
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `read EHLO response with invalid keywords`() {
        val input = """
            250-smtp.domain.example
            250-SIZE 52428800
            250-8BITMIME
            250-PIPELINING
            250-PIPE_CONNECT
            250-AUTH=PLAIN
            250-%1 crash when included in format string
            250 HELP
        """.toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readHelloResponse()

        assertThat(response).isInstanceOf<SmtpHelloResponse.Hello>()
            .prop(SmtpHelloResponse.Hello::keywords).transform { it.keys }.containsExactlyInAnyOrder(
                "SIZE",
                "8BITMIME",
                "PIPELINING",
                "HELP",
            )

        assertThat(logger.logEntries.map { it.message }).containsExactly(
            "Ignoring EHLO keyword line: PIPE_CONNECT",
            "Ignoring EHLO keyword line: AUTH=PLAIN",
            "Ignoring EHLO keyword line: %1 crash when included in format string",
        )
        assertThat(logger.logEntries.map { it.throwable?.message }).containsExactly(
            "EHLO keyword contains invalid character",
            "EHLO keyword contains invalid character",
            "EHLO keyword contains invalid character",
        )
    }

    @Test
    fun `read EHLO response with empty parameter`() {
        val input = """
            250-smtp.domain.example
            250 KEYWORD${" "}
        """.toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readHelloResponse()

        assertThat(response).isInstanceOf<SmtpHelloResponse.Hello>()
            .transform { it.keywords.keys }.isEmpty()

        assertThat(logger.logEntries).isNotNull().hasSize(1)
        assertThat(logger.logEntries.first().throwable).isNotNull().hasMessage("EHLO parameter must not be empty")
        assertThat(logger.logEntries.first().message).isEqualTo("Ignoring EHLO keyword line: KEYWORD ")
    }

    @Test
    fun `read EHLO response with invalid parameter`() {
        val input = """
            250-smtp.domain.example
            250-8BITMIME
            250 KEYWORD para${"\t"}meter
        """.toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readHelloResponse()

        assertThat(response).isInstanceOf<SmtpHelloResponse.Hello>()
            .transform { it.keywords.keys }.containsExactlyInAnyOrder("8BITMIME")

        assertThat(logger.logEntries).hasSize(1)
        assertThat(logger.logEntries.first().throwable).isNotNull()
            .hasMessage("EHLO parameter contains invalid character")
        assertThat(logger.logEntries.first().message)
            .isEqualTo("Ignoring EHLO keyword line: KEYWORD para${"\t"}meter")
    }

    @Test
    fun `error in EHLO response after successfully reading greeting`() {
        val input = """
            220 Greeting
            INVALID
        """.toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)
        parser.readGreeting()

        assertFailure {
            parser.readHelloResponse()
        }.isInstanceOf<SmtpResponseParserException>()
            .hasMessage("Unexpected character: I (73)")

        assertThat(logger.logEntries).containsExactly(
            LogEntry(
                throwable = null,
                message = """
                    SMTP response data on parser error:
                    I
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `positive response`() {
        val input = "200 OK".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = false)

        assertThat(response.isNegativeResponse).isFalse()
    }

    @Test
    fun `negative response`() {
        val input = "500 Oops".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = false)

        assertThat(response.isNegativeResponse).isTrue()
    }

    @Test
    fun `reply code only`() {
        val input = "502".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = false)

        assertThat(response.replyCode).isEqualTo(502)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).isEmpty()
        assertInputExhausted(input)
    }

    @Test
    fun `reply code and text`() {
        val input = "250 OK".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = false)

        assertThat(response.replyCode).isEqualTo(250)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).containsExactly("OK")
        assertInputExhausted(input)
    }

    @Test
    fun `reply code and text with enhanced status code`() {
        val input = "250 2.1.0 Originator <sender@domain.example> ok".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = true)

        assertThat(response.replyCode).isEqualTo(250)
        assertThat(response.enhancedStatusCode).isEqualTo(
            EnhancedStatusCode(statusClass = StatusCodeClass.SUCCESS, subject = 1, detail = 0),
        )
        assertThat(response.texts).containsExactly("Originator <sender@domain.example> ok")
        assertInputExhausted(input)
    }

    @Test
    fun `enhancedStatusCodes enabled and 3xx reply code`() {
        val input = "354 Ok Send data ending with <CRLF>.<CRLF>".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = true)

        assertThat(response.replyCode).isEqualTo(354)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).containsExactly("Ok Send data ending with <CRLF>.<CRLF>")
        assertInputExhausted(input)
    }

    @Test
    fun `multi-line response with text`() {
        val input = """
            500-Line one
            500 Line two
        """.toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = false)

        assertThat(response.replyCode).isEqualTo(500)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).containsExactly("Line one", "Line two")
        assertInputExhausted(input)
    }

    @Test
    fun `multi-line response with empty textstring`() {
        val input = """
            500-
            500 Line two
        """.toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = false)

        assertThat(response.replyCode).isEqualTo(500)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).containsExactly("", "Line two")
        assertInputExhausted(input)
    }

    @Test
    fun `multi-line response without text on last line`() {
        val input = """
            500-Line one
            500-Line two
            500
        """.toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = false)

        assertThat(response.replyCode).isEqualTo(500)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).containsExactly("Line one", "Line two")
        assertInputExhausted(input)
    }

    @Test
    fun `multi-line response with enhanced status code`() {
        val input = """
            250-2.1.0 Sender <sender@domain.example>
            250 2.1.0 OK
        """.toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = true)

        assertThat(response.replyCode).isEqualTo(250)
        assertThat(response.enhancedStatusCode).isEqualTo(
            EnhancedStatusCode(statusClass = StatusCodeClass.SUCCESS, subject = 1, detail = 0),
        )
        assertThat(response.texts).containsExactly("Sender <sender@domain.example>", "OK")
        assertInputExhausted(input)
    }

    @Test
    fun `read multiple responses`() {
        val input = """
            250 Sender <sender@domain.example> OK
            250 Recipient <recipient@domain.example> OK
        """.toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val responseOne = parser.readResponse(enhancedStatusCodes = false)

        assertThat(responseOne.replyCode).isEqualTo(250)
        assertThat(responseOne.enhancedStatusCode).isNull()
        assertThat(responseOne.texts).containsExactly("Sender <sender@domain.example> OK")

        val responseTwo = parser.readResponse(enhancedStatusCodes = false)

        assertThat(responseTwo.replyCode).isEqualTo(250)
        assertThat(responseTwo.enhancedStatusCode).isNull()
        assertThat(responseTwo.texts).containsExactly("Recipient <recipient@domain.example> OK")
        assertInputExhausted(input)
    }

    @Test
    fun `multi-line response with reply codes not matching`() {
        val input = """
            200-Line one
            500 Line two
        """.toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        assertFailure {
            parser.readResponse(enhancedStatusCodes = false)
        }.isInstanceOf<SmtpResponseParserException>()
            .hasMessage("Multi-line response with reply codes not matching: 200 != 500")

        assertThat(logger.logEntries).containsExactly(
            LogEntry(
                throwable = null,
                message = """
                    SMTP response data on parser error:
                    200-Line one
                    500
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `multi-line response with reply codes not matching and raw protocol logging disabled`() {
        val input = """
            200-Line one
            500 Line two
        """.toPeekableInputStream()
        val logger = TestSmtpLogger(isRawProtocolLoggingEnabled = false)
        val parser = SmtpResponseParser(logger, input)

        assertFailure {
            parser.readResponse(enhancedStatusCodes = false)
        }.isInstanceOf<SmtpResponseParserException>()
            .hasMessage("Multi-line response with reply codes not matching: 200 != 500")
        assertThat(logger.logEntries).isEmpty()
    }

    @Test
    fun `invalid 1st reply code digit`() {
        val input = "611".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        assertFailure {
            parser.readResponse(enhancedStatusCodes = false)
        }.isInstanceOf<SmtpResponseParserException>()
            .hasMessage("Unsupported 1st reply code digit: 6")
    }

    @Test
    fun `invalid 2nd reply code digit should only produce a log entry`() {
        val input = "280 Something".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = false)

        assertThat(response.replyCode).isEqualTo(280)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).containsExactly("Something")
        assertThat(logger.logEntries).containsExactly(
            LogEntry(throwable = null, message = "2nd digit of reply code outside of specified range (0..5): 8"),
        )
    }

    @Test
    fun `invalid 3rd reply code digit`() {
        val input = "20x".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        assertFailure {
            parser.readResponse(enhancedStatusCodes = false)
        }.isInstanceOf<SmtpResponseParserException>()
            .hasMessage("Unexpected character: x (120)")

        assertThat(logger.logEntries).containsExactly(
            LogEntry(
                throwable = null,
                message = """
                    SMTP response data on parser error:
                    20x
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `end of stream after reply code`() {
        val input = PeekableInputStream("200".byteInputStream())
        val parser = SmtpResponseParser(logger, input)

        assertFailure {
            parser.readResponse(enhancedStatusCodes = false)
        }.isInstanceOf<SmtpResponseParserException>()
            .hasMessage("Unexpected end of stream")

        assertThat(logger.logEntries).containsExactly(
            LogEntry(
                throwable = null,
                message = """
                    SMTP response data on parser error:
                    200
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `response ending with CR only`() {
        val input = PeekableInputStream("200\r".byteInputStream())
        val parser = SmtpResponseParser(logger, input)

        assertFailure {
            parser.readResponse(enhancedStatusCodes = false)
        }.isInstanceOf<SmtpResponseParserException>()
            .hasMessage("Unexpected end of stream")
    }

    @Test
    fun `response ending with LF only`() {
        val input = PeekableInputStream("200\n".byteInputStream())
        val parser = SmtpResponseParser(logger, input)

        assertFailure {
            parser.readResponse(enhancedStatusCodes = false)
        }.isInstanceOf<SmtpResponseParserException>()
            .hasMessage("Unexpected character: (10)")

        assertThat(logger.logEntries).containsExactly(
            LogEntry(
                throwable = null,
                message = """
                    SMTP response data on parser error:
                    200
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `reply code with space but without text`() {
        val input = "200 ".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = false)

        assertThat(response.replyCode).isEqualTo(200)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).isEmpty()
        assertInputExhausted(input)
        assertThat(logger.logEntries).containsExactly(
            LogEntry(throwable = null, message = "'textstring' expected, but CR found instead"),
        )
    }

    @Test
    fun `text containing non-ASCII character`() {
        val input = "200 über".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = false)

        assertThat(response.replyCode).isEqualTo(200)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).containsExactly("über")
        assertInputExhausted(input)
        assertThat(logger.logEntries).containsExactly(
            LogEntry(throwable = null, message = "Text contains characters not allowed in 'textstring'"),
        )
    }

    @Test
    fun `enhanced status code class does not match reply code`() {
        val input = "250 5.0.0 text".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = true)

        assertThat(response.replyCode).isEqualTo(250)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).containsExactly("5.0.0 text")
        assertInputExhausted(input)
        assertThat(logger.logEntries).hasSize(1)
        logger.logEntries.first().let { logEntry ->
            assertThat(logEntry.message).isEqualTo("Error parsing enhanced status code")
            assertThat(logEntry.throwable?.message).isEqualTo("Reply code doesn't match status code class: 2 != 5")
        }
    }

    @Test
    fun `response with invalid enhanced status code subject`() {
        val input = "250 2.1000.0 Text".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = true)

        assertThat(response.replyCode).isEqualTo(250)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).containsExactly("2.1000.0 Text")
        assertInputExhausted(input)
        assertThat(logger.logEntries).hasSize(1)
        logger.logEntries.first().let { logEntry ->
            assertThat(logEntry.message).isEqualTo("Error parsing enhanced status code")
            assertThat(logEntry.throwable?.message).isEqualTo("Unexpected character: 0 (48)")
        }
    }

    @Test
    fun `response with invalid enhanced status code detail`() {
        val input = "250 2.0.1000 Text".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = true)

        assertThat(response.replyCode).isEqualTo(250)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).containsExactly("2.0.1000 Text")
        assertInputExhausted(input)
        assertThat(logger.logEntries).hasSize(1)
        logger.logEntries.first().let { logEntry ->
            assertThat(logEntry.message).isEqualTo("Error parsing enhanced status code")
            assertThat(logEntry.throwable?.message).isEqualTo("Unexpected character: 0 (48)")
        }
    }

    @Test
    fun `response with missing enhanced status code`() {
        // Yahoo has been observed to send replies without enhanced status code even though the EHLO keyword is present
        val input = "550 Request failed; Mailbox unavailable".toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = true)

        assertThat(response.replyCode).isEqualTo(550)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).containsExactly("Request failed; Mailbox unavailable")
        assertInputExhausted(input)
        assertThat(logger.logEntries).hasSize(1)
        logger.logEntries.first().let { logEntry ->
            assertThat(logEntry.message).isEqualTo("Error parsing enhanced status code")
            assertThat(logEntry.throwable?.message).isEqualTo("Unexpected character: R (82)")
        }
    }

    @Test
    fun `multi-line response with enhanced status code missing in last line`() {
        val input = """
            550-5.2.1 Request failed
            550 Mailbox unavailable
        """.toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        assertFailure {
            parser.readResponse(enhancedStatusCodes = true)
        }.isInstanceOf<SmtpResponseParserException>()
            .hasMessage(
                "Multi-line response with enhanced status codes not matching: " +
                    "EnhancedStatusCode(statusClass=PERMANENT_FAILURE, subject=2, detail=1) != null",
            )
    }

    @Test
    fun `multi-line response with missing enhanced status code`() {
        val input = """
            550-Request failed
            550 Mailbox unavailable
        """.toPeekableInputStream()
        val parser = SmtpResponseParser(logger, input)

        val response = parser.readResponse(enhancedStatusCodes = true)

        assertThat(response.replyCode).isEqualTo(550)
        assertThat(response.enhancedStatusCode).isNull()
        assertThat(response.texts).containsExactly("Request failed", "Mailbox unavailable")
        assertInputExhausted(input)
    }

    private fun assertInputExhausted(input: PeekableInputStream) {
        assertThat(input.read()).isEqualTo(-1)
    }

    private fun String.toPeekableInputStream(): PeekableInputStream {
        return PeekableInputStream((this.trimIndent().crlf() + "\r\n").byteInputStream())
    }
}
