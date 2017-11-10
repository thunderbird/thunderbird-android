package com.fsck.k9.mail.store.imap;


import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponseList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class SearchResponseTest {
    @Test
    public void parse_withSingleSearchResponse_shouldExtractNumbers() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList(
                "* SEARCH 1 2 3",
                "* 23 EXISTS",
                "* SEARCH 4",
                "1 OK SEARCH completed");

        SearchResponse result = SearchResponse.parse(imapResponses);

        assertNotNull(result);
        assertEquals(asList(1L, 2L, 3L, 4L), result.getNumbers());
    }

    @Test
    public void parse_withMultipleSearchResponses_shouldExtractNumbers() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList(
                "* SEARCH 1 2 3",
                "* 23 EXISTS",
                "* SEARCH 4",
                "1 OK SEARCH completed",
                "* SEARCH 5 6",
                "* 19 EXPUNGED",
                "* SEARCH 7",
                "2 OK SEARCH completed",
                "* SEARCH 8",
                "3 OK SEARCH completed");

        SearchResponse result = SearchResponse.parse(imapResponses);

        assertNotNull(result);
        assertEquals(asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L), result.getNumbers());
    }

    @Test
    public void parse_withSingleTaggedSearchResponse_shouldReturnEmptyList() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList("x SEARCH 7 8 9");

        SearchResponse result = SearchResponse.parse(imapResponses);

        assertNotNull(result);
        assertEquals(Collections.emptyList(), result.getNumbers());
    }

    @Test
    public void parse_withSingleTooShortResponse_shouldReturnEmptyList() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList("* SEARCH");

        SearchResponse result = SearchResponse.parse(imapResponses);

        assertNotNull(result);
        assertEquals(Collections.emptyList(), result.getNumbers());
    }

    @Test
    public void parse_withSingleNoSearchResponse_shouldReturnEmptyList() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList("* 23 EXPUNGE");

        SearchResponse result = SearchResponse.parse(imapResponses);

        assertNotNull(result);
        assertEquals(Collections.emptyList(), result.getNumbers());
    }

    @Test
    public void parse_withSingleSearchResponseContainingInvalidNumber_shouldReturnEmptyList() throws Exception {
        List<ImapResponse> imapResponses = createImapResponseList("* SEARCH A");

        SearchResponse result = SearchResponse.parse(imapResponses);

        assertNotNull(result);
        assertEquals(Collections.emptyList(), result.getNumbers());
    }
}
