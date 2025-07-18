package com.fsck.k9.mail.store.imap;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fsck.k9.mail.filter.PeekableInputStream;


public class ImapResponseHelper {
    public static List<ImapResponse> createImapResponseList(String... responses) throws IOException {
        List<ImapResponse> imapResponses = new ArrayList<>();
        for (String response : responses) {
            imapResponses.add(createImapResponse(response));
        }
        return imapResponses;
    }

    public static ImapResponse createImapResponse(String response) throws IOException {
        return createImapResponse(response, false);
    }

    public static ImapResponse createImapResponse(String response, boolean utf8) throws IOException {
        String input = response + "\r\n";
        PeekableInputStream inputStream = new PeekableInputStream(new ByteArrayInputStream(input.getBytes()));
        ImapResponseParser parser = new ImapResponseParser(inputStream, new FolderNameCodec());
        parser.setUtf8Accepted(utf8);

        return parser.readResponse();
    }

    public static Set<Long> createNonContiguousIdSet(long start, long end, int interval) {
        Set<Long> ids = new HashSet<>();
        for (long i = start;i <= end;i += interval) {
            ids.add(i);
        }
        return ids;
    }
}
