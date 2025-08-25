package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.logging.legacy.Log

class NotificationController internal constructor(
    private val certificateErrorNotificationController: CertificateErrorNotificationController,
    private val authenticationErrorNotificationController: AuthenticationErrorNotificationController,
    private val syncNotificationController: SyncNotificationController,
    private val sendFailedNotificationController: SendFailedNotificationController,
    private val newMailNotificationController: NewMailNotificationController,
) {
    fun showCertificateErrorNotification(account: LegacyAccount, incoming: Boolean) {
        certificateErrorNotificationController.showCertificateErrorNotification(account, incoming)
    }

    fun clearCertificateErrorNotifications(account: LegacyAccount, incoming: Boolean) {
        certificateErrorNotificationController.clearCertificateErrorNotifications(account, incoming)
    }

    fun showAuthenticationErrorNotification(account: LegacyAccount, incoming: Boolean) {
        authenticationErrorNotificationController.showAuthenticationErrorNotification(account, incoming)
    }

    fun clearAuthenticationErrorNotification(account: LegacyAccount, incoming: Boolean) {
        authenticationErrorNotificationController.clearAuthenticationErrorNotification(account, incoming)
    }

    fun showSendingNotification(account: LegacyAccount) {
        syncNotificationController.showSendingNotification(account)
    }

    fun clearSendingNotification(account: LegacyAccount) {
        syncNotificationController.clearSendingNotification(account)
    }

    fun showSendFailedNotification(account: LegacyAccount, exception: Exception) {
        sendFailedNotificationController.showSendFailedNotification(account, exception)
    }

    fun clearSendFailedNotification(account: LegacyAccount) {
        sendFailedNotificationController.clearSendFailedNotification(account)
    }

    fun showFetchingMailNotification(account: LegacyAccount, folder: LocalFolder) {
        syncNotificationController.showFetchingMailNotification(account, folder)
    }

    fun showEmptyFetchingMailNotification(account: LegacyAccount) {
        syncNotificationController.showEmptyFetchingMailNotification(account)
    }

    fun clearFetchingMailNotification(account: LegacyAccount) {
        syncNotificationController.clearFetchingMailNotification(account)
    }

    fun restoreNewMailNotifications(accounts: List<LegacyAccount>) {
        newMailNotificationController.restoreNewMailNotifications(accounts)
    }

    fun addNewMailNotification(account: LegacyAccount, message: LocalMessage, silent: Boolean) {
        Log.v(
            "Creating notification for message %s:%s:%s",
            message.account.uuid,
            message.folder.databaseId,
            message.uid,
        )

        newMailNotificationController.addNewMailNotification(account, message, silent)
    }

    fun removeNewMailNotification(account: LegacyAccount, messageReference: MessageReference) {
        Log.v("Removing notification for message %s", messageReference)

        newMailNotificationController.removeNewMailNotifications(account, clearNewMessageState = true) {
            listOf(messageReference)
        }
    }

    fun clearNewMailNotifications(
        account: LegacyAccount,
        selector: (List<MessageReference>) -> List<MessageReference>,
    ) {
        Log.v("Removing some notifications for account %s", account.uuid)

        newMailNotificationController.removeNewMailNotifications(account, clearNewMessageState = false, selector)
    }

    fun clearNewMailNotifications(account: LegacyAccount, clearNewMessageState: Boolean) {
        Log.v("Removing all notifications for account %s", account.uuid)

        newMailNotificationController.clearNewMailNotifications(account, clearNewMessageState)
    }
}
