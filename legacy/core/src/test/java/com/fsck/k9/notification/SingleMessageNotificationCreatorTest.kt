package com.fsck.k9.notification

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.Address
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.testing.MockHelper.mockBuilder
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.preference.notification.NotificationPreference
import net.thunderbird.core.preference.notification.NotificationPreferenceManager
import net.thunderbird.core.testing.coroutines.MainDispatcherHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SingleMessageNotificationCreatorTest : RobolectricTest() {
    private val mainDispatcher = MainDispatcherHelper(UnconfinedTestDispatcher())
    private val notificationPreferenceManager = FakeNotificationPreferenceManager()
    private val resourceProvider = TestAvatarNotificationResourceProvider()
    private val notification = mock<Notification>()
    private val builder = mockBuilder<NotificationCompat.Builder> {
        on { build() } doReturn notification
    }

    private lateinit var testSubject: SingleMessageNotificationCreator

    @Before
    fun setUp() {
        mainDispatcher.setUp()
        testSubject = SingleMessageNotificationCreator(
            notificationHelper = createNotificationHelper(),
            actionCreator = createNotificationActionCreator(),
            resourceProvider = resourceProvider,
            lockScreenNotificationCreator = mock(),
            notificationPreferenceManager = notificationPreferenceManager,
            application = ApplicationProvider.getApplicationContext<Application>(),
        )
    }

    @After
    fun tearDown() {
        mainDispatcher.tearDown()
    }

    @Test
    fun `create notification looks up avatar when notification contact pictures are enabled`() = runTest {
        notificationPreferenceManager.setShowContactPictureInNotification(true)

        testSubject.createSingleNotification(
            baseNotificationData = createBaseNotificationData(),
            singleNotificationData = createSingleNotificationData(),
        ).join()

        assertThat(resourceProvider.avatarCalls).isEqualTo(1)
    }

    @Test
    fun `create notification skips avatar lookup when notification contact pictures are disabled`() = runTest {
        notificationPreferenceManager.setShowContactPictureInNotification(false)

        testSubject.createSingleNotification(
            baseNotificationData = createBaseNotificationData(),
            singleNotificationData = createSingleNotificationData(),
        ).join()

        assertThat(resourceProvider.avatarCalls).isEqualTo(0)
    }

    private fun createNotificationHelper(): NotificationHelper {
        return mock {
            on { createNotificationBuilder(any(), any()) } doReturn builder
        }
    }

    private fun createNotificationActionCreator(): NotificationActionCreator {
        val pendingIntent = mock<PendingIntent>()
        return mock {
            on { createViewMessagePendingIntent(any()) } doReturn pendingIntent
            on { createDismissMessagePendingIntent(any()) } doReturn pendingIntent
        }
    }

    private fun createBaseNotificationData(): BaseNotificationData {
        return BaseNotificationData(
            account = LegacyAccountDto("00000000-0000-0000-0000-000000000000"),
            accountName = "Account name",
            groupKey = "group",
            color = 0,
            newMessagesCount = 1,
            lockScreenNotificationData = LockScreenNotificationData.None,
            appearance = NotificationAppearance(
                ringtone = null,
                vibrationPattern = null,
                ledColor = null,
            ),
        )
    }

    private fun createSingleNotificationData(): SingleNotificationData {
        return SingleNotificationData(
            notificationId = 23,
            isSilent = true,
            timestamp = 9000,
            content = NotificationContent(
                messageReference = MessageReference("account", 1, "uid"),
                sender = Address("alice@example.com", "Alice"),
                subject = "Subject",
                preview = "Preview",
                summary = "Summary",
            ),
            actions = emptyList(),
            wearActions = emptyList(),
            addLockScreenNotification = false,
        )
    }

    private class TestAvatarNotificationResourceProvider :
        NotificationResourceProvider by TestNotificationResourceProvider() {
        var avatarCalls = 0

        override suspend fun avatar(address: Address): Bitmap? {
            avatarCalls += 1
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }

    private class FakeNotificationPreferenceManager : NotificationPreferenceManager {
        private val prefs = MutableStateFlow(NotificationPreference())

        override fun save(config: NotificationPreference) = Unit

        override fun getConfig(): NotificationPreference = prefs.value

        override fun getConfigFlow(): Flow<NotificationPreference> = prefs

        fun setShowContactPictureInNotification(isEnabled: Boolean) {
            prefs.update { it.copy(isShowContactPictureInNotification = isEnabled) }
        }
    }
}
