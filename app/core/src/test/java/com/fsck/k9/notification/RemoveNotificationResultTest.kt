package com.fsck.k9.notification

import com.fsck.k9.controller.MessageReference
import com.fsck.k9.notification.RemoveNotificationResult.Companion.cancelNotification
import com.fsck.k9.notification.RemoveNotificationResult.Companion.createNotification
import com.fsck.k9.notification.RemoveNotificationResult.Companion.unknownNotification
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private const val NOTIFICATION_ID = 23

class RemoveNotificationResultTest {
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
    fun createNotification_shouldCancelNotification_shouldReturnTrue() {
        val result = createNotification(notificationHolder)

        assertThat(result.shouldCreateNotification).isTrue()
    }

    @Test
    fun createNotification_getNotificationId_shouldReturnNotificationId() {
        val result = createNotification(notificationHolder)

        assertThat(result.notificationId).isEqualTo(NOTIFICATION_ID)
    }

    @Test
    fun createNotification_isUnknownNotification_shouldReturnFalse() {
        val result = createNotification(notificationHolder)

        assertThat(result.isUnknownNotification).isFalse()
    }

    @Test
    fun createNotification_getNotificationHolder_shouldReturnNotificationHolder() {
        val result = createNotification(notificationHolder)

        assertThat(result.notificationHolder).isEqualTo(notificationHolder)
    }

    @Test
    fun cancelNotification_shouldCancelNotification_shouldReturnFalse() {
        val result = cancelNotification(NOTIFICATION_ID)

        assertThat(result.shouldCreateNotification).isFalse()
    }

    @Test
    fun cancelNotification_getNotificationId_shouldReturnNotificationId() {
        val result = cancelNotification(NOTIFICATION_ID)

        assertThat(result.notificationId).isEqualTo(NOTIFICATION_ID)
    }

    @Test
    fun cancelNotification_isUnknownNotification_shouldReturnFalse() {
        val result = cancelNotification(NOTIFICATION_ID)

        assertThat(result.isUnknownNotification).isFalse()
    }

    @Test(expected = IllegalStateException::class)
    fun cancelNotification_getNotificationHolder_shouldThrowException() {
        val result = cancelNotification(NOTIFICATION_ID)

        result.notificationHolder
    }

    @Test
    fun unknownNotification_shouldCancelNotification_shouldReturnFalse() {
        val result = unknownNotification()

        assertThat(result.shouldCreateNotification).isFalse()
    }

    @Test(expected = IllegalStateException::class)
    fun unknownNotification_getNotificationId_shouldThrowException() {
        val result = unknownNotification()

        result.notificationId
    }

    @Test
    fun unknownNotification_isUnknownNotification_shouldReturnTrue() {
        val result = unknownNotification()

        assertThat(result.isUnknownNotification).isTrue()
    }

    @Test(expected = IllegalStateException::class)
    fun unknownNotification_getNotificationHolder_shouldThrowException() {
        val result = unknownNotification()

        result.notificationHolder
    }
}
