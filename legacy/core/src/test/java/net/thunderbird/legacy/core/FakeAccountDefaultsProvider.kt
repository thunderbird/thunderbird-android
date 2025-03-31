package net.thunderbird.legacy.core

import app.k9mail.legacy.account.AccountDefaultsProvider
import app.k9mail.legacy.account.Identity
import app.k9mail.legacy.account.LegacyAccount

class FakeAccountDefaultsProvider : AccountDefaultsProvider {
    override fun applyDefaults(account: LegacyAccount) {
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
}
