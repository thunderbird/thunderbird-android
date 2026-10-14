package com.fsck.k9.controller

import com.fsck.k9.notification.NotificationController
import com.fsck.k9.search.isNewMessages
import com.fsck.k9.search.isSingleFolder
import com.fsck.k9.search.folderIdsFor
import com.fsck.k9.search.unifiedFolderRole
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.FolderRole
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.search.legacy.LocalMessageSearch

internal class NotificationOperations(
    private val notificationController: NotificationController,
    private val accountManager: LegacyAccountManager,
    private val outboxFolderManager: OutboxFolderManager,
) {
    fun clearNotifications(search: LocalMessageSearch) {
        val unifiedFolderRole = search.unifiedFolderRole
        if (unifiedFolderRole != null) {
            clearUnifiedFolderNotifications(unifiedFolderRole)
        } else if (search.isNewMessages) {
            clearAllNotifications()
        } else if (search.isSingleFolder) {
            val account = search.firstAccount() ?: return
            val folderId = search.folderIds.first()
            clearNotifications(account.id, folderId)
        } else {
            // TODO: Remove notifications when updating the message list. That way we can easily remove only
            //  notifications for messages that are currently displayed in the list.
        }
    }

    private fun clearUnifiedFolderNotifications(role: FolderRole) {
        for ((accountId, folderId) in accountManager.folderIdsFor(role, outboxFolderManager)) {
            clearNotifications(accountId, folderId)
        }
    }

    private fun clearAllNotifications() {
        val accountIds = accountManager.findAll().map { it.id }
        for (accountId in accountIds) {
            notificationController.clearNewMailNotifications(accountId, clearNewMessageState = false)
        }
    }

    private fun clearNotifications(accountId: AccountId, folderId: Long) {
        notificationController.clearNewMailNotifications(accountId) { messageReferences ->
            messageReferences.filter { messageReference -> messageReference.folderId == folderId }
        }
    }

    private fun LocalMessageSearch.firstAccount(): LegacyAccount? {
        return accountManager.findById(accountIds.first())
    }
}
