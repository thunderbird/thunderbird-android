package com.fsck.k9.backend

import com.fsck.k9.backend.api.Backend
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.core.android.account.LegacyAccountDto

@Deprecated(
    message = "Use net.thunderbird.backend.api.BackendFactory<TAccount : BaseAccount> instead",
    replaceWith = ReplaceWith(
        expression = "BackendFactory<LegacyAccount>",
        "net.thunderbird.backend.api.BackendFactory",
        "net.thunderbird.core.android.account.LegacyAccount",
    ),
)
interface BackendFactory : BackendFactory<LegacyAccountDto> {
    override fun createBackend(account: LegacyAccountDto): Backend
}
