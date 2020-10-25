package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.mailstore.ThreadInfo as LegacyThreadInfo

// TODO: Remove dependency on LocalStore
internal class ThreadMessageOperations(private val localStore: LocalStore) {

    fun createOrUpdateParentThreadEntries(
        database: SQLiteDatabase,
        messageId: Long,
        destinationFolderId: Long
    ): ThreadInfo {
        val message = getLocalMessage(database, messageId) ?: error("Couldn't find local message [ID: $messageId]")
        val destinationFolder = getLocalFolder(destinationFolderId)

        val legacyThreadInfo: LegacyThreadInfo = destinationFolder.doMessageThreading(database, message)
        return legacyThreadInfo.toThreadInfo()
    }

    fun createOrUpdateThreadEntry(
        database: SQLiteDatabase,
        destinationMessageId: Long,
        threadInfo: ThreadInfo
    ) {
        val contentValues = ContentValues()
        contentValues.put("message_id", destinationMessageId)
        if (threadInfo.threadId == null) {
            if (threadInfo.rootId != null) {
                contentValues.put("root", threadInfo.rootId)
            }
            if (threadInfo.parentId != null) {
                contentValues.put("parent", threadInfo.parentId)
            }
            database.insert("threads", null, contentValues)
        } else {
            database.update("threads", contentValues, "id = ?", arrayOf(threadInfo.threadId.toString()))
        }
    }

    private fun getLocalMessage(database: SQLiteDatabase, messageId: Long): LocalMessage? {
        return database.query(
            "messages",
            arrayOf("uid", "folder_id"),
            "id = ?",
            arrayOf(messageId.toString()),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val uid = cursor.getString(0)
                val folderId = cursor.getLong(1)

                val folder = getLocalFolder(folderId)
                folder.getMessage(uid)
            } else {
                null
            }
        }
    }

    private fun getLocalFolder(destinationFolderId: Long): LocalFolder {
        val destinationFolder = localStore.getFolder(destinationFolderId)
        destinationFolder.open()

        return destinationFolder
    }

    private fun LegacyThreadInfo.toThreadInfo(): ThreadInfo {
        return ThreadInfo(
            threadId = threadId.minusOneToNull(),
            messageId = msgId.minusOneToNull(),
            messageIdHeader = messageId,
            rootId = rootId.minusOneToNull(),
            parentId = parentId.minusOneToNull()
        )
    }

    private fun Long.minusOneToNull() = if (this == -1L) null else this
}

internal data class ThreadInfo(
    val threadId: Long?,
    val messageId: Long?,
    val messageIdHeader: String?,
    val rootId: Long?,
    val parentId: Long?
)
