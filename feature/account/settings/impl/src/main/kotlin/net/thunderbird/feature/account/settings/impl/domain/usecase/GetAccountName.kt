package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.profile.AccountProfileRepository
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

internal class GetAccountName(
    private val repository: AccountProfileRepository,
) : UseCase.GetAccountName {

    override fun invoke(accountId: AccountId): Flow<Outcome<String, AccountSettingError>> {
        return repository.getById(accountId).map { profile ->
            if (profile != null) {
                Outcome.success(profile.name)
            } else {
                Outcome.failure(
                    AccountSettingError.NotFound(
                        message = "Account profile not found for accountId: ${accountId.asRaw()}",
                    ),
                )
            }
        }
    }
}
