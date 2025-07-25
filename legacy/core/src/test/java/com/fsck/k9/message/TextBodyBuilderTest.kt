package com.fsck.k9.message

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.message.quote.InsertableHtmlContent
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.GeneralSettings
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(Parameterized::class)
class TextBodyBuilderTest(val testData: TestData) {

    companion object {

        private const val MESSAGE_TEXT = "my message\r\nwith two lines"
        private const val MESSAGE_TEXT_HTML = "<div dir=\"auto\">my message<br>with two lines</div>"
        private const val QUOTED_TEXT = ">quoted text\r\n>-- \r\n>Other signature"
        private const val QUOTED_HTML_BODY = "<blockquote>quoted text</blockquote>"
        private const val QUOTED_HTML_TAGS_END = "</body>\n</html>"
        private const val QUOTED_HTML_TAGS_START = "<!DOCTYPE html><html><head></head><body>"
        private const val SIGNATURE_TEXT = "-- \r\n\r\nsignature\r\n  indented second line"
        private const val SIGNATURE_TEXT_HTML = "<div dir=\"auto\"><div class='k9mail-signature'>-- <br>" +
            "<br>signature<br>\u00A0 indented second line</div></div>"

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): Collection<TestData> {
            return listOf(
                TestData(
                    appendSignature = false,
                    includeQuotedText = false,
                    insertSeparator = false,
                    replyAfterQuote = false,
                    signatureBeforeQuotedText = false,
                    expectedPlainTextMessage = MESSAGE_TEXT,
                    expectedHtmlTextMessage = TextBodyBuilder.HTML_AND_BODY_START + MESSAGE_TEXT_HTML +
                        TextBodyBuilder.HTML_AND_BODY_END,
                ),
                TestData(
                    appendSignature = true,
                    includeQuotedText = false,
                    insertSeparator = false,
                    replyAfterQuote = false,
                    signatureBeforeQuotedText = false,
                    expectedPlainTextMessage = MESSAGE_TEXT + "\r\n" + SIGNATURE_TEXT,
                    expectedHtmlTextMessage = TextBodyBuilder.HTML_AND_BODY_START + MESSAGE_TEXT_HTML +
                        SIGNATURE_TEXT_HTML + TextBodyBuilder.HTML_AND_BODY_END,
                ),
                TestData(
                    appendSignature = false,
                    includeQuotedText = true,
                    insertSeparator = false,
                    replyAfterQuote = false,
                    signatureBeforeQuotedText = false,
                    expectedPlainTextMessage = MESSAGE_TEXT + "\r\n\r\n" + QUOTED_TEXT,
                    expectedHtmlTextMessage = QUOTED_HTML_TAGS_START + MESSAGE_TEXT_HTML + QUOTED_HTML_BODY +
                        QUOTED_HTML_TAGS_END,
                ),
                TestData(
                    appendSignature = false,
                    includeQuotedText = true,
                    insertSeparator = true,
                    replyAfterQuote = false,
                    signatureBeforeQuotedText = false,
                    expectedPlainTextMessage = MESSAGE_TEXT + "\r\n\r\n" + QUOTED_TEXT,
                    expectedHtmlTextMessage = QUOTED_HTML_TAGS_START + MESSAGE_TEXT_HTML + "<br><br>" +
                        QUOTED_HTML_BODY + QUOTED_HTML_TAGS_END,
                ),
                TestData(
                    appendSignature = false,
                    includeQuotedText = true,
                    insertSeparator = false,
                    replyAfterQuote = true,
                    signatureBeforeQuotedText = false,
                    expectedPlainTextMessage = QUOTED_TEXT + "\r\n" + MESSAGE_TEXT,
                    expectedHtmlTextMessage = QUOTED_HTML_TAGS_START + QUOTED_HTML_BODY + MESSAGE_TEXT_HTML +
                        QUOTED_HTML_TAGS_END,
                ),
                TestData(
                    appendSignature = false,
                    includeQuotedText = true,
                    insertSeparator = true,
                    replyAfterQuote = true,
                    signatureBeforeQuotedText = false,
                    expectedPlainTextMessage = QUOTED_TEXT + "\r\n" + MESSAGE_TEXT,
                    expectedHtmlTextMessage = QUOTED_HTML_TAGS_START + QUOTED_HTML_BODY + "<br clear=\"all\">" +
                        MESSAGE_TEXT_HTML + QUOTED_HTML_TAGS_END,
                ),
                TestData(
                    appendSignature = true,
                    includeQuotedText = true,
                    insertSeparator = false,
                    replyAfterQuote = false,
                    signatureBeforeQuotedText = false,
                    expectedPlainTextMessage = MESSAGE_TEXT + "\r\n\r\n" + QUOTED_TEXT + "\r\n" + SIGNATURE_TEXT,
                    expectedHtmlTextMessage = QUOTED_HTML_TAGS_START + MESSAGE_TEXT_HTML + QUOTED_HTML_BODY +
                        SIGNATURE_TEXT_HTML + QUOTED_HTML_TAGS_END,
                ),
                TestData(
                    appendSignature = true,
                    includeQuotedText = true,
                    insertSeparator = true,
                    replyAfterQuote = false,
                    signatureBeforeQuotedText = false,
                    expectedPlainTextMessage = MESSAGE_TEXT + "\r\n\r\n" + QUOTED_TEXT + "\r\n" + SIGNATURE_TEXT,
                    expectedHtmlTextMessage = QUOTED_HTML_TAGS_START + MESSAGE_TEXT_HTML + "<br><br>" +
                        QUOTED_HTML_BODY + SIGNATURE_TEXT_HTML + QUOTED_HTML_TAGS_END,
                ),
                TestData(
                    appendSignature = true,
                    includeQuotedText = true,
                    insertSeparator = false,
                    replyAfterQuote = true,
                    signatureBeforeQuotedText = false,
                    expectedPlainTextMessage = QUOTED_TEXT + "\r\n" + MESSAGE_TEXT + "\r\n" + SIGNATURE_TEXT,
                    expectedHtmlTextMessage = QUOTED_HTML_TAGS_START + QUOTED_HTML_BODY + MESSAGE_TEXT_HTML +
                        SIGNATURE_TEXT_HTML + QUOTED_HTML_TAGS_END,
                ),
                TestData(
                    appendSignature = true,
                    includeQuotedText = true,
                    insertSeparator = true,
                    replyAfterQuote = true,
                    signatureBeforeQuotedText = false,
                    expectedPlainTextMessage = QUOTED_TEXT + "\r\n" + MESSAGE_TEXT + "\r\n" + SIGNATURE_TEXT,
                    expectedHtmlTextMessage = QUOTED_HTML_TAGS_START + QUOTED_HTML_BODY + "<br clear=\"all\">" +
                        MESSAGE_TEXT_HTML + SIGNATURE_TEXT_HTML + QUOTED_HTML_TAGS_END,
                ),
                TestData(
                    appendSignature = true,
                    includeQuotedText = true,
                    insertSeparator = false,
                    replyAfterQuote = false,
                    signatureBeforeQuotedText = true,
                    expectedPlainTextMessage = MESSAGE_TEXT + "\r\n" + SIGNATURE_TEXT + "\r\n\r\n" + QUOTED_TEXT,
                    expectedHtmlTextMessage = QUOTED_HTML_TAGS_START + MESSAGE_TEXT_HTML + SIGNATURE_TEXT_HTML +
                        QUOTED_HTML_BODY + QUOTED_HTML_TAGS_END,
                ),
                TestData(
                    appendSignature = true,
                    includeQuotedText = true,
                    insertSeparator = true,
                    replyAfterQuote = false,
                    signatureBeforeQuotedText = true,
                    expectedPlainTextMessage = MESSAGE_TEXT + "\r\n" + SIGNATURE_TEXT + "\r\n\r\n" + QUOTED_TEXT,
                    expectedHtmlTextMessage = QUOTED_HTML_TAGS_START + MESSAGE_TEXT_HTML + SIGNATURE_TEXT_HTML +
                        "<br><br>" + QUOTED_HTML_BODY + QUOTED_HTML_TAGS_END,
                ),
            )
        }
    }

    private val toTest: TextBodyBuilder

    init {
        Log.logger = TestLogger()
        toTest = TextBodyBuilder(
            MESSAGE_TEXT,
            mock { on { getConfig() } doReturn GeneralSettings() },
        )
        toTest.setAppendSignature(testData.appendSignature)
        toTest.setIncludeQuotedText(testData.includeQuotedText)
        toTest.setInsertSeparator(testData.insertSeparator)
        toTest.setReplyAfterQuote(testData.replyAfterQuote)
        toTest.setSignatureBeforeQuotedText(testData.signatureBeforeQuotedText)
        toTest.setQuotedText(QUOTED_TEXT)
        val quotedHtmlContent = InsertableHtmlContent()
        quotedHtmlContent.setQuotedContent(
            StringBuilder(QUOTED_HTML_TAGS_START + QUOTED_HTML_BODY + QUOTED_HTML_TAGS_END),
        )
        quotedHtmlContent.setHeaderInsertionPoint(QUOTED_HTML_TAGS_START.length)
        quotedHtmlContent.footerInsertionPoint =
            QUOTED_HTML_TAGS_START.length + QUOTED_HTML_BODY.length
        toTest.setQuotedTextHtml(quotedHtmlContent)
        toTest.setSignature(SIGNATURE_TEXT)
    }

    @Test
    fun plainTextBody_expectCorrectRawText() {
        val textBody = toTest.buildTextPlain()

        assertThat(textBody.rawText).isEqualTo(testData.expectedPlainTextMessage)
    }

    @Test
    fun plainTextBodySubstring_expectMessage() {
        val textBody = toTest.buildTextPlain()

        val startIndex = textBody.composedMessageOffset!!
        val endIndex = startIndex + textBody.composedMessageLength!!
        assertThat(textBody.rawText.substring(startIndex, endIndex)).isEqualTo(MESSAGE_TEXT)
    }

    @Test
    fun htmlTextBody_expectCorrectRawText() {
        val textBody = toTest.buildTextHtml()

        assertThat(textBody.rawText).isEqualTo(testData.expectedHtmlTextMessage)
    }

    @Test
    fun htmlTextBodySubstring_expectMessage() {
        val textBody = toTest.buildTextHtml()

        val startIndex = textBody.composedMessageOffset!!
        val endIndex = startIndex + textBody.composedMessageLength!!
        assertThat(textBody.rawText.substring(startIndex, endIndex)).isEqualTo(MESSAGE_TEXT_HTML)
    }

    class TestData(
        val appendSignature: Boolean,
        val includeQuotedText: Boolean,
        val insertSeparator: Boolean,
        val replyAfterQuote: Boolean,
        val signatureBeforeQuotedText: Boolean,
        val expectedPlainTextMessage: String,
        val expectedHtmlTextMessage: String,
    ) {
        override fun toString(): String {
            return "appendSignature=$appendSignature," +
                "includeQuotedText=$includeQuotedText," +
                "insertSeparator=$insertSeparator," +
                "replyAfterQuote=$replyAfterQuote," +
                "signatureBeforeQuotedText=$signatureBeforeQuotedText"
        }
    }
}
