package com.fsck.k9.mail.store.imap;


import java.util.ArrayList;
import java.util.List;

import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;


class SearchResponse {
    private final List<Long> numbers;


    private SearchResponse(List<Long> numbers) {
        this.numbers = numbers;
    }

    public static SearchResponse parse(List<ImapResponse> responses) {
        List<Long> numbers = new ArrayList<>();

        for (ImapResponse response : responses) {
            parseSingleLine(response, numbers);
        }

        return new SearchResponse(numbers);
    }

    private static void parseSingleLine(ImapResponse response, List<Long> numbers) {
        if (response.isTagged() || response.size() < 2 || !equalsIgnoreCase(response.get(0), Responses.SEARCH)) {
            return;
        }

        int end = response.size();
        for (int i = 1; i < end; i++) {
            try {
                long number = response.getLong(i);
                numbers.add(number);
            } catch (NumberFormatException e) {
                return;
            }
        }
    }

    /**
     * @return A mutable list of numbers from the SEARCH response(s).
     */
    public List<Long> getNumbers() {
        return numbers;
    }
}
