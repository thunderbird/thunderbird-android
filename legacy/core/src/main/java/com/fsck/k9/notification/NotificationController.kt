package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId

class NotificationController internal constructor(
    private val certificateErrorNotificationController: CertificateErrorNotificationController,
    private val authenticationErrorNotificationController: AuthenticationErrorNotificationController,
    private val syncNotificationController: SyncNotificationController,
    private val sendFailedNotificationController: SendFailedNotificationController,
    private val newMailNotificationController: NewMailNotificationController,
    private val logger: Logger,
) {
    fun showCertificateErrorNotification(accountId: AccountId, incoming: Boolean) {
        certificateErrorNotificationController.showCertificateErrorNotification(accountId, incoming)
    }

    fun clearCertificateErrorNotifications(accountId: AccountId, incoming: Boolean) {
        certificateErrorNotificationController.clearCertificateErrorNotifications(accountId, incoming)
    }

    fun showAuthenticationErrorNotification(accountId: AccountId, incoming: Boolean) {
        authenticationErrorNotificationController.showAuthenticationErrorNotification(accountId, incoming)
    }

    fun clearAuthenticationErrorNotification(accountId: AccountId, incoming: Boolean) {
        authenticationErrorNotificationController.clearAuthenticationErrorNotification(accountId, incoming)
    }

    fun showSendingNotification(accountId: AccountId) {
        syncNotificationController.showSendingNotification(accountId)
    }

    fun clearSendingNotification(accountId: AccountId) {
        syncNotificationController.clearSendingNotification(accountId)
    }

    fun showSendFailedNotification(accountId: AccountId, exception: Exception) {
        sendFailedNotificationController.showSendFailedNotification(accountId, exception)
    }

    fun clearSendFailedNotification(accountId: AccountId) {
        sendFailedNotificationController.clearSendFailedNotification(accountId)
    }

    fun showFetchingMailNotification(accountId: AccountId, folder: LocalFolder) {
        syncNotificationController.showFetchingMailNotification(accountId, folder)
    }

    fun showEmptyFetchingMailNotification(accountId: AccountId) {
        syncNotificationController.showEmptyFetchingMailNotification(accountId)
    }

    fun clearFetchingMailNotification(accountId: AccountId) {
        syncNotificationController.clearFetchingMailNotification(accountId)
    }

    fun restoreNewMailNotifications(accounts: List<AccountId>) {
        newMailNotificationController.restoreNewMailNotifications(accounts)
    }

    fun addNewMailNotification(accountId: AccountId, message: LocalMessage, silent: Boolean) {
        logger.verbose {
            "Creating notification for message ${message.accountId}:${message.folder.databaseId}:${message.uid}"
        }

        newMailNotificationController.addNewMailNotification(accountId, message, silent)
    }

    fun removeNewMailNotification(accountId: AccountId, messageReference: MessageReference) {
        logger.verbose { "Removing notification for message $messageReference" }

        newMailNotificationController.removeNewMailNotifications(accountId, clearNewMessageState = true) {
            listOf(messageReference)
        }
    }

    fun clearNewMailNotifications(
        accountId: AccountId,
        selector: (List<MessageReference>) -> List<MessageReference>,
    ) {
        logger.verbose { "Removing some notifications for account $accountId" }

        newMailNotificationController.removeNewMailNotifications(accountId, clearNewMessageState = false, selector)
    }

    fun clearNewMailNotifications(accountId: AccountId, clearNewMessageState: Boolean) {
        logger.verbose { "Removing all notifications for account $accountId" }

        newMailNotificationController.clearNewMailNotifications(accountId, clearNewMessageState)
    }
}
