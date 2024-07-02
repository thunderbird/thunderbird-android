package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import timber.log.Timber

class NotificationController internal constructor(
    private val certificateErrorNotificationController: CertificateErrorNotificationController,
    private val authenticationErrorNotificationController: AuthenticationErrorNotificationController,
    private val syncNotificationController: SyncNotificationController,
    private val sendFailedNotificationController: SendFailedNotificationController,
    private val newMailNotificationController: NewMailNotificationController,
) {
    fun showCertificateErrorNotification(account: Account, incoming: Boolean) {
        certificateErrorNotificationController.showCertificateErrorNotification(account, incoming)
    }

    fun clearCertificateErrorNotifications(account: Account, incoming: Boolean) {
        certificateErrorNotificationController.clearCertificateErrorNotifications(account, incoming)
    }

    fun showAuthenticationErrorNotification(account: Account, incoming: Boolean) {
        authenticationErrorNotificationController.showAuthenticationErrorNotification(account, incoming)
    }

    fun clearAuthenticationErrorNotification(account: Account, incoming: Boolean) {
        authenticationErrorNotificationController.clearAuthenticationErrorNotification(account, incoming)
    }

    fun showSendingNotification(account: Account) {
        syncNotificationController.showSendingNotification(account)
    }

    fun clearSendingNotification(account: Account) {
        syncNotificationController.clearSendingNotification(account)
    }

    fun showSendFailedNotification(account: Account, exception: Exception) {
        sendFailedNotificationController.showSendFailedNotification(account, exception)
    }

    fun clearSendFailedNotification(account: Account) {
        sendFailedNotificationController.clearSendFailedNotification(account)
    }

    fun showFetchingMailNotification(account: Account, folder: LocalFolder) {
        syncNotificationController.showFetchingMailNotification(account, folder)
    }

    fun showEmptyFetchingMailNotification(account: Account) {
        syncNotificationController.showEmptyFetchingMailNotification(account)
    }

    fun clearFetchingMailNotification(account: Account) {
        syncNotificationController.clearFetchingMailNotification(account)
    }

    fun restoreNewMailNotifications(accounts: List<Account>) {
        newMailNotificationController.restoreNewMailNotifications(accounts)
    }

    fun addNewMailNotification(account: Account, message: LocalMessage, silent: Boolean) {
        Timber.v(
            "Creating notification for message %s:%s:%s",
            message.account.uuid,
            message.folder.databaseId,
            message.uid,
        )

        newMailNotificationController.addNewMailNotification(account, message, silent)
    }

    fun removeNewMailNotification(account: Account, messageReference: MessageReference) {
        Timber.v("Removing notification for message %s", messageReference)

        newMailNotificationController.removeNewMailNotifications(account, clearNewMessageState = true) {
            listOf(messageReference)
        }
    }

    fun clearNewMailNotifications(account: Account, selector: (List<MessageReference>) -> List<MessageReference>) {
        Timber.v("Removing some notifications for account %s", account.uuid)

        newMailNotificationController.removeNewMailNotifications(account, clearNewMessageState = false, selector)
    }

    fun clearNewMailNotifications(account: Account, clearNewMessageState: Boolean) {
        Timber.v("Removing all notifications for account %s", account.uuid)

        newMailNotificationController.clearNewMailNotifications(account, clearNewMessageState)
    }
}
