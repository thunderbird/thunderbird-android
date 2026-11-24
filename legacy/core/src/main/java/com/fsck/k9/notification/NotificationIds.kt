package com.fsck.k9.notification

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

    fun getNewMailSummaryNotificationId(accountNumber: Int): Int {
        return getBaseNotificationId(accountNumber) + OFFSET_NEW_MAIL_SUMMARY
    }

    fun getAllMessageNotificationIds(accountNumber: Int): List<Int> {
        val summaryNotificationId = getNewMailSummaryNotificationId(accountNumber)
        val singleMessageNotificationIds = (0 until NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS).map { index ->
            getSingleMessageNotificationId(accountNumber, index)
        }
        return singleMessageNotificationIds + summaryNotificationId
    }

    fun getSingleMessageNotificationId(accountNumber: Int, index: Int): Int {
        require(index in 0 until NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS) { "Invalid index: $index" }

        return getBaseNotificationId(accountNumber) + OFFSET_NEW_MAIL_SINGLE + index
    }

    fun getFetchingMailNotificationId(accountNumber: Int): Int {
        return getBaseNotificationId(accountNumber) + OFFSET_FETCHING_MAIL
    }

    fun getSendFailedNotificationId(accountNumber: Int): Int {
        return getBaseNotificationId(accountNumber) + OFFSET_SEND_FAILED_NOTIFICATION
    }

    fun getCertificateErrorNotificationId(accountNumber: Int, incoming: Boolean): Int {
        val offset = if (incoming) OFFSET_CERTIFICATE_ERROR_INCOMING else OFFSET_CERTIFICATE_ERROR_OUTGOING

        return getBaseNotificationId(accountNumber) + offset
    }

    fun getAuthenticationErrorNotificationId(accountNumber: Int, incoming: Boolean): Int {
        val offset = if (incoming) OFFSET_AUTHENTICATION_ERROR_INCOMING else OFFSET_AUTHENTICATION_ERROR_OUTGOING

        return getBaseNotificationId(accountNumber) + offset
    }

    private fun getBaseNotificationId(accountNumber: Int): Int {
        /* skip notification ID 0 */
        return 1 + NUMBER_OF_GENERAL_NOTIFICATIONS +
            accountNumber * NUMBER_OF_NOTIFICATIONS_PER_ACCOUNT
    }
}
