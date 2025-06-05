package net.thunderbird.feature.account.core

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.profile.AccountProfile

interface AccountCoreExternalContract {

    interface AccountProfileLocalDataSource {
        fun getById(accountId: AccountId): Flow<AccountProfile?>

        suspend fun update(accountProfile: AccountProfile)
    }
}
