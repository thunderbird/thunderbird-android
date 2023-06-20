package com.fsck.k9.notification

import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import app.k9mail.core.android.testing.RobolectricTest
import com.fsck.k9.Account
import com.fsck.k9.testing.MockHelper.mockBuilder
import org.junit.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class LockScreenNotificationCreatorTest : RobolectricTest() {
    private val account = Account("00000000-0000-0000-0000-000000000000")
    private val resourceProvider = TestNotificationResourceProvider()
    private val builder = createFakeNotificationBuilder()
    private val publicBuilder = createFakeNotificationBuilder()
    private var notificationCreator = LockScreenNotificationCreator(
        notificationHelper = createFakeNotificationHelper(publicBuilder),
        resourceProvider = resourceProvider,
    )

    @Test
    fun `no lock screen notification`() {
        val baseNotificationData = createBaseNotificationData(LockScreenNotificationData.None)

        notificationCreator.configureLockScreenNotification(builder, baseNotificationData)

        verify(builder).setVisibility(NotificationCompat.VISIBILITY_SECRET)
    }

    @Test
    fun `app name`() {
        val baseNotificationData = createBaseNotificationData(LockScreenNotificationData.AppName)

        notificationCreator.configureLockScreenNotification(builder, baseNotificationData)

        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
    }

    @Test
    fun `regular notification on lock screen`() {
        val baseNotificationData = createBaseNotificationData(LockScreenNotificationData.Public)

        notificationCreator.configureLockScreenNotification(builder, baseNotificationData)

        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    @Test
    fun `list of sender names`() {
        val baseNotificationData = createBaseNotificationData(
            lockScreenNotificationData = LockScreenNotificationData.SenderNames("Alice, Bob"),
            newMessagesCount = 2,
        )

        notificationCreator.configureLockScreenNotification(builder, baseNotificationData)

        verify(publicBuilder).setSmallIcon(resourceProvider.iconNewMail)
        verify(publicBuilder).setNumber(2)
        verify(publicBuilder).setContentTitle("2 new messages")
        verify(publicBuilder).setContentText("Alice, Bob")
        verify(builder).setPublicVersion(publicBuilder.build())
    }

    @Test
    fun `new message count`() {
        val baseNotificationData = createBaseNotificationData(
            lockScreenNotificationData = LockScreenNotificationData.MessageCount,
            accountName = "Account name",
            newMessagesCount = 23,
        )

        notificationCreator.configureLockScreenNotification(builder, baseNotificationData)

        verify(publicBuilder).setSmallIcon(resourceProvider.iconNewMail)
        verify(publicBuilder).setNumber(23)
        verify(publicBuilder).setContentTitle("23 new messages")
        verify(publicBuilder).setContentText("Account name")
        verify(builder).setPublicVersion(publicBuilder.build())
    }

    private fun createFakeNotificationBuilder(): NotificationCompat.Builder {
        return mockBuilder {
            on { build() } doReturn mock()
        }
    }

    private fun createFakeNotificationHelper(builder: NotificationCompat.Builder): NotificationHelper {
        return mock {
            on { getContext() } doReturn ApplicationProvider.getApplicationContext()
            on { createNotificationBuilder(any(), any()) } doReturn builder
        }
    }

    private fun createBaseNotificationData(
        lockScreenNotificationData: LockScreenNotificationData,
        accountName: String = "irrelevant",
        newMessagesCount: Int = 0,
    ): BaseNotificationData {
        return BaseNotificationData(
            account = account,
            accountName = accountName,
            groupKey = "irrelevant",
            color = 0,
            newMessagesCount = newMessagesCount,
            lockScreenNotificationData = lockScreenNotificationData,
            appearance = NotificationAppearance(
                ringtone = null,
                vibrationPattern = longArrayOf(),
                ledColor = 0,
            ),
        )
    }
}
