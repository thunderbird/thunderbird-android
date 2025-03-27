package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.ui.compose.preference.api.Preference
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.api.profile.AccountProfile
import net.thunderbird.feature.account.api.profile.AccountProfileRepository
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.ResourceProvider
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.SettingsError.NoSettingsAvailable
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsOutcome

internal class GetGeneralPreferences(
    private val repository: AccountProfileRepository,
    private val resourceProvider: ResourceProvider.GeneralResourceProvider,
) : UseCase.GetGeneralPreferences {
    override fun invoke(accountId: AccountId): Flow<AccountSettingsOutcome> {
        return repository.getById(accountId).map { profile ->
            if (profile != null) {
                Outcome.success(generatePreferences(accountId, profile))
            } else {
                Outcome.failure(NoSettingsAvailable)
            }
        }
    }

    private fun generatePreferences(accountId: AccountId, profile: AccountProfile): ImmutableList<Preference> {
        return persistentListOf(
            PreferenceSetting.Text(
                id = generateId(accountId, GENERAL_NAME_ID),
                title = resourceProvider.nameTitle,
                description = resourceProvider.nameDescription,
                icon = resourceProvider.nameIcon,
                value = profile.name,
            ),
        )
    }

    private fun generateId(accountId: AccountId, preferenceId: String): String {
        return "${accountId.value}-$preferenceId"
    }

    private companion object {
        const val GENERAL_NAME_ID = "general-name"
    }
}
