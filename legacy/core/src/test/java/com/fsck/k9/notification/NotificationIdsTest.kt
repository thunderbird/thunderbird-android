package com.fsck.k9.notification

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsNoDuplicates
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW
import net.thunderbird.core.android.account.LegacyAccount
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

        assertThat(actual = notificationIds1 intersect notificationIds2, name = "Reused notification IDs").isEmpty()
    }

    @Test
    fun `no gaps between general and account notification IDs`() {
        // We avoid gaps. So this test failing is an indication that getGeneralNotificationIds() and/or
        // getAccountNotificationIds() need to be updated.
        val account = createAccount(0)

        val generalNotificationIds = getGeneralNotificationIds()
        val accountNotificationIds = getAccountNotificationIds(account)

        val maxGeneralNotificationId = requireNotNull(generalNotificationIds.maxOrNull())
        val minAccountNotificationId = requireNotNull(accountNotificationIds.minOrNull())
        assertThat(maxGeneralNotificationId + 1).isEqualTo(minAccountNotificationId)
    }

    @Test
    fun `no gaps in notification IDs of an account`() {
        // We avoid gaps. So this test failing is an indication that getAccountNotificationIds() needs to be updated.
        val account = createAccount(0)

        val notificationIds = getAccountNotificationIds(account)

        val minNotificationId = requireNotNull(notificationIds.minOrNull())
        val maxNotificationId = requireNotNull(notificationIds.maxOrNull())
        val notificationIdRange = (minNotificationId..maxNotificationId)
        assertThat(actual = notificationIdRange - notificationIds, name = "Skipped notification IDs").isEmpty()
    }

    @Test
    fun `no gap between notification IDs of adjacent accounts`() {
        // We avoid gaps. So this test failing is an indication that getAccountNotificationIds() needs to be updated.
        val account1 = createAccount(1)
        val account2 = createAccount(2)

        val notificationIds1 = getAccountNotificationIds(account1)
        val notificationIds2 = getAccountNotificationIds(account2)

        val maxNotificationId1 = requireNotNull(notificationIds1.maxOrNull())
        val minNotificationId2 = requireNotNull(notificationIds2.minOrNull())
        assertThat(maxNotificationId1 + 1).isEqualTo(minNotificationId2)
    }

    @Test
    fun `all message notification IDs`() {
        val account = createAccount(1)

        val notificationIds = NotificationIds.getAllMessageNotificationIds(account)

        val expected = getNewMessageNotificationIds(account) + NotificationIds.getNewMailSummaryNotificationId(account)
        assertThat(notificationIds).containsExactly(*expected)
    }

    private fun getGeneralNotificationIds(): List<Int> {
        return listOf(NotificationIds.PUSH_NOTIFICATION_ID, NotificationIds.BACKGROUND_WORK_NOTIFICATION_ID)
    }

    private fun getAccountNotificationIds(account: LegacyAccount): List<Int> {
        return listOf(
            NotificationIds.getSendFailedNotificationId(account),
            NotificationIds.getCertificateErrorNotificationId(account, true),
            NotificationIds.getCertificateErrorNotificationId(account, false),
            NotificationIds.getAuthenticationErrorNotificationId(account, true),
            NotificationIds.getAuthenticationErrorNotificationId(account, false),
            NotificationIds.getFetchingMailNotificationId(account),
            NotificationIds.getNewMailSummaryNotificationId(account),
        ) + getNewMessageNotificationIds(account)
    }

    private fun getNewMessageNotificationIds(account: LegacyAccount): Array<Int> {
        return (0 until MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS).map { index ->
            NotificationIds.getSingleMessageNotificationId(account, index)
        }.toTypedArray()
    }

    private fun createAccount(accountNumber: Int): LegacyAccount {
        return LegacyAccount(ACCOUNT_ID_RAW).apply {
            this.accountNumber = accountNumber
        }
    }
}
