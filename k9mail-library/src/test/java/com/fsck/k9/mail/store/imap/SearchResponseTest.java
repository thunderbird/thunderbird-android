package com.fsck.k9.mail.store.imap;


import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponse;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class SearchResponseTest {
    @Test
    public void parse_withSearchResponse_shouldExtractNumbers() throws Exception {
        List<ImapResponse> responses = asList(createImapResponse("* SEARCH 1 2 3"),
                createImapResponse("* 23 EXISTS"),
                createImapResponse("* SEARCH 4"));

        SearchResponse result = SearchResponse.parse(responses);

        assertNotNull(result);
        assertEquals(asList(1L, 2L, 3L, 4L), result.getNumbers());
    }

    @Test
    public void parse_withTaggedSearchResponse_shouldReturnEmptyList() throws Exception {
        List<ImapResponse> responses = singletonList(createImapResponse("x SEARCH 1 2 3"));

        SearchResponse result = SearchResponse.parse(responses);

        assertNotNull(result);
        assertEquals(Collections.emptyList(), result.getNumbers());
    }

    @Test
    public void parse_withTooShortResponse_shouldReturnEmptyList() throws Exception {
        List<ImapResponse> responses = singletonList(createImapResponse("* SEARCH"));

        SearchResponse result = SearchResponse.parse(responses);

        assertNotNull(result);
        assertEquals(Collections.emptyList(), result.getNumbers());
    }

    @Test
    public void parse_withoutSearchResponse_shouldReturnEmptyList() throws Exception {
        List<ImapResponse> responses = singletonList(createImapResponse("* 23 EXPUNGE"));

        SearchResponse result = SearchResponse.parse(responses);

        assertNotNull(result);
        assertEquals(Collections.emptyList(), result.getNumbers());
    }

    @Test
    public void parse_withSearchResponseContainingInvalidNumber_shouldReturnEmptyList() throws Exception {
        List<ImapResponse> responses = singletonList(createImapResponse("* SEARCH A"));

        SearchResponse result = SearchResponse.parse(responses);

        assertNotNull(result);
        assertEquals(Collections.emptyList(), result.getNumbers());
    }
}
