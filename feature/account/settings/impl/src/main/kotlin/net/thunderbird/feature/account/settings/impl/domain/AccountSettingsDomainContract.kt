package net.thunderbird.feature.account.settings.impl.domain

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.ui.setting.Settings
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.profile.AccountAvatar

internal interface AccountSettingsDomainContract {

    interface UseCase {
        fun interface GetAccountName {
            operator fun invoke(accountId: AccountId): Flow<Outcome<String, AccountSettingError>>
        }

        fun interface GetGeneralSettings {
            operator fun invoke(accountId: AccountId): Flow<Outcome<Settings, AccountSettingError>>
        }

        fun interface UpdateGeneralSettings {
            suspend operator fun invoke(
                accountId: AccountId,
                command: UpdateGeneralSettingCommand,
            ): Outcome<Unit, AccountSettingError>
        }

        fun interface ValidateAccountName {
            operator fun invoke(name: String): ValidationOutcome
        }

        fun interface ValidateAvatarMonogram {
            operator fun invoke(monogram: String): ValidationOutcome
        }
    }

    sealed interface UpdateGeneralSettingCommand {
        data class UpdateName(val value: String) : UpdateGeneralSettingCommand
        data class UpdateColor(val value: Int) : UpdateGeneralSettingCommand
        data class UpdateAvatar(val value: AccountAvatar) : UpdateGeneralSettingCommand
    }

    interface ResourceProvider {
        interface GeneralResourceProvider {
            fun profileUi(
                name: String,
                color: Int,
            ): @Composable (Modifier) -> Unit

            val profileIndicatorTitle: () -> String
            val profileIndicatorMonogram: () -> String
            val profileIndicatorImage: () -> String
            val profileIndicatorIcon: () -> String

            val nameTitle: () -> String
            val nameDescription: () -> String?
            val nameIcon: () -> ImageVector?

            val colorTitle: () -> String
            val colorDescription: () -> String?
            val colorIcon: () -> ImageVector?
            val colors: ImmutableList<Int>
        }
    }

    sealed interface AccountSettingError {
        data class NotFound(
            val message: String,
        ) : AccountSettingError
    }
}
