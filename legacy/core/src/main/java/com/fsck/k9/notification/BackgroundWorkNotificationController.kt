package com.fsck.k9.notification

import android.app.Notification

interface BackgroundWorkNotificationController {
    val notificationId: Int

    fun createNotification(text: String): Notification
}
