package com.fsck.k9.message

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import com.fsck.k9.notification.FakePlatformConfigProvider
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.GeneralSettings
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * Exercises the HTML signature short-circuit added for the HTML signature feature.
 *
 * The existing parameterized [TextBodyBuilderTest] covers the plain-text signature
 * path; these cases add coverage for [TextBodyBuilder.setSignatureIsHtml].
 */
class TextBodyBuilderHtmlSignatureTest {

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    private fun newBuilder(messageContent: String = "hello"): TextBodyBuilder {
        return TextBodyBuilder(
            messageContent,
            mock { on { getConfig() } doReturn GeneralSettings(platformConfigProvider = FakePlatformConfigProvider()) },
        ).apply {
            setAppendSignature(true)
            setIncludeQuotedText(false)
        }
    }

    @Test
    fun `html signature is embedded verbatim in html body without text-to-html conversion`() {
        val htmlSignature = """<p>Sent from <b>Thunderbird</b></p>"""
        val builder = newBuilder().apply {
            setSignatureIsHtml(true)
            setSignature(htmlSignature)
        }

        val body = builder.buildTextHtml().rawText

        // The <b> tag is preserved — it would have been escaped to &lt;b&gt; if we had
        // gone through HtmlConverter.textToHtmlFragment().
        assertThat(body).contains("<b>Thunderbird</b>")
        assertThat(body).doesNotContain("&lt;b&gt;")
    }

    @Test
    fun `html signature has script tags removed when embedded in html body`() {
        val htmlSignature = """<p>Hi</p><script>alert('xss')</script>"""
        val builder = newBuilder().apply {
            setSignatureIsHtml(true)
            setSignature(htmlSignature)
        }

        val body = builder.buildTextHtml().rawText

        assertThat(body).contains("<p>Hi</p>")
        assertThat(body).doesNotContain("<script>")
        assertThat(body).doesNotContain("alert")
    }

    @Test
    fun `html signature is converted to plain text when building plain body`() {
        val htmlSignature = """<p>Sent from <b>Thunderbird</b></p>"""
        val builder = newBuilder().apply {
            setSignatureIsHtml(true)
            setSignature(htmlSignature)
        }

        val body = builder.buildTextPlain().rawText

        // The HTML tags should be stripped for the plain-text path.
        assertThat(body).contains("Sent from Thunderbird")
        assertThat(body).doesNotContain("<b>")
        assertThat(body).doesNotContain("<p>")
    }

    @Test
    fun `plain signature still goes through text-to-html conversion in html body`() {
        val plainSignature = "-- \r\nAlice"
        val builder = newBuilder().apply {
            setSignatureIsHtml(false)
            setSignature(plainSignature)
        }

        val body = builder.buildTextHtml().rawText

        // The plain-text path wraps the signature in a k9mail-signature div.
        assertThat(body).contains("k9mail-signature")
    }
}
