package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.api.profile.AccountProfileRepository
import net.thunderbird.feature.account.settings.impl.domain.AccountNameOutcome
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

internal class GetAccountName(
    private val repository: AccountProfileRepository,
) : UseCase.GetAccountName {

    override fun invoke(accountId: AccountId): Flow<AccountNameOutcome> {
        return repository.getById(accountId).map { profile ->
            if (profile != null) {
                Outcome.success(profile.name)
            } else {
                Outcome.failure(
                    AccountSettingsDomainContract.SettingsError.NotFound(
                        message = "Account profile not found for accountId: ${accountId.value}",
                    ),
                )
            }
        }
    }
}
