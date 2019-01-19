package com.fsck.k9.mail.store.imap;

import java.io.IOException;

interface UntaggedHandler {
    void handleAsyncUntaggedResponse(ImapResponse response) throws IOException;
}
