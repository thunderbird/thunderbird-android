package com.fsck.k9.backend

import com.fsck.k9.backend.api.Backend
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.core.android.account.LegacyAccount

@Deprecated(
    message = "Use net.thunderbird.backend.api.BackendFactory<TAccount : BaseAccount> instead",
    replaceWith = ReplaceWith(
        expression = "BackendFactory<LegacyAccount>",
        "net.thunderbird.backend.api.BackendFactory",
        "net.thunderbird.core.android.account.LegacyAccount",
    ),
)
interface BackendFactory : BackendFactory<LegacyAccount> {
    override fun createBackend(account: LegacyAccount): Backend
}
