package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.RobolectricTest
import com.fsck.k9.controller.MessageReference
import com.google.common.truth.Truth.assertThat
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

        val result = notificationDataStore.addNotification(account, createNotificationContent("9"), TIMESTAMP)

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
        val latestContent = createNotificationContent("10")
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
        assertThat(resultOne.notificationData.newMessagesCount).isEqualTo(1)

        val contentTwo = createNotificationContent("2")
        val resultTwo = notificationDataStore.addNotification(account, contentTwo, TIMESTAMP)
        assertThat(resultTwo.notificationData.newMessagesCount).isEqualTo(2)
    }

    @Test
    fun testIsSingleMessageNotification() {
        val resultOne = notificationDataStore.addNotification(account, createNotificationContent("1"), TIMESTAMP)
        assertThat(resultOne.notificationData.isSingleMessageNotification).isTrue()

        val resultTwo = notificationDataStore.addNotification(account, createNotificationContent("2"), TIMESTAMP)
        assertThat(resultTwo.notificationData.isSingleMessageNotification).isFalse()
    }

    @Test
    fun testGetHolderForLatestNotification() {
        val content = createNotificationContent("1")
        val addResult = notificationDataStore.addNotification(account, content, TIMESTAMP)

        assertThat(addResult.notificationData.activeNotifications.first()).isEqualTo(addResult.notificationHolder)
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
            summary = "irrelevant"
        )
    }
}
