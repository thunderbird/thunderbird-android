package com.fsck.k9.notification

import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.testing.MockHelper.mockBuilder
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.BackgroundSync
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.SubTheme
import net.thunderbird.core.preference.notification.NotificationPreference
import net.thunderbird.core.preference.privacy.PrivacySettings
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

class CertificateErrorNotificationControllerTest : RobolectricTest() {
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
        verify(builder).setPublicVersion(lockScreenNotification)
        verify(lockScreenNotificationBuilder).setContentTitle("Certificate error")
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

    private fun createFakeAccount(): LegacyAccount {
        return mock {
            on { accountNumber } doReturn ACCOUNT_NUMBER
            on { displayName } doReturn ACCOUNT_NAME
            on { uuid } doReturn "test-uuid"
        }
    }

    internal inner class TestCertificateErrorNotificationController : CertificateErrorNotificationController(
        notificationHelper,
        mock(),
        resourceProvider,
        mock {
            on { getSettings() } doReturn GeneralSettings(
                backgroundSync = BackgroundSync.ALWAYS,
                showRecentChanges = true,
                appTheme = AppTheme.DARK,
                messageComposeTheme = SubTheme.DARK,
                isShowCorrespondentNames = true,
                fixedMessageViewTheme = true,
                messageViewTheme = SubTheme.DARK,
                isShowStarredCount = false,
                isShowUnifiedInbox = false,
                isShowMessageListStars = false,
                isShowAnimations = false,
                shouldShowSetupArchiveFolderDialog = false,
                isMessageListSenderAboveSubject = false,
                isShowContactName = false,
                isShowContactPicture = false,
                isChangeContactNameColor = false,
                isColorizeMissingContactPictures = false,
                isUseBackgroundAsUnreadIndicator = false,
                isShowComposeButtonOnMessageList = false,
                isThreadedViewEnabled = false,
                isUseMessageViewFixedWidthFont = false,
                isAutoFitWidth = false,
                notification = NotificationPreference(),
                privacy = PrivacySettings(),
            )
        },
    ) {
        override fun createContentIntent(account: LegacyAccount, incoming: Boolean): PendingIntent {
            return contentIntent
        }
    }
}
