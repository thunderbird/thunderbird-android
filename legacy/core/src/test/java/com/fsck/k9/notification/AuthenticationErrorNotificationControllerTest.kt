package com.fsck.k9.notification

import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.fsck.k9.FakeLegacyAccount
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.testing.MockHelper.mockBuilder
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.display.DisplaySettings
import net.thunderbird.core.preference.network.NetworkSettings
import net.thunderbird.core.preference.notification.NotificationPreference
import net.thunderbird.core.preference.privacy.PrivacySettings
import net.thunderbird.core.testing.TestClock
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
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
private const val INCOMING_NOTIFICATION_ID = 101
private const val OUTGOING_NOTIFICATION_ID = 102

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

    private val accountId = AccountIdFactory.create()
    private val account = FakeLegacyAccount.ACCOUNT.copy(
        id = accountId,
        accountNumber = ACCOUNT_NUMBER,
        profile = ProfileDto(
            id = accountId,
            name = ACCOUNT_NAME,
            color = -1,
            avatar = AvatarDto(
                id = accountId,
                avatarType = AvatarTypeDto.MONOGRAM,
                avatarMonogram = "TA",
                avatarImageUri = null,
                avatarIconName = null,
            ),
        ),
    )

    private val accountManager = mock<LegacyAccountManager> {
        on { findById(accountId) } doReturn account
    }

    private val notificationIdRegistry = mock<AccountNotificationIdRegistry> {
        on {
            getOrAllocate(accountId, AccountNotificationKind.AuthenticationErrorIncoming)
        } doReturn INCOMING_NOTIFICATION_ID
        on {
            getOrAllocate(accountId, AccountNotificationKind.AuthenticationErrorOutgoing)
        } doReturn OUTGOING_NOTIFICATION_ID
    }

    private val controller = TestAuthenticationErrorNotificationController()
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
    fun showAuthenticationErrorNotification_withIncomingServer_shouldCreateNotification() {
        controller.showAuthenticationErrorNotification(account.id, INCOMING)

        verify(notificationHelper).notify(INCOMING_NOTIFICATION_ID, notification)
        assertAuthenticationErrorNotificationContents()
    }

    @Test
    fun clearAuthenticationErrorNotification_withIncomingServer_shouldCancelNotification() {
        controller.clearAuthenticationErrorNotification(account.id, INCOMING)

        verify(notificationManager).cancel(INCOMING_NOTIFICATION_ID)
    }

    @Test
    fun showAuthenticationErrorNotification_withOutgoingServer_shouldCreateNotification() {
        controller.showAuthenticationErrorNotification(account.id, OUTGOING)

        verify(notificationHelper).notify(OUTGOING_NOTIFICATION_ID, notification)
        assertAuthenticationErrorNotificationContents()
    }

    @Test
    fun clearAuthenticationErrorNotification_withOutgoingServer_shouldCancelNotification() {
        controller.clearAuthenticationErrorNotification(account.id, OUTGOING)

        verify(notificationManager).cancel(OUTGOING_NOTIFICATION_ID)
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
            on {
                createNotificationBuilder(
                    any(),
                    any(),
                    any(),
                )
            }.doReturn(
                notificationBuilder,
                lockScreenNotificationBuilder,
            )
        }
    }

    internal inner class TestAuthenticationErrorNotificationController :
        AuthenticationErrorNotificationController(
            notificationHelper = notificationHelper,
            actionCreator = mock(),
            resourceProvider = resourceProvider,
            generalSettingsManager = mock {
                on { getSettings() } doReturn GeneralSettings(
                    network = NetworkSettings(),
                    display = DisplaySettings(),
                    notification = NotificationPreference(),
                    privacy = PrivacySettings(),
                    platformConfigProvider = FakePlatformConfigProvider(),
                )
            },
            accountManager = accountManager,
            notificationIdRegistry = notificationIdRegistry,
        ) {

        override fun createContentIntent(account: LegacyAccount, incoming: Boolean): PendingIntent {
            return contentIntent
        }
    }
}
