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
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never

private const val INCOMING = true
private const val OUTGOING = false
private const val ACCOUNT_NUMBER = 1
private const val ACCOUNT_NAME = "TestAccount"

class AuthenticationErrorNotificationControllerTest : RobolectricTest() {
    private val resourceProvider = TestNotificationResourceProvider()
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
    private val controller = TestAuthenticationErrorNotificationController()
    private val contentIntent = mock<PendingIntent>()

    @Test
    fun showAuthenticationErrorNotification_withIncomingServer_shouldCreateNotification() {
        val notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, INCOMING)

        controller.showAuthenticationErrorNotification(account, INCOMING)

        verify(notificationHelper).notify(notificationId, notification)
        assertAuthenticationErrorNotificationContents()
    }

    @Test
    fun clearAuthenticationErrorNotification_withIncomingServer_shouldCancelNotification() {
        val notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, INCOMING)

        controller.clearAuthenticationErrorNotification(account, INCOMING)

        verify(notificationManager).cancel(notificationId)
    }

    @Test
    fun showAuthenticationErrorNotification_withOutgoingServer_shouldCreateNotification() {
        val notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, OUTGOING)

        controller.showAuthenticationErrorNotification(account, OUTGOING)

        verify(notificationHelper).notify(notificationId, notification)
        assertAuthenticationErrorNotificationContents()
    }

    @Test
    fun clearAuthenticationErrorNotification_withOutgoingServer_shouldCancelNotification() {
        val notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, OUTGOING)

        controller.clearAuthenticationErrorNotification(account, OUTGOING)

        verify(notificationManager).cancel(notificationId)
    }

    private fun assertAuthenticationErrorNotificationContents() {
        verify(builder).setSmallIcon(resourceProvider.iconWarning)
        verify(builder).setTicker("Authentication failed")
        verify(builder).setContentTitle("Authentication failed")
        verify(builder).setContentText("Authentication failed for $ACCOUNT_NAME. Update your server settings.")
        verify(builder).setContentIntent(contentIntent)
        verify(builder).setPublicVersion(lockScreenNotification)
        verify(lockScreenNotificationBuilder).setContentTitle("Authentication failed")
        verify(lockScreenNotificationBuilder, never()).setContentText(any())
        verify(lockScreenNotificationBuilder, never()).setTicker(any())
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
            on { displayName } doReturn ACCOUNT_NAME
        }
    }

    internal inner class TestAuthenticationErrorNotificationController :
        AuthenticationErrorNotificationController(notificationHelper, mock(), resourceProvider) {

        override fun createContentIntent(account: Account, incoming: Boolean): PendingIntent {
            return contentIntent
        }
    }
}
