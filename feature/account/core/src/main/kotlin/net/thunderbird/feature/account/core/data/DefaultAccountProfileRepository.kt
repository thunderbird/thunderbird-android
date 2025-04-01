package net.thunderbird.feature.account.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.api.profile.AccountProfile
import net.thunderbird.feature.account.api.profile.AccountProfileRepository
import net.thunderbird.feature.account.core.AccountCoreExternalContract.AccountProfileLocalDataSource

class DefaultAccountProfileRepository(
    private val localDataSource: AccountProfileLocalDataSource,
) : AccountProfileRepository {

    override fun getById(accountId: AccountId): Flow<AccountProfile?> {
        return localDataSource.getById(accountId)
            .distinctUntilChanged()
    }

    override suspend fun update(accountProfile: AccountProfile) {
        localDataSource.update(accountProfile)
    }
}
