package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UpdateSearchSettingsCommand
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

internal class UpdateSearchSettings(
    private val repository: LegacyAccountRepository,
) : UseCase.UpdateSearchSettings {
    override suspend fun invoke(
        accountId: AccountId,
        command: UpdateSearchSettingsCommand,
    ): Outcome<Unit, AccountSettingsDomainContract.AccountSettingError> {
        return repository.getById(accountId).firstOrNull()?.let { account: LegacyAccount ->
            when (command) {
                is UpdateSearchSettingsCommand.UpdateServerSearchLimit -> {
                    repository.update(account.copy(remoteSearchNumResults = command.value))
                }
            }
            Outcome.success(Unit)
        } ?: run { Outcome.failure(AccountSettingsDomainContract.AccountSettingError.NotFound("Account not found")) }
    }
}
