package com.fsck.k9.mail.store.imap;


import java.util.ArrayList;
import java.util.List;

import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;


class UidSearchResponse extends SelectedStateResponse {
    private List<Long> numbers;

    private UidSearchResponse(List<ImapResponse> imapResponse) {
        super(imapResponse);
    }

    public static UidSearchResponse parse(List<ImapResponse> imapResponses) {
        return new UidSearchResponse(imapResponses);
    }

    @Override
    void parseResponse(List<ImapResponse> imapResponses) {
        numbers = new ArrayList<>();
        for (ImapResponse response : imapResponses) {
            parseSingleLine(response, numbers);
        }
    }

    @Override
    void combine(SelectedStateResponse selectedStateResponse) {
        UidSearchResponse searchResponse = (UidSearchResponse) selectedStateResponse;
        this.numbers.addAll(searchResponse.getNumbers());
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
