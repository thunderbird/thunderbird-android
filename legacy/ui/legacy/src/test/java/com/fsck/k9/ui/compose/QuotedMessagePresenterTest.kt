package com.fsck.k9.ui.compose

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.message.quote.InsertableHtmlContent
import org.junit.Test

class QuotedMessagePresenterTest {
    @Test
    fun `normal-sized quoted HTML should be persisted`() {
        // Arrange
        val quotedHtmlContent = createQuotedHtmlContent("quoted content")

        // Act
        val result = QuotedMessagePresenter.shouldPersistQuotedHtml(
            quotedHtmlContent,
            QuotedMessagePresenter.MAX_QUOTED_HTML_CHARACTERS,
        )

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `oversized quoted HTML should not be persisted`() {
        // Arrange
        val quotedHtmlContent = createQuotedHtmlContent("a".repeat(1_100_000))

        // Act
        val result = QuotedMessagePresenter.shouldPersistQuotedHtml(
            quotedHtmlContent,
            QuotedMessagePresenter.MAX_QUOTED_HTML_CHARACTERS,
        )

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `null quoted HTML should be persisted`() {
        // Arrange
        val quotedHtmlContent: InsertableHtmlContent? = null

        // Act
        val result = QuotedMessagePresenter.shouldPersistQuotedHtml(
            quotedHtmlContent,
            QuotedMessagePresenter.MAX_QUOTED_HTML_CHARACTERS,
        )

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `quoted HTML at the maximum size should be persisted`() {
        // Arrange
        val quotedHtmlContent = createQuotedHtmlContent(
            "a".repeat(QuotedMessagePresenter.MAX_QUOTED_HTML_CHARACTERS),
        )

        // Act
        val result = QuotedMessagePresenter.shouldPersistQuotedHtml(
            quotedHtmlContent,
            QuotedMessagePresenter.MAX_QUOTED_HTML_CHARACTERS,
        )

        // Assert
        assertThat(result).isTrue()
    }

    private fun createQuotedHtmlContent(content: String): InsertableHtmlContent {
        return InsertableHtmlContent().apply {
            setQuotedContent(StringBuilder(content))
        }
    }
}
