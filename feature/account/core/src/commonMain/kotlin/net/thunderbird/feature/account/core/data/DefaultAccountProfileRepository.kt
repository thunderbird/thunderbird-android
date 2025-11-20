package net.thunderbird.feature.account.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.core.AccountCoreExternalContract.AccountProfileLocalDataSource
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.profile.AccountProfileRepository

class DefaultAccountProfileRepository(
    private val localDataSource: AccountProfileLocalDataSource,
) : AccountProfileRepository {

    override fun getAll(): Flow<List<AccountProfile>> {
        return localDataSource.getAll()
            .distinctUntilChanged()
    }

    override fun getById(accountId: AccountId): Flow<AccountProfile?> {
        return localDataSource.getById(accountId)
            .distinctUntilChanged()
    }

    override suspend fun update(accountProfile: AccountProfile) {
        localDataSource.update(accountProfile)
    }
}
