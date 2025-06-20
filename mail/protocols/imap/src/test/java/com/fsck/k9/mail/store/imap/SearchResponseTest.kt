package com.fsck.k9.mail.store.imap

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.prop
import com.fsck.k9.mail.store.imap.SearchResponse.Companion.parse
import jdk.dynalink.linker.support.Guards.isNotNull
import kotlin.test.Test
import org.junit.Assert

class SearchResponseTest {
    @Test
    fun parse_withSingleSearchResponse_shouldExtractNumbers() {
        // Arrange
        val imapResponses = ImapResponseHelper.createImapResponseList(
            "* SEARCH 1 2 3",
            "* 23 EXISTS",
            "* SEARCH 4",
            "1 OK SEARCH completed",
        )

        // Act
        val result = parse(imapResponses)

        // Assert
        assertThat(result).all {
            isNotNull()
            prop("numbers") { it.numbers }
                .containsExactly(1L, 2L, 3L, 4L)
        }
    }

    @Test
    fun parse_withMultipleSearchResponses_shouldExtractNumbers() {
        // Arrange
        val imapResponses = ImapResponseHelper.createImapResponseList(
            "* SEARCH 1 2 3",
            "* 23 EXISTS",
            "* SEARCH 4",
            "1 OK SEARCH completed",
            "* SEARCH 5 6",
            "* 19 EXPUNGED",
            "* SEARCH 7",
            "2 OK SEARCH completed",
            "* SEARCH 8",
            "3 OK SEARCH completed",
        )

        // Act
        val result = parse(imapResponses)

        // Assert
        assertThat(result).all {
            isNotNull()
            prop("numbers") { it.numbers }
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L)
        }
    }

    @Test
    fun parse_withSingleTaggedSearchResponse_shouldReturnEmptyList() {
        // Arrange
        val imapResponses = ImapResponseHelper.createImapResponseList("x SEARCH 7 8 9")

        // Act
        val result = parse(imapResponses)

        Assert.assertNotNull(result)
        Assert.assertEquals(emptyList<Any>(), result.numbers)

        // Assert
        assertThat(result).all {
            isNotNull()
            prop("numbers") { it.numbers }
                .isEmpty()
        }
    }

    @Test
    fun parse_withSingleTooShortResponse_shouldReturnEmptyList() {
        // Arrange
        val imapResponses = ImapResponseHelper.createImapResponseList("* SEARCH")

        // Act
        val result = parse(imapResponses)

        // Assert
        assertThat(result).all {
            isNotNull()
            prop("numbers") { it.numbers }
                .isEmpty()
        }
    }

    @Test
    fun parse_withSingleNoSearchResponse_shouldReturnEmptyList() {
        // Arrange
        val imapResponses = ImapResponseHelper.createImapResponseList("* 23 EXPUNGE")

        // Act
        val result = parse(imapResponses)

        // Assert
        assertThat(result).all {
            isNotNull()
            prop("numbers") { it.numbers }
                .isEmpty()
        }
    }

    @Test
    fun parse_withSingleSearchResponseContainingInvalidNumber_shouldReturnEmptyList() {
        // Arrange
        val imapResponses = ImapResponseHelper.createImapResponseList("* SEARCH A")

        // Act
        val result = parse(imapResponses)

        // Assert
        assertThat(result).all {
            isNotNull()
            prop("numbers") { it.numbers }
                .isEmpty()
        }
    }
}
