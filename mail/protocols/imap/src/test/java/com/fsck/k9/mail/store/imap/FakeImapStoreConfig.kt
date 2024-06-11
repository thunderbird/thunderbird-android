package com.fsck.k9.mail.store.imap

class FakeImapStoreConfig : ImapStoreConfig {
    var expungeImmediately = true

    override var logLabel: String = "irrelevant"

    override fun isSubscribedFoldersOnly(): Boolean {
        throw UnsupportedOperationException("not implemented")
    }

    override fun isExpungeImmediately(): Boolean = expungeImmediately

    override fun clientInfo(): ImapClientInfo {
        throw UnsupportedOperationException("not implemented")
    }
}
