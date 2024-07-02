package com.fsck.k9.storage.notifications

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import app.k9mail.core.android.common.database.getIntOrNull
import app.k9mail.core.android.common.database.getLongOrNull
import app.k9mail.core.android.common.database.map

fun SQLiteDatabase.createNotification(
    messageId: Long,
    notificationId: Int? = null,
    timestamp: Long = 0L,
): Long {
    val values = ContentValues().apply {
        put("message_id", messageId)
        put("notification_id", notificationId)
        put("timestamp", timestamp)
    }

    return insert("notifications", null, values)
}

fun SQLiteDatabase.readNotifications(): List<NotificationEntry> {
    val cursor = rawQuery("SELECT * FROM notifications", null)
    return cursor.use {
        cursor.map {
            NotificationEntry(
                messageId = cursor.getLongOrNull("message_id"),
                notificationId = cursor.getIntOrNull("notification_id"),
                timestamp = cursor.getLongOrNull("timestamp"),
            )
        }
    }
}

data class NotificationEntry(
    val messageId: Long?,
    val notificationId: Int?,
    val timestamp: Long?,
)
