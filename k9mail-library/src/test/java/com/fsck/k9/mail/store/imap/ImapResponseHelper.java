package com.fsck.k9.mail.store.imap;


import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.fsck.k9.mail.filter.PeekableInputStream;


public class ImapResponseHelper {
    public static ImapResponse createImapResponse(String response) throws IOException {
        String input = response + "\r\n";
        PeekableInputStream inputStream = new PeekableInputStream(new ByteArrayInputStream(input.getBytes()));
        ImapResponseParser parser = new ImapResponseParser(inputStream);

        return parser.readResponse();
    }
}
