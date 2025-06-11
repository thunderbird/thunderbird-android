package net.thunderbird.core.android.account

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.account.AccountId

interface LegacyAccountWrapperManager {
    fun getAll(): Flow<List<LegacyAccountWrapper>>

    fun getById(id: AccountId): Flow<LegacyAccountWrapper?>

    suspend fun update(account: LegacyAccountWrapper)
}
