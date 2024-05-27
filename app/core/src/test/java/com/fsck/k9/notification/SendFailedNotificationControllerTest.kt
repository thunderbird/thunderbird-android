package com.fsck.k9.notification

import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import app.k9mail.core.android.testing.RobolectricTest
import com.fsck.k9.Account
import com.fsck.k9.testing.MockHelper.mockBuilder
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never

private const val ACCOUNT_NUMBER = 1
private const val ACCOUNT_NAME = "TestAccount"

class SendFailedNotificationControllerTest : RobolectricTest() {
    private val resourceProvider: NotificationResourceProvider = TestNotificationResourceProvider()
    private val notification = mock<Notification>()
    private val lockScreenNotification = mock<Notification>()
    private val notificationManager = mock<NotificationManagerCompat>()
    private val builder = createFakeNotificationBuilder(notification)
    private val lockScreenNotificationBuilder = createFakeNotificationBuilder(lockScreenNotification)
    private val notificationHelper = createFakeNotificationHelper(
        notificationManager,
        builder,
        lockScreenNotificationBuilder,
    )
    private val account = createFakeAccount()
    private val contentIntent = mock<PendingIntent>()
    private val notificationId = NotificationIds.getSendFailedNotificationId(account)
    private val controller = SendFailedNotificationController(
        notificationHelper = notificationHelper,
        actionBuilder = createActionBuilder(contentIntent),
        resourceProvider = resourceProvider,
    )

    @Test
    fun testShowSendFailedNotification() {
        val exception = Exception()

        controller.showSendFailedNotification(account, exception)

        verify(notificationHelper).notify(notificationId, notification)
        verify(builder).setSmallIcon(resourceProvider.iconWarning)
        verify(builder).setTicker("Failed to send some messages")
        verify(builder).setContentTitle("Failed to send some messages")
        verify(builder).setContentText("Exception")
        verify(builder).setContentIntent(contentIntent)
        verify(builder).setPublicVersion(lockScreenNotification)
        verify(lockScreenNotificationBuilder).setContentTitle("Failed to send some messages")
        verify(lockScreenNotificationBuilder, never()).setContentText(any())
        verify(lockScreenNotificationBuilder, never()).setTicker(any())
    }

    @Test
    fun testClearSendFailedNotification() {
        controller.clearSendFailedNotification(account)

        verify(notificationManager).cancel(notificationId)
    }

    private fun createFakeNotificationBuilder(notification: Notification): NotificationCompat.Builder {
        return mockBuilder {
            on { build() } doReturn notification
        }
    }

    private fun createFakeNotificationHelper(
        notificationManager: NotificationManagerCompat,
        notificationBuilder: NotificationCompat.Builder,
        lockScreenNotificationBuilder: NotificationCompat.Builder,
    ): NotificationHelper {
        return mock {
            on { getContext() } doReturn ApplicationProvider.getApplicationContext()
            on { getNotificationManager() } doReturn notificationManager
            on { createNotificationBuilder(any(), any()) }.doReturn(notificationBuilder, lockScreenNotificationBuilder)
        }
    }

    private fun createFakeAccount(): Account {
        return mock {
            on { accountNumber } doReturn ACCOUNT_NUMBER
            on { name } doReturn ACCOUNT_NAME
        }
    }

    private fun createActionBuilder(contentIntent: PendingIntent): NotificationActionCreator {
        return mock {
            on { createViewFolderListPendingIntent(any()) } doReturn contentIntent
            on { createViewFolderPendingIntent(any(), anyLong()) } doReturn contentIntent
        }
    }
}
