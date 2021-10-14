package com.fsck.k9.notification

import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.fsck.k9.Account
import com.fsck.k9.RobolectricTest
import com.fsck.k9.testing.MockHelper.mockBuilder
import org.junit.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

private const val INCOMING = true
private const val OUTGOING = false
private const val ACCOUNT_NUMBER = 1
private const val ACCOUNT_NAME = "TestAccount"

class CertificateErrorNotificationControllerTest : RobolectricTest() {
    private val resourceProvider: NotificationResourceProvider = TestNotificationResourceProvider()
    private val notification = mock<Notification>()
    private val notificationManager = mock<NotificationManagerCompat>()
    private val builder = createFakeNotificationBuilder(notification)
    private val notificationHelper = createFakeNotificationHelper(notificationManager, builder)
    private val account = createFakeAccount()
    private val controller = TestCertificateErrorNotificationController()
    private val contentIntent = mock<PendingIntent>()

    @Test
    fun testShowCertificateErrorNotificationForIncomingServer() {
        val notificationId = NotificationIds.getCertificateErrorNotificationId(account, INCOMING)

        controller.showCertificateErrorNotification(account, INCOMING)

        verify(notificationManager).notify(notificationId, notification)
        assertCertificateErrorNotificationContents()
    }

    @Test
    fun testClearCertificateErrorNotificationsForIncomingServer() {
        val notificationId = NotificationIds.getCertificateErrorNotificationId(account, INCOMING)

        controller.clearCertificateErrorNotifications(account, INCOMING)

        verify(notificationManager).cancel(notificationId)
    }

    @Test
    fun testShowCertificateErrorNotificationForOutgoingServer() {
        val notificationId = NotificationIds.getCertificateErrorNotificationId(account, OUTGOING)

        controller.showCertificateErrorNotification(account, OUTGOING)

        verify(notificationManager).notify(notificationId, notification)
        assertCertificateErrorNotificationContents()
    }

    @Test
    fun testClearCertificateErrorNotificationsForOutgoingServer() {
        val notificationId = NotificationIds.getCertificateErrorNotificationId(account, OUTGOING)

        controller.clearCertificateErrorNotifications(account, OUTGOING)

        verify(notificationManager).cancel(notificationId)
    }

    private fun assertCertificateErrorNotificationContents() {
        verify(builder).setSmallIcon(resourceProvider.iconWarning)
        verify(builder).setTicker("Certificate error for $ACCOUNT_NAME")
        verify(builder).setContentTitle("Certificate error for $ACCOUNT_NAME")
        verify(builder).setContentText("Check your server settings")
        verify(builder).setContentIntent(contentIntent)
        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
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
        }
    }

    private fun createFakeAccount(): Account {
        return mock {
            on { accountNumber } doReturn ACCOUNT_NUMBER
            on { description } doReturn ACCOUNT_NAME
            on { uuid } doReturn "test-uuid"
        }
    }

    internal inner class TestCertificateErrorNotificationController : CertificateErrorNotificationController(
        notificationHelper, mock(), resourceProvider
    ) {
        override fun createContentIntent(account: Account, incoming: Boolean): PendingIntent {
            return contentIntent
        }
    }
}
