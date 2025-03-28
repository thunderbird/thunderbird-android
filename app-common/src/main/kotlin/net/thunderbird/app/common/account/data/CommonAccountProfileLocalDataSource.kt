package net.thunderbird.app.common.account.data

import app.k9mail.legacy.account.LegacyAccountWrapperManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.api.profile.AccountProfile
import net.thunderbird.feature.account.core.AccountCoreExternalContract.AccountProfileLocalDataSource

class CommonAccountProfileLocalDataSource(
    private val accountManager: LegacyAccountWrapperManager,
) : AccountProfileLocalDataSource {

    override fun getById(accountId: AccountId): Flow<AccountProfile?> {
        return accountManager.getById(accountId.value)
            .onEach { println("Flow emitted account: $it") }
            .map { account ->
                account?.let {
                    AccountProfile(
                        accountId = AccountId.from(account.uuid),
                        name = account.displayName,
                        color = account.chipColor,
                    )
                }
            }
    }

    override suspend fun update(accountProfile: AccountProfile) {
        val currentAccount = accountManager.getById(accountProfile.accountId.value)
            .firstOrNull() ?: return

        val updatedAccount = currentAccount.copy(
            displayName = accountProfile.name,
            chipColor = accountProfile.color,
        )

        accountManager.update(updatedAccount)
    }
}
