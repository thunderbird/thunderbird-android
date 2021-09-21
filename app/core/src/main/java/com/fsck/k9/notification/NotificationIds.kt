package com.fsck.k9.notification

import com.fsck.k9.Account

internal object NotificationIds {
    const val PUSH_NOTIFICATION_ID = 1

    private const val NUMBER_OF_GENERAL_NOTIFICATIONS = 1
    private const val OFFSET_SEND_FAILED_NOTIFICATION = 0
    private const val OFFSET_CERTIFICATE_ERROR_INCOMING = 1
    private const val OFFSET_CERTIFICATE_ERROR_OUTGOING = 2
    private const val OFFSET_AUTHENTICATION_ERROR_INCOMING = 3
    private const val OFFSET_AUTHENTICATION_ERROR_OUTGOING = 4
    private const val OFFSET_FETCHING_MAIL = 5
    private const val OFFSET_NEW_MAIL_SUMMARY = 6
    private const val OFFSET_NEW_MAIL_SINGLE = 7
    private const val NUMBER_OF_MISC_ACCOUNT_NOTIFICATIONS = 7
    private const val NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS = NotificationData.MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS
    private const val NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT =
        NUMBER_OF_MISC_ACCOUNT_NOTIFICATIONS + NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS

    @JvmStatic
    fun getNewMailSummaryNotificationId(account: Account): Int {
        return getBaseNotificationId(account) + OFFSET_NEW_MAIL_SUMMARY
    }

    @JvmStatic
    fun getSingleMessageNotificationId(account: Account, index: Int): Int {
        require(index in 0 until NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS) { "Invalid index: $index" }

        return getBaseNotificationId(account) + OFFSET_NEW_MAIL_SINGLE + index
    }

    @JvmStatic
    fun getFetchingMailNotificationId(account: Account): Int {
        return getBaseNotificationId(account) + OFFSET_FETCHING_MAIL
    }

    @JvmStatic
    fun getSendFailedNotificationId(account: Account): Int {
        return getBaseNotificationId(account) + OFFSET_SEND_FAILED_NOTIFICATION
    }

    @JvmStatic
    fun getCertificateErrorNotificationId(account: Account, incoming: Boolean): Int {
        val offset = if (incoming) OFFSET_CERTIFICATE_ERROR_INCOMING else OFFSET_CERTIFICATE_ERROR_OUTGOING

        return getBaseNotificationId(account) + offset
    }

    @JvmStatic
    fun getAuthenticationErrorNotificationId(account: Account, incoming: Boolean): Int {
        val offset = if (incoming) OFFSET_AUTHENTICATION_ERROR_INCOMING else OFFSET_AUTHENTICATION_ERROR_OUTGOING

        return getBaseNotificationId(account) + offset
    }

    private fun getBaseNotificationId(account: Account): Int {
        return 1 /* skip notification ID 0 */ + NUMBER_OF_GENERAL_NOTIFICATIONS +
            account.accountNumber * NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT
    }
}
