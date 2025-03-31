package net.thunderbird.legacy.core

import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.AccountDefaultsProvider
import app.k9mail.legacy.account.Identity

class FakeAccountDefaultsProvider : AccountDefaultsProvider {
    override fun applyDefaults(account: Account) {
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
