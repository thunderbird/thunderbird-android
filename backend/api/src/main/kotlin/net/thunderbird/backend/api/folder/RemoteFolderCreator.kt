package net.thunderbird.backend.api.folder

import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.folders.FolderServerId
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId

interface RemoteFolderCreator {
    /**
     * Creates a folder on the remote server. If the folder already exists and [mustCreate] is `false`,
     * the operation will succeed returning [RemoteFolderCreationOutcome.Success.AlreadyExists].
     *
     * @param folderServerId The folder server ID.
     * @param mustCreate If `true`, the folder must be created returning
     * [RemoteFolderCreationOutcome.Error.FailedToCreateRemoteFolder]. If `false`, the folder will be created
     * only if it doesn't exist.
     * @param folderType The folder type. This requires special handling for some servers. Default [FolderType.REGULAR].
     * @return The result of the operation.
     * @see RemoteFolderCreationOutcome.Success
     * @see RemoteFolderCreationOutcome.Error
     */
    suspend fun create(
        folderServerId: FolderServerId,
        mustCreate: Boolean,
        folderType: FolderType = FolderType.REGULAR,
    ): Outcome<RemoteFolderCreationOutcome.Success, RemoteFolderCreationOutcome.Error>

    interface Factory {
        fun create(accountId: AccountId): RemoteFolderCreator
    }
}

sealed interface RemoteFolderCreationOutcome {
    sealed interface Success : RemoteFolderCreationOutcome {
        /**
         * Used to flag that the folder was created successfully.
         */
        data object Created : Success

        /**
         * Used to flag that the folder creation was skipped because the folder already exists and
         * the creation is NOT mandatory.
         */
        data object AlreadyExists : Success
    }

    sealed interface Error : RemoteFolderCreationOutcome {
        /**
         * Used to flag that the folder creation has failed because the folder already exists and
         * the creation is mandatory.
         */
        data object AlreadyExists : Error

        /**
         * Used to flag that the folder creation failed on the remote server.
         * @param reason The reason why the folder creation failed.
         */
        data class FailedToCreateRemoteFolder(
            val reason: String,
        ) : Error

        /**
         * Used to flag that the Create Folder operation is not supported by the server.
         * E.g. POP3 servers don't support creating archive folders.
         */
        data object NotSupportedOperation : Error
    }
}
