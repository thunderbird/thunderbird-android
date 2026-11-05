package com.fsck.k9.controller

import app.k9mail.legacy.message.controller.MessagingListener
import com.fsck.k9.search.folderIdsFor
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.mail.folder.LegacyFolderIdFactory
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager

/**
 * Synchronizes the folders of a unified folder: each account's folder with the given role.
 *
 * Like refreshing a single folder, it also sends the account's pending messages.
 */
class UnifiedFolderSynchronizer(
    private val accountManager: LegacyAccountManager,
    private val outboxFolderManager: OutboxFolderManager,
    private val folderSynchronizer: FolderSynchronizer,
) {
    fun synchronize(role: FolderRole, listener: MessagingListener?) {
        for ((accountId, folderId) in accountManager.folderIdsFor(role, outboxFolderManager)) {
            folderSynchronizer.synchronizeFolder(accountId, LegacyFolderIdFactory.of(folderId), listener)
            folderSynchronizer.sendPendingMessages(accountId, listener)
        }
    }
}
