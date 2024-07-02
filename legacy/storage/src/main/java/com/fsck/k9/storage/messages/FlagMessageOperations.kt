package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mail.Flag
import com.fsck.k9.mailstore.LockableDatabase

internal val SPECIAL_FLAGS = setOf(Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED, Flag.FORWARDED)

internal class FlagMessageOperations(private val lockableDatabase: LockableDatabase) {

    fun setFlag(messageIds: Collection<Long>, flag: Flag, set: Boolean) {
        require(messageIds.isNotEmpty()) { "'messageIds' must not be empty" }

        if (flag in SPECIAL_FLAGS) {
            setSpecialFlags(messageIds, flag, set)
        } else {
            throw UnsupportedOperationException("not implemented")
        }
    }

    fun setMessageFlag(folderId: Long, messageServerId: String, flag: Flag, set: Boolean) {
        when (flag) {
            Flag.DELETED -> setBoolean(folderId, messageServerId, "deleted", set)
            Flag.SEEN -> setBoolean(folderId, messageServerId, "read", set)
            Flag.FLAGGED -> setBoolean(folderId, messageServerId, "flagged", set)
            Flag.ANSWERED -> setBoolean(folderId, messageServerId, "answered", set)
            Flag.FORWARDED -> setBoolean(folderId, messageServerId, "forwarded", set)
            else -> rebuildFlagsColumnValue(folderId, messageServerId, flag, set)
        }
    }

    private fun setSpecialFlags(messageIds: Collection<Long>, flag: Flag, set: Boolean) {
        val columnName = when (flag) {
            Flag.SEEN -> "read"
            Flag.FLAGGED -> "flagged"
            Flag.ANSWERED -> "answered"
            Flag.FORWARDED -> "forwarded"
            else -> error("Unsupported flag: $flag")
        }
        val columnValue = if (set) 1 else 0

        val contentValues = ContentValues().apply {
            put(columnName, columnValue)
        }

        lockableDatabase.execute(true) { database ->
            performChunkedOperation(
                arguments = messageIds,
                argumentTransformation = Long::toString,
            ) { selectionSet, selectionArguments ->
                database.update("messages", contentValues, "id $selectionSet", selectionArguments)
            }
        }
    }

    private fun rebuildFlagsColumnValue(folderId: Long, messageServerId: String, flag: Flag, set: Boolean) {
        lockableDatabase.execute(true) { database ->
            val oldFlags = database.readFlagsColumn(folderId, messageServerId)

            val newFlags = if (set) oldFlags + flag else oldFlags - flag
            val newFlagsString = newFlags.joinToString(separator = ",")

            val values = ContentValues().apply {
                put("flags", newFlagsString)
            }

            database.update(
                "messages",
                values,
                "folder_id = ? AND uid = ?",
                arrayOf(folderId.toString(), messageServerId),
            )
        }
    }

    private fun SQLiteDatabase.readFlagsColumn(folderId: Long, messageServerId: String): Set<Flag> {
        return query(
            "messages",
            arrayOf("flags"),
            "folder_id = ? AND uid = ?",
            arrayOf(folderId.toString(), messageServerId),
            null,
            null,
            null,
        ).use { cursor ->
            if (!cursor.moveToFirst()) error("Message not found $folderId:$messageServerId")

            if (!cursor.isNull(0)) {
                cursor.getString(0).split(',').map { flagString -> Flag.valueOf(flagString) }.toSet()
            } else {
                emptySet()
            }
        }
    }

    private fun setBoolean(folderId: Long, messageServerId: String, columnName: String, value: Boolean) {
        lockableDatabase.execute(false) { database ->
            val values = ContentValues().apply {
                put(columnName, if (value) 1 else 0)
            }

            database.update(
                "messages",
                values,
                "folder_id = ? AND uid = ?",
                arrayOf(folderId.toString(), messageServerId),
            )
        }
    }
}
