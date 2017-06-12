package com.fsck.k9.mail.store.imap;

import java.util.List;


abstract class SelectedStateResponse {

    SelectedStateResponse(List<ImapResponse> imapResponse) {
        parseResponse(imapResponse);
    }

    abstract void parseResponse(List<ImapResponse> imapResponses);

    abstract void combine(SelectedStateResponse selectedStateResponse);
}
