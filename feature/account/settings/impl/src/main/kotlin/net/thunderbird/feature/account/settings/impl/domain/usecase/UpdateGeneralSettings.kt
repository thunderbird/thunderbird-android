package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.profile.AccountProfileRepository
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UpdateGeneralSettingCommand
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

internal class UpdateGeneralSettings(
    private val repository: AccountProfileRepository,
) : UseCase.UpdateGeneralSettings {
    override suspend fun invoke(
        accountId: AccountId,
        command: UpdateGeneralSettingCommand,
    ): Outcome<Unit, AccountSettingError> {
        return when (command) {
            is UpdateGeneralSettingCommand.UpdateName -> updateAccountProfile(accountId) {
                copy(name = command.value)
            }

            is UpdateGeneralSettingCommand.UpdateColor -> updateAccountProfile(accountId) {
                copy(color = command.value)
            }

            is UpdateGeneralSettingCommand.UpdateAvatar -> updateAccountProfile(accountId) {
                copy(avatar = command.value)
            }
        }
    }

    private suspend fun updateAccountProfile(
        accountId: AccountId,
        update: AccountProfile.() -> AccountProfile,
    ): Outcome<Unit, AccountSettingError> {
        val accountProfile = repository.getById(accountId).firstOrNull()
            ?: return Outcome.failure(
                AccountSettingError.NotFound(
                    message = "Account profile not found for accountId: $accountId",
                ),
            )
        val updatedAccountProfile = update(accountProfile)

        repository.update(updatedAccountProfile)

        return Outcome.success(Unit)
    }
}
