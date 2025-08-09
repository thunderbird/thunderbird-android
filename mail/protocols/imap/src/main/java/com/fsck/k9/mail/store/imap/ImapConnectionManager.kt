package com.fsck.k9.mail.store.imap

import net.thunderbird.core.common.exception.MessagingException

internal interface ImapConnectionManager {
    @Throws(MessagingException::class)
    fun getConnection(): ImapConnection

    fun releaseConnection(connection: ImapConnection?)
}
