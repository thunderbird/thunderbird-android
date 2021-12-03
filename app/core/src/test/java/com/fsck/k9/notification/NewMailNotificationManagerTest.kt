package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.TestClock
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mailstore.LocalMessage
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing

private const val ACCOUNT_UUID = "00000000-0000-4000-0000-000000000000"
private const val ACCOUNT_NAME = "Personal"
private const val ACCOUNT_COLOR = 0xFF112233L.toInt()
private const val FOLDER_ID = 42L
private const val TIMESTAMP = 23L

class NewMailNotificationManagerTest {
    private val account = createAccount()
    private val notificationContentCreator = mock<NotificationContentCreator>()
    private val clock = TestClock(TIMESTAMP)
    private val manager = NewMailNotificationManager(
        notificationContentCreator,
        createNotificationRepository(),
        BaseNotificationDataCreator(),
        SingleMessageNotificationDataCreator(),
        SummaryNotificationDataCreator(SingleMessageNotificationDataCreator()),
        clock
    )

    @Test
    fun `add first notification`() {
        val message = addMessageToNotificationContentCreator(
            sender = "sender",
            subject = "subject",
            preview = "preview",
            summary = "summary",
            messageUid = "msg-1"
        )

        val result = manager.addNewMailNotification(account, message, silent = false)

        assertThat(result.singleNotificationData.first().content).isEqualTo(
            NotificationContent(
                messageReference = createMessageReference("msg-1"),
                sender = "sender",
                subject = "subject",
                preview = "preview",
                summary = "summary"
            )
        )
        assertThat(result.summaryNotificationData).isInstanceOf(SummarySingleNotificationData::class.java)
        val summaryNotificationData = result.summaryNotificationData as SummarySingleNotificationData
        assertThat(summaryNotificationData.singleNotificationData.isSilent).isFalse()
    }

    @Test
    fun `add second notification`() {
        val messageOne = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Hi Bob",
            preview = "How are you?",
            summary = "Alice Hi Bob",
            messageUid = "msg-1"
        )
        val messageTwo = addMessageToNotificationContentCreator(
            sender = "Zoe",
            subject = "Meeting",
            preview = "We need to talk",
            summary = "Zoe Meeting",
            messageUid = "msg-2"
        )
        manager.addNewMailNotification(account, messageOne, silent = false)
        val timestamp = TIMESTAMP + 1000
        clock.time = timestamp

        val result = manager.addNewMailNotification(account, messageTwo, silent = false)

        assertThat(result.singleNotificationData.first().content).isEqualTo(
            NotificationContent(
                messageReference = createMessageReference("msg-2"),
                sender = "Zoe",
                subject = "Meeting",
                preview = "We need to talk",
                summary = "Zoe Meeting"
            )
        )
        assertThat(result.baseNotificationData.newMessagesCount).isEqualTo(2)
        assertThat(result.summaryNotificationData).isInstanceOf(SummaryInboxNotificationData::class.java)
        val summaryNotificationData = result.summaryNotificationData as SummaryInboxNotificationData
        assertThat(summaryNotificationData.content).isEqualTo(listOf("Zoe Meeting", "Alice Hi Bob"))
        assertThat(summaryNotificationData.messageReferences).isEqualTo(
            listOf(
                createMessageReference("msg-2"),
                createMessageReference("msg-1")
            )
        )
        assertThat(summaryNotificationData.additionalMessagesCount).isEqualTo(0)
        assertThat(summaryNotificationData.isSilent).isFalse()
    }

    @Test
    fun `add one more notification when already displaying the maximum number of notifications`() {
        addMaximumNumberOfNotifications()
        val message = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Another one",
            preview = "Are you tired of me yet?",
            summary = "Alice Another one",
            messageUid = "msg-x"
        )

        val result = manager.addNewMailNotification(account, message, silent = false)

        val notificationId = NotificationIds.getSingleMessageNotificationId(account, index = 0)
        assertThat(result.cancelNotificationIds).isEqualTo(listOf(notificationId))
        assertThat(result.singleNotificationData.first().notificationId).isEqualTo(notificationId)
    }

    @Test
    fun `remove notification when none was added before should return null`() {
        val result = manager.removeNewMailNotification(account, createMessageReference("any"))

        assertThat(result).isNull()
    }

    @Test
    fun `remove notification with untracked notification ID should return null`() {
        val message = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Another one",
            preview = "Are you tired of me yet?",
            summary = "Alice Another one",
            messageUid = "msg-x"
        )
        manager.addNewMailNotification(account, message, silent = false)

        val result = manager.removeNewMailNotification(account, createMessageReference("untracked"))

        assertThat(result).isNull()
    }

    @Test
    fun `remove last remaining notification`() {
        val message = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Hello",
            preview = "How are you?",
            summary = "Alice Hello",
            messageUid = "msg-1"
        )
        manager.addNewMailNotification(account, message, silent = false)

        val result = manager.removeNewMailNotification(account, createMessageReference("msg-1"))

        assertNotNull(result) { data ->
            assertThat(data.cancelNotificationIds).containsExactly(
                NotificationIds.getNewMailSummaryNotificationId(account),
                NotificationIds.getSingleMessageNotificationId(account, 0)
            )
            assertThat(data.singleNotificationData).isEmpty()
            assertThat(data.summaryNotificationData).isNull()
        }
    }

    @Test
    fun `remove one of three notifications`() {
        val messageOne = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "One",
            preview = "preview",
            summary = "Alice One",
            messageUid = "msg-1"
        )
        manager.addNewMailNotification(account, messageOne, silent = false)
        val messageTwo = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Two",
            preview = "preview",
            summary = "Alice Two",
            messageUid = "msg-2"
        )
        val dataTwo = manager.addNewMailNotification(account, messageTwo, silent = true)
        val notificationIdTwo = dataTwo.singleNotificationData.first().notificationId
        val messageThree = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Three",
            preview = "preview",
            summary = "Alice Three",
            messageUid = "msg-3"
        )
        manager.addNewMailNotification(account, messageThree, silent = true)

        val result = manager.removeNewMailNotification(account, createMessageReference("msg-2"))

        assertNotNull(result) { data ->
            assertThat(data.cancelNotificationIds).isEqualTo(listOf(notificationIdTwo))
            assertThat(data.singleNotificationData).isEmpty()
            assertThat(data.baseNotificationData.newMessagesCount).isEqualTo(2)
            assertThat(data.summaryNotificationData).isInstanceOf(SummaryInboxNotificationData::class.java)
            val summaryNotificationData = data.summaryNotificationData as SummaryInboxNotificationData
            assertThat(summaryNotificationData.content).isEqualTo(listOf("Alice Three", "Alice One"))
            assertThat(summaryNotificationData.messageReferences).isEqualTo(
                listOf(
                    createMessageReference("msg-3"),
                    createMessageReference("msg-1")
                )
            )
        }
    }

    @Test
    fun `remove notification when additional notifications are available`() {
        val message = addMessageToNotificationContentCreator(
            sender = "Alice",
            subject = "Another one",
            preview = "Are you tired of me yet?",
            summary = "Alice Another one",
            messageUid = "msg-restore"
        )
        manager.addNewMailNotification(account, message, silent = false)
        addMaximumNumberOfNotifications()

        val result = manager.removeNewMailNotification(account, createMessageReference("msg-1"))

        assertNotNull(result) { data ->
            assertThat(data.cancelNotificationIds).hasSize(1)
            assertThat(data.baseNotificationData.newMessagesCount)
                .isEqualTo(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS)

            val singleNotificationData = data.singleNotificationData.first()
            assertThat(singleNotificationData.notificationId).isEqualTo(data.cancelNotificationIds.first())
            assertThat(singleNotificationData.isSilent).isTrue()
            assertThat(singleNotificationData.content).isEqualTo(
                NotificationContent(
                    messageReference = createMessageReference("msg-restore"),
                    sender = "Alice",
                    subject = "Another one",
                    preview = "Are you tired of me yet?",
                    summary = "Alice Another one"
                )
            )
        }
    }

    private fun createAccount(): Account {
        return Account(ACCOUNT_UUID).apply {
            description = ACCOUNT_NAME
            chipColor = ACCOUNT_COLOR
            notificationSetting.vibrateTimes = 1
        }
    }

    private fun addMaximumNumberOfNotifications() {
        repeat(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS) { index ->
            val message = addMessageToNotificationContentCreator(
                sender = "sender",
                subject = "subject",
                preview = "preview",
                summary = "summary",
                messageUid = "msg-$index"
            )
            manager.addNewMailNotification(account, message, silent = true)
        }
    }

    private fun addMessageToNotificationContentCreator(
        sender: String,
        subject: String,
        preview: String,
        summary: String,
        messageUid: String
    ): LocalMessage {
        val message = mock<LocalMessage>()

        stubbing(notificationContentCreator) {
            on { createFromMessage(account, message) } doReturn
                NotificationContent(
                    messageReference = createMessageReference(messageUid),
                    sender, subject, preview, summary
                )
        }

        return message
    }

    private fun createMessageReference(messageUid: String): MessageReference {
        return MessageReference(ACCOUNT_UUID, FOLDER_ID, messageUid)
    }

    private fun createNotificationRepository(): NotificationRepository {
        val notificationStoreProvider = object : NotificationStoreProvider {
            override fun getNotificationStore(account: Account): NotificationStore {
                return object : NotificationStore {
                    override fun persistNotificationChanges(operations: List<NotificationStoreOperation>) = Unit
                    override fun clearNotifications() = Unit
                }
            }
        }
        return NotificationRepository(notificationStoreProvider)
    }
}
