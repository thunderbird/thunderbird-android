package net.thunderbird.feature.account.profile

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.account.AccountId

interface AccountProfileRepository {

    fun getById(accountId: AccountId): Flow<AccountProfile?>

    suspend fun update(accountProfile: AccountProfile)
}
