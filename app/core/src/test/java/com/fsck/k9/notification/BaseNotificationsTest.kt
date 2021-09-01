package com.fsck.k9.notification

import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.K9.NotificationQuickDelete
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.testing.MockHelper.mockBuilder
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

private const val ACCOUNT_COLOR = 0xAABBCC
private const val ACCOUNT_NAME = "AccountName"
private const val ACCOUNT_NUMBER = 2
private const val NOTIFICATION_SUMMARY = "Summary"
private const val SENDER = "MessageSender"
private const val SUBJECT = "Subject"
private const val NOTIFICATION_PREVIEW = "Preview"

class BaseNotificationsTest {
    private val resourceProvider = TestNotificationResourceProvider()
    private val notifications = createTestNotifications()

    @Test
    fun testCreateAndInitializeNotificationBuilder() {
        val account = createFakeAccount()

        val builder = notifications.createAndInitializeNotificationBuilder(account)

        verify(builder).setSmallIcon(resourceProvider.iconNewMail)
        verify(builder).color = ACCOUNT_COLOR
        verify(builder).setAutoCancel(true)
    }

    @Test
    fun testIsDeleteActionEnabled_NotificationQuickDelete_ALWAYS() {
        K9.notificationQuickDeleteBehaviour = NotificationQuickDelete.ALWAYS

        val result = notifications.isDeleteActionEnabled()

        assertThat(result).isTrue()
    }

    @Test
    fun testIsDeleteActionEnabled_NotificationQuickDelete_FOR_SINGLE_MSG() {
        K9.notificationQuickDeleteBehaviour = NotificationQuickDelete.FOR_SINGLE_MSG

        val result = notifications.isDeleteActionEnabled()

        assertThat(result).isTrue()
    }

    @Test
    fun testIsDeleteActionEnabled_NotificationQuickDelete_NEVER() {
        K9.notificationQuickDeleteBehaviour = NotificationQuickDelete.NEVER

        val result = notifications.isDeleteActionEnabled()

        assertThat(result).isFalse()
    }

    @Test
    fun testCreateBigTextStyleNotification() {
        val account = createFakeAccount()
        val notificationId = 23
        val holder = createNotificationHolder(notificationId)

        val builder = notifications.createBigTextStyleNotification(account, holder, notificationId)

        verify(builder).setTicker(NOTIFICATION_SUMMARY)
        verify(builder).setGroup("newMailNotifications-$ACCOUNT_NUMBER")
        verify(builder).setContentTitle(SENDER)
        verify(builder).setContentText(SUBJECT)
        verify(builder).setSubText(ACCOUNT_NAME)
        verify(notifications.bigTextStyle).bigText(NOTIFICATION_PREVIEW)
        verify(builder).setStyle(notifications.bigTextStyle)
    }

    private fun createNotificationHolder(notificationId: Int): NotificationHolder {
        return NotificationHolder(
            notificationId = notificationId,
            content = NotificationContent(
                messageReference = MessageReference("irrelevant", 1, "irrelevant", null),
                sender = SENDER,
                subject = SUBJECT,
                preview = NOTIFICATION_PREVIEW,
                summary = NOTIFICATION_SUMMARY,
                isStarred = false
            )
        )
    }

    private fun createTestNotifications(): TestNotifications {
        return TestNotifications(
            notificationHelper = createFakeNotificationHelper(),
            actionCreator = mock(),
            resourceProvider = resourceProvider
        )
    }

    private fun createFakeNotificationHelper(): NotificationHelper {
        return mock {
            on { createNotificationBuilder(any(), any()) } doReturn mockBuilder()
            on { getAccountName(any()) } doReturn ACCOUNT_NAME
        }
    }

    private fun createFakeAccount(): Account {
        return mock {
            on { accountNumber } doReturn ACCOUNT_NUMBER
            on { chipColor } doReturn ACCOUNT_COLOR
        }
    }

    internal class TestNotifications(
        notificationHelper: NotificationHelper,
        actionCreator: NotificationActionCreator,
        resourceProvider: NotificationResourceProvider
    ) : BaseNotifications(notificationHelper, actionCreator, resourceProvider) {
        val bigTextStyle = mock<NotificationCompat.BigTextStyle>()

        override fun createBigTextStyle(builder: NotificationCompat.Builder?): NotificationCompat.BigTextStyle {
            return bigTextStyle
        }
    }
}
