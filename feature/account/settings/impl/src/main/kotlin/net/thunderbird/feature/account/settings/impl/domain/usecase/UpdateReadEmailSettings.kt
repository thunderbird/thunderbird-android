package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.core.android.account.ShowPictures
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UpdateReadMessageSettingsCommand
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

internal class UpdateReadEmailSettings(
    private val repository: LegacyAccountRepository,
) : UseCase.UpdateReadMailSettings {
    override suspend fun invoke(
        accountId: AccountId,
        command: UpdateReadMessageSettingsCommand,
    ): Outcome<Unit, AccountSettingsDomainContract.AccountSettingError> {
        return repository.getById(accountId).firstOrNull()?.let { account: LegacyAccount ->
            when (command) {
                is UpdateReadMessageSettingsCommand.UpdateIsMarkMessageAsReadOnView -> {
                    repository.update(account.copy(isMarkMessageAsReadOnView = command.value))
                }

                is UpdateReadMessageSettingsCommand.UpdateShowPictures -> {
                    when (command.value) {
                        ShowPictures.NEVER.name -> {
                            repository.update(account.copy(showPictures = ShowPictures.NEVER))
                        }

                        ShowPictures.ALWAYS.name -> {
                            repository.update(account.copy(showPictures = ShowPictures.ALWAYS))
                        }

                        ShowPictures.ONLY_FROM_CONTACTS.name -> {
                            repository.update(account.copy(showPictures = ShowPictures.ONLY_FROM_CONTACTS))
                        }
                    }
                }
            }
            Outcome.success(Unit)
        } ?: run { Outcome.failure(AccountSettingsDomainContract.AccountSettingError.NotFound("Account not found")) }
    }
}
