package com.fsck.k9.mailstore

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

class DefaultMigrationHelper(
    private val accountId: AccountId,
    private val accountManager: LegacyAccountManager,
): MigrationsHelper {
    override fun getAccount(): LegacyAccount {
        return accountManager.findById(accountId) ?: error("Account $accountId not found")
    }

    override fun saveAccount(account: LegacyAccount) {
        accountManager.updateSync(account)
    }
}
