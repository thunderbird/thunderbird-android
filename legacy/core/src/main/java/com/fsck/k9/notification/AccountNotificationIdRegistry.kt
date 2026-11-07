package com.fsck.k9.notification

import java.util.concurrent.ConcurrentHashMap
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.UNASSIGNED_ACCOUNT_NUMBER
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.notification.api.NotificationId

/**
 * Registry for managing and allocating unique notification IDs for specific account-related notifications.
 *
 * This interface provides methods to either retrieve an existing notification ID or allocate a new one
 * for a given combination of account ID and notification kind. It ensures unique notification IDs are
 * assigned for each account and notification type.
 */
internal interface AccountNotificationIdRegistry {
    fun getOrAllocate(accountId: AccountId, kind: AccountNotificationKind): NotificationId

    fun getOrAllocate(accountId: AccountId, kind: AccountNotificationKind, index: Int): NotificationId

    /**
     * Returns the IDs of all new mail notifications of the account, or an empty list when its account number is
     * unknown, e.g. for an account removed before the app was restarted.
     */
    fun getAllNewMailNotificationIds(accountId: AccountId): List<NotificationId>
}

internal class DefaultAccountNotificationIdRegistry(
    private val accountManager: LegacyAccountManager,
) : AccountNotificationIdRegistry {
    // Notifications of a removed account must still be cancellable, so remember the last known account numbers
    private val lastKnownAccountNumbers = ConcurrentHashMap<AccountId, Int>()

    override fun getOrAllocate(
        accountId: AccountId,
        kind: AccountNotificationKind,
    ): NotificationId = getOrAllocateForIndex(accountId, kind, null)

    override fun getOrAllocate(
        accountId: AccountId,
        kind: AccountNotificationKind,
        index: Int,
    ): NotificationId {
        require(kind == AccountNotificationKind.SingleMessage) { "Only SingleMessage notifications have an index" }
        require(index in 0 until MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS) { "Invalid index: $index" }
        return getOrAllocateForIndex(accountId, kind, index)
    }

    override fun getAllNewMailNotificationIds(accountId: AccountId): List<NotificationId> {
        val accountNumber = findAccountNumber(accountId) ?: return emptyList()
        return NotificationIds.getAllMessageNotificationIds(accountNumber).map(::NotificationId)
    }

    private fun getOrAllocateForIndex(
        accountId: AccountId,
        kind: AccountNotificationKind,
        index: Int?,
    ): NotificationId {
        if (kind == AccountNotificationKind.SingleMessage) {
            requireNotNull(index) { "SingleMessage notification requires an index" }
            require(index in 0 until MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS) { "Invalid index: $index" }
        } else {
            require(index == null) { "Only SingleMessage notifications have an index" }
        }
        return NotificationId(getNotificationId(getAccountNumber(accountId), kind, index))
    }

    private fun getAccountNumber(accountId: AccountId): Int {
        return requireNotNull(findAccountNumber(accountId)) { "Account not found" }
    }

    private fun findAccountNumber(accountId: AccountId): Int? {
        val account = accountManager.findById(accountId) ?: return lastKnownAccountNumbers[accountId]
        require(account.accountNumber != UNASSIGNED_ACCOUNT_NUMBER) { "Account number not assigned" }
        lastKnownAccountNumbers[accountId] = account.accountNumber
        return account.accountNumber
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
