package net.thunderbird.feature.account.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.core.AccountCoreExternalContract
import net.thunderbird.feature.account.profile.AccountProfile

open class FakeAccountProfileDataSource(
    val profiles: MutableStateFlow<List<AccountProfile>> = MutableStateFlow(emptyList()),
) : AccountCoreExternalContract.AccountProfileLocalDataSource {

    override fun getAll(): Flow<List<AccountProfile>> = profiles

    override fun getById(accountId: AccountId): Flow<AccountProfile?> {
        return profiles.map { list ->
            list.find { it.id == accountId }
        }
    }

    override suspend fun update(accountProfile: AccountProfile) {
        TODO("Not yet implemented")
    }
}
