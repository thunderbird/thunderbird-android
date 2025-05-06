package net.thunderbird.backend.api

import com.fsck.k9.backend.api.BackendStorage
import net.thunderbird.core.account.BaseAccount

interface BackendStorageFactory<in TAccount : BaseAccount> {
    fun createBackendStorage(account: TAccount): BackendStorage
}
