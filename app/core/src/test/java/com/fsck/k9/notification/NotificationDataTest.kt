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

class NotificationDataTest : RobolectricTest() {
    private val account = createFakeAccount()
    private val notificationData = NotificationData(account)

    @Test
    fun testAddNotificationContent() {
        val content = createNotificationContent("1")

        val result = notificationData.addNotificationContent(content)

        assertThat(result.shouldCancelNotification).isFalse()

        val holder = result.notificationHolder

        assertThat(holder).isNotNull()
        assertThat(holder.notificationId).isEqualTo(NotificationIds.getSingleMessageNotificationId(account, 0))
        assertThat(holder.content).isEqualTo(content)
    }

    @Test
    fun testAddNotificationContentWithReplacingNotification() {
        notificationData.addNotificationContent(createNotificationContent("1"))
        notificationData.addNotificationContent(createNotificationContent("2"))
        notificationData.addNotificationContent(createNotificationContent("3"))
        notificationData.addNotificationContent(createNotificationContent("4"))
        notificationData.addNotificationContent(createNotificationContent("5"))
        notificationData.addNotificationContent(createNotificationContent("6"))
        notificationData.addNotificationContent(createNotificationContent("7"))
        notificationData.addNotificationContent(createNotificationContent("8"))

        val result = notificationData.addNotificationContent(createNotificationContent("9"))

        assertThat(result.shouldCancelNotification).isTrue()
        assertThat(result.notificationId).isEqualTo(NotificationIds.getSingleMessageNotificationId(account, 0))
    }

    @Test
    fun testRemoveNotificationForMessage() {
        val content = createNotificationContent("1")
        notificationData.addNotificationContent(content)

        val result = notificationData.removeNotificationForMessage(content.messageReference)

        assertThat(result.isUnknownNotification).isFalse()
        assertThat(result.notificationId).isEqualTo(NotificationIds.getSingleMessageNotificationId(account, 0))
        assertThat(result.shouldCreateNotification).isFalse()
    }

    @Test
    fun testRemoveNotificationForMessageWithRecreatingNotification() {
        notificationData.addNotificationContent(createNotificationContent("1"))
        val content = createNotificationContent("2")
        notificationData.addNotificationContent(content)
        notificationData.addNotificationContent(createNotificationContent("3"))
        notificationData.addNotificationContent(createNotificationContent("4"))
        notificationData.addNotificationContent(createNotificationContent("5"))
        notificationData.addNotificationContent(createNotificationContent("6"))
        notificationData.addNotificationContent(createNotificationContent("7"))
        notificationData.addNotificationContent(createNotificationContent("8"))
        notificationData.addNotificationContent(createNotificationContent("9"))
        val latestContent = createNotificationContent("10")
        notificationData.addNotificationContent(latestContent)

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
            notificationData.addNotificationContent(content)
            notificationData.removeNotificationForMessage(content.messageReference)
        }
    }

    @Test
    fun testNewMessagesCount() {
        assertThat(notificationData.newMessagesCount).isEqualTo(0)

        val contentOne = createNotificationContent("1")
        notificationData.addNotificationContent(contentOne)
        assertThat(notificationData.newMessagesCount).isEqualTo(1)

        val contentTwo = createNotificationContent("2")
        notificationData.addNotificationContent(contentTwo)
        assertThat(notificationData.newMessagesCount).isEqualTo(2)
    }

    @Test
    fun testIsSingleMessageNotification() {
        assertThat(notificationData.isSingleMessageNotification).isFalse()

        notificationData.addNotificationContent(createNotificationContent("1"))
        assertThat(notificationData.isSingleMessageNotification).isTrue()

        notificationData.addNotificationContent(createNotificationContent("2"))
        assertThat(notificationData.isSingleMessageNotification).isFalse()
    }

    @Test
    fun testGetHolderForLatestNotification() {
        val content = createNotificationContent("1")
        val addResult = notificationData.addNotificationContent(content)

        val holder = notificationData.holderForLatestNotification

        assertThat(holder).isEqualTo(addResult.notificationHolder)
    }

    @Test
    fun testGetContentForSummaryNotification() {
        notificationData.addNotificationContent(createNotificationContent("1"))
        val content4 = createNotificationContent("2")
        notificationData.addNotificationContent(content4)
        val content3 = createNotificationContent("3")
        notificationData.addNotificationContent(content3)
        val content2 = createNotificationContent("4")
        notificationData.addNotificationContent(content2)
        val content1 = createNotificationContent("5")
        notificationData.addNotificationContent(content1)
        val content0 = createNotificationContent("6")
        notificationData.addNotificationContent(content0)

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
        notificationData.addNotificationContent(createNotificationContent("1"))
        notificationData.addNotificationContent(createNotificationContent("2"))

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
        notificationData.addNotificationContent(createNotificationContent(messageReference8))
        notificationData.addNotificationContent(createNotificationContent(messageReference7))
        notificationData.addNotificationContent(createNotificationContent(messageReference6))
        notificationData.addNotificationContent(createNotificationContent(messageReference5))
        notificationData.addNotificationContent(createNotificationContent(messageReference4))
        notificationData.addNotificationContent(createNotificationContent(messageReference3))
        notificationData.addNotificationContent(createNotificationContent(messageReference2))
        notificationData.addNotificationContent(createNotificationContent(messageReference1))
        notificationData.addNotificationContent(createNotificationContent(messageReference0))

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
        notificationData.addNotificationContent(createNotificationContent(messageReference8))
        notificationData.addNotificationContent(createNotificationContent(messageReference7))
        notificationData.addNotificationContent(createNotificationContent(messageReference6))
        notificationData.addNotificationContent(createNotificationContent(messageReference5))
        notificationData.addNotificationContent(createNotificationContent(messageReference4))
        notificationData.addNotificationContent(createNotificationContent(messageReference3))
        notificationData.addNotificationContent(createNotificationContent(messageReference2))
        notificationData.addNotificationContent(createNotificationContent(messageReference1))
        notificationData.addNotificationContent(createNotificationContent(messageReference0))

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
