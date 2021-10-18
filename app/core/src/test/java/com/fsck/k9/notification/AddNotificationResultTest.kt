package com.fsck.k9.notification

import com.fsck.k9.controller.MessageReference
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private const val NOTIFICATION_ID = 23

class AddNotificationResultTest {
    private val notificationHolder = NotificationHolder(
        notificationId = NOTIFICATION_ID,
        content = NotificationContent(
            messageReference = MessageReference("irrelevant", 1, "irrelevant"),
            sender = "irrelevant",
            subject = "irrelevant",
            preview = "irrelevant",
            summary = "irrelevant"
        )
    )

    @Test
    fun newNotification_shouldCancelNotification_shouldReturnFalse() {
        val result = AddNotificationResult.newNotification(notificationHolder)

        assertThat(result.shouldCancelNotification).isFalse()
    }

    @Test(expected = IllegalStateException::class)
    fun newNotification_getNotificationId_shouldReturnNotificationId() {
        val result = AddNotificationResult.newNotification(notificationHolder)

        result.notificationId
    }

    @Test
    fun replaceNotification_shouldCancelNotification_shouldReturnTrue() {
        val result = AddNotificationResult.replaceNotification(notificationHolder)

        assertThat(result.shouldCancelNotification).isTrue()
    }

    @Test
    fun replaceNotification_getNotificationId_shouldReturnNotificationId() {
        val result = AddNotificationResult.replaceNotification(notificationHolder)

        assertThat(result.notificationId).isEqualTo(NOTIFICATION_ID)
    }

    @Test
    fun getNotificationHolder_shouldReturnNotificationHolder() {
        val result = AddNotificationResult.replaceNotification(notificationHolder)

        assertThat(result.notificationHolder).isEqualTo(notificationHolder)
    }
}
