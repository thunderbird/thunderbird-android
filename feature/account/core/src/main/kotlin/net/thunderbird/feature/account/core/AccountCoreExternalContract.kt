package net.thunderbird.feature.account.core

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.api.profile.AccountProfile

interface AccountCoreExternalContract {

    interface AccountProfileLocalDataSource {
        fun getById(accountId: AccountId): Flow<AccountProfile?>

        suspend fun update(accountProfile: AccountProfile)
    }
}
