package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.profile.AccountProfileRepository

class FakeAccountProfileRepository(
    val profiles: MutableStateFlow<List<AccountProfile>> = MutableStateFlow(emptyList()),
) : AccountProfileRepository {

    override fun getAll(): Flow<List<AccountProfile>> = profiles

    override fun getById(accountId: AccountId): Flow<AccountProfile?> {
        TODO("Not yet implemented")
    }

    override suspend fun update(accountProfile: AccountProfile) {
        TODO("Not yet implemented")
    }
}
