package com.fsck.k9.backend

import app.k9mail.legacy.account.LegacyAccount
import com.fsck.k9.backend.api.Backend
import net.thunderbird.backend.api.BackendFactory

@Deprecated(
    message = "Use net.thunderbird.backend.api.BackendFactory<TAccount : BaseAccount> instead",
    replaceWith = ReplaceWith(
        expression = "BackendFactory<LegacyAccount>",
        "net.thunderbird.backend.api.BackendFactory",
        "app.k9mail.legacy.account.LegacyAccount",
    ),
)
interface BackendFactory : BackendFactory<LegacyAccount> {
    override fun createBackend(account: LegacyAccount): Backend
}
