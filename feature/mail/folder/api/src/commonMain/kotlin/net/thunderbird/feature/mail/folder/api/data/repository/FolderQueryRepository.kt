package net.thunderbird.feature.mail.folder.api.data.repository

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderServerId
import net.thunderbird.feature.mail.folder.api.data.FolderError

interface FolderQueryRepository {
    /**
     * Returns a [Folder] for the given [accountId] and [folderId].
     *
     * @param accountId The account identifier.
     * @param folderId The folder identifier.
     */
    suspend fun findById(accountId: AccountId, folderId: Long): Outcome<Folder?, FolderError>

    /**
     * Returns the server ID for the given [accountId] and [folderId].
     *
     * @param accountId The account identifier.
     * @param folderId The folder identifier.
     */
    suspend fun findFolderServerIdById(accountId: AccountId, folderId: Long): Outcome<FolderServerId?, FolderError>

    /**
     * Returns the folder ID for the given [accountId] and [folderServerId].
     *
     * @param accountId The account identifier.
     * @param folderServerId The folder server identifier.
     */
    suspend fun findIdByServerId(accountId: AccountId, folderServerId: FolderServerId): Outcome<Long?, FolderError>

    /**
     * Returns `true` if the folder with [folderId] is present for the given [accountId].
     *
     * @param accountId The account identifier.
     * @param folderId The folder identifier.
     */
    suspend fun isPresent(accountId: AccountId, folderId: Long): Boolean
}
