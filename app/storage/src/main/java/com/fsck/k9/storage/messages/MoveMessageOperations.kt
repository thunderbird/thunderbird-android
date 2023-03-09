package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import app.k9mail.core.android.common.database.getIntOrNull
import app.k9mail.core.android.common.database.getLongOrNull
import app.k9mail.core.android.common.database.getStringOrNull
import com.fsck.k9.K9
import com.fsck.k9.mailstore.LockableDatabase
import java.util.UUID
import timber.log.Timber

internal class MoveMessageOperations(
    private val database: LockableDatabase,
    private val threadMessageOperations: ThreadMessageOperations,
) {
    fun moveMessage(messageId: Long, destinationFolderId: Long): Long {
        Timber.d("Moving message [ID: $messageId] to folder [ID: $destinationFolderId]")

        return database.execute(true) { database ->
            val threadInfo =
                threadMessageOperations.createOrUpdateParentThreadEntries(database, messageId, destinationFolderId)
            val destinationMessageId = createMessageEntry(database, messageId, destinationFolderId, threadInfo)
            threadMessageOperations.createThreadEntryIfNecessary(database, destinationMessageId, threadInfo)

            convertOriginalMessageEntryToPlaceholderEntry(database, messageId)
            moveFulltextEntry(database, messageId, destinationMessageId)

            destinationMessageId
        }
    }

    private fun moveFulltextEntry(database: SQLiteDatabase, messageId: Long, destinationMessageId: Long) {
        val values = ContentValues().apply {
            put("docid", destinationMessageId)
        }

        database.update("messages_fulltext", values, "docid = ?", arrayOf(messageId.toString()))
    }

    private fun createMessageEntry(
        database: SQLiteDatabase,
        messageId: Long,
        destinationFolderId: Long,
        threadInfo: ThreadInfo?,
    ): Long {
        val destinationUid = K9.LOCAL_UID_PREFIX + UUID.randomUUID().toString()

        val contentValues = database.query(
            "messages",
            arrayOf(
                "subject", "date", "flags", "sender_list", "to_list", "cc_list", "bcc_list", "reply_to_list",
                "attachment_count", "internal_date", "message_id", "preview_type", "preview", "mime_type",
                "normalized_subject_hash", "read", "flagged", "answered", "forwarded", "message_part_id",
                "encryption_type",
            ),
            "id = ?",
            arrayOf(messageId.toString()),
            null,
            null,
            null,
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                error("Couldn't find local message [ID: $messageId]")
            }

            ContentValues().apply {
                put("uid", destinationUid)
                put("folder_id", destinationFolderId)
                put("deleted", 0)
                put("empty", 0)
                put("subject", cursor.getStringOrNull("subject"))
                put("date", cursor.getLongOrNull("date"))
                put("flags", cursor.getStringOrNull("flags"))
                put("sender_list", cursor.getStringOrNull("sender_list"))
                put("to_list", cursor.getStringOrNull("to_list"))
                put("cc_list", cursor.getStringOrNull("cc_list"))
                put("bcc_list", cursor.getStringOrNull("bcc_list"))
                put("reply_to_list", cursor.getStringOrNull("reply_to_list"))
                put("attachment_count", cursor.getIntOrNull("attachment_count"))
                put("internal_date", cursor.getLongOrNull("internal_date"))
                put("message_id", cursor.getStringOrNull("message_id"))
                put("preview_type", cursor.getStringOrNull("preview_type"))
                put("preview", cursor.getStringOrNull("preview"))
                put("mime_type", cursor.getStringOrNull("mime_type"))
                put("normalized_subject_hash", cursor.getLongOrNull("normalized_subject_hash"))
                put("read", cursor.getIntOrNull("read"))
                put("flagged", cursor.getIntOrNull("flagged"))
                put("answered", cursor.getIntOrNull("answered"))
                put("forwarded", cursor.getIntOrNull("forwarded"))
                put("message_part_id", cursor.getLongOrNull("message_part_id"))
                put("encryption_type", cursor.getStringOrNull("encryption_type"))
            }
        }

        val placeHolderMessageId = threadInfo?.messageId
        return if (placeHolderMessageId != null) {
            database.update("messages", contentValues, "id = ?", arrayOf(placeHolderMessageId.toString()))
            placeHolderMessageId
        } else {
            database.insert("messages", null, contentValues)
        }
    }

    private fun convertOriginalMessageEntryToPlaceholderEntry(database: SQLiteDatabase, messageId: Long) {
        val contentValues = ContentValues().apply {
            put("deleted", 1)
            put("empty", 0)
            put("read", 1)
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
            putNull("flagged")
            putNull("answered")
            putNull("forwarded")
            putNull("message_part_id")
            putNull("encryption_type")
        }

        database.update("messages", contentValues, "id = ?", arrayOf(messageId.toString()))
    }
}
