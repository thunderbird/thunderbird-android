package net.thunderbird.feature.account.settings.impl.domain.usecase

import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.core.android.account.ShowPictures
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
        return repository.update(accountId) { account -> account.updatedWith(command) }
            ?.let { Outcome.success(Unit) }
            ?: Outcome.failure(AccountSettingsDomainContract.AccountSettingError.NotFound("Account not found"))
    }

    private fun LegacyAccount.updatedWith(command: UpdateReadMessageSettingsCommand): LegacyAccount {
        return when (command) {
            is UpdateReadMessageSettingsCommand.UpdateIsMarkMessageAsReadOnView -> {
                copy(isMarkMessageAsReadOnView = command.value)
            }

            is UpdateReadMessageSettingsCommand.UpdateShowPictures -> {
                when (command.value) {
                    ShowPictures.NEVER.name -> copy(showPictures = ShowPictures.NEVER)
                    ShowPictures.ALWAYS.name -> copy(showPictures = ShowPictures.ALWAYS)
                    ShowPictures.ONLY_FROM_CONTACTS.name -> copy(showPictures = ShowPictures.ONLY_FROM_CONTACTS)
                    else -> this
                }
            }
        }
    }
}
