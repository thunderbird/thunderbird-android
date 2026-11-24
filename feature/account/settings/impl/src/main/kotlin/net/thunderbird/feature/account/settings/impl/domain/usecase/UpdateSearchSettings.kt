package net.thunderbird.feature.account.settings.impl.domain.usecase

import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountRepository
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
        return repository.update(accountId) { account: LegacyAccount ->
            when (command) {
                is UpdateSearchSettingsCommand.UpdateServerSearchLimit -> {
                    account.copy(remoteSearchNumResults = command.value)
                }
            }
        }
            ?.let { Outcome.success(Unit) }
            ?: Outcome.failure(AccountSettingsDomainContract.AccountSettingError.NotFound("Account not found"))
    }
}
