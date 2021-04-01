package com.fsck.k9.storage.messages

import android.database.Cursor
import com.fsck.k9.helper.map
import com.fsck.k9.mail.FolderClass
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mailstore.FolderDetailsAccessor
import com.fsck.k9.mailstore.FolderMapper
import com.fsck.k9.mailstore.LockableDatabase
import com.fsck.k9.mailstore.toFolderType

internal class RetrieveFolderOperations(private val lockableDatabase: LockableDatabase) {
    fun <T> getFolder(folderId: Long, mapper: FolderMapper<T>): T? {
        return lockableDatabase.execute(false) { db ->
            db.query(
                "folders",
                FOLDER_COLUMNS,
                "id = ?",
                arrayOf(folderId.toString()),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val cursorFolderAccessor = CursorFolderAccessor(cursor)
                    mapper.map(cursorFolderAccessor)
                } else {
                    null
                }
            }
        }
    }

    fun <T> getFolders(excludeLocalOnly: Boolean = false, mapper: FolderMapper<T>): List<T> {
        val selection = if (excludeLocalOnly) "local_only = 0" else null
        return lockableDatabase.execute(false) { db ->
            db.query("folders", FOLDER_COLUMNS, selection, null, null, null, "id").use { cursor ->
                val cursorFolderAccessor = CursorFolderAccessor(cursor)
                cursor.map {
                    mapper.map(cursorFolderAccessor)
                }
            }
        }
    }
}

private class CursorFolderAccessor(val cursor: Cursor) : FolderDetailsAccessor {
    override val id: Long
        get() = cursor.getLong(0)

    override val name: String
        get() = cursor.getString(1)

    override val type: FolderType
        get() = cursor.getString(2).toFolderType()

    override val serverId: String
        get() = cursor.getString(3)

    override val isLocalOnly: Boolean
        get() = cursor.getInt(4) == 1

    override val isInTopGroup: Boolean
        get() = cursor.getInt(5) == 1

    override val isIntegrate: Boolean
        get() = cursor.getInt(6) == 1

    override val syncClass: FolderClass
        get() = FolderClass.valueOf(cursor.getString(7))

    override val displayClass: FolderClass
        get() = FolderClass.valueOf(cursor.getString(8))

    override val notifyClass: FolderClass
        get() = FolderClass.valueOf(cursor.getString(9))

    override val pushClass: FolderClass
        get() = FolderClass.valueOf(cursor.getString(10))
}

private val FOLDER_COLUMNS = arrayOf(
    "id",
    "name",
    "type",
    "server_id",
    "local_only",
    "top_group",
    "integrate",
    "poll_class",
    "display_class",
    "notify_class",
    "push_class"
)
