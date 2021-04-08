package com.fsck.k9.mailstore

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendFolder.MoreMessages
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import java.util.Date
import com.fsck.k9.mailstore.MoreMessages as StoreMoreMessages

class K9BackendFolder(
    private val localStore: LocalStore,
    private val messageStore: MessageStore,
    private val folderServerId: String
) : BackendFolder {
    private val database = localStore.database
    private val databaseId: String
    private val folderId: Long
    private val localFolder = localStore.getFolder(folderServerId)
    override val name: String
    override val visibleLimit: Int

    init {
        data class Init(val folderId: Long, val name: String, val visibleLimit: Int)

        val init = messageStore.getFolder(folderServerId) { folder ->
            Init(
                folderId = folder.id,
                name = folder.name,
                visibleLimit = folder.visibleLimit
            )
        } ?: error("Couldn't find folder $folderServerId")

        databaseId = init.folderId.toString()
        folderId = init.folderId
        name = init.name
        visibleLimit = init.visibleLimit
    }

    override fun getLastUid(): Long? {
        return messageStore.getLastUid(folderId)
    }

    override fun getMessageServerIds(): Set<String> {
        return messageStore.getMessageServerIds(folderId)
    }

    override fun getAllMessagesAndEffectiveDates(): Map<String, Long?> {
        return messageStore.getAllMessagesAndEffectiveDates(folderId)
    }

    override fun destroyMessages(messageServerIds: List<String>) {
        messageStore.destroyMessages(folderId, messageServerIds)
    }

    override fun clearAllMessages() {
        val messageServerIds = messageStore.getMessageServerIds(folderId)
        messageStore.destroyMessages(folderId, messageServerIds)
    }

    override fun getMoreMessages(): MoreMessages {
        return messageStore.getFolder(folderId) { folder ->
            folder.moreMessages.toMoreMessages()
        } ?: MoreMessages.UNKNOWN
    }

    override fun setMoreMessages(moreMessages: MoreMessages) {
        messageStore.setMoreMessages(folderId, moreMessages.toStoreMoreMessages())
    }

    override fun setLastChecked(timestamp: Long) {
        database.setLong(column = "last_updated", value = timestamp)
    }

    override fun setStatus(status: String?) {
        database.setString(column = "status", value = status)
    }

    override fun isMessagePresent(messageServerId: String): Boolean {
        return database.execute(false) { db ->
            val cursor = db.query(
                "messages",
                arrayOf("id"),
                "folder_id = ? AND uid = ?",
                arrayOf(databaseId, messageServerId),
                null, null, null
            )

            cursor.use {
                cursor.moveToFirst()
            }
        }
    }

    override fun getMessageFlags(messageServerId: String): Set<Flag> {
        fun String?.extractFlags(): MutableSet<Flag> {
            return if (this == null || this.isBlank()) {
                mutableSetOf()
            } else {
                this.split(',').map { Flag.valueOf(it) }.toMutableSet()
            }
        }

        return database.execute(false) { db ->
            val cursor = db.query(
                "messages",
                arrayOf("deleted", "read", "flagged", "answered", "forwarded", "flags"),
                "folder_id = ? AND uid = ?",
                arrayOf(databaseId, messageServerId),
                null, null, null
            )

            cursor.use {
                if (!cursor.moveToFirst()) {
                    throw IllegalStateException("Couldn't read flags for $folderServerId:$messageServerId")
                }

                val deleted = cursor.getInt(0) == 1
                val read = cursor.getInt(1) == 1
                val flagged = cursor.getInt(2) == 1
                val answered = cursor.getInt(3) == 1
                val forwarded = cursor.getInt(4) == 1
                val flagsColumnValue = cursor.getString(5)

                val flags = flagsColumnValue.extractFlags().apply {
                    if (deleted) add(Flag.DELETED)
                    if (read) add(Flag.SEEN)
                    if (flagged) add(Flag.FLAGGED)
                    if (answered) add(Flag.ANSWERED)
                    if (forwarded) add(Flag.FORWARDED)
                }

                flags
            }
        }
    }

    override fun setMessageFlag(messageServerId: String, flag: Flag, value: Boolean) {
        when (flag) {
            Flag.DELETED -> database.setMessagesBoolean(messageServerId, "deleted", value)
            Flag.SEEN -> database.setMessagesBoolean(messageServerId, "read", value)
            Flag.FLAGGED -> database.setMessagesBoolean(messageServerId, "flagged", value)
            Flag.ANSWERED -> database.setMessagesBoolean(messageServerId, "answered", value)
            Flag.FORWARDED -> database.setMessagesBoolean(messageServerId, "forwarded", value)
            else -> {
                val flagsColumnValue = database.getString(
                    table = "messages",
                    column = "flags",
                    selection = "folder_id = ? AND uid = ?",
                    selectionArgs = *arrayOf(databaseId, messageServerId)
                ) ?: ""

                val flags = flagsColumnValue.split(',').toMutableSet()
                if (value) {
                    flags.add(flag.toString())
                } else {
                    flags.remove(flag.toString())
                }

                val serializedFlags = flags.joinToString(separator = ",")

                database.setString(
                    table = "messages",
                    column = "flags",
                    selection = "folder_id = ? AND uid = ?",
                    selectionArgs = *arrayOf(databaseId, messageServerId),
                    value = serializedFlags
                )
            }
        }

        localStore.notifyChange()
    }

    // TODO: Move implementation from LocalFolder to this class
    override fun saveCompleteMessage(message: Message) {
        requireMessageServerId(message)

        localFolder.appendMessages(listOf(message))

        val localMessage = localFolder.getMessage(message.uid)
        localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true)
    }

    // TODO: Move implementation from LocalFolder to this class
    override fun savePartialMessage(message: Message) {
        requireMessageServerId(message)

        localFolder.appendMessages(listOf(message))

        val localMessage = localFolder.getMessage(message.uid)
        localMessage.setFlag(Flag.X_DOWNLOADED_PARTIAL, true)
    }

    override fun getOldestMessageDate(): Date? {
        return database.rawQuery("SELECT MIN(date) FROM messages WHERE folder_id = ?", databaseId) { cursor ->
            if (cursor.moveToFirst()) {
                Date(cursor.getLong(0))
            } else {
                null
            }
        }
    }

    override fun getFolderExtraString(name: String): String? {
        return database.execute(false) { db ->
            val cursor = db.query(
                "folder_extra_values",
                arrayOf("value_text"),
                "name = ? AND folder_id = ?",
                arrayOf(name, databaseId),
                null, null, null
            )

            cursor.use {
                if (it.moveToFirst()) {
                    it.getStringOrNull(0)
                } else {
                    null
                }
            }
        }
    }

    override fun setFolderExtraString(name: String, value: String?) {
        database.execute(false) { db ->
            val contentValues = ContentValues().apply {
                put("name", name)
                if (value != null) {
                    put("value_text", value)
                } else {
                    putNull("value_text")
                }
                put("folder_id", databaseId)
            }
            db.insertWithOnConflict("folder_extra_values", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    override fun getFolderExtraNumber(name: String): Long? {
        return database.execute(false) { db ->
            val cursor = db.query(
                "folder_extra_values",
                arrayOf("value_integer"),
                "name = ? AND folder_id = ?",
                arrayOf(name, databaseId),
                null, null, null
            )

            cursor.use {
                if (it.moveToFirst()) {
                    it.getLongOrNull(0)
                } else {
                    null
                }
            }
        }
    }

    override fun setFolderExtraNumber(name: String, value: Long) {
        database.execute(false) { db ->
            val contentValues = ContentValues().apply {
                put("name", name)
                put("value_integer", value)
                put("folder_id", databaseId)
            }
            db.insertWithOnConflict("folder_extra_values", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    private fun LockableDatabase.getString(
        table: String = "folders",
        column: String,
        selection: String = "id = ?",
        vararg selectionArgs: String = arrayOf(databaseId)
    ): String? {
        return execute(false) { db ->
            val cursor = db.query(table, arrayOf(column), selection, selectionArgs, null, null, null)
            cursor.use {
                if (it.moveToFirst()) {
                    it.getStringOrNull(0)
                } else {
                    throw IllegalStateException("Couldn't find value for column $table.$column")
                }
            }
        }
    }

    private fun LockableDatabase.setString(
        table: String = "folders",
        column: String,
        value: String?,
        selection: String = "id = ?",
        vararg selectionArgs: String = arrayOf(databaseId)
    ) {
        execute(false) { db ->
            val contentValues = ContentValues().apply {
                put(column, value)
            }
            db.update(table, contentValues, selection, selectionArgs)
        }
    }

    private fun LockableDatabase.setMessagesBoolean(
        messageServerId: String,
        column: String,
        value: Boolean
    ) {
        execute(false) { db ->
            val contentValues = ContentValues().apply {
                put(column, if (value) 1 else 0)
            }
            db.update("messages", contentValues, "folder_id = ? AND uid = ?", arrayOf(databaseId, messageServerId))
        }
    }

    private fun LockableDatabase.setLong(
        table: String = "folders",
        column: String,
        value: Long,
        selection: String = "id = ?",
        vararg selectionArgs: String = arrayOf(databaseId)
    ) {
        execute(false) { db ->
            val contentValues = ContentValues().apply {
                put(column, value)
            }
            db.update(table, contentValues, selection, selectionArgs)
        }
    }

    private fun StoreMoreMessages.toMoreMessages(): MoreMessages = when (this) {
        StoreMoreMessages.UNKNOWN -> MoreMessages.UNKNOWN
        StoreMoreMessages.FALSE -> MoreMessages.FALSE
        StoreMoreMessages.TRUE -> MoreMessages.TRUE
    }

    private fun MoreMessages.toStoreMoreMessages(): StoreMoreMessages = when (this) {
        MoreMessages.UNKNOWN -> StoreMoreMessages.UNKNOWN
        MoreMessages.FALSE -> StoreMoreMessages.FALSE
        MoreMessages.TRUE -> StoreMoreMessages.TRUE
    }

    private fun requireMessageServerId(message: Message) {
        if (message.uid.isNullOrEmpty()) {
            error("Message requires a server ID to be set")
        }
    }
}
