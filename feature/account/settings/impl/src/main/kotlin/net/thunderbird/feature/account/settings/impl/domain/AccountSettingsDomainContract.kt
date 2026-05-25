package net.thunderbird.feature.account.settings.impl.domain

import com.eygraber.uri.Uri
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.LegacyAccount
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

        fun interface GetLegacyAccount {
            suspend operator fun invoke(accountId: AccountId): Outcome<LegacyAccount, AccountSettingError>
        }

        fun interface UpdateGeneralSettings {
            suspend operator fun invoke(
                accountId: AccountId,
                command: UpdateGeneralSettingCommand,
            ): Outcome<Unit, AccountSettingError>
        }

        fun interface UpdateReadMailSettings {
            suspend operator fun invoke(
                accountId: AccountId,
                command: UpdateReadMessageSettingsCommand,
            ): Outcome<Unit, AccountSettingError>
        }

        fun interface UpdateSearchSettings {
            suspend operator fun invoke(
                accountId: AccountId,
                command: UpdateSearchSettingsCommand,
            ): Outcome<Unit, AccountSettingError>
        }

        fun interface UpdateFetchingMailSettings {
            suspend operator fun invoke(
                accountId: AccountId,
                command: UpdateFetchingMailSettingsCommand,
            ): Outcome<Unit, AccountSettingError>
        }

        fun interface UpdateAvatarImage {
            suspend operator fun invoke(
                accountId: AccountId,
                imageUri: Uri,
            ): Outcome<Avatar.Image, AccountSettingError>
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

    sealed interface UpdateReadMessageSettingsCommand {
        data class UpdateShowPictures(val value: String) : UpdateReadMessageSettingsCommand
        data class UpdateIsMarkMessageAsReadOnView(val value: Boolean) : UpdateReadMessageSettingsCommand
    }

    sealed interface UpdateSearchSettingsCommand {
        data class UpdateServerSearchLimit(val value: Int) : UpdateSearchSettingsCommand
    }

    sealed interface UpdateFetchingMailSettingsCommand {
        data class UpdateLocalFolderSize(val value: Int) : UpdateFetchingMailSettingsCommand
        data class UpdateSyncMessageFrom(val value: Int) : UpdateFetchingMailSettingsCommand
        data class UpdateFetchMessageUpTo(val value: Int) : UpdateFetchingMailSettingsCommand
        data class UpdateFolderPollFrequency(val value: Int) : UpdateFetchingMailSettingsCommand
        data class UpdateSyncServerDeletions(val value: Boolean) : UpdateFetchingMailSettingsCommand
        data class UpdateMarkAsReadWhenDeleted(val value: Boolean) : UpdateFetchingMailSettingsCommand
        data class UpdateDeletePolicy(val value: String) : UpdateFetchingMailSettingsCommand
        data class UpdateExpungePolicy(val value: String) : UpdateFetchingMailSettingsCommand
        data class UpdateMaxPushFolders(val value: Int) : UpdateFetchingMailSettingsCommand
        data class UpdateIdleRefreshMinutes(val value: Int) : UpdateFetchingMailSettingsCommand
    }

    sealed interface AccountSettingError {
        data class NotFound(
            val message: String,
        ) : AccountSettingError

        data class StorageError(
            val message: String,
        ) : AccountSettingError

        data class UnsupportedFormat(
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
