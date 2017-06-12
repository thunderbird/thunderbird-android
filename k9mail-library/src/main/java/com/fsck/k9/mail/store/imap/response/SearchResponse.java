package com.fsck.k9.mail.store.imap.response;


import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.Responses;
import com.fsck.k9.mail.store.imap.command.ImapCommandFactory;

import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;


public class SearchResponse extends BaseResponse {

    private final List<Long> numbers;

    private SearchResponse(ImapCommandFactory commandFactory, List<Long> numbers) {
        super(commandFactory);
        this.numbers = numbers;
    }

    public static SearchResponse parseMultiple(ImapCommandFactory commandFactory, List<List<ImapResponse>> responsesList) {

        List<Long> numbers = new ArrayList<>();

        //Currently searching is done only on the basis of either uids or sequence numbers
        //So it is ok to take the union of all the responses
        for (List<ImapResponse> responseList : responsesList) {
            for (ImapResponse response : responseList) {
                parseSingleLine(response, numbers);
            }
        }

        return new SearchResponse(commandFactory, numbers);
    }

    public static SearchResponse parse(ImapCommandFactory commandFactory, List<ImapResponse> responses) {

        List<Long> numbers = new ArrayList<>();

        for (ImapResponse response : responses) {
            parseSingleLine(response, numbers);
        }

        return new SearchResponse(commandFactory, numbers);
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
