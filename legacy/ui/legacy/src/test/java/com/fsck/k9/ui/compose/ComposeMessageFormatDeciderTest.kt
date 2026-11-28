package com.fsck.k9.ui.compose

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.activity.MessageCompose.Action
import com.fsck.k9.message.SimpleMessageFormat
import com.fsck.k9.message.SimpleMessageFormat.HTML
import com.fsck.k9.message.SimpleMessageFormat.TEXT
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.MessageFormat
import org.junit.Test

class ComposeMessageFormatDeciderTest {

    @Test
    fun `auto mode compose should prefer text`() {
        assertAutoFormat(action = Action.COMPOSE, quote = NO_QUOTE, htmlSignature = false,
            expected = TEXT
        )
    }

    @Test
    fun `auto mode compose with HTML signature should use HTML`() {
        assertAutoFormat(action = Action.COMPOSE, quote = NO_QUOTE, htmlSignature = true, expected = HTML)
    }

    @Test
    fun `auto mode reply to shown text quote should use text even with HTML signature`() {
        assertAutoFormat(action = Action.REPLY, quote = SHOWN_TEXT_QUOTE, htmlSignature = true,
            expected = TEXT
        )
    }

    @Test
    fun `auto mode reply with hidden text quote and HTML signature should use HTML`() {
        assertAutoFormat(action = Action.REPLY, quote = HIDDEN_TEXT_QUOTE, htmlSignature = true, expected = HTML)
    }

    @Test
    fun `auto mode reply to shown HTML quote should use HTML`() {
        assertAutoFormat(action = Action.REPLY, quote = SHOWN_HTML_QUOTE, htmlSignature = false, expected = HTML)
    }

    @Test
    fun `auto mode reply with hidden HTML quote should use text`() {
        assertAutoFormat(action = Action.REPLY, quote = HIDDEN_HTML_QUOTE, htmlSignature = false,
            expected = TEXT
        )
    }

    @Test
    fun `auto mode edit draft saved as HTML with HTML signature should use HTML`() {
        assertAutoFormat(action = Action.EDIT_DRAFT, quote = SHOWN_HTML_QUOTE, htmlSignature = true, expected = HTML)
    }

    @Test
    fun `auto mode edit draft saved as text after quote was edited should use text even with HTML signature`() {
        // The draft only stored text/plain, so the reloaded quote is text
        assertAutoFormat(action = Action.EDIT_DRAFT, quote = SHOWN_TEXT_QUOTE, htmlSignature = true,
            expected = TEXT
        )
    }

    @Test
    fun `auto mode edited quote should use text even with HTML signature`() {
        // editing the quote forces the message to use TEXT as we don't have HTML quote edit support
        assertAutoFormat(action = Action.REPLY, quote = EDITED_QUOTE, htmlSignature = true,
            expected = TEXT
        )
    }

    @Test
    fun `text account format should always use text`() {
        // Arrange
        val testSubject = ComposeMessageFormatDecider(SHOWN_HTML_QUOTE)

        // Act
        val result = testSubject.decide(MessageFormat.TEXT, createIdentity(htmlSignature = true), Action.REPLY, false)

        // Assert
        assertThat(result).isEqualTo(TEXT)
    }

    @Test
    fun `HTML account format should use HTML for new messages`() {
        // Arrange
        val testSubject = ComposeMessageFormatDecider(NO_QUOTE)

        // Act
        val result = testSubject.decide(
            accountMessageFormat = MessageFormat.HTML,
            identity = createIdentity(htmlSignature = false),
            action = Action.COMPOSE,
            isForceTextMessageFormat = false,
        )

        // Assert
        assertThat(result).isEqualTo(HTML)
    }

    @Test
    fun `HTML account format should use text when quote was edited`() {
        // Arrange
        val testSubject = ComposeMessageFormatDecider(EDITED_QUOTE)

        // Act
        val result = testSubject.decide(MessageFormat.HTML, createIdentity(htmlSignature = true), Action.REPLY, false)

        // Assert
        assertThat(result).isEqualTo(TEXT)
    }

    @Test
    fun `forced text message format should use text`() {
        // Arrange
        val testSubject = ComposeMessageFormatDecider(SHOWN_HTML_QUOTE)

        // Act
        val result = testSubject.decide(MessageFormat.HTML, createIdentity(htmlSignature = true), Action.REPLY, true)

        // Assert
        assertThat(result).isEqualTo(TEXT)
    }

    private fun assertAutoFormat(
        action: Action,
        quote: QuotedTextState,
        htmlSignature: Boolean,
        expected: SimpleMessageFormat,
    ) {
        // Arrange
        val testSubject = ComposeMessageFormatDecider(quote)

        // Act
        val result = testSubject.decide(MessageFormat.AUTO, createIdentity(htmlSignature), action, false)

        // Assert
        assertThat(result).isEqualTo(expected)
    }

    private fun createIdentity(htmlSignature: Boolean) = Identity(
        signature = "-- signature",
        signatureUse = true,
        signatureIsHtml = htmlSignature,
    )

    private class FakeQuotedTextState(
        private val includeQuotedText: Boolean,
        private val isForcePlainText: Boolean = false,
        private val isQuotedTextText: Boolean,
    ) : QuotedTextState {
        override fun includeQuotedText() = includeQuotedText
        override fun isForcePlainText() = isForcePlainText
        override fun isQuotedTextText() = isQuotedTextText
    }

    private companion object {
        val NO_QUOTE = FakeQuotedTextState(includeQuotedText = false, isQuotedTextText = false)
        val SHOWN_TEXT_QUOTE = FakeQuotedTextState(includeQuotedText = true, isQuotedTextText = true)
        val SHOWN_HTML_QUOTE = FakeQuotedTextState(includeQuotedText = true, isQuotedTextText = false)
        val HIDDEN_TEXT_QUOTE = FakeQuotedTextState(includeQuotedText = false, isQuotedTextText = true)
        val HIDDEN_HTML_QUOTE = FakeQuotedTextState(includeQuotedText = false, isQuotedTextText = false)
        val EDITED_QUOTE = FakeQuotedTextState(
            includeQuotedText = true,
            isForcePlainText = true,
            isQuotedTextText = true,
        )
    }
}
