package com.fsck.k9.message.signature

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.message.html.HtmlHelper.extractText
import com.fsck.k9.message.signature.HtmlSignatureRemover.Companion.stripSignature
import com.fsck.k9.testing.removeNewlines
import org.junit.Test

class HtmlSignatureRemoverTest {
    @Test
    fun `old K-9 Mail signature format`() {
        val html =
            """This is the body text<br>-- <br>Sent from my Android device with K-9 Mail. Please excuse my brevity."""

        val withoutSignature = stripSignature(html)

        assertThat(extractText(withoutSignature)).isEqualTo("This is the body text")
    }

    @Test
    fun `old Thunderbird signature format`() {
        val html =
            """
            <html>
              <head>
                <meta http-equiv="content-type" content="text/html; charset=utf-8">
              </head>
              <body bgcolor="#FFFFFF" text="#000000">
                <p>This is the body text<br>
                </p>
                -- <br>
                <div class="moz-signature">Sent from my Android device with K-9 Mail. Please excuse my brevity.</div>
              </body>
            </html>
            """.trimIndent()

        val withoutSignature = stripSignature(html)

        assertThat(extractText(withoutSignature)).isEqualTo("This is the body text")
    }

    @Test
    fun `signature before blockquote tag`() {
        val html =
            """
            <html>
            <head></head>
            <body>
            <div>
            This is the body text<br>
            -- <br>
            <blockquote>Sent from my Android device with K-9 Mail. Please excuse my brevity.</blockquote>
            </div>
            </body>
            </html>
            """.trimIndent().removeNewlines()

        val withoutSignature = stripSignature(html)

        assertThat(withoutSignature).isEqualTo(
            """<html><head></head><body><div>This is the body text</div></body></html>""",
        )
    }

    @Test
    fun `should not strip signature inside blockquote tag`() {
        val html =
            """
            <html>
            <head></head>
            <body>
            <blockquote>
            This is some quoted text<br>
            -- <br>
            Inner signature
            </blockquote>
            <div>
            This is the body text
            </div>
            </body>
            </html>
            """.trimIndent().removeNewlines()

        val withoutSignature = stripSignature(html)

        assertThat(withoutSignature).isEqualTo(html)
    }

    @Test
    fun `signature between blockquote tags`() {
        val html =
            """
            <html>
            <head></head>
            <body>
            <blockquote>Some quote</blockquote>
            <div>This is the body text<br>
            -- <br>
            <blockquote>Sent from my Android device with K-9 Mail. Please excuse my brevity.</blockquote>
            <br>-- <br>Signature inside signature
            </div>
            </body>
            </html>
            """.trimIndent().removeNewlines()

        val withoutSignature = stripSignature(html)

        assertThat(withoutSignature).isEqualTo(
            """
            <html>
            <head></head>
            <body>
            <blockquote>Some quote</blockquote>
            <div>This is the body text</div>
            </body>
            </html>
            """.trimIndent().removeNewlines(),
        )
    }

    @Test
    fun `signature after last blockquote tag`() {
        val html =
            """
            <html>
            <head></head>
            <body>
            This is the body text<br>
            <blockquote>Some quote</blockquote>
            <br>
            -- <br>
            Sent from my Android device with K-9 Mail. Please excuse my brevity.
            </body>
            </html>
            """.trimIndent().removeNewlines()

        val withoutSignature = stripSignature(html)

        assertThat(withoutSignature).isEqualTo(
            """
            <html>
            <head></head>
            <body>
            This is the body text<br>
            <blockquote>Some quote</blockquote>
            </body>
            </html>
            """.trimIndent().removeNewlines(),
        )
    }

    @Test
    fun `K-9 Mail signature format`() {
        val html =
            """
            <!DOCTYPE html>
            <html>
            <body>
            This is the body text.<br>
            <br>
            <div class='k9mail-signature'>
            -- <br>
            And this is the signature text.
            </div>
            </body>
            </html>
            """.trimIndent().removeNewlines()

        val withoutSignature = stripSignature(html)

        assertThat(withoutSignature).isEqualTo(
            """
            <!doctype html>
            <html>
            <head></head>
            <body>
            This is the body text.<br>
            <br>
            </body>
            </html>
            """.trimIndent().removeNewlines(),
        )
    }

    @Test
    fun `signature delimiter with non-breaking space character entity`() {
        val html = "Body text<br>--&nbsp;<br>Signature text"

        val withoutSignature = stripSignature(html)

        assertThat(extractText(withoutSignature)).isEqualTo("Body text")
    }

    @Test
    fun `signature delimiter with non-breaking space`() {
        val html = "Body text<br>--\u00A0<br>Signature text"

        val withoutSignature = stripSignature(html)

        assertThat(extractText(withoutSignature)).isEqualTo("Body text")
    }
}
