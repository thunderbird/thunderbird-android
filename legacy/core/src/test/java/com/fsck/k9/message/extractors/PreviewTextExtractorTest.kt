package com.fsck.k9.message.extractors

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.internet.MimeBodyPart
import com.fsck.k9.message.MessageCreationHelper
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.Before
import org.junit.Test

class PreviewTextExtractorTest {
    private val previewTextExtractor = PreviewTextExtractor()

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test(expected = PreviewExtractionException::class)
    fun extractPreview_withEmptyBody_shouldThrow() {
        val part = MimeBodyPart(null, "text/plain")

        previewTextExtractor.extractPreview(part)
    }

    @Test
    fun extractPreview_withSimpleTextPlain() {
        val text = "The quick brown fox jumps over the lazy dog"
        val part = MessageCreationHelper.createTextPart("text/plain", text)

        val preview = previewTextExtractor.extractPreview(part)

        assertThat(preview).isEqualTo(text)
    }

    @Test
    fun extractPreview_withSimpleTextHtml() {
        val text = "<b>The quick brown fox jumps over the lazy dog</b>"
        val part = MessageCreationHelper.createTextPart("text/html", text)

        val preview = previewTextExtractor.extractPreview(part)

        assertThat(preview).isEqualTo("The quick brown fox jumps over the lazy dog")
    }

    @Test
    fun extractPreview_withLongTextPlain() {
        val text = "" +
            "10--------20--------30--------40--------50--------" +
            "60--------70--------80--------90--------100-------" +
            "110-------120-------130-------140-------150-------" +
            "160-------170-------180-------190-------200-------" +
            "210-------220-------230-------240-------250-------" +
            "260-------270-------280-------290-------300-------" +
            "310-------320-------330-------340-------350-------" +
            "360-------370-------380-------390-------400-------" +
            "410-------420-------430-------440-------450-------" +
            "460-------470-------480-------490-------500-------" +
            "510-------520-------"
        val part = MessageCreationHelper.createTextPart("text/plain", text)

        val preview = previewTextExtractor.extractPreview(part)

        assertThat(preview).isEqualTo(text.substring(0, 511) + "…")
    }

    @Test
    fun extractPreview_shouldStripSignature() {
        val text =
            """
            Some text
            -- 
            Signature
            """.trimIndent()
        val part = MessageCreationHelper.createTextPart("text/plain", text)

        val preview = previewTextExtractor.extractPreview(part)

        assertThat(preview).isEqualTo("Some text")
    }

    @Test
    fun extractPreview_shouldStripHorizontalLine() {
        val text =
            """
            line 1
            ----
            line 2
            """.trimIndent()
        val part = MessageCreationHelper.createTextPart("text/plain", text)

        val preview = previewTextExtractor.extractPreview(part)

        assertThat(preview).isEqualTo("line 1 line 2")
    }

    @Test
    fun extractPreview_shouldStripQuoteHeaderAndQuotedText() {
        val text =
            """
            some text
            
            On 01/02/03 someone wrote:
            > some quoted text
            > some other quoted text
            """.trimIndent()
        val part = MessageCreationHelper.createTextPart("text/plain", text)

        val preview = previewTextExtractor.extractPreview(part)

        assertThat(preview).isEqualTo("some text")
    }

    @Test
    fun extractPreview_shouldStripGenericQuoteHeader() {
        val text =
            """
            Am 13.12.2015 um 23:42 schrieb Hans:
            > hallo
            hi there
            
            """.trimIndent()
        val part = MessageCreationHelper.createTextPart("text/plain", text)

        val preview = previewTextExtractor.extractPreview(part)

        assertThat(preview).isEqualTo("hi there")
    }

    @Test
    fun extractPreview_shouldStripHorizontalRules() {
        val text =
            """
            line 1------------------------------
            line 2
            """.trimIndent()
        val part = MessageCreationHelper.createTextPart("text/plain", text)

        val preview = previewTextExtractor.extractPreview(part)

        assertThat(preview).isEqualTo("line 1 line 2")
    }

    @Test
    fun extractPreview_shouldReplaceUrl() {
        val text = "some url: https://k9mail.org/"
        val part = MessageCreationHelper.createTextPart("text/plain", text)

        val preview = previewTextExtractor.extractPreview(part)

        assertThat(preview).isEqualTo("some url: ...")
    }

    @Test
    fun extractPreview_shouldCollapseAndTrimWhitespace() {
        val text = " whitespace     is\t\tfun  "
        val part = MessageCreationHelper.createTextPart("text/plain", text)

        val preview = previewTextExtractor.extractPreview(part)

        assertThat(preview).isEqualTo("whitespace is fun")
    }

    @Test
    fun extractPreview_lineEndingWithColon() {
        val text =
            """
            Here's a list:
            - item 1
            - item 2
            """.trimIndent()
        val part = MessageCreationHelper.createTextPart("text/plain", text)

        val preview = previewTextExtractor.extractPreview(part)

        assertThat(preview).isEqualTo("Here's a list: - item 1 - item 2")
    }

    @Test
    fun extractPreview_inlineReplies() {
        val text =
            """
            On 2020-09-30 at 03:12 Bob wrote:
            > Hi Alice
            Hi Bob
            
            > How are you?
            I'm fine. Thanks for asking.
            
            > Bye
            See you tomorrow
            """.trimIndent()
        val part = MessageCreationHelper.createTextPart("text/plain", text)

        val preview = previewTextExtractor.extractPreview(part)

        assertThat(preview).isEqualTo("Hi Bob […] I'm fine. Thanks for asking. […] See you tomorrow")
    }

    @Test
    fun extractPreview_quoteHeaderContainingLineBreak() {
        val text =
            """
            Reply text
            
            On 2020-09-30 at 03:12
            Bob wrote:
            > Quoted text
            """.trimIndent()
        val part = MessageCreationHelper.createTextPart("text/plain", text)

        val preview = previewTextExtractor.extractPreview(part)

        assertThat(preview).isEqualTo("Reply text")
    }

    @Test
    fun extractPreview_emptyBody() {
        val text = ""
        val part = MessageCreationHelper.createTextPart("text/plain", text)

        val preview = previewTextExtractor.extractPreview(part)

        assertThat(preview).isEqualTo("")
    }
}
