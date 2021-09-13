package com.fsck.k9.notification

import com.fsck.k9.Account
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class NotificationIdsTest {
    @Test
    fun `all general notification IDs are unique`() {
        val notificationIds = getGeneralNotificationIds()

        assertThat(notificationIds).containsNoDuplicates()
    }

    @Test
    fun `avoid notification ID 0`() {
        val notificationIds = getGeneralNotificationIds()

        assertThat(notificationIds).doesNotContain(0)
    }

    @Test
    fun `all notification IDs of an account are unique`() {
        val account = createAccount(0)

        val notificationIds = getAccountNotificationIds(account)

        assertThat(notificationIds).containsNoDuplicates()
    }

    @Test
    fun `notification IDs of adjacent accounts do not overlap`() {
        val account1 = createAccount(0)
        val account2 = createAccount(1)

        val notificationIds1 = getAccountNotificationIds(account1)
        val notificationIds2 = getAccountNotificationIds(account2)

        assertWithMessage("Reused notification IDs").that(notificationIds1 intersect notificationIds2).isEmpty()
    }

    @Test
    // We avoid gaps. So this test failing is an indication that getGeneralNotificationIds() and/or
    // getAccountNotificationIds() need to be updated.
    fun `no gaps between general and account notification IDs`() {
        val account = createAccount(0)

        val generalNotificationIds = getGeneralNotificationIds()
        val accountNotificationIds = getAccountNotificationIds(account)

        val maxGeneralNotificationId = requireNotNull(generalNotificationIds.maxOrNull())
        val minAccountNotificationId = requireNotNull(accountNotificationIds.minOrNull())
        assertThat(maxGeneralNotificationId + 1).isEqualTo(minAccountNotificationId)
    }

    @Test
    // We avoid gaps. So this test failing is an indication that getAccountNotificationIds() needs to be updated.
    fun `no gaps in notification IDs of an account`() {
        val account = createAccount(0)

        val notificationIds = getAccountNotificationIds(account)

        val minNotificationId = requireNotNull(notificationIds.minOrNull())
        val maxNotificationId = requireNotNull(notificationIds.maxOrNull())
        val notificationIdRange = (minNotificationId..maxNotificationId)
        assertWithMessage("Skipped notification IDs").that(notificationIdRange - notificationIds).isEmpty()
    }

    @Test
    // We avoid gaps. So this test failing is an indication that getAccountNotificationIds() needs to be updated.
    fun `no gap between notification IDs of adjacent accounts`() {
        val account1 = createAccount(1)
        val account2 = createAccount(2)

        val notificationIds1 = getAccountNotificationIds(account1)
        val notificationIds2 = getAccountNotificationIds(account2)

        val maxNotificationId1 = requireNotNull(notificationIds1.maxOrNull())
        val minNotificationId2 = requireNotNull(notificationIds2.minOrNull())
        assertThat(maxNotificationId1 + 1).isEqualTo(minNotificationId2)
    }

    fun getGeneralNotificationIds(): List<Int> {
        return listOf(NotificationIds.PUSH_NOTIFICATION_ID)
    }

    fun getAccountNotificationIds(account: Account): List<Int> {
        return listOf(
            NotificationIds.getSendFailedNotificationId(account),
            NotificationIds.getCertificateErrorNotificationId(account, true),
            NotificationIds.getCertificateErrorNotificationId(account, false),
            NotificationIds.getAuthenticationErrorNotificationId(account, true),
            NotificationIds.getAuthenticationErrorNotificationId(account, false),
            NotificationIds.getFetchingMailNotificationId(account),
            NotificationIds.getNewMailSummaryNotificationId(account),
        ) + (0 until NotificationData.MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS).map { index ->
            NotificationIds.getSingleMessageNotificationId(account, index)
        }
    }

    fun createAccount(accountNumber: Int): Account {
        return Account("uuid").apply {
            this.accountNumber = accountNumber
        }
    }
}
