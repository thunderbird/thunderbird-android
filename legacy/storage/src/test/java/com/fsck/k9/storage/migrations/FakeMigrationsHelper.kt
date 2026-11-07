package com.fsck.k9.storage.migrations

import com.fsck.k9.mailstore.MigrationsHelper
import net.thunderbird.core.android.account.LegacyAccount

class FakeMigrationsHelper(
    override var account: LegacyAccount,
) : MigrationsHelper {
    override fun updateAccount(transform: (LegacyAccount) -> LegacyAccount) {
        account = transform(account)
    }
}
