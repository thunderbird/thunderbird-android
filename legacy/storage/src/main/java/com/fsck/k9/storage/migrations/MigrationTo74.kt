package com.fsck.k9.storage.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.Account.DeletePolicy

/**
 * Remove all placeholder entries in 'messages' table
 *
 * When the user moves or deletes an email, the row in the 'messages' table is first updated with 'deleted = 1', turning
 * it into a placeholder message. After the remote operation has completed, the row is removed.
 *
 * Placeholder messages are created to prevent an email from being downloaded again during a sync before the remote
 * operation has finished. The placeholder message is also necessary to remember deleted messages when using a delete
 * policy other than "delete from server".
 *
 * Previously these placeholder messages often weren't removed when they should have been. This will slowly grow the
 * database.
 * Here we remove all placeholder messages when the delete policy is "delete from server". This might also include
 * placeholder messages that shouldn't be removed, because the remote operation hasn't been performed yet.
 * However, since the app tries to execute all pending remote operations before doing a message sync, it's unlikely that
 * deleted messages are re-downloaded. And if they are, the next sync after the remote operation has completed will
 * remove them again.
 */
internal class MigrationTo74(private val db: SQLiteDatabase, private val account: Account) {

    fun removeDeletedMessages() {
        if (account.deletePolicy != DeletePolicy.ON_DELETE) return

        db.query("messages", arrayOf("id"), "deleted = 1", null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                destroyMessage(messageId = cursor.getLong(0))
            }
        }
    }

    private fun destroyMessage(messageId: Long) {
        if (hasThreadChildren(messageId)) {
            // This message has children in the thread structure so we need to make it an empty message.
            val cv = ContentValues().apply {
                put("deleted", 0)
                put("empty", 1)
                put("preview_type", "none")
                put("read", 0)
                put("flagged", 0)
                put("answered", 0)
                put("forwarded", 0)
                putNull("subject")
                putNull("sender_list")
                putNull("date")
                putNull("to_list")
                putNull("cc_list")
                putNull("bcc_list")
                putNull("preview")
                putNull("reply_to_list")
                putNull("message_part_id")
                putNull("flags")
                putNull("attachment_count")
                putNull("internal_date")
                putNull("mime_type")
                putNull("encryption_type")
            }
            db.update("messages", cv, "id = ?", arrayOf(messageId.toString()))

            // Nothing else to do
            return
        }

        // Get the message ID of the parent message if it's empty
        var currentId = getEmptyThreadParent(messageId)

        // Delete the placeholder message
        deleteMessageRow(messageId)

        // Walk the thread tree to delete all empty parents without children
        while (currentId != -1L) {
            if (hasThreadChildren(currentId)) {
                // We made sure there are no empty leaf nodes and can stop now.
                break
            }

            // Get ID of the (empty) parent for the next iteration
            val newId = getEmptyThreadParent(currentId)

            // Delete the empty message
            deleteMessageRow(currentId)
            currentId = newId
        }
    }

    private fun hasThreadChildren(messageId: Long): Boolean {
        return db.rawQuery(
            "SELECT COUNT(t2.id) " +
                "FROM threads t1 " +
                "JOIN threads t2 ON (t2.parent = t1.id) " +
                "WHERE t1.message_id = ?",
            arrayOf(messageId.toString()),
        ).use { cursor ->
            cursor.moveToFirst() && !cursor.isNull(0) && cursor.getLong(0) > 0L
        }
    }

    private fun getEmptyThreadParent(messageId: Long): Long {
        return db.rawQuery(
            "SELECT m.id " +
                "FROM threads t1 " +
                "JOIN threads t2 ON (t1.parent = t2.id) " +
                "LEFT JOIN messages m ON (t2.message_id = m.id) " +
                "WHERE t1.message_id = ? AND m.empty = 1",
            arrayOf(messageId.toString()),
        ).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else -1
        }
    }

    private fun deleteMessageRow(messageId: Long) {
        // Delete the message
        db.delete("messages", "id = ?", arrayOf(messageId.toString()))

        // Delete row in 'threads' table
        db.delete("threads", "message_id = ?", arrayOf(messageId.toString()))
    }
}
