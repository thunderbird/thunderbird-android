package net.thunderbird.feature.account.api.profile

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.account.api.AccountId

interface AccountProfileRepository {

    fun getById(accountId: AccountId): Flow<AccountProfile?>

    suspend fun update(accountProfile: AccountProfile)
}
