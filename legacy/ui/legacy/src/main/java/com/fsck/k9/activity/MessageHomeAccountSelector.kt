package com.fsck.k9.activity

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.search.legacy.LocalMessageSearch

internal fun LocalMessageSearch.resolveAccount(
    currentAccount: LegacyAccount?,
    accountManager: LegacyAccountManager,
): LegacyAccount? {
    return if (searchAllAccounts()) {
        null
    } else {
        accountIds.singleOrNull()
            ?.let { accountManager.findById(it) }
            ?: currentAccount
    }
}
