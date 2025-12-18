package net.thunderbird.feature.mail.message.list.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.FolderServerId
import net.thunderbird.feature.mail.folder.api.RemoteFolder

interface DomainContract {
    interface UseCase {
        fun interface GetAccountFolders {
            suspend operator fun invoke(accountId: AccountId): Outcome<List<RemoteFolder>, AccountFolderError>
        }

        fun interface CreateArchiveFolder {
            operator fun invoke(
                accountId: AccountId,
                folderName: String,
            ): Flow<Outcome<CreateArchiveFolderOutcome.Success, CreateArchiveFolderOutcome.Error>>
        }

        fun interface SetArchiveFolder {
            suspend operator fun invoke(
                accountId: AccountId,
                folder: RemoteFolder,
            ): Outcome<SetAccountFolderOutcome.Success, SetAccountFolderOutcome.Error>
        }

        fun interface BuildSwipeActions {
            operator fun invoke(): StateFlow<Map<AccountId, SwipeActions>>
        }
    }
}

data class AccountFolderError(val exception: Exception)

sealed interface SetAccountFolderOutcome {
    data object Success : SetAccountFolderOutcome
    sealed interface Error : SetAccountFolderOutcome {
        data object AccountNotFound : Error
        data class UnhandledError(val throwable: Throwable) : Error
    }
}

sealed interface CreateArchiveFolderOutcome {
    sealed interface Success : CreateArchiveFolderOutcome {
        data object LocalFolderCreated : Success
        data class SyncStarted(val serverId: FolderServerId) : Success
        data object UpdatingSpecialFolders : Success
        data object Created : Success
    }

    sealed interface Error : CreateArchiveFolderOutcome {
        data class LocalFolderCreationError(val folderName: String) : Error
        data class InvalidFolderName(val folderName: String) : Error
        data object AccountNotFound : Error
        data class UnhandledError(val throwable: Throwable) : Error
        sealed interface SyncError : Error {
            data class Failed(
                val serverId: FolderServerId,
                val message: String,
                val exception: Exception?,
            ) : SyncError
        }
    }
}
