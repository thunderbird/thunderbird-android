package com.fsck.k9.controller

import com.fsck.k9.search.folderIdsFor
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager

/**
 * Marks every message as read in each account's folder with a given [FolderRole].
 *
 * This is the unified-folder equivalent of [MessagingController.markAllMessagesRead]. Accounts that do not have a
 * folder for the role are skipped.
 */
class UnifiedFolderReadMarker(
    private val accountManager: LegacyAccountManager,
    private val outboxFolderManager: OutboxFolderManager,
    private val markAllMessagesRead: (accountId: AccountId, folderId: Long) -> Unit,
) {
    fun markAllAsRead(role: FolderRole) {
        for ((accountId, folderId) in accountManager.folderIdsFor(role, outboxFolderManager)) {
            markAllMessagesRead(accountId, folderId)
        }
    }
}
