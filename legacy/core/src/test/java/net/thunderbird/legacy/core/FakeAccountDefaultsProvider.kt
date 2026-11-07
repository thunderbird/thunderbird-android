package net.thunderbird.legacy.core

import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.preference.storage.Storage

class FakeAccountDefaultsProvider : AccountDefaultsProvider {
    override fun applyDefaults(account: LegacyAccount): LegacyAccount {
        return account.copy(
            identities = listOf(
                Identity(
                    signatureUse = false,
                    signature = null,
                    description = "Fake identity",
                ),
            ),
        )
    }

    override fun applyOverwrites(account: LegacyAccount, storage: Storage): LegacyAccount {
        return account
    }
}
