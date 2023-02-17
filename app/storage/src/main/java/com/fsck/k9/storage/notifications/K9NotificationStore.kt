package com.fsck.k9.storage.notifications

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mailstore.LockableDatabase
import com.fsck.k9.notification.NotificationStore
import com.fsck.k9.notification.NotificationStoreOperation

class K9NotificationStore(private val lockableDatabase: LockableDatabase) : NotificationStore {
    override fun persistNotificationChanges(operations: List<NotificationStoreOperation>) {
        lockableDatabase.execute(true) { db ->
            for (operation in operations) {
                when (operation) {
                    is NotificationStoreOperation.Add -> {
                        addNotification(db, operation.messageReference, operation.notificationId, operation.timestamp)
                    }
                    is NotificationStoreOperation.ChangeToActive -> {
                        setNotificationId(db, operation.messageReference, operation.notificationId)
                    }
                    is NotificationStoreOperation.ChangeToInactive -> {
                        clearNotificationId(db, operation.messageReference)
                    }
                    is NotificationStoreOperation.Remove -> {
                        removeNotification(db, operation.messageReference)
                    }
                }
            }
        }
    }

    override fun clearNotifications() {
        lockableDatabase.execute(false) { db ->
            db.delete("notifications", null, null)
        }
    }

    private fun addNotification(
        database: SQLiteDatabase,
        messageReference: MessageReference,
        notificationId: Int,
        timestamp: Long,
    ) {
        database.execSQL(
            "INSERT INTO notifications(message_id, notification_id, timestamp) " +
                "SELECT id, ?, ? FROM messages WHERE folder_id = ? AND uid = ?",
            arrayOf(notificationId, timestamp, messageReference.folderId, messageReference.uid),
        )
    }

    private fun setNotificationId(database: SQLiteDatabase, messageReference: MessageReference, notificationId: Int) {
        database.execSQL(
            "UPDATE notifications SET notification_id = ? WHERE message_id IN " +
                "(SELECT id FROM messages WHERE folder_id = ? AND uid = ?)",
            arrayOf(notificationId, messageReference.folderId, messageReference.uid),
        )
    }

    private fun clearNotificationId(database: SQLiteDatabase, messageReference: MessageReference) {
        database.execSQL(
            "UPDATE notifications SET notification_id = NULL WHERE message_id IN " +
                "(SELECT id FROM messages WHERE folder_id = ? AND uid = ?)",
            arrayOf(messageReference.folderId, messageReference.uid),
        )
    }

    private fun removeNotification(database: SQLiteDatabase, messageReference: MessageReference) {
        database.execSQL(
            "DELETE FROM notifications WHERE message_id IN (SELECT id FROM messages WHERE folder_id = ? AND uid = ?)",
            arrayOf(messageReference.folderId, messageReference.uid),
        )
    }
}
