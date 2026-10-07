package com.fsck.k9.storage.migrations

import com.fsck.k9.mailstore.MigrationsHelper
import net.thunderbird.core.android.account.LegacyAccount

class FakeMigrationsHelper(
    private var account: LegacyAccount,
) : MigrationsHelper {
    override fun getAccount(): LegacyAccount = account

    override fun saveAccount(account: LegacyAccount) {
        this.account = account
    }
}
