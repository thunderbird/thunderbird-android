package net.thunderbird.feature.account.settings.impl.domain

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.ui.compose.preference.api.Preference
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.SettingsError

internal typealias AccountSettingsOutcome = Outcome<ImmutableList<Preference>, SettingsError>

internal interface AccountSettingsDomainContract {

    interface UseCase {

        fun interface GetGeneralPreferences {
            operator fun invoke(accountId: AccountId): Flow<AccountSettingsOutcome>
        }
    }

    interface ResourceProvider {
        interface GeneralResourceProvider {
            val nameTitle: () -> String
            val nameDescription: () -> String?
            val nameIcon: () -> ImageVector?
        }
    }

    sealed interface SettingsError {
        data object NoSettingsAvailable : SettingsError
    }
}
