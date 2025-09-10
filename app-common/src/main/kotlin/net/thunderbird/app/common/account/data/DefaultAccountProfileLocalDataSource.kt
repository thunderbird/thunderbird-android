package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.core.AccountCoreExternalContract.AccountProfileLocalDataSource
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.storage.mapper.AccountProfileDataMapper

internal class DefaultAccountProfileLocalDataSource(
    private val accountManager: LegacyAccountManager,
    private val dataMapper: AccountProfileDataMapper,
) : AccountProfileLocalDataSource {

    override fun getById(accountId: AccountId): Flow<AccountProfile?> {
        return accountManager.getById(accountId)
            .map { account ->
                account?.let { dto ->
                    dataMapper.toDomain(dto.profile)
                }
            }
    }

    override suspend fun update(accountProfile: AccountProfile) {
        val currentAccount = accountManager.getById(accountProfile.id)
            .firstOrNull() ?: return

        val accountProfile = dataMapper.toDto(accountProfile)

        val updatedAccount = currentAccount.copy(
            profile = accountProfile,
        )

        accountManager.update(updatedAccount)
    }
}
