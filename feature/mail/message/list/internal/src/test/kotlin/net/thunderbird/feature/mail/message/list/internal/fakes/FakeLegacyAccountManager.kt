package net.thunderbird.feature.mail.message.list.internal.fakes

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.account.AccountId
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager as SharedFakeLegacyAccountManager

/** Shared [SharedFakeLegacyAccountManager] that also records the accounts observed via [observeById]. */
internal class FakeLegacyAccountManager(
    accounts: List<LegacyAccount> = emptyList(),
) : SharedFakeLegacyAccountManager(accounts) {
    val getByIdFlowCalls = mutableListOf<AccountId>()

    override fun observeById(accountId: AccountId): Flow<LegacyAccount?> {
        getByIdFlowCalls += accountId
        return super.observeById(accountId)
    }
}
