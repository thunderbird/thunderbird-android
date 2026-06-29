package com.fsck.k9.activity

import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.feature.search.legacy.LocalMessageSearch

internal fun LocalMessageSearch.resolveAccount(
    currentAccount: LegacyAccountDto?,
    accountManager: LegacyAccountDtoManager,
): LegacyAccountDto? {
    return if (searchAllAccounts()) {
        null
    } else {
        accountUuids.singleOrNull()
            ?.let { accountManager.getAccount(it) }
            ?: currentAccount
    }
}
