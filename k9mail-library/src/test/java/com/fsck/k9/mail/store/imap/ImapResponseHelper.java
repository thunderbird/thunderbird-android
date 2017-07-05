package com.fsck.k9.mail.store.imap;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.filter.PeekableInputStream;


public class ImapResponseHelper {

    @SafeVarargs
    public static List<List<ImapResponse>> createMultipleImapResponses(List<String>... responses) throws IOException {
        List<List<ImapResponse>> imapResponses = new ArrayList<>();
        for (List<String> response : responses) {
            imapResponses.add(createImapResponseList(response));
        }
        return imapResponses;
    }

    private static List<ImapResponse> createImapResponseList(List<String> responses) throws IOException {
        List<ImapResponse> imapResponses = new ArrayList<>();
        for (String response : responses) {
            imapResponses.add(createImapResponse(response));
        }
        return imapResponses;
    }

    public static ImapResponse createImapResponse(String response) throws IOException {
        String input = response + "\r\n";
        PeekableInputStream inputStream = new PeekableInputStream(new ByteArrayInputStream(input.getBytes()));
        ImapResponseParser parser = new ImapResponseParser(inputStream);

        return parser.readResponse();
    }

    public static List<Long> createNonContiguousIdSet(long start, long end, int interval) {
        List<Long> ids = new ArrayList<>();
        for (long i = start;i <= end;i += interval) {
            ids.add(i);
        }
        return ids;
    }
}
