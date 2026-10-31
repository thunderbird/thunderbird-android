package com.fsck.k9.ui.messagelist

import com.fsck.k9.search.getLegacyAccounts
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.mail.folder.api.data.repository.FolderQueryRepository
import net.thunderbird.feature.search.legacy.LocalMessageSearch

/**
 * Finds out whether the single folder displayed by a message list no longer exists.
 */
class DeletedFolderDetector(
    private val accountManager: LegacyAccountManager,
    private val folderQueryRepository: FolderQueryRepository,
) {
    /**
     * @return The ID of the deleted folder, or `null` if [search] doesn't target a single folder of a single account
     * or that folder still exists.
     */
    suspend fun findDeletedFolderId(search: LocalMessageSearch): Long? {
        val folderId = search.folderIds.singleOrNull()
        val account = search.getLegacyAccounts(accountManager).singleOrNull()
        if (folderId == null || account == null) return null

        return folderId.takeUnless { folderQueryRepository.isPresent(account.id, folderId) }
    }
}
