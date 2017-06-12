package com.fsck.k9.mail.store.imap;


import java.util.Collections;
import java.util.List;

import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.UidSearchResponse;
import org.junit.Test;

import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponse;
import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createMultipleImapResponses;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class UidSearchResponseTest {

    private static final List<String> SEARCH_RESPONSE_1 = asList("* SEARCH 1 2 3",
            "* 23 EXISTS",
            "* SEARCH 4");
    private static final List<String> SEARCH_RESPONSE_2 = asList("* SEARCH 5 6",
            "* 19 EXPUNGED",
            "* SEARCH 7");
    private static final List<String> SEARCH_RESPONSE_3 = singletonList("* SEARCH 8");
    private static final List<String> SEARCH_RESPONSE_TAGGED = singletonList("x SEARCH 7 8 9");
    private static final List<String> SEARCH_RESPONSE_SHORT = singletonList("* SEARCH");
    private static final List<String> SEARCH_RESPONSE_NONE = singletonList("* 23 EXPUNGE");
    private static final List<String> SEARCH_RESPONSE_INVALID = singletonList("* SEARCH A");

    private static final List<Long> SEARCH_RESPONSE_1_NUMBERS = asList(1L, 2L, 3L, 4L);
    private static final List<Long> SEARCH_RESPONSE_123_NUMBERS = asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
    private static final List<Long> SEARCH_RESPONSE_12_NUMBERS = asList(1L, 2L, 3L, 4L, 5L, 6L, 7L);

    @Test
    public void parse_withSingleSearchResponse_shouldExtractNumbers() throws Exception {
        List<List<ImapResponse>> responses = createMultipleImapResponses(SEARCH_RESPONSE_1);

        UidSearchResponse result = UidSearchResponse.parse(responses);

        assertNotNull(result);
        assertEquals(SEARCH_RESPONSE_1_NUMBERS, result.getNumbers());
    }

    @Test
    public void parse_withMultipleSearchResponses_shouldExtractNumbers() throws Exception {
        List<List<ImapResponse>> responses = createMultipleImapResponses(SEARCH_RESPONSE_1, SEARCH_RESPONSE_2,
                SEARCH_RESPONSE_3);

        UidSearchResponse result = UidSearchResponse.parse(responses);

        assertNotNull(result);
        assertEquals(SEARCH_RESPONSE_123_NUMBERS, result.getNumbers());
    }

    @Test
    public void parse_withSingleTaggedSearchResponse_shouldReturnEmptyList() throws Exception {
        List<List<ImapResponse>> responses = createMultipleImapResponses(SEARCH_RESPONSE_TAGGED);

        UidSearchResponse result = UidSearchResponse.parse(responses);

        assertNotNull(result);
        assertEquals(Collections.emptyList(), result.getNumbers());
    }

    @Test
    public void parse_withMultipleSearchResponsesAndSingleTaggedSearchResponse_shouldExtractNumbers() throws Exception {
        List<List<ImapResponse>> responses = createMultipleImapResponses(SEARCH_RESPONSE_1, SEARCH_RESPONSE_2,
                SEARCH_RESPONSE_TAGGED);

        UidSearchResponse result = UidSearchResponse.parse(responses);

        assertNotNull(result);
        assertEquals(SEARCH_RESPONSE_12_NUMBERS, result.getNumbers());
    }

    @Test
    public void parse_withSingleTooShortResponse_shouldReturnEmptyList() throws Exception {
        List<List<ImapResponse>> responses = createMultipleImapResponses(SEARCH_RESPONSE_SHORT);

        UidSearchResponse result = UidSearchResponse.parse(responses);

        assertNotNull(result);
        assertEquals(Collections.emptyList(), result.getNumbers());
    }

    @Test
    public void parse_withMultipleSearchResponsesAndSingleTooShortResponse_shouldExtractNumbers() throws Exception {
        List<List<ImapResponse>> responses = createMultipleImapResponses(SEARCH_RESPONSE_1, SEARCH_RESPONSE_2,
                SEARCH_RESPONSE_SHORT);

        UidSearchResponse result = UidSearchResponse.parse(responses);

        assertNotNull(result);
        assertEquals(SEARCH_RESPONSE_12_NUMBERS, result.getNumbers());
    }

    @Test
    public void parse_withSingleNoSearchResponse_shouldReturnEmptyList() throws Exception {
        List<List<ImapResponse>> responses = createMultipleImapResponses(SEARCH_RESPONSE_NONE);

        UidSearchResponse result = UidSearchResponse.parse(responses);

        assertNotNull(result);
        assertEquals(Collections.emptyList(), result.getNumbers());
    }

    @Test
    public void parse_withMultipleSearchResponsesAndSingleNoSearchResponse_shouldExtractNumbers() throws Exception {
        List<List<ImapResponse>> responses = createMultipleImapResponses(SEARCH_RESPONSE_1, SEARCH_RESPONSE_2,
                SEARCH_RESPONSE_NONE);

        UidSearchResponse result = UidSearchResponse.parse(responses);

        assertNotNull(result);
        assertEquals(SEARCH_RESPONSE_12_NUMBERS, result.getNumbers());
    }

    @Test
    public void parse_withSingleSearchResponseContainingInvalidNumber_shouldReturnEmptyList() throws Exception {
        List<List<ImapResponse>> responses = singletonList(singletonList(createImapResponse("* SEARCH A")));

        UidSearchResponse result = UidSearchResponse.parse(responses);

        assertNotNull(result);
        assertEquals(Collections.emptyList(), result.getNumbers());
    }

    @Test
    public void parse_withMultipleSearchResponsesAndSingleSearchResponseContainingInvalidNumber_shouldExtractNumbers()
            throws Exception {
        List<List<ImapResponse>> responses = createMultipleImapResponses(SEARCH_RESPONSE_1, SEARCH_RESPONSE_2,
                SEARCH_RESPONSE_INVALID);

        UidSearchResponse result = UidSearchResponse.parse(responses);

        assertNotNull(result);
        assertEquals(SEARCH_RESPONSE_12_NUMBERS, result.getNumbers());
    }

}
