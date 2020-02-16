package com.fsck.k9.mailstore

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendFolder.MoreMessages
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import java.util.Date

class K9BackendFolder(
    private val preferences: Preferences,
    private val account: Account,
    private val localStore: LocalStore,
    private val folderServerId: String
) : BackendFolder {
    private val database = localStore.database
    private val databaseId: String
    private val localFolder = localStore.getFolder(folderServerId)
    override val name: String
    override val visibleLimit: Int

    init {
        data class Init(val databaseId: String, val name: String, val visibleLimit: Int)

        val init = database.query(
                "folders",
                arrayOf("id", "name", "visible_limit"),
                "server_id = ?",
                folderServerId
        ) { cursor ->
            if (cursor.moveToFirst()) {
                Init(
                        databaseId = cursor.getString(0),
                        name = cursor.getString(1),
                        visibleLimit = cursor.getInt(2)
                )
            } else {
                throw IllegalStateException("Couldn't find folder $folderServerId")
            }
        }

        databaseId = init.databaseId
        name = init.name
        visibleLimit = init.visibleLimit
    }

    override fun getLastUid(): Long? {
        return database.rawQuery("SELECT MAX(uid) FROM messages WHERE folder_id = ?", databaseId) { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLongOrNull(0)
            } else {
                null
            }
        }
    }

    override fun getMessageServerIds(): Set<String> {
        return database.rawQuery("SELECT uid FROM messages" +
            " WHERE empty = 0 AND deleted = 0 AND folder_id = ? AND uid NOT LIKE '${K9.LOCAL_UID_PREFIX}%'" +
            " ORDER BY date DESC", databaseId) { cursor ->
            val result = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                val uid = cursor.getString(0)
                result.add(uid)
            }
            result
        }
    }

    override fun getAllMessagesAndEffectiveDates(): Map<String, Long?> {
        return database.rawQuery("SELECT uid, date FROM messages" +
                " WHERE empty = 0 AND deleted = 0 AND folder_id = ? AND uid NOT LIKE '${K9.LOCAL_UID_PREFIX}%'" +
                " ORDER BY date DESC", databaseId) { cursor ->
            val result = mutableMapOf<String, Long?>()
            while (cursor.moveToNext()) {
                val uid = cursor.getString(0)
                val date = cursor.getLongOrNull(1)
                result[uid] = date
            }
            result
        }
    }

    // TODO: Move implementation from LocalFolder to this class
    override fun destroyMessages(messageServerIds: List<String>) {
        val localMessages = localFolder.getMessagesByUids(messageServerIds)
        localFolder.destroyMessages(localMessages)
    }

    override fun getMoreMessages(): MoreMessages {
        val moreMessages = database.getString(column = "more_messages") ?: "unknown"
        return moreMessages.toMoreMessages()
    }

    override fun setMoreMessages(moreMessages: MoreMessages) {
        database.setString(column = "more_messages", value = moreMessages.toDatabaseValue())
    }

    override fun setLastChecked(timestamp: Long) {
        database.setLong(column = "last_updated", value = timestamp)
    }

    override fun setStatus(status: String?) {
        database.setString(column = "status", value = status)
    }

    override fun getPushState(): String? {
        return database.getString(column = "push_state")
    }

    override fun setPushState(pushState: String?) {
        return database.setString(column = "push_state", value = pushState)
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
                    null, null, null)

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

    override fun getLatestOldMessageSeenTime(): Date = Date(account.latestOldMessageSeenTime)

    override fun setLatestOldMessageSeenTime(date: Date) {
        account.latestOldMessageSeenTime = date.time
        preferences.saveAccount(account)
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

    private fun String.toMoreMessages(): MoreMessages = when (this) {
        "unknown" -> MoreMessages.UNKNOWN
        "false" -> MoreMessages.FALSE
        "true" -> MoreMessages.TRUE
        else -> throw AssertionError("Unknown value for MoreMessages: $this")
    }

    private fun MoreMessages.toDatabaseValue(): String = when (this) {
        MoreMessages.UNKNOWN -> "unknown"
        MoreMessages.FALSE -> "false"
        MoreMessages.TRUE -> "true"
    }

    private fun requireMessageServerId(message: Message) {
        if (message.uid.isNullOrEmpty()) {
            error("Message requires a server ID to be set")
        }
    }
}
