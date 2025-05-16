package net.thunderbird.backend.api

import com.fsck.k9.backend.api.BackendStorage
import net.thunderbird.feature.mail.account.api.BaseAccount

interface BackendStorageFactory<in TAccount : BaseAccount> {
    fun createBackendStorage(account: TAccount): BackendStorage
}
