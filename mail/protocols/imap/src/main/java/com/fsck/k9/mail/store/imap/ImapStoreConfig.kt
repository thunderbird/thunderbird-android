package com.fsck.k9.mail.store.imap

interface ImapStoreConfig {
    val logLabel: String
    fun isSubscribedFoldersOnly(): Boolean
    fun useCompression(): Boolean
}
