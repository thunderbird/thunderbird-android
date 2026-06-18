package net.thunderbird.feature.account.settings.impl.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.LegacyAccountRepository
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UpdateFetchingMailSettingsCommand as Command

internal class UpdateFetchingMailSettings(
    private val repository: LegacyAccountRepository,
) : UseCase.UpdateFetchingMailSettings {

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override suspend fun invoke(
        accountId: AccountId,
        command: Command,
    ): Outcome<Unit, AccountSettingsDomainContract.AccountSettingError> {
        val account = repository.getById(accountId)
            .firstOrNull()
            ?: return Outcome.failure(
                AccountSettingsDomainContract.AccountSettingError.NotFound(
                    "Account not found",
                ),
            )

        val updatedAccount = when (command) {
            is Command.UpdateLocalFolderSize -> {
                account.copy(displayCount = command.value)
            }

            is Command.UpdateSyncMessageFrom -> {
                account.copy(maximumPolledMessageAge = command.value)
            }

            is Command.UpdateFetchMessageUpTo -> {
                account.copy(maximumAutoDownloadMessageSize = command.value)
            }

            is Command.UpdateFolderPollFrequency -> {
                account.copy(automaticCheckIntervalMinutes = command.value)
            }

            is Command.UpdateSyncServerDeletions -> {
                account.copy(isSyncRemoteDeletions = command.value)
            }

            is Command.UpdateMarkAsReadWhenDeleted -> {
                account.copy(isMarkMessageAsReadOnDelete = command.value)
            }

            is Command.UpdateDeletePolicy -> {
                account.copy(
                    deletePolicy = when (command.value) {
                        "NEVER" -> DeletePolicy.NEVER
                        "ON_DELETE" -> DeletePolicy.ON_DELETE
                        "MARK_AS_READ" -> DeletePolicy.MARK_AS_READ
                        else -> error("Invalid delete policy value: ${command.value}")
                    },
                )
            }

            is Command.UpdateExpungePolicy -> {
                account.copy(
                    expungePolicy = when (command.value) {
                        "EXPUNGE_IMMEDIATELY" -> Expunge.EXPUNGE_IMMEDIATELY
                        "EXPUNGE_ON_POLL" -> Expunge.EXPUNGE_ON_POLL
                        "EXPUNGE_MANUALLY" -> Expunge.EXPUNGE_MANUALLY
                        else -> error("Invalid expunge policy value: ${command.value}")
                    },
                )
            }

            is Command.UpdateMaxPushFolders -> {
                account.copy(maxPushFolders = command.value)
            }

            is Command.UpdateIdleRefreshMinutes -> {
                account.copy(idleRefreshMinutes = command.value)
            }
        }

        repository.update(updatedAccount)

        return Outcome.success(Unit)
    }
}
