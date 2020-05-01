package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.NetworkType

interface ImapStoreConfig {
    val logLabel: String
    fun isSubscribedFoldersOnly(): Boolean
    fun useCompression(type: NetworkType): Boolean
}
