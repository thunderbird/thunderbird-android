package com.fsck.k9.mail.internet

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.length
import assertk.fail
import kotlin.test.Test

class MimeHeaderEncoderTest {
    @Test
    fun `short subject containing only ASCII characters should not be encoded`() {
        val result = MimeHeaderEncoder.encode(name = "Subject", value = "Hello World!")

        assertThat(result).isEqualTo("Hello World!")
    }

    @Test
    fun `short subject containing non-ASCII characters should be encoded`() {
        val result = MimeHeaderEncoder.encode(name = "Subject", value = "Gänseblümchen")

        assertThat(result).isEqualTo("=?UTF-8?Q?G=C3=A4nsebl=C3=BCmchen?=")
    }

    @Test
    fun `subject with recommended line length should not be folded`() {
        val subject = "a".repeat(RECOMMENDED_MAX_LINE_LENGTH - "Subject: ".length)

        val result = MimeHeaderEncoder.encode(name = "Subject", value = subject)

        assertThat(result).isEqualTo(subject)
    }

    @Test
    fun `subject exceeding recommended line length should be folded`() {
        val subject = "a".repeat(34) + " " + "a".repeat(35)

        val result = MimeHeaderEncoder.encode(name = "Subject", value = subject)

        assertThat(result).all {
            transform { it.lines() }.each {
                it.length().isLessThanOrEqualTo(RECOMMENDED_MAX_LINE_LENGTH)
            }
            isValidHeader(name = "Subject")
            decodesTo(subject)
        }
    }

    @Test
    fun `subject exceeding maximum line length should be encoded`() {
        val subject = "a".repeat(999)

        val result = MimeHeaderEncoder.encode(name = "Subject", value = subject)

        assertThat(result).all {
            isValidHeader(name = "Subject")
            decodesTo(subject)
        }
    }

    private fun Assert<String>.isValidHeader(name: String) = given { value ->
        try {
            MimeHeaderChecker.checkHeader(name, value)
        } catch (e: MimeHeaderParserException) {
            fail(AssertionError("Not a valid RFC5322 header", e))
        }
    }

    private fun Assert<String>.decodesTo(expected: String) = given { value ->
        assertThat(MimeUtility.unfoldAndDecode(value)).isEqualTo(expected)
    }
}
