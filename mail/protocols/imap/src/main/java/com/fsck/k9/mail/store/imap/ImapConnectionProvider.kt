package com.fsck.k9.mail.store.imap

internal interface ImapConnectionProvider {
    fun getConnection(folder: ImapFolder): ImapConnection?
}
