package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.Flow
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

    override fun getAll(): Flow<List<AccountProfile>> {
        return accountManager.observeAll()
            .map { accounts ->
                accounts.map { dto ->
                    dataMapper.toDomain(dto.profile)
                }
            }
    }

    override fun getById(accountId: AccountId): Flow<AccountProfile?> {
        return accountManager.observeById(accountId)
            .map { account ->
                account?.let { dto ->
                    dataMapper.toDomain(dto.profile)
                }
            }
    }

    override suspend fun update(accountProfile: AccountProfile) {
        val accountProfileDto = dataMapper.toDto(accountProfile)

        accountManager.update(accountProfile.id) { account -> account.copy(profile = accountProfileDto) }
    }
}
