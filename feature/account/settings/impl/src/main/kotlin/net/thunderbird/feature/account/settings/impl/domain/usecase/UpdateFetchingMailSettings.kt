package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

internal class UpdateFetchingMailSettings(
    private val repository: LegacyAccountRepository,
) : UseCase.UpdateFetchingMailSettings {

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override suspend fun invoke(
        accountId: AccountId,
        command: AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand,
    ): Outcome<Unit, AccountSettingsDomainContract.AccountSettingError> {
        val account = repository.getById(accountId)
            .firstOrNull()
            ?: return Outcome.failure(
                AccountSettingsDomainContract.AccountSettingError.NotFound(
                    "Account not found",
                ),
            )

        when (command) {
            is AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateLocalFolderSize -> {
                repository.update(account.copy(displayCount = command.value))
            }

            is AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateSyncMessageFrom -> {
                repository.update(account.copy(maximumPolledMessageAge = command.value))
            }

            is AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateFetchMessageUpTo -> {
                repository.update(account.copy(maximumAutoDownloadMessageSize = command.value))
            }

            is AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateFolderPollFrequency -> {
                repository.update(account.copy(automaticCheckIntervalMinutes = command.value))
            }

            is AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateSyncServerDeletions -> {
                repository.update(account.copy(isSyncRemoteDeletions = command.value))
            }

            is AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateMarkAsReadWhenDeleted -> {
                repository.update(account.copy(isMarkMessageAsReadOnDelete = command.value))
            }

            is AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateWhenIDeleteAMessage -> {
                when (command.value) {
                    "NEVER" -> {
                        repository.update(account.copy(deletePolicy = DeletePolicy.NEVER))
                    }

                    "ON_DELETE" -> {
                        repository.update(account.copy(deletePolicy = DeletePolicy.ON_DELETE))
                    }

                    "MARK_AS_READ" -> {
                        repository.update(account.copy(deletePolicy = DeletePolicy.MARK_AS_READ))
                    }

                    else -> {
                        error("Invalid delete policy value: ${command.value}")
                    }
                }
            }

            is AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand.UpdateEraseDeletedMessageOnServer -> {
                when (command.value) {
                    "EXPUNGE_IMMEDIATELY" -> {
                        repository.update(account.copy(expungePolicy = Expunge.EXPUNGE_IMMEDIATELY))
                    }

                    "EXPUNGE_ON_POLL" -> {
                        repository.update(account.copy(expungePolicy = Expunge.EXPUNGE_ON_POLL))
                    }

                    "EXPUNGE_MANUALLY" -> {
                        repository.update(account.copy(expungePolicy = Expunge.EXPUNGE_MANUALLY))
                    }

                    else -> {
                        error("Invalid expunge policy value: ${command.value}")
                    }
                }
            }

            is AccountSettingsDomainContract
                .UpdateFetchingMailSettingsCommand.UpdateOnMaxFolderToCheckWithPushChange,
            -> {
                repository.update(account.copy(maxPushFolders = command.value))
            }

            is AccountSettingsDomainContract
                .UpdateFetchingMailSettingsCommand.UpdateRefreshIdleConnectionFrequencyChange,
            -> {
                repository.update(account.copy(idleRefreshMinutes = command.value))
            }
        }

        return Outcome.success(Unit)
    }
}
