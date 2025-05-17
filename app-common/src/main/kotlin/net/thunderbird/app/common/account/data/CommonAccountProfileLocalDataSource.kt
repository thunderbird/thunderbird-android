package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import net.thunderbird.core.android.account.LegacyAccountWrapperManager
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.api.profile.AccountProfile
import net.thunderbird.feature.account.core.AccountCoreExternalContract.AccountProfileLocalDataSource

internal class CommonAccountProfileLocalDataSource(
    private val accountManager: LegacyAccountWrapperManager,
) : AccountProfileLocalDataSource {

    override fun getById(accountId: AccountId): Flow<AccountProfile?> {
        return accountManager.getById(accountId.value)
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
