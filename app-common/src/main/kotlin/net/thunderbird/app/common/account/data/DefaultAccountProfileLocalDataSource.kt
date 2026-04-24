package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.account.core.AccountCoreExternalContract.AccountProfileLocalDataSource
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.storage.mapper.AccountProfileDataMapper

internal class DefaultAccountProfileLocalDataSource(
    private val accountManager: LegacyAccountManager,
    private val dataMapper: AccountProfileDataMapper,
    private val avatarMonogramCreator: AvatarMonogramCreator,
) : AccountProfileLocalDataSource {

    override fun getAll(): Flow<List<AccountProfile>> {
        return accountManager.getAll()
            .map { accounts ->
                accounts.map { dto ->
                    dataMapper.toDomain(dto.profile).regenerateMonogramIfNeeded(dto)
                }
            }
    }

    override fun getById(accountId: AccountId): Flow<AccountProfile?> {
        return accountManager.getById(accountId)
            .map { account ->
                account?.let { dto ->
                    dataMapper.toDomain(dto.profile).regenerateMonogramIfNeeded(dto)
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

    private suspend fun AccountProfile.regenerateMonogramIfNeeded(dto: LegacyAccount): AccountProfile {
        return when (val avatar = avatar) {
            is Avatar.Monogram if avatar.value == AvatarMonogramCreator.AVATAR_MONOGRAM_DEFAULT -> {
                val monogram = avatarMonogramCreator.create(dto.name, dto.email)
                copy(avatar = Avatar.Monogram(monogram)).also {
                    update(it)
                }
            }

            else -> this
        }
    }
}
