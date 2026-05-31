package com.fsck.k9.storage.messages

import android.content.ContentValues
import com.fsck.k9.mailstore.LockableDatabase
import net.thunderbird.core.common.mail.Flag

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
            database.updateMessageFlagsByServerId(folderId, messageServerId) { oldFlags ->
                if (set) {
                    oldFlags + flag
                } else {
                    oldFlags - flag
                }
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
