package com.fsck.k9.mailstore

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getStringOrNull
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendFolder.MoreMessages
import com.fsck.k9.backend.api.MessageRemovalListener
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
        return database.rawQuery("SELECT MAX(uid) FROM messages WHERE folder_id = ?", folderServerId) { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLongOrNull(0)
            } else {
                null
            }
        }
    }

    override fun getAllMessagesAndEffectiveDates(): Map<String, Long?> {
        return database.rawQuery("SELECT uid, date FROM messages" +
                " WHERE empty = 0 AND deleted = 0 AND folder_id = ?" +
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

    override fun getMoreMessages(): BackendFolder.MoreMessages {
        val moreMessages = database.getString(column = "more_messages") ?: "unknown"
        return moreMessages.toMoreMessages()
    }

    override fun setMoreMessages(moreMessages: BackendFolder.MoreMessages) {
        database.setString(column = "more_messages", value = moreMessages.toDatabaseValue())
    }

    override fun getUnreadMessageCount(): Int {
        return database.rawQuery("SELECT COUNT(id) FROM messages" +
                " WHERE folder_id = ? AND empty = 0 AND deleted = 0 AND read = 0", databaseId) { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                throw IllegalStateException("Couldn't get unread message count for folder $folderServerId")
            }
        }
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

    // TODO: Move implementation from LocalFolder to this class
    override fun purgeToVisibleLimit(listener: MessageRemovalListener) {
        localFolder.purgeToVisibleLimit(listener)
    }

    override fun isMessagePresent(messageServerId: String): Boolean {
        return database.execute(false) { db ->
            val cursor = db.query("messages", arrayOf("id"), "uid = ?", arrayOf(messageServerId), null, null, null)
            cursor.use {
                cursor.moveToFirst()
            }
        }
    }

    override fun getMessageFlags(messageServerId: String): Set<Flag> {
        return database.execute(false) { db ->
            val cursor = db.query(
                    "messages",
                    arrayOf("deleted", "read", "flagged", "answered", "forwarded", "flags"),
                    "uid = ?",
                    arrayOf(messageServerId),
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

                val flags = flagsColumnValue.split(',').map { Flag.valueOf(it) }.toMutableSet().apply {
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
                        selection = "uid = ?",
                        selectionArgs = *arrayOf(messageServerId)
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
                        selection = "uid = ?",
                        selectionArgs = *arrayOf(messageServerId),
                        value = serializedFlags
                )
            }
        }

        localStore.notifyChange()
    }

    // TODO: Move implementation from LocalFolder to this class
    override fun saveCompleteMessage(message: Message) {
        localFolder.appendMessages(listOf(message))

        val localMessage = localFolder.getMessage(message.uid)
        localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true)
    }

    // TODO: Move implementation from LocalFolder to this class
    override fun savePartialMessage(message: Message) {
        localFolder.appendMessages(listOf(message))

        val localMessage = localFolder.getMessage(message.uid)
        localMessage.setFlag(Flag.X_DOWNLOADED_PARTIAL, true)
    }

    override fun getLatestOldMessageSeenTime(): Date = Date(account.latestOldMessageSeenTime)

    override fun setLatestOldMessageSeenTime(date: Date) {
        account.latestOldMessageSeenTime = date.time
        account.save(preferences)
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
            db.update("messages", contentValues, "uid = ?", arrayOf(messageServerId))
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

    private fun <T> LockableDatabase.query(
            table: String,
            columns: Array<String>,
            selection: String,
            vararg selectionArgs: String,
            block: (Cursor) -> T
    ): T {
        return execute(false) { db ->
            val cursor = db.query(table, columns, selection, selectionArgs, null, null, null)
            cursor.use(block)
        }
    }

    private fun <T> LockableDatabase.rawQuery(sql: String, vararg selectionArgs: String, block: (Cursor) -> T): T {
        return execute(false) { db ->
            val cursor = db.rawQuery(sql, selectionArgs)
            cursor.use(block)
        }
    }

    private fun Cursor.getLongOrNull(columnIndex: Int): Long? = if (isNull(columnIndex)) null else getLong(columnIndex)

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
}
