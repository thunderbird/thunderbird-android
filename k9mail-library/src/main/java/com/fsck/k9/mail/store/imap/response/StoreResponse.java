package com.fsck.k9.mail.store.imap.response;


import java.util.List;

import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.command.ImapCommandFactory;


public class StoreResponse extends BaseResponse {

    private StoreResponse(ImapCommandFactory commandFactory, List<ImapResponse> imapResponse) {
        super(commandFactory, imapResponse);
    }

    @Override
    void parseResponse(List<ImapResponse> imapResponses) {
        //Not used
    }

    public static StoreResponse parse(ImapCommandFactory commandFactory, List<List<ImapResponse>> imapResponses) {
        StoreResponse combinedResponse = null;
        for (List<ImapResponse> imapResponse : imapResponses) {
            StoreResponse searchResponse = new StoreResponse(commandFactory, imapResponse);
            if (combinedResponse == null) {
                combinedResponse = searchResponse;
            } else {
                combinedResponse.combine(searchResponse);
            }
        }

        return combinedResponse;
    }
}
