package net.thunderbird.feature.mail.folder.api.data.repository

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.FolderDetails
import net.thunderbird.feature.mail.folder.api.data.FolderError

interface FolderDetailsRepository {
    /**
     * Returns a [FolderDetails] for the given [accountId] and [folderId].
     *
     * @param accountId The account identifier.
     * @param folderId The folder identifier.
     */
    suspend fun findById(accountId: AccountId, folderId: Long): Outcome<FolderDetails?, FolderError>

    /**
     * Updates the folder details for the given [accountId].
     *
     * @param accountId The account identifier.
     * @param folderDetails The folder details to update.
     */
    suspend fun update(accountId: AccountId, folderDetails: FolderDetails): Outcome<Unit, FolderError>

    /**
     * Partially updates a folder details for the given [accountId].
     *
     * @param accountId The account identifier.
     * @param partialUpdate The folder details to update; null fields will be ignored.
     */
    suspend fun update(accountId: AccountId, partialUpdate: PartialUpdatableFolderDetails): Outcome<Unit, FolderError>
}

data class PartialUpdatableFolderDetails(
    val folderId: Long,
    val includeInUnifiedInbox: Boolean? = null,
    val integrate: Boolean? = null,
    val syncEnabled: Boolean? = null,
    val visible: Boolean? = null,
    val notificationsEnabled: Boolean? = null,
    val isPushEnabled: Boolean? = null,
)
