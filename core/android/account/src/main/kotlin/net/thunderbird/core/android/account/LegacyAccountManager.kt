package net.thunderbird.core.android.account

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.account.api.AccountManager

interface LegacyAccountManager : AccountManager<LegacyAccount> {
    fun getAll(): Flow<List<LegacyAccount>>

    fun getById(id: AccountId): Flow<LegacyAccount?>

    suspend fun update(account: LegacyAccount)
}
