package net.thunderbird.feature.account.settings.impl.domain

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.ui.compose.preference.api.Preference
import net.thunderbird.core.ui.compose.preference.api.PreferenceSetting
import net.thunderbird.feature.account.api.AccountId
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.SettingsError

internal typealias AccountNameOutcome = Outcome<String, SettingsError>
internal typealias AccountSettingsOutcome = Outcome<ImmutableList<Preference>, SettingsError>

internal interface AccountSettingsDomainContract {

    interface UseCase {

        fun interface GetAccountName {
            operator fun invoke(accountId: AccountId): Flow<AccountNameOutcome>
        }

        fun interface GetGeneralPreferences {
            operator fun invoke(accountId: AccountId): Flow<AccountSettingsOutcome>
        }

        fun interface UpdateGeneralPreferences {
            suspend operator fun invoke(
                accountId: AccountId,
                preference: PreferenceSetting<*>,
            ): Outcome<Unit, SettingsError>
        }
    }

    interface ResourceProvider {
        interface GeneralResourceProvider {
            fun profileUi(
                name: String,
                color: Int,
            ): @Composable (Modifier) -> Unit

            val nameTitle: () -> String
            val nameDescription: () -> String?
            val nameIcon: () -> ImageVector?

            val colorTitle: () -> String
            val colorDescription: () -> String?
            val colorIcon: () -> ImageVector?
            val colors: List<Int>
        }
    }

    sealed interface SettingsError {
        data class NotFound(
            val message: String,
        ) : SettingsError
    }
}
