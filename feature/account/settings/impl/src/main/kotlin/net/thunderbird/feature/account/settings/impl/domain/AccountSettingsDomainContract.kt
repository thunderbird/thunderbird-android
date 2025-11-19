package net.thunderbird.feature.account.settings.impl.domain

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.profile.AccountProfile

internal interface AccountSettingsDomainContract {

    interface UseCase {
        fun interface GetAccountName {
            operator fun invoke(accountId: AccountId): Flow<Outcome<String, AccountSettingError>>
        }

        fun interface GetAccountProfile {
            operator fun invoke(accountId: AccountId): Flow<Outcome<AccountProfile, AccountSettingError>>
        }

        fun interface UpdateGeneralSettings {
            suspend operator fun invoke(
                accountId: AccountId,
                command: UpdateGeneralSettingCommand,
            ): Outcome<Unit, AccountSettingError>
        }

        fun interface ValidateAccountName {
            operator fun invoke(name: String): Outcome<Unit, ValidateAccountNameError>
        }

        fun interface ValidateAvatarMonogram {
            operator fun invoke(monogram: String): Outcome<Unit, ValidateMonogramError>
        }
    }

    sealed interface UpdateGeneralSettingCommand {
        data class UpdateName(val value: String) : UpdateGeneralSettingCommand
        data class UpdateColor(val value: Int) : UpdateGeneralSettingCommand
        data class UpdateAvatar(val value: Avatar) : UpdateGeneralSettingCommand
    }

    interface ResourceProvider {
        interface GeneralResourceProvider {
            fun profileUi(
                name: String,
                color: Int,
                avatar: Avatar?,
            ): @Composable (Modifier) -> Unit

            val avatarTitle: () -> String
            val avatarDescription: () -> String?
            val avatarOptionMonogram: () -> String
            val avatarOptionImage: () -> String
            val avatarOptionIcon: () -> String

            val nameTitle: () -> String
            val nameDescription: () -> String?
            val nameIcon: () -> ImageVector?

            // Validation error messages
            val nameEmptyError: () -> String
            val nameTooLongError: () -> String

            val monogramTitle: () -> String
            val monogramDescription: () -> String?
            val monogramEmptyError: () -> String
            val monogramTooLongError: () -> String

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

    sealed interface ValidateAccountNameError : ValidationError {
        data object EmptyName : ValidateAccountNameError
        data object TooLongName : ValidateAccountNameError
    }

    sealed interface ValidateMonogramError : ValidationError {
        data object EmptyMonogram : ValidateMonogramError
        data object TooLongMonogram : ValidateMonogramError
    }
}
