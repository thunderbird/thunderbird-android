package net.thunderbird.legacy.core

import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.preference.storage.Storage

class FakeAccountDefaultsProvider : AccountDefaultsProvider {
    override fun applyDefaults(account: LegacyAccountDto) {
        with(account) {
            // Just ensure a working account object is created

            identities = ArrayList<Identity>()

            val identity = Identity(
                signatureUse = false,
                signature = null,
                description = "Fake identity",
            )
            identities.add(identity)
        }
    }

    override fun applyOverwrites(account: LegacyAccountDto, storage: Storage) = Unit
}
