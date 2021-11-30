package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.RobolectricTest
import com.fsck.k9.controller.MessageReference
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

private const val ACCOUNT_UUID = "1-2-3"
private const val ACCOUNT_NUMBER = 23
private const val FOLDER_ID = 42L
private const val TIMESTAMP = 0L

class NotificationDataTest : RobolectricTest() {
    private val account = createFakeAccount()
    private val notificationData = NotificationData(account)

    @Test
    fun testAddNotificationContent() {
        val content = createNotificationContent("1")

        val result = notificationData.addNotificationContent(content, TIMESTAMP)

        assertThat(result.shouldCancelNotification).isFalse()

        val holder = result.notificationHolder

        assertThat(holder).isNotNull()
        assertThat(holder.notificationId).isEqualTo(NotificationIds.getSingleMessageNotificationId(account, 0))
        assertThat(holder.content).isEqualTo(content)
    }

    @Test
    fun testAddNotificationContentWithReplacingNotification() {
        notificationData.addNotificationContent(createNotificationContent("1"), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent("2"), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent("3"), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent("4"), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent("5"), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent("6"), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent("7"), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent("8"), TIMESTAMP)

        val result = notificationData.addNotificationContent(createNotificationContent("9"), TIMESTAMP)

        assertThat(result.shouldCancelNotification).isTrue()
        assertThat(result.notificationId).isEqualTo(NotificationIds.getSingleMessageNotificationId(account, 0))
    }

    @Test
    fun testRemoveNotificationForMessage() {
        val content = createNotificationContent("1")
        notificationData.addNotificationContent(content, TIMESTAMP)

        val result = notificationData.removeNotificationForMessage(content.messageReference)

        assertThat(result.isUnknownNotification).isFalse()
        assertThat(result.notificationId).isEqualTo(NotificationIds.getSingleMessageNotificationId(account, 0))
        assertThat(result.shouldCreateNotification).isFalse()
    }

    @Test
    fun testRemoveNotificationForMessageWithRecreatingNotification() {
        notificationData.addNotificationContent(createNotificationContent("1"), TIMESTAMP)
        val content = createNotificationContent("2")
        notificationData.addNotificationContent(content, TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent("3"), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent("4"), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent("5"), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent("6"), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent("7"), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent("8"), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent("9"), TIMESTAMP)
        val latestContent = createNotificationContent("10")
        notificationData.addNotificationContent(latestContent, TIMESTAMP)

        val result = notificationData.removeNotificationForMessage(latestContent.messageReference)

        assertThat(result.isUnknownNotification).isFalse()
        assertThat(result.notificationId).isEqualTo(NotificationIds.getSingleMessageNotificationId(account, 1))
        assertThat(result.shouldCreateNotification).isTrue()
        assertNotNull(result.notificationHolder) { holder ->
            assertThat(holder.notificationId).isEqualTo(NotificationIds.getSingleMessageNotificationId(account, 1))
            assertThat(holder.content).isEqualTo(content)
        }
    }

    @Test
    fun testRemoveDoesNotLeakNotificationIds() {
        for (i in 1..NotificationData.MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS + 1) {
            val content = createNotificationContent(i.toString())
            notificationData.addNotificationContent(content, TIMESTAMP)
            notificationData.removeNotificationForMessage(content.messageReference)
        }
    }

    @Test
    fun testNewMessagesCount() {
        assertThat(notificationData.newMessagesCount).isEqualTo(0)

        val contentOne = createNotificationContent("1")
        notificationData.addNotificationContent(contentOne, TIMESTAMP)
        assertThat(notificationData.newMessagesCount).isEqualTo(1)

        val contentTwo = createNotificationContent("2")
        notificationData.addNotificationContent(contentTwo, TIMESTAMP)
        assertThat(notificationData.newMessagesCount).isEqualTo(2)
    }

    @Test
    fun testIsSingleMessageNotification() {
        assertThat(notificationData.isSingleMessageNotification).isFalse()

        notificationData.addNotificationContent(createNotificationContent("1"), TIMESTAMP)
        assertThat(notificationData.isSingleMessageNotification).isTrue()

        notificationData.addNotificationContent(createNotificationContent("2"), TIMESTAMP)
        assertThat(notificationData.isSingleMessageNotification).isFalse()
    }

    @Test
    fun testGetHolderForLatestNotification() {
        val content = createNotificationContent("1")
        val addResult = notificationData.addNotificationContent(content, TIMESTAMP)

        val holder = notificationData.holderForLatestNotification

        assertThat(holder).isEqualTo(addResult.notificationHolder)
    }

    @Test
    fun testGetContentForSummaryNotification() {
        notificationData.addNotificationContent(createNotificationContent("1"), TIMESTAMP)
        val content4 = createNotificationContent("2")
        notificationData.addNotificationContent(content4, TIMESTAMP)
        val content3 = createNotificationContent("3")
        notificationData.addNotificationContent(content3, TIMESTAMP)
        val content2 = createNotificationContent("4")
        notificationData.addNotificationContent(content2, TIMESTAMP)
        val content1 = createNotificationContent("5")
        notificationData.addNotificationContent(content1, TIMESTAMP)
        val content0 = createNotificationContent("6")
        notificationData.addNotificationContent(content0, TIMESTAMP)

        val contents = notificationData.getContentForSummaryNotification()

        assertThat(contents.size.toLong()).isEqualTo(5)
        assertThat(contents[0]).isEqualTo(content0)
        assertThat(contents[1]).isEqualTo(content1)
        assertThat(contents[2]).isEqualTo(content2)
        assertThat(contents[3]).isEqualTo(content3)
        assertThat(contents[4]).isEqualTo(content4)
    }

    @Test
    fun testGetActiveNotificationIds() {
        notificationData.addNotificationContent(createNotificationContent("1"), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent("2"), TIMESTAMP)

        val notificationIds = notificationData.getActiveNotificationIds()

        assertThat(notificationIds.size).isEqualTo(2)
        assertThat(notificationIds[0]).isEqualTo(NotificationIds.getSingleMessageNotificationId(account, 1))
        assertThat(notificationIds[1]).isEqualTo(NotificationIds.getSingleMessageNotificationId(account, 0))
    }

    @Test
    fun testGetAccount() {
        assertThat(notificationData.account).isEqualTo(account)
    }

    @Test
    fun testGetAllMessageReferences() {
        val messageReference0 = createMessageReference("1")
        val messageReference1 = createMessageReference("2")
        val messageReference2 = createMessageReference("3")
        val messageReference3 = createMessageReference("4")
        val messageReference4 = createMessageReference("5")
        val messageReference5 = createMessageReference("6")
        val messageReference6 = createMessageReference("7")
        val messageReference7 = createMessageReference("8")
        val messageReference8 = createMessageReference("9")
        notificationData.addNotificationContent(createNotificationContent(messageReference8), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference7), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference6), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference5), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference4), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference3), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference2), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference1), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference0), TIMESTAMP)

        val messageReferences = notificationData.getAllMessageReferences()

        assertThat(messageReferences).isEqualTo(
            listOf(
                messageReference0,
                messageReference1,
                messageReference2,
                messageReference3,
                messageReference4,
                messageReference5,
                messageReference6,
                messageReference7,
                messageReference8
            )
        )
    }

    @Test
    fun testOverflowNotifications() {
        val messageReference0 = createMessageReference("1")
        val messageReference1 = createMessageReference("2")
        val messageReference2 = createMessageReference("3")
        val messageReference3 = createMessageReference("4")
        val messageReference4 = createMessageReference("5")
        val messageReference5 = createMessageReference("6")
        val messageReference6 = createMessageReference("7")
        val messageReference7 = createMessageReference("8")
        val messageReference8 = createMessageReference("9")
        notificationData.addNotificationContent(createNotificationContent(messageReference8), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference7), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference6), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference5), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference4), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference3), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference2), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference1), TIMESTAMP)
        notificationData.addNotificationContent(createNotificationContent(messageReference0), TIMESTAMP)

        assertThat(notificationData.hasSummaryOverflowMessages()).isTrue()
        assertThat(notificationData.getSummaryOverflowMessagesCount()).isEqualTo(4)
    }

    private fun createFakeAccount(): Account {
        return mock {
            on { accountNumber } doReturn ACCOUNT_NUMBER
        }
    }

    private fun createMessageReference(uid: String): MessageReference {
        return MessageReference(ACCOUNT_UUID, FOLDER_ID, uid)
    }

    private fun createNotificationContent(uid: String): NotificationContent {
        val messageReference = createMessageReference(uid)
        return createNotificationContent(messageReference)
    }

    private fun createNotificationContent(messageReference: MessageReference): NotificationContent {
        return NotificationContent(
            messageReference = messageReference,
            sender = "irrelevant",
            subject = "irrelevant",
            preview = "irrelevant",
            summary = "irrelevant"
        )
    }
}
