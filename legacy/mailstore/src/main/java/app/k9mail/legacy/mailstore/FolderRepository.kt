package app.k9mail.legacy.mailstore

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderDetails
import net.thunderbird.feature.mail.folder.api.RemoteFolder

/**
 * Provides access to local and remote folder information and management.
 *
 * TODO: Refactor FolderRepository into focused contracts.
 *       Problem: This interface mixes unrelated responsibilities (read models, remote listing, push tracking,
 *       and per-flag mutations), which makes it hard to test and evolve.
 */
@Suppress("TooManyFunctions")
interface FolderRepository {
    /**
     * Returns a [Folder] for the given [accountId] and [folderId].
     *
     * @param accountId The account identifier.
     * @param folderId The folder identifier.
     */
    suspend fun getFolder(accountId: AccountId, folderId: Long): Folder?

    /**
     * Returns a [FolderDetails] for the given [accountId] and [folderId].
     *
     * @param accountId The account identifier.
     * @param folderId The folder identifier.
     */
    suspend fun getFolderDetails(accountId: AccountId, folderId: Long): FolderDetails?

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

    /**
     * Updates the folder details for the given [accountId].
     *
     * @param accountId The account identifier.
     * @param folderDetails The folder details to update.
     */
    fun updateFolderDetails(accountId: AccountId, folderDetails: FolderDetails)

    /**
     * Sets whether the folder with [folderId] should be included in the unified inbox.
     *
     * @param accountId The account identifier.
     * @param folderId The folder identifier.
     * @param includeInUnifiedInbox Whether to include the folder in the unified inbox.
     */
    fun setIncludeInUnifiedInbox(accountId: AccountId, folderId: Long, includeInUnifiedInbox: Boolean)

    /**
     * Sets whether the folder with [folderId] is visible.
     *
     * @param accountId The account identifier.
     * @param folderId The folder identifier.
     * @param visible Whether the folder is visible.
     */
    fun setVisible(accountId: AccountId, folderId: Long, visible: Boolean)

    /**
     * Sets whether synchronization is enabled for the folder with [folderId].
     *
     * @param accountId The account identifier.
     * @param folderId The folder identifier.
     * @param enable Whether synchronization is enabled.
     */
    fun setSyncEnabled(accountId: AccountId, folderId: Long, enable: Boolean)

    /**
     * Sets whether notifications are enabled for the folder with [folderId].
     *
     * @param accountId The account identifier.
     * @param folderId The folder identifier.
     * @param enable Whether notifications are enabled.
     */
    fun setNotificationsEnabled(accountId: AccountId, folderId: Long, enable: Boolean)

    /**
     * Disables push for the given [accountId].
     *
     * @param accountId The account identifier.
     */
    fun setPushDisabled(accountId: AccountId)

    /**
     * Returns `true` if there's at least one folder with push enabled for the given [accountId].
     *
     * @param accountId The account identifier.
     */
    fun hasPushEnabledFolder(accountId: AccountId): Boolean

    /**
     * Returns a [Flow] that emits `true` if there's at least one folder with push enabled for the given [accountId].
     *
     * @param accountId The account identifier.
     */
    fun hasPushEnabledFolderFlow(accountId: AccountId): Flow<Boolean>
}
