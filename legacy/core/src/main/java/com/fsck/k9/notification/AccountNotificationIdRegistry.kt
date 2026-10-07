package com.fsck.k9.notification

import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.UNASSIGNED_ACCOUNT_NUMBER
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

internal enum class AccountNotificationKind {
    CertificateErrorIncoming,
    CertificateErrorOutgoing,
    AuthenticationErrorIncoming,
    AuthenticationErrorOutgoing,

    Sync,
    SendFailed,

    SingleMessage,

    NewMailSummary,
}

/**
 * Registry for managing and allocating unique notification IDs for specific account-related notifications.
 *
 * This interface provides methods to either retrieve an existing notification ID or allocate a new one
 * for a given combination of account ID and notification kind. It ensures unique notification IDs are
 * assigned for each account and notification type.
 */
internal interface AccountNotificationIdRegistry {
    fun getOrAllocate(accountId: AccountId, kind: AccountNotificationKind): Int

    fun getOrAllocate(accountId: AccountId, kind: AccountNotificationKind, index: Int): Int

    fun getAllNewMailNotificationIds(accountId: AccountId): List<Int>
}

internal class DefaultAccountNotificationIdRegistry(
    private val accountManager: LegacyAccountManager,
) : AccountNotificationIdRegistry {

    override fun getOrAllocate(
        accountId: AccountId,
        kind: AccountNotificationKind,
    ): Int = getOrAllocateForIndex(accountId, kind, null)

    override fun getOrAllocate(
        accountId: AccountId,
        kind: AccountNotificationKind,
        index: Int,
    ): Int {
        require(kind == AccountNotificationKind.SingleMessage) { "Only SingleMessage notifications have an index" }
        require(index in 0 until MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS) { "Invalid index: $index" }
        return getOrAllocateForIndex(accountId, kind, index)
    }

    override fun getAllNewMailNotificationIds(accountId: AccountId): List<Int> = buildList {
        repeat(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS) { index ->
            add(getOrAllocate(accountId, AccountNotificationKind.SingleMessage, index))
        }
        add(getOrAllocate(accountId, AccountNotificationKind.NewMailSummary))
    }

    private fun getOrAllocateForIndex(
        accountId: AccountId,
        kind: AccountNotificationKind,
        index: Int?,
    ): Int {
        if (kind == AccountNotificationKind.SingleMessage) {
            requireNotNull(index) { "SingleMessage notification requires an index" }
            require(index in 0 until MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS) { "Invalid index: $index" }
        } else {
            require(index == null) { "Only SingleMessage notifications have an index" }
        }
        val account = accountManager.findById(accountId)
            ?: throw IllegalArgumentException("Account not found")
        require(account.accountNumber != UNASSIGNED_ACCOUNT_NUMBER) { "Account number not assigned" }
        return getNotificationId(account.accountNumber, kind, index)
    }

    private fun getNotificationId(accountNumber: Int, kind: AccountNotificationKind, index: Int?): Int {
        return when (kind) {
            AccountNotificationKind.CertificateErrorIncoming -> NotificationIds.getCertificateErrorNotificationId(
                accountNumber,
                true,
            )

            AccountNotificationKind.CertificateErrorOutgoing -> NotificationIds.getCertificateErrorNotificationId(
                accountNumber,
                false,
            )

            AccountNotificationKind.AuthenticationErrorIncoming -> NotificationIds.getAuthenticationErrorNotificationId(
                accountNumber,
                true,
            )

            AccountNotificationKind.AuthenticationErrorOutgoing -> NotificationIds.getAuthenticationErrorNotificationId(
                accountNumber,
                false,
            )

            AccountNotificationKind.Sync -> NotificationIds.getFetchingMailNotificationId(accountNumber)
            AccountNotificationKind.SendFailed -> NotificationIds.getSendFailedNotificationId(accountNumber)

            AccountNotificationKind.SingleMessage -> NotificationIds.getSingleMessageNotificationId(
                accountNumber,
                requireNotNull(index) { "SingleMessage notification requires an index" },
            )

            AccountNotificationKind.NewMailSummary -> NotificationIds.getNewMailSummaryNotificationId(accountNumber)
        }
    }
}

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
