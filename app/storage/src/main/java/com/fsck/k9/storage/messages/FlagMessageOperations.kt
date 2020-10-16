package com.fsck.k9.storage.messages

import android.content.ContentValues
import com.fsck.k9.mail.Flag
import com.fsck.k9.mailstore.LockableDatabase

private val SPECIAL_FLAGS = setOf(Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED, Flag.FORWARDED)

internal class FlagMessageOperations(private val lockableDatabase: LockableDatabase) {

    fun setFlag(messageIds: Collection<Long>, flag: Flag, set: Boolean) {
        require(messageIds.isNotEmpty()) { "'messageIds' must not be empty" }

        if (flag in SPECIAL_FLAGS) {
            setSpecialFlags(messageIds, flag, set)
        } else {
            rebuildFlagsColumnValue(messageIds, flag, set)
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
                argumentTransformation = Long::toString
            ) { selectionSet, selectionArguments ->
                database.update("messages", contentValues, "id $selectionSet", selectionArguments)
            }
        }
    }

    private fun rebuildFlagsColumnValue(messageIds: Collection<Long>, flag: Flag, set: Boolean) {
        throw UnsupportedOperationException("not implemented")
    }
}
