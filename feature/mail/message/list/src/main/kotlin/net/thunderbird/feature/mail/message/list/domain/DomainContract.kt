package net.thunderbird.feature.mail.message.list.domain

import com.fsck.k9.mail.folders.FolderServerId
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.mail.folder.api.RemoteFolder

interface DomainContract {
    interface UseCase {
        fun interface GetAccountFolders {
            suspend operator fun invoke(accountUuid: String): Outcome<List<RemoteFolder>, AccountFolderError>
        }

        fun interface CreateArchiveFolder {
            operator fun invoke(
                accountUuid: String,
                folderName: String,
            ): Flow<Outcome<CreateArchiveFolderOutcome.Success, CreateArchiveFolderOutcome.Error>>
        }

        fun interface SetArchiveFolder {
            suspend operator fun invoke(
                accountUuid: String,
                folder: RemoteFolder,
            ): Outcome<SetAccountFolderOutcome.Success, SetAccountFolderOutcome.Error>
        }

        fun interface BuildSwipeActions<out TAccount : BaseAccount> {
            operator fun invoke(
                accountUuids: Set<String>,
                isIncomingServerPop3: (TAccount) -> Boolean,
                hasArchiveFolder: (TAccount) -> Boolean,
            ): Map<String, SwipeActions>
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
