package com.fsck.k9.backend

import app.k9mail.legacy.account.LegacyAccount
import com.fsck.k9.backend.api.Backend

interface BackendFactory {
    fun createBackend(account: LegacyAccount): Backend
}
