package net.thunderbird.backend.api

import com.fsck.k9.backend.api.Backend
import net.thunderbird.core.account.BaseAccount

interface BackendFactory<TAccount : BaseAccount> {
    fun createBackend(account: TAccount): Backend
}
