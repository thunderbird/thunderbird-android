package com.fsck.k9.notification

import app.k9mail.core.android.testing.RobolectricTest
import app.k9mail.legacy.account.Account
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import com.fsck.k9.controller.MessageReference
import kotlin.test.assertNotNull
import org.junit.Test

private const val ACCOUNT_UUID = "1-2-3"
private const val ACCOUNT_NUMBER = 23
private const val FOLDER_ID = 42L
private const val TIMESTAMP = 0L

class NotificationDataStoreTest : RobolectricTest() {
    private val account = createAccount()
    private val notificationDataStore = NotificationDataStore()

    @Test
    fun testAddNotificationContent() {
        val content = createNotificationContent("1")

        val result = notificationDataStore.addNotification(account, content, TIMESTAMP)

        assertNotNull(result)
        assertThat(result.shouldCancelNotification).isFalse()

        val holder = result.notificationHolder

        assertThat(holder).isNotNull()
        assertThat(holder.notificationId).isEqualTo(NotificationIds.getSingleMessageNotificationId(account, 0))
        assertThat(holder.content).isEqualTo(content)
    }

    @Test
    fun testAddNotificationContentWithReplacingNotification() {
        notificationDataStore.addNotification(account, createNotificationContent("1"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("2"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("3"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("4"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("5"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("6"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("7"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("8"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("9"), TIMESTAMP)

        val result = notificationDataStore.addNotification(account, createNotificationContent("10"), TIMESTAMP)

        assertNotNull(result)
        assertThat(result.shouldCancelNotification).isTrue()
        assertThat(result.cancelNotificationId).isEqualTo(NotificationIds.getSingleMessageNotificationId(account, 0))
    }

    @Test
    fun testRemoveNotificationForMessage() {
        val content = createNotificationContent("1")
        notificationDataStore.addNotification(account, content, TIMESTAMP)

        val result = notificationDataStore.removeNotifications(account) { listOf(content.messageReference) }

        assertNotNull(result) { removeResult ->
            assertThat(removeResult.cancelNotificationIds)
                .containsExactly(NotificationIds.getSingleMessageNotificationId(account, 0))
            assertThat(removeResult.notificationHolders).isEmpty()
        }
    }

    @Test
    fun testRemoveNotificationForMessageWithRecreatingNotification() {
        notificationDataStore.addNotification(account, createNotificationContent("1"), TIMESTAMP)
        val content = createNotificationContent("2")
        notificationDataStore.addNotification(account, content, TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("3"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("4"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("5"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("6"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("7"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("8"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("9"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("10"), TIMESTAMP)
        val latestContent = createNotificationContent("11")
        notificationDataStore.addNotification(account, latestContent, TIMESTAMP)

        val result = notificationDataStore.removeNotifications(account) { listOf(latestContent.messageReference) }

        assertNotNull(result) { removeResult ->
            assertThat(removeResult.cancelNotificationIds)
                .containsExactly(NotificationIds.getSingleMessageNotificationId(account, 1))
            assertThat(removeResult.notificationHolders).hasSize(1)

            val holder = removeResult.notificationHolders.first()
            assertThat(holder.notificationId).isEqualTo(NotificationIds.getSingleMessageNotificationId(account, 1))
            assertThat(holder.content).isEqualTo(content)
        }
    }

    @Test
    fun `remove multiple notifications`() {
        repeat(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS + 1) { index ->
            notificationDataStore.addNotification(account, createNotificationContent(index.toString()), TIMESTAMP)
        }

        val result = notificationDataStore.removeNotifications(account) { it.dropLast(1) }

        assertNotNull(result) { removeResult ->
            assertThat(removeResult.notificationData.newMessagesCount).isEqualTo(1)
            assertThat(removeResult.cancelNotificationIds).hasSize(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS)
        }
    }

    @Test
    fun `remove all notifications`() {
        repeat(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS + 1) { index ->
            notificationDataStore.addNotification(account, createNotificationContent(index.toString()), TIMESTAMP)
        }

        val result = notificationDataStore.removeNotifications(account) { it }

        assertNotNull(result) { removeResult ->
            assertThat(removeResult.notificationData.newMessagesCount).isEqualTo(0)
            assertThat(removeResult.notificationHolders).hasSize(0)
            assertThat(removeResult.notificationStoreOperations).hasSize(MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS + 1)
            for (notificationStoreOperation in removeResult.notificationStoreOperations) {
                assertThat(notificationStoreOperation).isInstanceOf<NotificationStoreOperation.Remove>()
            }
        }
    }

    @Test
    fun testRemoveDoesNotLeakNotificationIds() {
        for (i in 1..MAX_NUMBER_OF_NEW_MESSAGE_NOTIFICATIONS + 1) {
            val content = createNotificationContent(i.toString())
            notificationDataStore.addNotification(account, content, TIMESTAMP)
            notificationDataStore.removeNotifications(account) { listOf(content.messageReference) }
        }
    }

    @Test
    fun testNewMessagesCount() {
        val contentOne = createNotificationContent("1")
        val resultOne = notificationDataStore.addNotification(account, contentOne, TIMESTAMP)
        assertNotNull(resultOne)
        assertThat(resultOne.notificationData.newMessagesCount).isEqualTo(1)

        val contentTwo = createNotificationContent("2")
        val resultTwo = notificationDataStore.addNotification(account, contentTwo, TIMESTAMP)
        assertNotNull(resultTwo)
        assertThat(resultTwo.notificationData.newMessagesCount).isEqualTo(2)
    }

    @Test
    fun testIsSingleMessageNotification() {
        val resultOne = notificationDataStore.addNotification(account, createNotificationContent("1"), TIMESTAMP)
        assertNotNull(resultOne)
        assertThat(resultOne.notificationData.isSingleMessageNotification).isTrue()

        val resultTwo = notificationDataStore.addNotification(account, createNotificationContent("2"), TIMESTAMP)
        assertNotNull(resultTwo)
        assertThat(resultTwo.notificationData.isSingleMessageNotification).isFalse()
    }

    @Test
    fun testGetHolderForLatestNotification() {
        val content = createNotificationContent("1")
        val addResult = notificationDataStore.addNotification(account, content, TIMESTAMP)

        assertNotNull(addResult)
        assertThat(addResult.notificationData.activeNotifications.first()).isEqualTo(addResult.notificationHolder)
    }

    @Test
    fun `adding notification for message with active notification should update notification`() {
        val content1 = createNotificationContent("1")
        val content2 = createNotificationContent("1")

        val resultOne = notificationDataStore.addNotification(account, content1, TIMESTAMP)
        val resultTwo = notificationDataStore.addNotification(account, content2, TIMESTAMP)

        assertNotNull(resultOne)
        assertNotNull(resultTwo)
        assertThat(resultTwo.notificationData.activeNotifications).hasSize(1)
        assertThat(resultTwo.notificationData.activeNotifications.first().content).isSameInstanceAs(content2)
        assertThat(resultTwo.notificationStoreOperations).isEmpty()
        with(resultTwo.notificationHolder) {
            assertThat(notificationId).isEqualTo(resultOne.notificationHolder.notificationId)
            assertThat(timestamp).isEqualTo(resultOne.notificationHolder.timestamp)
            assertThat(content).isSameInstanceAs(content2)
        }
        assertThat(resultTwo.shouldCancelNotification).isFalse()
    }

    @Test
    fun `adding notification for message with inactive notification should update notificationData`() {
        notificationDataStore.addNotification(account, createNotificationContent("1"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("2"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("3"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("4"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("5"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("6"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("7"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("8"), TIMESTAMP)
        notificationDataStore.addNotification(account, createNotificationContent("9"), TIMESTAMP)
        val latestNotificationContent = createNotificationContent("10")
        notificationDataStore.addNotification(account, latestNotificationContent, TIMESTAMP)
        val content = createNotificationContent("1")

        val resultOne = notificationDataStore.addNotification(account, content, TIMESTAMP)

        assertThat(resultOne).isNull()

        val resultTwo = notificationDataStore.removeNotifications(account) {
            listOf(latestNotificationContent.messageReference)
        }

        assertNotNull(resultTwo)
        val notificationHolder = resultTwo.notificationData.activeNotifications.first { notificationHolder ->
            notificationHolder.content.messageReference == content.messageReference
        }
        assertThat(notificationHolder.content).isSameInstanceAs(content)
    }

    private fun createAccount(): Account {
        return Account("00000000-0000-4000-0000-000000000000").apply {
            accountNumber = ACCOUNT_NUMBER
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
            summary = "irrelevant",
        )
    }
}
