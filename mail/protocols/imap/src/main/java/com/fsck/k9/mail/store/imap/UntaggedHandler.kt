package com.fsck.k9.mail.store.imap

import java.io.IOException

internal fun interface UntaggedHandler {
    @Throws(IOException::class)
    fun handleAsyncUntaggedResponse(response: ImapResponse)
}
