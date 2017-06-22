package com.fsck.k9.mail.store.imap.selectedstate.response;

import java.util.List;

import com.fsck.k9.mail.store.imap.ImapResponse;


public abstract class SelectedStateResponse {

    SelectedStateResponse(List<ImapResponse> imapResponse) {
        parseResponse(imapResponse);
    }

    abstract void parseResponse(List<ImapResponse> imapResponses);

    abstract void combine(SelectedStateResponse selectedStateResponse);
}
