package com.fsck.k9.notification

import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.K9.LockScreenNotificationVisibility
import com.fsck.k9.RobolectricTest
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.testing.MockHelper.mockBuilder
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing

private const val ACCOUNT_NAME = "Hugo"
private const val NEW_MESSAGE_COUNT = 3

class LockScreenNotificationCreatorTest : RobolectricTest() {
    private val resourceProvider = TestNotificationResourceProvider()
    private val builder = createFakeNotificationBuilder()
    private val publicBuilder = createFakeNotificationBuilder()
    private var notificationData = createFakeNotificationData()
    private var notificationCreator = LockScreenNotificationCreator(
        notificationHelper = createFakeNotificationHelper(publicBuilder),
        resourceProvider = resourceProvider
    )

    @Test
    fun configureLockScreenNotification_NOTHING() {
        K9.lockScreenNotificationVisibility = LockScreenNotificationVisibility.NOTHING

        notificationCreator.configureLockScreenNotification(builder, notificationData)

        verify(builder).setVisibility(NotificationCompat.VISIBILITY_SECRET)
    }

    @Test
    fun configureLockScreenNotification_APP_NAME() {
        K9.lockScreenNotificationVisibility = LockScreenNotificationVisibility.APP_NAME

        notificationCreator.configureLockScreenNotification(builder, notificationData)

        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
    }

    @Test
    fun configureLockScreenNotification_EVERYTHING() {
        K9.lockScreenNotificationVisibility = LockScreenNotificationVisibility.EVERYTHING

        notificationCreator.configureLockScreenNotification(builder, notificationData)

        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    @Test
    fun configureLockScreenNotification_SENDERS_withSingleMessage() {
        K9.lockScreenNotificationVisibility = LockScreenNotificationVisibility.SENDERS
        val holder = NotificationHolder(
            notificationId = 42,
            content = createNotificationContent(sender = "alice@example.com")
        )
        stubbing(notificationData) {
            on { newMessagesCount } doReturn 1
            on { holderForLatestNotification } doReturn holder
        }

        notificationCreator.configureLockScreenNotification(builder, notificationData)

        verify(publicBuilder).setSmallIcon(resourceProvider.iconNewMail)
        verify(publicBuilder).setNumber(1)
        verify(publicBuilder).setContentTitle("1 new message")
        verify(publicBuilder).setContentText("alice@example.com")
        verify(builder).setPublicVersion(publicBuilder.build())
    }

    @Test
    fun configureLockScreenNotification_SENDERS_withMultipleMessages() {
        K9.lockScreenNotificationVisibility = LockScreenNotificationVisibility.SENDERS
        val content1 = createNotificationContent("alice@example.com")
        val content2 = createNotificationContent("Bob <bob@example.com>")
        val content3 = createNotificationContent("\"Peter Lustig\" <peter@example.com>")
        stubbing(notificationData) {
            on { newMessagesCount } doReturn NEW_MESSAGE_COUNT
            on { getContentForSummaryNotification() } doReturn listOf(content1, content2, content3)
        }

        notificationCreator.configureLockScreenNotification(builder, notificationData)

        verify(publicBuilder).setSmallIcon(resourceProvider.iconNewMail)
        verify(publicBuilder).setNumber(NEW_MESSAGE_COUNT)
        verify(publicBuilder).setContentTitle("$NEW_MESSAGE_COUNT new messages")
        verify(publicBuilder).setContentText(
            "alice@example.com, Bob <bob@example.com>, \"Peter Lustig\" <peter@example.com>"
        )
        verify(builder).setPublicVersion(publicBuilder.build())
    }

    @Test
    fun configureLockScreenNotification_SENDERS_makeSureWeGetEnoughSenderNames() {
        assertThat(
            NotificationData.MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION >=
                LockScreenNotificationCreator.MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION
        ).isTrue()
    }

    @Test
    fun createCommaSeparatedListOfSenders_withMoreSendersThanShouldBeDisplayed() {
        val content1 = createNotificationContent("alice@example.com")
        val content2 = createNotificationContent("bob@example.com")
        val content3 = createNotificationContent("cloe@example.com")
        val content4 = createNotificationContent("dagobert@example.com")
        val content5 = createNotificationContent("ed@example.com")
        val content6 = createNotificationContent("fiona@example.com")

        val result = notificationCreator.createCommaSeparatedListOfSenders(
            listOf(content1, content2, content3, content4, content5, content6)
        )

        assertThat(result).isEqualTo(
            "alice@example.com, bob@example.com, cloe@example.com, dagobert@example.com, ed@example.com"
        )
    }

    @Test
    fun configureLockScreenNotification_MESSAGE_COUNT() {
        K9.lockScreenNotificationVisibility = LockScreenNotificationVisibility.MESSAGE_COUNT
        stubbing(notificationData) {
            on { newMessagesCount } doReturn NEW_MESSAGE_COUNT
        }

        notificationCreator.configureLockScreenNotification(builder, notificationData)

        verify(publicBuilder).setSmallIcon(resourceProvider.iconNewMail)
        verify(publicBuilder).setNumber(NEW_MESSAGE_COUNT)
        verify(publicBuilder).setContentTitle("$NEW_MESSAGE_COUNT new messages")
        verify(publicBuilder).setContentText(ACCOUNT_NAME)
        verify(builder).setPublicVersion(publicBuilder.build())
    }

    private fun createFakeAccount(): Account {
        return mock {
            on { description } doReturn ACCOUNT_NAME
        }
    }

    private fun createFakeNotificationBuilder(): NotificationCompat.Builder {
        return mockBuilder {
            on { build() } doReturn mock()
        }
    }

    private fun createFakeNotificationHelper(builder: NotificationCompat.Builder): NotificationHelper {
        return mock {
            on { getContext() } doReturn ApplicationProvider.getApplicationContext()
            on { getAccountName(any()) } doReturn ACCOUNT_NAME
            on { createNotificationBuilder(any(), any()) } doReturn builder
        }
    }

    private fun createFakeNotificationData(): NotificationData {
        val fakeAccount = createFakeAccount()
        return mock {
            on { account } doReturn fakeAccount
        }
    }

    private fun createNotificationContent(sender: String): NotificationContent {
        val messageReference = MessageReference(accountUuid = "irrelevant", folderId = 1, uid = "irrelevant")
        return NotificationContent(
            messageReference = messageReference,
            sender = sender,
            subject = "irrelevant",
            preview = "irrelevant",
            summary = "irrelevant"
        )
    }
}
