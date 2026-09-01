package app.k9mail.legacy.mailstore

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.data.repository.PushFolderTrackingRepository

/**
 * Provides access to local and remote folder information and management.
 *
 * TODO: Refactor FolderRepository into focused contracts.
 *       Problem: This interface mixes unrelated responsibilities (read models, remote listing, push tracking,
 *       and per-flag mutations), which makes it hard to test and evolve.
 */
@Suppress("TooManyFunctions")
interface FolderRepository : PushFolderTrackingRepository {
    /**
     * Returns a [Folder] for the given [accountId] and [folderId].
     *
     * @param accountId The account identifier.
     * @param folderId The folder identifier.
     */
    suspend fun getFolder(accountId: AccountId, folderId: Long): Folder?

    /**
     * Returns a list of [RemoteFolder]s for the given [accountId].
     *
     * @param accountId The account identifier.
     * @throws MessagingException if there's a problem accessing the folders.
     */
    @Throws(MessagingException::class)
    fun getRemoteFolders(accountId: AccountId): List<RemoteFolder>

    /**
     * Returns a list of [RemoteFolderDetails] for the given [accountId].
     *
     * @param accountId The account identifier.
     */
    fun getRemoteFolderDetails(accountId: AccountId): List<RemoteFolderDetails>

    /**
     * Returns a [Flow] of [RemoteFolder]s for the given [accountId] that should be used for push.
     *
     * @param accountId The account identifier.
     */
    fun getPushFoldersFlow(accountId: AccountId): Flow<List<RemoteFolder>>

    /**
     * Returns a list of [RemoteFolder]s for the given [accountId] that should be used for push.
     *
     * @param accountId The account identifier.
     */
    fun getPushFolders(accountId: AccountId): List<RemoteFolder>

    /**
     * Returns the server ID for the given [accountId] and [folderId].
     *
     * @param accountId The account identifier.
     * @param folderId The folder identifier.
     */
    fun getFolderServerId(accountId: AccountId, folderId: Long): String?

    /**
     * Returns the folder ID for the given [accountId] and [folderServerId].
     *
     * @param accountId The account identifier.
     * @param folderServerId The folder server identifier.
     */
    fun getFolderId(accountId: AccountId, folderServerId: String): Long?

    /**
     * Returns `true` if the folder with [folderId] is present for the given [accountId].
     *
     * @param accountId The account identifier.
     * @param folderId The folder identifier.
     */
    fun isFolderPresent(accountId: AccountId, folderId: Long): Boolean
}
