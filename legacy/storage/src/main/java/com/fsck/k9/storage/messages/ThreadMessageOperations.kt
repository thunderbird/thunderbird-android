package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.message.MessageHeaderParser

internal class ThreadMessageOperations {

    fun createOrUpdateParentThreadEntries(
        database: SQLiteDatabase,
        messageId: Long,
        destinationFolderId: Long,
    ): ThreadInfo? {
        val threadHeaders = getMessageThreadHeaders(database, messageId)
        return doMessageThreading(database, destinationFolderId, threadHeaders)
    }

    fun getMessageThreadHeaders(database: SQLiteDatabase, messageId: Long): ThreadHeaders {
        return database.rawQuery(
            """
SELECT messages.message_id, message_parts.header 
FROM messages 
LEFT JOIN message_parts ON (messages.message_part_id = message_parts.id) 
WHERE messages.id = ?
            """,
            arrayOf(messageId.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) error("Message not found: $messageId")

            val messageIdHeader = cursor.getString(0)
            val headerBytes = cursor.getBlob(1)

            var inReplyToHeader: String? = null
            var referencesHeader: String? = null
            if (headerBytes != null) {
                MessageHeaderParser.parse(headerBytes.inputStream()) { name, value ->
                    when (name.lowercase()) {
                        "in-reply-to" -> inReplyToHeader = value
                        "references" -> referencesHeader = value
                    }
                }
            }

            ThreadHeaders(messageIdHeader, inReplyToHeader, referencesHeader)
        }
    }

    fun createThreadEntryIfNecessary(database: SQLiteDatabase, messageId: Long, threadInfo: ThreadInfo?) {
        if (threadInfo?.threadId == null) {
            createThreadEntry(database, messageId, threadInfo?.rootId, threadInfo?.parentId)
        }
    }

    fun createThreadEntry(database: SQLiteDatabase, messageId: Long, rootId: Long?, parentId: Long?): Long {
        val values = ContentValues().apply {
            put("message_id", messageId)
            put("root", rootId)
            put("parent", parentId)
        }

        return database.insert("threads", null, values)
    }

    // TODO: Use MessageIdParser
    fun doMessageThreading(database: SQLiteDatabase, folderId: Long, threadHeaders: ThreadHeaders): ThreadInfo? {
        val messageIdHeader = threadHeaders.messageIdHeader
        val msgThreadInfo = getThreadInfo(database, folderId, messageIdHeader, onlyEmpty = true)

        val references = threadHeaders.referencesHeader.extractMessageIdValues()
        val inReplyTo = threadHeaders.inReplyToHeader.extractMessageIdValue()

        val messageIdValues = if (inReplyTo == null || inReplyTo in references) {
            references
        } else {
            references + inReplyTo
        }

        if (messageIdValues.isEmpty()) {
            // This is not a reply, nothing to do for us.
            return msgThreadInfo
        }

        var rootId: Long? = null
        var parentId: Long? = null
        for (reference in messageIdValues) {
            val threadInfo = getThreadInfo(database, folderId, reference, onlyEmpty = false)
            if (threadInfo == null) {
                parentId = createEmptyMessage(database, folderId, reference, rootId, parentId)
                if (rootId == null) {
                    rootId = parentId
                }
            } else {
                if (rootId == null) {
                    rootId = threadInfo.rootId
                } else if (threadInfo.rootId != rootId) {
                    // Merge this thread into our thread
                    updateThreadToNewRoot(database, threadInfo.rootId, rootId, parentId)
                }
                parentId = threadInfo.threadId
            }
        }

        msgThreadInfo?.threadId?.let { threadId ->
            // msgThreadInfo.rootId might be outdated. Fetch current value.
            val oldRootId = getThreadRoot(database, threadId)
            if (oldRootId != rootId) {
                // Connect the existing thread to the newly created thread
                updateThreadToNewRoot(database, oldRootId, rootId!!, parentId)
            }
        }

        return ThreadInfo(msgThreadInfo?.threadId, msgThreadInfo?.messageId, rootId!!, parentId)
    }

    private fun updateThreadToNewRoot(database: SQLiteDatabase, oldRootId: Long, rootId: Long, parentId: Long?) {
        // Let all children know who's the new root
        val values = ContentValues()
        values.put("root", rootId)
        database.update("threads", values, "root = ?", arrayOf(oldRootId.toString()))

        // Connect the message to the current parent
        values.put("parent", parentId)
        database.update("threads", values, "id = ?", arrayOf(oldRootId.toString()))
    }

    private fun createEmptyMessage(
        database: SQLiteDatabase,
        folderId: Long,
        messageIdHeader: String,
        rootId: Long?,
        parentId: Long?,
    ): Long {
        val messageValues = ContentValues().apply {
            put("message_id", messageIdHeader)
            put("folder_id", folderId)
            put("empty", 1)
        }
        val messageId = database.insert("messages", null, messageValues)

        val threadValues = ContentValues().apply {
            put("message_id", messageId)
            put("root", rootId)
            put("parent", parentId)
        }
        return database.insert("threads", null, threadValues)
    }

    private fun getThreadInfo(
        db: SQLiteDatabase,
        folderId: Long,
        messageIdHeader: String?,
        onlyEmpty: Boolean,
    ): ThreadInfo? {
        if (messageIdHeader == null) return null

        return db.rawQuery(
            """
SELECT t.id, t.message_id, t.root, t.parent 
FROM messages m 
LEFT JOIN threads t ON (t.message_id = m.id) 
WHERE m.folder_id = ? AND m.message_id = ? 
${if (onlyEmpty) "AND m.empty = 1 " else ""}
ORDER BY m.id 
LIMIT 1
            """,
            arrayOf(folderId.toString(), messageIdHeader),
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val threadId = cursor.getLong(0)
                val messageId = cursor.getLong(1)
                val rootId = cursor.getLong(2)
                val parentId = if (cursor.isNull(3)) null else cursor.getLong(3)
                ThreadInfo(threadId, messageId, rootId, parentId)
            } else {
                null
            }
        }
    }

    private fun getThreadRoot(database: SQLiteDatabase, threadId: Long): Long {
        return database.rawQuery(
            "SELECT root FROM threads WHERE id = ?",
            arrayOf(threadId.toString()),
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(0)
            } else {
                error("Thread with ID $threadId not found")
            }
        }
    }

    private fun String?.extractMessageIdValues(): List<String> {
        return this?.let { headerValue -> Utility.extractMessageIds(headerValue) } ?: emptyList()
    }

    private fun String?.extractMessageIdValue(): String? {
        return this?.let { headerValue -> Utility.extractMessageId(headerValue) }
    }
}

internal data class ThreadInfo(
    val threadId: Long?,
    val messageId: Long?,
    val rootId: Long,
    val parentId: Long?,
)

internal data class ThreadHeaders(
    val messageIdHeader: String?,
    val inReplyToHeader: String?,
    val referencesHeader: String?,
)

internal fun Message.toThreadHeaders(): ThreadHeaders {
    return ThreadHeaders(
        messageIdHeader = messageId,
        inReplyToHeader = getHeader("In-Reply-To").firstOrNull(),
        referencesHeader = getHeader("References").firstOrNull(),
    )
}
