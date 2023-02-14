package com.fsck.k9.storage.messages

import android.content.ContentValues
import com.fsck.k9.mailstore.LockableDatabase

internal class UpdateMessageOperations(private val lockableDatabase: LockableDatabase) {

    fun setNewMessageState(folderId: Long, messageServerId: String, newMessage: Boolean) {
        lockableDatabase.execute(false) { database ->
            val values = ContentValues().apply {
                put("new_message", if (newMessage) 1 else 0)
            }

            database.update(
                "messages",
                values,
                "folder_id = ? AND uid = ?",
                arrayOf(folderId.toString(), messageServerId),
            )
        }
    }

    fun clearNewMessageState() {
        lockableDatabase.execute(false) { database ->
            database.execSQL("UPDATE messages SET new_message = 0")
        }
    }
}
