package net.thunderbird.backend.api

import com.fsck.k9.backend.api.BackendStorage
import net.thunderbird.feature.account.AccountId

interface BackendStorageFactory {
    fun createBackendStorage(accountId: AccountId): BackendStorage
}
