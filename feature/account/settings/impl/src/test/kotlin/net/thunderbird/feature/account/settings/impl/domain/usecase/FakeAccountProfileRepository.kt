package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.profile.AccountProfileRepository

internal class FakeAccountProfileRepository(
    initialAccountProfile: AccountProfile? = null,
) : AccountProfileRepository {

    private val accountProfileState = MutableStateFlow<AccountProfile?>(initialAccountProfile)
    private val accountProfile: StateFlow<AccountProfile?> = accountProfileState

    override fun getById(accountId: AccountId): Flow<AccountProfile?> {
        return accountProfile
    }

    override suspend fun update(accountProfile: AccountProfile) {
        accountProfileState.update {
            accountProfile
        }
    }
}
