package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

internal class GetLegacyAccount(
    private val repository: LegacyAccountRepository,
) : UseCase.GetLegacyAccount {
    override suspend fun invoke(
        accountId: AccountId,
    ): Outcome<LegacyAccount, AccountSettingsDomainContract.AccountSettingError> {
        val account = repository.getById(accountId).firstOrNull()
        if (account != null) {
            return Outcome.success(account)
        }
        return Outcome.failure(AccountSettingsDomainContract.AccountSettingError.NotFound("Account not found"))
    }
}
