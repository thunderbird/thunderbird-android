package com.fsck.k9.mail.store.imap.response;


import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.Responses;
import com.fsck.k9.mail.store.imap.command.ImapCommandFactory;

import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;


public class SearchResponse extends BaseResponse {

    private List<Long> numbers;

    private SearchResponse(ImapCommandFactory commandFactory, List<ImapResponse> imapResponse) {
        super(commandFactory, imapResponse);
    }

    public static SearchResponse parse(ImapCommandFactory commandFactory, List<List<ImapResponse>> imapResponses) {

        SearchResponse combinedResponse = null;
        for (List<ImapResponse> imapResponse : imapResponses) {
            SearchResponse searchResponse = new SearchResponse(commandFactory, imapResponse);
            if (combinedResponse == null) {
                combinedResponse = searchResponse;
            } else {
                combinedResponse.combine(searchResponse);
            }
        }

        return combinedResponse;
    }

    @Override
    void parseResponse(List<ImapResponse> imapResponses) {

        numbers = new ArrayList<>();

        for (ImapResponse response : imapResponses) {
            parseSingleLine(response, numbers);
        }
    }

    @Override
    void combine(BaseResponse baseResponse) {
        super.combine(baseResponse);
        SearchResponse searchResponse = (SearchResponse) baseResponse;
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
