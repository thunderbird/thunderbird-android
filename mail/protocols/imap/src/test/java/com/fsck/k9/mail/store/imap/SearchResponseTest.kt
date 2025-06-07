package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.store.imap.SearchResponse.Companion.parse
import org.junit.Assert
import kotlin.test.Test

class SearchResponseTest {
    @Test
    fun parse_withSingleSearchResponse_shouldExtractNumbers() {
        val imapResponses = ImapResponseHelper.createImapResponseList(
            "* SEARCH 1 2 3",
            "* 23 EXISTS",
            "* SEARCH 4",
            "1 OK SEARCH completed",
        )

        val result = parse(imapResponses)

        Assert.assertNotNull(result)
        Assert.assertEquals(mutableListOf(1L, 2L, 3L, 4L), result.numbers)
    }

    @Test
    fun parse_withMultipleSearchResponses_shouldExtractNumbers() {
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

        val result = parse(imapResponses)

        Assert.assertNotNull(result)
        Assert.assertEquals(mutableListOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L), result.numbers)
    }

    @Test
    fun parse_withSingleTaggedSearchResponse_shouldReturnEmptyList() {
        val imapResponses = ImapResponseHelper.createImapResponseList("x SEARCH 7 8 9")

        val result = parse(imapResponses)

        Assert.assertNotNull(result)
        Assert.assertEquals(emptyList<Any>(), result.numbers)
    }

    @Test
    fun parse_withSingleTooShortResponse_shouldReturnEmptyList() {
        val imapResponses = ImapResponseHelper.createImapResponseList("* SEARCH")

        val result = parse(imapResponses)

        Assert.assertNotNull(result)
        Assert.assertEquals(emptyList<Any>(), result.numbers)
    }

    @Test
    fun parse_withSingleNoSearchResponse_shouldReturnEmptyList() {
        val imapResponses = ImapResponseHelper.createImapResponseList("* 23 EXPUNGE")

        val result = parse(imapResponses)

        Assert.assertNotNull(result)
        Assert.assertEquals(emptyList<Any>(), result.numbers)
    }

    @Test
    fun parse_withSingleSearchResponseContainingInvalidNumber_shouldReturnEmptyList() {
        val imapResponses = ImapResponseHelper.createImapResponseList("* SEARCH A")

        val result = parse(imapResponses)

        Assert.assertNotNull(result)
        Assert.assertEquals(emptyList<Any>(), result.numbers)
    }
}
