package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import net.thunderbird.core.android.account.LegacyAccountWrapperManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.core.AccountCoreExternalContract.AccountProfileLocalDataSource
import net.thunderbird.feature.account.profile.AccountProfile

internal class CommonAccountProfileLocalDataSource(
    private val accountManager: LegacyAccountWrapperManager,
) : AccountProfileLocalDataSource {

    override fun getById(accountId: AccountId): Flow<AccountProfile?> {
        return accountManager.getById(accountId.asRaw())
            .map { account ->
                account?.let {
                    AccountProfile(
                        id = AccountIdFactory.create(account.uuid),
                        name = account.displayName,
                        color = account.chipColor,
                    )
                }
            }
    }

    override suspend fun update(accountProfile: AccountProfile) {
        val currentAccount = accountManager.getById(accountProfile.id.asRaw())
            .firstOrNull() ?: return

        val updatedAccount = currentAccount.copy(
            displayName = accountProfile.name,
            chipColor = accountProfile.color,
        )

        accountManager.update(updatedAccount)
    }
}
