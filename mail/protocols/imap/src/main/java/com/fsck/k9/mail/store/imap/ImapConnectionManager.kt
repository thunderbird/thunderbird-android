package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.MessagingException

internal interface ImapConnectionManager {
    @Throws(MessagingException::class)
    fun getConnection(): ImapConnection

    fun releaseConnection(connection: ImapConnection?)
}
