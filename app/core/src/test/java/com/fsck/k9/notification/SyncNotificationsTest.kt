package com.fsck.k9.notification

import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.fsck.k9.Account
import com.fsck.k9.RobolectricTest
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.notification.NotificationIds.getFetchingMailNotificationId
import com.fsck.k9.testing.MockHelper.mockBuilder
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

private const val ACCOUNT_NUMBER = 1
private const val ACCOUNT_NAME = "TestAccount"
private const val FOLDER_SERVER_ID = "INBOX"
private const val FOLDER_NAME = "Inbox"

class SyncNotificationsTest : RobolectricTest() {
    private val resourceProvider: NotificationResourceProvider = TestNotificationResourceProvider()
    private val notification = mock<Notification>()
    private val notificationManager = mock<NotificationManagerCompat>()
    private val builder = createFakeNotificationBuilder(notification)
    private val account = createFakeAccount()
    private val contentIntent = mock<PendingIntent>()
    private val syncNotifications = SyncNotifications(
        notificationHelper = createFakeNotificationHelper(notificationManager, builder),
        actionBuilder = createActionBuilder(contentIntent),
        resourceProvider = resourceProvider
    )

    @Test
    fun testShowSendingNotification() {
        val notificationId = getFetchingMailNotificationId(account)

        syncNotifications.showSendingNotification(account)

        verify(notificationManager).notify(notificationId, notification)
        verify(builder).setSmallIcon(resourceProvider.iconSendingMail)
        verify(builder).setTicker("Sending mail: $ACCOUNT_NAME")
        verify(builder).setContentTitle("Sending mail")
        verify(builder).setContentText(ACCOUNT_NAME)
        verify(builder).setContentIntent(contentIntent)
        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    @Test
    fun testClearSendingNotification() {
        val notificationId = getFetchingMailNotificationId(account)

        syncNotifications.clearSendingNotification(account)

        verify(notificationManager).cancel(notificationId)
    }

    @Test
    fun testGetFetchingMailNotificationId() {
        val localFolder = createFakeLocalFolder()
        val notificationId = getFetchingMailNotificationId(account)

        syncNotifications.showFetchingMailNotification(account, localFolder)

        verify(notificationManager).notify(notificationId, notification)
        verify(builder).setSmallIcon(resourceProvider.iconCheckingMail)
        verify(builder).setTicker("Checking mail: $ACCOUNT_NAME:$FOLDER_NAME")
        verify(builder).setContentTitle("Checking mail")
        verify(builder).setContentText("$ACCOUNT_NAME:$FOLDER_NAME")
        verify(builder).setContentIntent(contentIntent)
        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    @Test
    fun testShowEmptyFetchingMailNotification() {
        val notificationId = getFetchingMailNotificationId(account)

        syncNotifications.showEmptyFetchingMailNotification(account)

        verify(notificationManager).notify(notificationId, notification)
        verify(builder).setSmallIcon(resourceProvider.iconCheckingMail)
        verify(builder).setContentTitle("Checking mail")
        verify(builder).setContentText(ACCOUNT_NAME)
        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    @Test
    fun testClearSendFailedNotification() {
        val notificationId = getFetchingMailNotificationId(account)

        syncNotifications.clearFetchingMailNotification(account)

        verify(notificationManager).cancel(notificationId)
    }

    private fun createFakeNotificationBuilder(notification: Notification): NotificationCompat.Builder {
        return mockBuilder {
            on { build() } doReturn notification
        }
    }

    private fun createFakeNotificationHelper(
        notificationManager: NotificationManagerCompat,
        builder: NotificationCompat.Builder
    ): NotificationHelper {
        return mock {
            on { getContext() } doReturn ApplicationProvider.getApplicationContext()
            on { getNotificationManager() } doReturn notificationManager
            on { createNotificationBuilder(any(), any()) } doReturn builder
            on { getAccountName(any()) } doReturn ACCOUNT_NAME
        }
    }

    private fun createFakeAccount(): Account {
        return mock {
            on { accountNumber } doReturn ACCOUNT_NUMBER
            on { description } doReturn ACCOUNT_NAME
            on { outboxFolderId } doReturn 33L
        }
    }

    private fun createActionBuilder(contentIntent: PendingIntent): NotificationActionCreator {
        return mock {
            on { createViewFolderPendingIntent(eq(account), anyLong(), anyInt()) } doReturn contentIntent
        }
    }

    private fun createFakeLocalFolder(): LocalFolder {
        return mock {
            on { serverId } doReturn FOLDER_SERVER_ID
            on { name } doReturn FOLDER_NAME
        }
    }
}
