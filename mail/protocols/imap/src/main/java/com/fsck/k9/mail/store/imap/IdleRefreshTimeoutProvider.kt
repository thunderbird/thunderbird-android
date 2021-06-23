package com.fsck.k9.mail.store.imap

interface IdleRefreshTimeoutProvider {
    val idleRefreshTimeoutMs: Long
}
