package com.fsck.k9.mail.store.imap;

interface UntaggedHandler {
    void handleAsyncUntaggedResponse(ImapResponse response);
}
