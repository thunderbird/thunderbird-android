package com.fsck.k9.mailstore

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

class DefaultMigrationHelper(
    private val accountId: AccountId,
    private val accountManager: LegacyAccountManager,
): MigrationsHelper {
    override val account: LegacyAccount
        get() = accountManager.findById(accountId) ?: error("Account $accountId not found")

    override fun updateAccount(transform: (LegacyAccount) -> LegacyAccount) {
        accountManager.updateSync(accountId, transform) ?: error("Account $accountId not found")
    }
}
