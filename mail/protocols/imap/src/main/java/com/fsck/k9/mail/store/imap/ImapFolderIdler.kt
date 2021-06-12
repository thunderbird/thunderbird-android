package com.fsck.k9.mail.store.imap

interface ImapFolderIdler {
    fun idle(): IdleResult

    fun stop()
}

enum class IdleResult {
    SYNC,
    STOPPED,
    NOT_SUPPORTED
}
