package com.fsck.k9.notification

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsNoDuplicates
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
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
        val notificationIds = getAccountNotificationIds(0)

        assertThat(notificationIds).containsNoDuplicates()
    }

    @Test
    fun `notification IDs of adjacent accounts do not overlap`() {
        val notificationIds1 = getAccountNotificationIds(0)
        val notificationIds2 = getAccountNotificationIds(1)

        assertThat(actual = notificationIds1 intersect notificationIds2, name = "Reused notification IDs").isEmpty()
    }

    @Test
    fun `no gaps between general and account notification IDs`() {
        // We avoid gaps. So this test failing is an indication that getGeneralNotificationIds() and/or
        // getAccountNotificationIds() need to be updated.
        val generalNotificationIds = getGeneralNotificationIds()
        val accountNotificationIds = getAccountNotificationIds(0)

        val maxGeneralNotificationId = requireNotNull(generalNotificationIds.maxOrNull())
        val minAccountNotificationId = requireNotNull(accountNotificationIds.minOrNull())
        assertThat(maxGeneralNotificationId + 1).isEqualTo(minAccountNotificationId)
    }

    @Test
    fun `no gaps in notification IDs of an account`() {
        // We avoid gaps. So this test failing is an indication that getAccountNotificationIds() needs to be updated.
        val notificationIds = getAccountNotificationIds(0)

        val minNotificationId = requireNotNull(notificationIds.minOrNull())
        val maxNotificationId = requireNotNull(notificationIds.maxOrNull())
        val notificationIdRange = (minNotificationId..maxNotificationId)
        assertThat(actual = notificationIdRange - notificationIds, name = "Skipped notification IDs").isEmpty()
    }

    @Test
    fun `no gap between notification IDs of adjacent accounts`() {
        // We avoid gaps. So this test failing is an indication that getAccountNotificationIds() needs to be updated.
        val notificationIds1 = getAccountNotificationIds(1)
        val notificationIds2 = getAccountNotificationIds(2)

        val maxNotificationId1 = requireNotNull(notificationIds1.maxOrNull())
        val minNotificationId2 = requireNotNull(notificationIds2.minOrNull())
        assertThat(maxNotificationId1 + 1).isEqualTo(minNotificationId2)
    }

    @Test
    fun `all message notification IDs`() {
        val accountNumber = 1

        val notificationIds = NotificationIds.getAllMessageNotificationIds(accountNumber)

        val expected = getNewMessageNotificationIds(accountNumber) +
            NotificationIds.getNewMailSummaryNotificationId(accountNumber)
        assertThat(notificationIds).containsExactly(*expected)
    }

    private fun getGeneralNotificationIds(): List<Int> {
        return listOf(NotificationIds.PUSH_NOTIFICATION_ID, NotificationIds.BACKGROUND_WORK_NOTIFICATION_ID)
    }

    private fun getAccountNotificationIds(accountNumber: Int): List<Int> {
        return listOf(
            NotificationIds.getSendFailedNotificationId(accountNumber),
            NotificationIds.getCertificateErrorNotificationId(accountNumber, true),
            NotificationIds.getCertificateErrorNotificationId(accountNumber, false),
            NotificationIds.getAuthenticationErrorNotificationId(accountNumber, true),
            NotificationIds.getAuthenticationErrorNotificationId(accountNumber, false),
            NotificationIds.getFetchingMailNotificationId(accountNumber),
            NotificationIds.getNewMailSummaryNotificationId(accountNumber),
        ) + getNewMessageNotificationIds(accountNumber)
    }

    private fun getNewMessageNotificationIds(accountNumber: Int): Array<Int> {
        return (0 until MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS).map { index ->
            NotificationIds.getSingleMessageNotificationId(accountNumber, index)
        }.toTypedArray()
    }
}
