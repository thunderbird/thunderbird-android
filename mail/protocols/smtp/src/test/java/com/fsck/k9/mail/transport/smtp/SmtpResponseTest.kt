package com.fsck.k9.mail.transport.smtp

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class SmtpResponseTest {
    @Test
    fun `log reply code only`() {
        val response = SmtpResponse(
            replyCode = 200,
            enhancedStatusCode = null,
            texts = emptyList(),
        )

        val output = response.toLogString(omitText = false, linePrefix = "SMTP <<< ")

        assertThat(output).isEqualTo("SMTP <<< 200")
    }

    @Test
    fun `log reply code only with omitText = true`() {
        val response = SmtpResponse(
            replyCode = 200,
            enhancedStatusCode = null,
            texts = emptyList(),
        )

        val output = response.toLogString(omitText = true, linePrefix = "SMTP <<< ")

        assertThat(output).isEqualTo("SMTP <<< 200")
    }

    @Test
    fun `log reply code and text`() {
        val response = SmtpResponse(
            replyCode = 200,
            enhancedStatusCode = null,
            texts = listOf("OK"),
        )

        val output = response.toLogString(omitText = false, linePrefix = "SMTP <<< ")

        assertThat(output).isEqualTo("SMTP <<< 200 OK")
    }

    @Test
    fun `log reply code and text with omitText = true`() {
        val response = SmtpResponse(
            replyCode = 250,
            enhancedStatusCode = null,
            texts = listOf("Sender <sender@domain.example> OK"),
        )

        val output = response.toLogString(omitText = true, linePrefix = "SMTP <<< ")

        assertThat(output).isEqualTo("SMTP <<< 250 [omitted]")
    }

    @Test
    fun `log reply code and status code`() {
        val response = SmtpResponse(
            replyCode = 200,
            enhancedStatusCode = EnhancedStatusCode(statusClass = StatusCodeClass.SUCCESS, subject = 0, detail = 0),
            texts = emptyList(),
        )

        val output = response.toLogString(omitText = false, linePrefix = "SMTP <<< ")

        assertThat(output).isEqualTo("SMTP <<< 200 2.0.0")
    }

    @Test
    fun `log reply code and status code with omitText = true`() {
        val response = SmtpResponse(
            replyCode = 200,
            enhancedStatusCode = EnhancedStatusCode(statusClass = StatusCodeClass.SUCCESS, subject = 0, detail = 0),
            texts = emptyList(),
        )

        val output = response.toLogString(omitText = true, linePrefix = "SMTP <<< ")

        assertThat(output).isEqualTo("SMTP <<< 200 2.0.0")
    }

    @Test
    fun `log reply code, status code, and text`() {
        val response = SmtpResponse(
            replyCode = 200,
            enhancedStatusCode = EnhancedStatusCode(statusClass = StatusCodeClass.SUCCESS, subject = 0, detail = 0),
            texts = listOf("OK"),
        )

        val output = response.toLogString(omitText = false, linePrefix = "SMTP <<< ")

        assertThat(output).isEqualTo("SMTP <<< 200 2.0.0 OK")
    }

    @Test
    fun `log reply code, status code, and text with omitText = true`() {
        val response = SmtpResponse(
            replyCode = 200,
            enhancedStatusCode = EnhancedStatusCode(statusClass = StatusCodeClass.SUCCESS, subject = 0, detail = 0),
            texts = listOf("OK"),
        )

        val output = response.toLogString(omitText = true, linePrefix = "SMTP <<< ")

        assertThat(output).isEqualTo("SMTP <<< 200 2.0.0 [omitted]")
    }

    @Test
    fun `log reply code and multi-line text`() {
        val response = SmtpResponse(
            replyCode = 250,
            enhancedStatusCode = null,
            texts = listOf("Sender <sender@domain.example>", "OK"),
        )

        val output = response.toLogString(omitText = false, linePrefix = "SMTP <<< ")

        assertThat(output).isEqualTo(
            """
            SMTP <<< 250-Sender <sender@domain.example>
            SMTP <<< 250 OK
            """.trimIndent(),
        )
    }

    @Test
    fun `log reply code and multi-line text with omitText = true`() {
        val response = SmtpResponse(
            replyCode = 250,
            enhancedStatusCode = null,
            texts = listOf("Sender <sender@domain.example>", "OK"),
        )

        val output = response.toLogString(omitText = true, linePrefix = "SMTP <<< ")

        assertThat(output).isEqualTo("SMTP <<< 250 [omitted]")
    }

    @Test
    fun `log reply code, status code, and multi-line text`() {
        val response = SmtpResponse(
            replyCode = 250,
            enhancedStatusCode = EnhancedStatusCode(statusClass = StatusCodeClass.SUCCESS, subject = 1, detail = 0),
            texts = listOf("Sender <sender@domain.example>", "OK"),
        )

        val output = response.toLogString(omitText = false, linePrefix = "SMTP <<< ")

        assertThat(output).isEqualTo(
            """
            SMTP <<< 250-2.1.0 Sender <sender@domain.example>
            SMTP <<< 250 2.1.0 OK
            """.trimIndent(),
        )
    }

    @Test
    fun `log reply code, status code, and multi-line text with omitText = true`() {
        val response = SmtpResponse(
            replyCode = 250,
            enhancedStatusCode = EnhancedStatusCode(statusClass = StatusCodeClass.SUCCESS, subject = 1, detail = 0),
            texts = listOf("Sender <sender@domain.example>", "OK"),
        )

        val output = response.toLogString(omitText = true, linePrefix = "SMTP <<< ")

        assertThat(output).isEqualTo("SMTP <<< 250 2.1.0 [omitted]")
    }
}
