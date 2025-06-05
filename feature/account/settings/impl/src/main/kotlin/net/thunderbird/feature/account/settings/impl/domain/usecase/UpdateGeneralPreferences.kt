package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.profile.AccountProfileRepository
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.SettingsError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.domain.entity.GeneralPreference
import net.thunderbird.feature.account.settings.impl.domain.entity.generateId

internal class UpdateGeneralPreferences(
    private val repository: AccountProfileRepository,
) : UseCase.UpdateGeneralPreferences {
    override suspend fun invoke(
        accountId: AccountId,
        preference: PreferenceSetting<*>,
    ): Outcome<Unit, SettingsError> {
        return when (preference.id) {
            GeneralPreference.NAME.generateId(accountId) -> {
                updateAccountProfile(accountId) {
                    copy(name = preference.value as String)
                }
            }

            GeneralPreference.COLOR.generateId(accountId) -> {
                updateAccountProfile(accountId) {
                    copy(color = preference.value as Int)
                }
            }

            else -> Outcome.failure(
                SettingsError.NotFound(
                    message = "Unknown preference id: ${preference.id}",
                ),
            )
        }
    }

    private suspend fun updateAccountProfile(
        accountId: AccountId,
        update: AccountProfile.() -> AccountProfile,
    ): Outcome<Unit, SettingsError> {
        val accountProfile = repository.getById(accountId).firstOrNull()
            ?: return Outcome.failure(
                SettingsError.NotFound(
                    message = "Account profile not found for accountId: $accountId",
                ),
            )
        val updatedAccountProfile = update(accountProfile)

        repository.update(updatedAccountProfile)

        return Outcome.success(Unit)
    }
}
