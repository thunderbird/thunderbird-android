package com.fsck.k9.backend

import app.k9mail.legacy.account.Account
import com.fsck.k9.backend.api.Backend

interface BackendFactory {
    fun createBackend(account: Account): Backend
}
