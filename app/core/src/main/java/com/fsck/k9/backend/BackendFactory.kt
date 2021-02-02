package com.fsck.k9.backend

import com.fsck.k9.Account
import com.fsck.k9.backend.api.Backend

interface BackendFactory {
    fun createBackend(account: Account): Backend
}
