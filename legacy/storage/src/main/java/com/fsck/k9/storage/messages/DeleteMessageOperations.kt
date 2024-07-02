package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mailstore.LockableDatabase

internal class DeleteMessageOperations(
    private val lockableDatabase: LockableDatabase,
    private val attachmentFileManager: AttachmentFileManager,
) {
    fun destroyMessages(folderId: Long, messageServerIds: Collection<String>) {
        for (messageServerId in messageServerIds) {
            destroyMessage(folderId, messageServerId)
        }
    }

    private fun destroyMessage(folderId: Long, messageServerId: String) {
        val (messageId, rootMessagePartId, hasThreadChildren) = getMessageData(folderId, messageServerId) ?: return

        lockableDatabase.execute(true) { database ->
            database.deleteMessagePartFiles(rootMessagePartId)

            if (hasThreadChildren) {
                // We're not deleting the 'messages' row so we'll have to manually delete the associated
                // 'message_parts' and 'messages_fulltext' rows.
                database.deleteMessagePartRows(rootMessagePartId)
                database.deleteFulltextIndexEntry(messageId)

                // This message has children in the thread structure so we need to make it an empty message.
                database.convertToEmptyMessage(messageId)
            } else {
                database.deleteMessageRows(messageId)
            }
        }
    }

    private fun getMessageData(folderId: Long, messageServerId: String): MessageData? {
        return lockableDatabase.execute(false) { database ->
            database.rawQuery(
                """
SELECT messages.id, messages.message_part_id, COUNT(threads2.id) 
FROM messages 
LEFT JOIN threads threads1 ON (threads1.message_id = messages.id)  
LEFT JOIN threads threads2 ON (threads2.parent = threads1.id) 
WHERE folder_id = ? AND uid = ?
                """,
                arrayOf(folderId.toString(), messageServerId),
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    MessageData(
                        messageId = cursor.getLong(0),
                        messagePartId = cursor.getLong(1),
                        hasThreadChildren = !cursor.isNull(2) && cursor.getInt(2) > 0,
                    )
                } else {
                    null
                }
            }
        }
    }

    private fun SQLiteDatabase.deleteMessagePartFiles(rootMessagePartId: Long) {
        query(
            "message_parts",
            arrayOf("id"),
            "root = ? AND data_location = $DATA_LOCATION_ON_DISK",
            arrayOf(rootMessagePartId.toString()),
            null,
            null,
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val messagePartId = cursor.getLong(0)
                attachmentFileManager.deleteFile(messagePartId)
            }
        }
    }

    private fun SQLiteDatabase.deleteMessagePartRows(rootMessagePartId: Long) {
        delete("message_parts", "root = ?", arrayOf(rootMessagePartId.toString()))
    }

    private fun SQLiteDatabase.deleteFulltextIndexEntry(messageId: Long) {
        delete("messages_fulltext", "docid = ?", arrayOf(messageId.toString()))
    }

    private fun SQLiteDatabase.convertToEmptyMessage(messageId: Long) {
        val values = ContentValues().apply {
            put("deleted", 0)
            put("empty", 1)
            putNull("subject")
            putNull("date")
            putNull("flags")
            putNull("sender_list")
            putNull("to_list")
            putNull("cc_list")
            putNull("bcc_list")
            putNull("reply_to_list")
            putNull("attachment_count")
            putNull("internal_date")
            put("preview_type", "none")
            putNull("preview")
            putNull("mime_type")
            putNull("normalized_subject_hash")
            putNull("message_part_id")
            putNull("encryption_type")
        }

        update("messages", values, "id = ?", arrayOf(messageId.toString()))
    }

    private fun SQLiteDatabase.deleteMessageRows(messageId: Long) {
        // Delete the message and all empty parent messages that have no other children in the thread structure
        var currentMessageId: Long? = messageId
        while (currentMessageId != null) {
            val nextMessageId = getEmptyThreadParent(currentMessageId)

            deleteMessageRow(currentMessageId)

            if (nextMessageId == null || hasThreadChildren(nextMessageId)) {
                return
            }

            currentMessageId = nextMessageId
        }
    }

    private fun SQLiteDatabase.getEmptyThreadParent(messageId: Long): Long? {
        return rawQuery(
            """
SELECT messages.id 
FROM threads threads1 
JOIN threads threads2 ON (threads1.parent = threads2.id) 
JOIN messages ON (threads2.message_id = messages.id AND messages.empty = 1) 
WHERE threads1.message_id = ?
            """,
            arrayOf(messageId.toString()),
        ).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                cursor.getLong(0)
            } else {
                null
            }
        }
    }

    private fun SQLiteDatabase.deleteMessageRow(messageId: Long) {
        delete("messages", "id = ?", arrayOf(messageId.toString()))
    }

    private fun SQLiteDatabase.hasThreadChildren(messageId: Long): Boolean {
        return rawQuery(
            """
SELECT COUNT(threads2.id) 
FROM threads threads1 
JOIN threads threads2 ON (threads2.parent = threads1.id) 
WHERE threads1.message_id = ?
            """,
            arrayOf(messageId.toString()),
        ).use { cursor ->
            cursor.moveToFirst() && !cursor.isNull(0) && cursor.getLong(0) > 0L
        }
    }
}

private data class MessageData(
    val messageId: Long,
    val messagePartId: Long,
    val hasThreadChildren: Boolean,
)
