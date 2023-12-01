package com.fsck.k9.controller

import com.fsck.k9.Account
import com.fsck.k9.mailstore.MessageStoreManager
import com.fsck.k9.notification.NotificationController
import com.fsck.k9.preferences.AccountManager
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.isNewMessages
import com.fsck.k9.search.isSingleFolder
import com.fsck.k9.search.isUnifiedInbox

internal class NotificationOperations(
    private val notificationController: NotificationController,
    private val accountManager: AccountManager,
    private val messageStoreManager: MessageStoreManager,
) {
    fun clearNotifications(search: LocalSearch) {
        if (search.isUnifiedInbox) {
            clearUnifiedInboxNotifications()
        } else if (search.isNewMessages) {
            clearAllNotifications()
        } else if (search.isSingleFolder) {
            val account = search.firstAccount() ?: return
            val folderId = search.folderIds.first()
            clearNotifications(account, folderId)
        } else {
            // TODO: Remove notifications when updating the message list. That way we can easily remove only
            //  notifications for messages that are currently displayed in the list.
        }
    }

    private fun clearUnifiedInboxNotifications() {
        for (account in accountManager.getAccounts()) {
            val messageStore = messageStoreManager.getMessageStore(account)

            val folderIds = messageStore.getFolders(excludeLocalOnly = true) { folderDetails ->
                if (folderDetails.isIntegrate) folderDetails.id else null
            }.filterNotNull().toSet()

            if (folderIds.isNotEmpty()) {
                notificationController.clearNewMailNotifications(account) { messageReferences ->
                    messageReferences.filter { messageReference -> messageReference.folderId in folderIds }
                }
            }
        }
    }

    private fun clearAllNotifications() {
        for (account in accountManager.getAccounts()) {
            notificationController.clearNewMailNotifications(account, clearNewMessageState = false)
        }
    }

    private fun clearNotifications(account: Account, folderId: Long) {
        notificationController.clearNewMailNotifications(account) { messageReferences ->
            messageReferences.filter { messageReference -> messageReference.folderId == folderId }
        }
    }

    private fun LocalSearch.firstAccount(): Account? {
        return accountManager.getAccount(accountUuids.first())
    }
}
