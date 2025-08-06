package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.profile.AccountAvatar
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.profile.AccountProfileRepository
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.SettingsError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.domain.entity.GeneralPreference
import net.thunderbird.feature.account.settings.impl.domain.entity.generateId

internal class UpdateGeneralSettings(
    private val repository: AccountProfileRepository,
) : UseCase.UpdateGeneralSettings {
    override suspend fun invoke(
        accountId: AccountId,
        setting: SettingValue<*>,
    ): Outcome<Unit, SettingsError> {
        return when (setting.id) {
            GeneralPreference.PROFILE_INDICATOR.generateId(accountId) -> {
                val avatar = setting.value as AccountAvatar
                updateAccountProfile(accountId) {
                    copy(avatar = avatar)
                }
            }

            GeneralPreference.NAME.generateId(accountId) -> {
                updateAccountProfile(accountId) {
                    copy(name = setting.value as String)
                }
            }

            GeneralPreference.COLOR.generateId(accountId) -> {
                updateAccountProfile(accountId) {
                    copy(color = setting.value as Int)
                }
            }

            else -> Outcome.failure(
                SettingsError.NotFound(
                    message = "Unknown setting id: ${setting.id}",
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
