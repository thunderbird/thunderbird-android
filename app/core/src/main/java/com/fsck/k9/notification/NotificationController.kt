package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage

class NotificationController internal constructor(
    private val certificateErrorNotificationController: CertificateErrorNotificationController,
    private val authenticationErrorNotificationController: AuthenticationErrorNotificationController,
    private val syncNotificationController: SyncNotificationController,
    private val sendFailedNotificationController: SendFailedNotificationController,
    private val newMailNotificationController: NewMailNotificationController
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

    fun addNewMailNotification(account: Account, message: LocalMessage, silent: Boolean) {
        newMailNotificationController.addNewMailNotification(account, message, silent)
    }

    fun removeNewMailNotification(account: Account, messageReference: MessageReference) {
        newMailNotificationController.removeNewMailNotification(account, messageReference)
    }

    fun clearNewMailNotifications(account: Account) {
        newMailNotificationController.clearNewMailNotifications(account)
    }
}
