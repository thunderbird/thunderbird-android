package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.Flag

internal interface InternalImapStore {
    val logLabel: String
    val config: ImapStoreConfig

    /**
     * The IMAP prefix combined with the Path delimiter given by the server.
     */
    val combinedPrefix: String?

    fun getPermanentFlagsIndex(): MutableSet<Flag>
}
