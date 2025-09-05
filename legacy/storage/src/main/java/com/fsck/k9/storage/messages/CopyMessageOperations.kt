package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getBlobOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.fsck.k9.K9
import com.fsck.k9.mailstore.LockableDatabase
import java.util.UUID
import net.thunderbird.feature.account.AccountId

internal class CopyMessageOperations(
    private val lockableDatabase: LockableDatabase,
    private val attachmentFileManager: AttachmentFileManager,
    private val threadMessageOperations: ThreadMessageOperations,
    private val accountId: AccountId,
) {
    fun copyMessage(messageId: Long, destinationFolderId: Long): Long {
        return lockableDatabase.execute(true) { database ->
            val newMessageId = copyMessage(database, messageId, destinationFolderId)

            copyFulltextEntry(database, newMessageId, messageId)

            newMessageId
        }
    }

    private fun copyMessage(
        database: SQLiteDatabase,
        messageId: Long,
        destinationFolderId: Long,
    ): Long {
        val rootMessagePart = copyMessageParts(database, messageId)

        val threadInfo = threadMessageOperations.doMessageThreading(
            database,
            folderId = destinationFolderId,
            threadHeaders = threadMessageOperations.getMessageThreadHeaders(database, messageId),
        )

        return if (threadInfo?.messageId != null) {
            updateMessageRow(
                database,
                sourceMessageId = messageId,
                destinationMessageId = threadInfo.messageId,
                destinationFolderId,
                rootMessagePart,
            )
        } else {
            val newMessageId = insertMessageRow(
                database,
                sourceMessageId = messageId,
                destinationFolderId = destinationFolderId,
                rootMessagePartId = rootMessagePart,
            )

            if (threadInfo?.threadId == null) {
                threadMessageOperations.createThreadEntry(
                    database,
                    newMessageId,
                    threadInfo?.rootId,
                    threadInfo?.parentId,
                )
            }

            newMessageId
        }
    }

    private fun copyMessageParts(database: SQLiteDatabase, messageId: Long): Long {
        return database.rawQuery(
            """
SELECT
  message_parts.id,
  message_parts.type,
  message_parts.root,
  message_parts.parent,
  message_parts.seq,
  message_parts.mime_type,
  message_parts.decoded_body_size,
  message_parts.display_name,
  message_parts.header,
  message_parts.encoding,
  message_parts.charset,
  message_parts.data_location,
  message_parts.data,
  message_parts.preamble,
  message_parts.epilogue,
  message_parts.boundary,
  message_parts.content_id,
  message_parts.server_extra 
FROM messages 
JOIN message_parts ON (message_parts.root = messages.message_part_id) 
WHERE messages.id = ? 
ORDER BY message_parts.seq
            """,
            arrayOf(messageId.toString()),
        ).use { cursor ->
            if (!cursor.moveToNext()) error("No message part found for message with ID $messageId")

            val rootMessagePart = cursor.readMessagePart()

            val rootMessagePartId = writeMessagePart(
                database = database,
                databaseMessagePart = rootMessagePart,
                newRootId = null,
                newParentId = -1,
            )

            val messagePartIdMapping = mutableMapOf<Long, Long>()
            messagePartIdMapping[rootMessagePart.id] = rootMessagePartId

            while (cursor.moveToNext()) {
                val messagePart = cursor.readMessagePart()

                messagePartIdMapping[messagePart.id] = writeMessagePart(
                    database = database,
                    databaseMessagePart = messagePart,
                    newRootId = rootMessagePartId,
                    newParentId = messagePartIdMapping[messagePart.parent] ?: error("parent ID not found"),
                )
            }

            rootMessagePartId
        }
    }

    private fun writeMessagePart(
        database: SQLiteDatabase,
        databaseMessagePart: DatabaseMessagePart,
        newRootId: Long?,
        newParentId: Long,
    ): Long {
        val values = ContentValues().apply {
            put("type", databaseMessagePart.type)
            put("root", newRootId)
            put("parent", newParentId)
            put("seq", databaseMessagePart.seq)
            put("mime_type", databaseMessagePart.mimeType)
            put("decoded_body_size", databaseMessagePart.decodedBodySize)
            put("display_name", databaseMessagePart.displayName)
            put("header", databaseMessagePart.header)
            put("encoding", databaseMessagePart.encoding)
            put("charset", databaseMessagePart.charset)
            put("data_location", databaseMessagePart.dataLocation)
            put("data", databaseMessagePart.data)
            put("preamble", databaseMessagePart.preamble)
            put("epilogue", databaseMessagePart.epilogue)
            put("boundary", databaseMessagePart.boundary)
            put("content_id", databaseMessagePart.contentId)
            put("server_extra", databaseMessagePart.serverExtra)
        }

        val messagePartId = database.insert("message_parts", null, values)

        if (databaseMessagePart.dataLocation == DataLocation.ON_DISK) {
            attachmentFileManager.copyFile(databaseMessagePart.id, messagePartId)
        }

        return messagePartId
    }

    private fun updateMessageRow(
        database: SQLiteDatabase,
        sourceMessageId: Long,
        destinationMessageId: Long,
        destinationFolderId: Long,
        rootMessagePartId: Long,
    ): Long {
        val values = readMessageToContentValues(database, sourceMessageId, destinationFolderId, rootMessagePartId)

        database.update("messages", values, "id = ?", arrayOf(destinationMessageId.toString()))
        return destinationMessageId
    }

    private fun insertMessageRow(
        database: SQLiteDatabase,
        sourceMessageId: Long,
        destinationFolderId: Long,
        rootMessagePartId: Long,
    ): Long {
        val values = readMessageToContentValues(database, sourceMessageId, destinationFolderId, rootMessagePartId)

        return database.insert("messages", null, values)
    }

    private fun readMessageToContentValues(
        database: SQLiteDatabase,
        sourceMessageId: Long,
        destinationFolderId: Long,
        rootMessagePartId: Long,
    ): ContentValues {
        val values = readMessageToContentValues(database, sourceMessageId)

        return values.apply {
            put("folder_id", destinationFolderId)
            put("uid", K9.LOCAL_UID_PREFIX + UUID.randomUUID().toString())
            put("message_part_id", rootMessagePartId)
            put("account_id", accountId.asRaw())
        }
    }

    private fun copyFulltextEntry(database: SQLiteDatabase, newMessageId: Long, messageId: Long) {
        database.execSQL(
            "INSERT OR REPLACE INTO messages_fulltext (docid, fulltext) " +
                "SELECT ?, fulltext FROM messages_fulltext WHERE docid = ?",
            arrayOf(newMessageId.toString(), messageId.toString()),
        )
    }

    private fun readMessageToContentValues(database: SQLiteDatabase, messageId: Long): ContentValues {
        return database.query(
            "messages",
            arrayOf(
                "deleted",
                "subject",
                "date",
                "flags",
                "sender_list",
                "to_list",
                "cc_list",
                "bcc_list",
                "reply_to_list",
                "attachment_count",
                "internal_date",
                "message_id",
                "preview_type",
                "preview",
                "mime_type",
                "normalized_subject_hash",
                "empty",
                "read",
                "flagged",
                "answered",
                "forwarded",
                "encryption_type",
            ),
            "id = ?",
            arrayOf(messageId.toString()),
            null,
            null,
            null,
        ).use { cursor ->
            if (!cursor.moveToNext()) error("Message with ID $messageId not found")

            ContentValues().apply {
                put("deleted", cursor.getInt(0))
                put("subject", cursor.getStringOrNull(1))
                put("date", cursor.getLong(2))
                put("flags", cursor.getStringOrNull(3))
                put("sender_list", cursor.getStringOrNull(4))
                put("to_list", cursor.getStringOrNull(5))
                put("cc_list", cursor.getStringOrNull(6))
                put("bcc_list", cursor.getStringOrNull(7))
                put("reply_to_list", cursor.getStringOrNull(8))
                put("attachment_count", cursor.getInt(9))
                put("internal_date", cursor.getLong(10))
                put("message_id", cursor.getStringOrNull(11))
                put("preview_type", cursor.getStringOrNull(12))
                put("preview", cursor.getStringOrNull(13))
                put("mime_type", cursor.getStringOrNull(14))
                put("normalized_subject_hash", cursor.getLong(15))
                put("empty", cursor.getInt(16))
                put("read", cursor.getInt(17))
                put("flagged", cursor.getInt(18))
                put("answered", cursor.getInt(19))
                put("forwarded", cursor.getInt(20))
                put("encryption_type", cursor.getStringOrNull(21))
            }
        }
    }

    private fun Cursor.readMessagePart(): DatabaseMessagePart {
        return DatabaseMessagePart(
            id = getLong(0),
            type = getInt(1),
            root = getLong(2),
            parent = getLong(3),
            seq = getInt(4),
            mimeType = getString(5),
            decodedBodySize = getLongOrNull(6),
            displayName = getStringOrNull(7),
            header = getBlobOrNull(8),
            encoding = getStringOrNull(9),
            charset = getStringOrNull(10),
            dataLocation = getInt(11),
            data = getBlobOrNull(12),
            preamble = getBlobOrNull(13),
            epilogue = getBlobOrNull(14),
            boundary = getStringOrNull(15),
            contentId = getStringOrNull(16),
            serverExtra = getStringOrNull(17),
        )
    }
}

private class DatabaseMessagePart(
    val id: Long,
    val type: Int,
    val root: Long,
    val parent: Long,
    val seq: Int,
    val mimeType: String?,
    val decodedBodySize: Long?,
    val displayName: String?,
    val header: ByteArray?,
    val encoding: String?,
    val charset: String?,
    val dataLocation: Int,
    val data: ByteArray?,
    val preamble: ByteArray?,
    val epilogue: ByteArray?,
    val boundary: String?,
    val contentId: String?,
    val serverExtra: String?,
)
