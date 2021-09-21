package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage

class NotificationController internal constructor(
    private val certificateErrorNotifications: CertificateErrorNotifications,
    private val authenticationErrorNotifications: AuthenticationErrorNotifications,
    private val syncNotifications: SyncNotifications,
    private val sendFailedNotifications: SendFailedNotifications,
    private val newMailNotifications: NewMailNotifications
) {
    fun showCertificateErrorNotification(account: Account, incoming: Boolean) {
        certificateErrorNotifications.showCertificateErrorNotification(account, incoming)
    }

    fun clearCertificateErrorNotifications(account: Account, incoming: Boolean) {
        certificateErrorNotifications.clearCertificateErrorNotifications(account, incoming)
    }

    fun showAuthenticationErrorNotification(account: Account, incoming: Boolean) {
        authenticationErrorNotifications.showAuthenticationErrorNotification(account, incoming)
    }

    fun clearAuthenticationErrorNotification(account: Account, incoming: Boolean) {
        authenticationErrorNotifications.clearAuthenticationErrorNotification(account, incoming)
    }

    fun showSendingNotification(account: Account) {
        syncNotifications.showSendingNotification(account)
    }

    fun clearSendingNotification(account: Account) {
        syncNotifications.clearSendingNotification(account)
    }

    fun showSendFailedNotification(account: Account, exception: Exception) {
        sendFailedNotifications.showSendFailedNotification(account, exception)
    }

    fun clearSendFailedNotification(account: Account) {
        sendFailedNotifications.clearSendFailedNotification(account)
    }

    fun showFetchingMailNotification(account: Account, folder: LocalFolder) {
        syncNotifications.showFetchingMailNotification(account, folder)
    }

    fun showEmptyFetchingMailNotification(account: Account) {
        syncNotifications.showEmptyFetchingMailNotification(account)
    }

    fun clearFetchingMailNotification(account: Account) {
        syncNotifications.clearFetchingMailNotification(account)
    }

    fun addNewMailNotification(
        account: Account,
        message: LocalMessage,
        previousUnreadMessageCount: Int,
        silent: Boolean
    ) {
        newMailNotifications.addNewMailNotification(account, message, previousUnreadMessageCount, silent)
    }

    fun removeNewMailNotification(account: Account, messageReference: MessageReference) {
        newMailNotifications.removeNewMailNotification(account, messageReference)
    }

    fun clearNewMailNotifications(account: Account) {
        newMailNotifications.clearNewMailNotifications(account)
    }
}
