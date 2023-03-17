package com.fsck.k9.mail.internet

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class PartExtensionsTest {
    @Test
    fun `get charset without charset parameter`() {
        assertGetCharset(headerValue = "text/plain", expectedCharset = null)
    }

    @Test
    fun `get charset with single charset parameter`() {
        assertGetCharset(headerValue = "text/plain; charset=UTF-8", expectedCharset = "utf-8")
    }

    @Test
    fun `get charset with single quoted charset parameter`() {
        assertGetCharset(headerValue = "text/plain; charset=\"iso-8859-1\"", expectedCharset = "ISO-8859-1")
    }

    @Test
    fun `get charset with two charset parameters where values match exactly`() {
        assertGetCharset(headerValue = "text/plain; charset=utf-8; charset=utf-8", expectedCharset = "utf-8")
    }

    @Test
    fun `get charset with two charset parameters where values differ in case`() {
        assertGetCharset(headerValue = "text/plain; charset=utf-8; charset=UTF-8", expectedCharset = "utf-8")
    }

    @Test
    fun `get charset with two charset parameters where values differ in quoting`() {
        assertGetCharset(headerValue = "text/plain; charset=utf-8; charset=\"utf-8\"", expectedCharset = "utf-8")
    }

    @Test
    fun `get charset with two charset parameters with conflicting values`() {
        assertGetCharset(headerValue = "text/plain; charset=utf-8; charset=iso-8859-1", expectedCharset = null)
    }

    @Test
    fun `get charset with extended parameter syntax`() {
        assertGetCharset(headerValue = "text/plain; charset*=us-ascii'en-us'utf-8", expectedCharset = null)
    }

    private fun assertGetCharset(headerValue: String, expectedCharset: String?) {
        val part = MimeBodyPart.create(null, headerValue)

        val charset = part.charset

        assertThat(charset).isEqualTo(other = expectedCharset, ignoreCase = true)
    }
}
