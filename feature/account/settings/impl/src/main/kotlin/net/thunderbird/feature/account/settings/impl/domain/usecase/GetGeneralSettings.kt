package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.ui.setting.SettingDecoration
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.profile.AccountProfile
import net.thunderbird.feature.account.profile.AccountProfileRepository
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ResourceProvider
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.SettingsError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsOutcome
import net.thunderbird.feature.account.settings.impl.domain.entity.GeneralPreference
import net.thunderbird.feature.account.settings.impl.domain.entity.generateId

internal class GetGeneralSettings(
    private val repository: AccountProfileRepository,
    private val resourceProvider: ResourceProvider.GeneralResourceProvider,
) : UseCase.GetGeneralSettings {
    override fun invoke(accountId: AccountId): Flow<AccountSettingsOutcome> {
        return repository.getById(accountId).map { profile ->
            if (profile != null) {
                Outcome.success(generateSettings(accountId, profile))
            } else {
                Outcome.failure(
                    SettingsError.NotFound(
                        message = "Account profile not found for accountId: ${accountId.asRaw()}",
                    ),
                )
            }
        }
    }

    private fun generateSettings(accountId: AccountId, profile: AccountProfile): Settings {
        return persistentListOf(
            SettingDecoration.Custom(
                id = GeneralPreference.PROFILE.generateId(accountId),
                customUi = resourceProvider.profileUi(
                    name = profile.name,
                    color = profile.color,
                ),
            ),
            SettingValue.Text(
                id = GeneralPreference.NAME.generateId(accountId),
                title = resourceProvider.nameTitle,
                description = resourceProvider.nameDescription,
                icon = resourceProvider.nameIcon,
                value = profile.name,
            ),
            SettingValue.Color(
                id = GeneralPreference.COLOR.generateId(accountId),
                title = resourceProvider.colorTitle,
                description = resourceProvider.colorDescription,
                icon = resourceProvider.colorIcon,
                value = profile.color,
                colors = resourceProvider.colors,
            ),
        )
    }
}
