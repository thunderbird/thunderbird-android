package com.fsck.k9.notification

import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.testing.MockHelper.mockBuilder
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.display.DisplaySettings
import net.thunderbird.core.preference.network.NetworkSettings
import net.thunderbird.core.preference.notification.NotificationPreference
import net.thunderbird.core.preference.privacy.PrivacySettings
import net.thunderbird.core.testing.TestClock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
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

    @OptIn(ExperimentalTime::class)
    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<Clock> { TestClock() }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testShowCertificateErrorNotificationForIncomingServer() {
        val notificationId = NotificationIds.getCertificateErrorNotificationId(account, INCOMING)

        controller.showCertificateErrorNotification(account, INCOMING)

        verify(notificationHelper).notify(notificationId, notification)
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

        verify(notificationHelper).notify(notificationId, notification)
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

    private fun createFakeAccount(): LegacyAccountDto {
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
                network = NetworkSettings(),
                display = DisplaySettings(),
                notification = NotificationPreference(),
                privacy = PrivacySettings(),
                platformConfigProvider = FakePlatformConfigProvider(),
            )
        },
    ) {
        override fun createContentIntent(account: LegacyAccountDto, incoming: Boolean): PendingIntent {
            return contentIntent
        }
    }
}

class FakePlatformConfigProvider : PlatformConfigProvider {
    override val isDebug: Boolean
        get() = true
}
