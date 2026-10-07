package com.fsck.k9.controller

import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.notification.NotificationController
import com.fsck.k9.search.isNewMessages
import com.fsck.k9.search.isSingleFolder
import com.fsck.k9.search.isUnifiedInbox
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.search.legacy.LocalMessageSearch

internal class NotificationOperations(
    private val notificationController: NotificationController,
    private val accountManager: LegacyAccountManager,
    private val messageStoreManager: MessageStoreManager,
) {
    fun clearNotifications(search: LocalMessageSearch) {
        if (search.isUnifiedInbox) {
            clearUnifiedInboxNotifications()
        } else if (search.isNewMessages) {
            // TODO: A new messages search is always bound to a single account (see
            //  LocalMessageSearchType.NewMessages), so only the notifications of that account should be cleared
            //  instead of those of all accounts.
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

    private fun clearUnifiedInboxNotifications() {
        val accountIds = accountManager.findAll().map { it.id }
        for (accountId in accountIds) {
            val messageStore = messageStoreManager.getMessageStore(accountId)

            val folderIds = messageStore.getFolders(excludeLocalOnly = true) { folderDetails ->
                if (folderDetails.isIntegrate) folderDetails.id else null
            }.filterNotNull().toSet()

            if (folderIds.isNotEmpty()) {
                notificationController.clearNewMailNotifications(accountId) { messageReferences ->
                    messageReferences.filter { messageReference -> messageReference.folderId in folderIds }
                }
            }
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
