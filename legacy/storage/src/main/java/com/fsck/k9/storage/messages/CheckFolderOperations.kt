package com.fsck.k9.storage.messages

import com.fsck.k9.mailstore.LockableDatabase

internal class CheckFolderOperations(private val lockableDatabase: LockableDatabase) {
    fun hasPushEnabledFolder(): Boolean {
        return lockableDatabase.execute(false) { database ->
            database.rawQuery("SELECT id FROM folders WHERE push_enabled = 1 LIMIT 1", null, null).use { cursor ->
                cursor.count > 0
            }
        }
    }
}
