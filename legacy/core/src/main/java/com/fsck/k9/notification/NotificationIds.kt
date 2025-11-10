package com.fsck.k9.notification

import net.thunderbird.core.android.account.LegacyAccountDto

internal object NotificationIds {
    const val PUSH_NOTIFICATION_ID = 1
    const val BACKGROUND_WORK_NOTIFICATION_ID = 2

    private const val NUMBER_OF_GENERAL_NOTIFICATIONS = 2
    private const val OFFSET_SEND_FAILED_NOTIFICATION = 0
    private const val OFFSET_CERTIFICATE_ERROR_INCOMING = 1
    private const val OFFSET_CERTIFICATE_ERROR_OUTGOING = 2
    private const val OFFSET_AUTHENTICATION_ERROR_INCOMING = 3
    private const val OFFSET_AUTHENTICATION_ERROR_OUTGOING = 4
    private const val OFFSET_FETCHING_MAIL = 5
    private const val OFFSET_NEW_MAIL_SUMMARY = 6
    private const val OFFSET_NEW_MAIL_SINGLE = 7
    private const val NUMBER_OF_MISC_ACCOUNT_NOTIFICATIONS = 7
    private const val NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS = MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS
    private const val NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT =
        NUMBER_OF_MISC_ACCOUNT_NOTIFICATIONS + NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS

    fun getNewMailSummaryNotificationId(account: LegacyAccountDto): Int {
        return getBaseNotificationId(account) + OFFSET_NEW_MAIL_SUMMARY
    }

    fun getSingleMessageNotificationId(account: LegacyAccountDto, index: Int): Int {
        require(index in 0 until NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS) { "Invalid index: $index" }

        return getBaseNotificationId(account) + OFFSET_NEW_MAIL_SINGLE + index
    }

    fun getAllMessageNotificationIds(account: LegacyAccountDto): List<Int> {
        val singleMessageNotificationIdRange = (0 until NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS).map { index ->
            getBaseNotificationId(account) + OFFSET_NEW_MAIL_SINGLE + index
        }

        return singleMessageNotificationIdRange.toList() + getNewMailSummaryNotificationId(account)
    }

    fun getFetchingMailNotificationId(account: LegacyAccountDto): Int {
        return getBaseNotificationId(account) + OFFSET_FETCHING_MAIL
    }

    fun getSendFailedNotificationId(account: LegacyAccountDto): Int {
        return getBaseNotificationId(account) + OFFSET_SEND_FAILED_NOTIFICATION
    }

    fun getCertificateErrorNotificationId(account: LegacyAccountDto, incoming: Boolean): Int {
        val offset = if (incoming) OFFSET_CERTIFICATE_ERROR_INCOMING else OFFSET_CERTIFICATE_ERROR_OUTGOING

        return getBaseNotificationId(account) + offset
    }

    fun getAuthenticationErrorNotificationId(account: LegacyAccountDto, incoming: Boolean): Int {
        val offset = if (incoming) OFFSET_AUTHENTICATION_ERROR_INCOMING else OFFSET_AUTHENTICATION_ERROR_OUTGOING

        return getBaseNotificationId(account) + offset
    }

    private fun getBaseNotificationId(account: LegacyAccountDto): Int {
        /* skip notification ID 0 */
        return 1 + NUMBER_OF_GENERAL_NOTIFICATIONS +
            account.accountNumber * NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT
    }
}
