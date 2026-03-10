package net.thunderbird.backend.api

import com.fsck.k9.backend.api.Backend
import net.thunderbird.feature.account.AccountId

interface BackendFactory {
    fun createBackend(accountId: AccountId): Backend
}
