package com.fsck.k9.backend.imap

import com.fsck.k9.mail.store.imap.IdleRefreshManager
import com.fsck.k9.mail.store.imap.IdleRefreshTimer

class BackendIdleRefreshManager : IdleRefreshManager {
    override fun startTimer(timeout: Long, callback: () -> Unit): IdleRefreshTimer {
        TODO("implement")
    }

    override fun resetTimers() {
        TODO("implement")
    }
}
