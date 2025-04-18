package com.fsck.k9.storage.messages

import android.database.Cursor
import androidx.core.database.getLongOrNull
import app.k9mail.core.android.common.database.map
import app.k9mail.legacy.mailstore.FolderDetailsAccessor
import app.k9mail.legacy.mailstore.FolderMapper
import app.k9mail.legacy.mailstore.MoreMessages
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mailstore.FolderNotFoundException
import com.fsck.k9.mailstore.LockableDatabase
import com.fsck.k9.mailstore.toFolderType
import com.fsck.k9.search.SqlQueryBuilder
import net.thunderbird.feature.search.ConditionsTreeNode

internal class RetrieveFolderOperations(private val lockableDatabase: LockableDatabase) {
    fun <T> getFolder(folderId: Long, mapper: FolderMapper<T>): T? {
        return getFolder(
            selection = "id = ?",
            selectionArguments = arrayOf(folderId.toString()),
            mapper = mapper,
        )
    }

    fun <T> getFolder(folderServerId: String, mapper: FolderMapper<T>): T? {
        return getFolder(
            selection = "server_id = ?",
            selectionArguments = arrayOf(folderServerId),
            mapper = mapper,
        )
    }

    private fun <T> getFolder(selection: String, selectionArguments: Array<String>, mapper: FolderMapper<T>): T? {
        return lockableDatabase.execute(false) { db ->
            db.query(
                "folders",
                FOLDER_COLUMNS,
                selection,
                selectionArguments,
                null,
                null,
                null,
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

    fun <T> getDisplayFolders(includeHiddenFolders: Boolean, outboxFolderId: Long?, mapper: FolderMapper<T>): List<T> {
        return lockableDatabase.execute(false) { db ->
            val displayModeSelection = getDisplayModeSelection(includeHiddenFolders)
            val outboxFolderIdOrZero = outboxFolderId ?: 0

            val query =
                """
SELECT ${FOLDER_COLUMNS.joinToString()}, (
  SELECT COUNT(messages.id)
  FROM messages
  WHERE messages.folder_id = folders.id
    AND messages.empty = 0 AND messages.deleted = 0
    AND (messages.read = 0 OR folders.id = ?)
), (
  SELECT COUNT(messages.id)
  FROM messages
  WHERE messages.folder_id = folders.id
    AND messages.empty = 0 AND messages.deleted = 0
    AND messages.flagged = 1
)
FROM folders
$displayModeSelection
                """

            db.rawQuery(query, arrayOf(outboxFolderIdOrZero.toString())).use { cursor ->
                val cursorFolderAccessor = CursorFolderAccessor(cursor)
                cursor.map {
                    mapper.map(cursorFolderAccessor)
                }
            }
        }
    }

    private fun getDisplayModeSelection(includeHiddenFolders: Boolean): String {
        return if (includeHiddenFolders) {
            ""
        } else {
            "WHERE visible = 1"
        }
    }

    fun getFolderId(folderServerId: String): Long? {
        return lockableDatabase.execute(false) { db ->
            db.query(
                "folders",
                arrayOf("id"),
                "server_id = ?",
                arrayOf(folderServerId),
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            }
        }
    }

    fun getFolderServerId(folderId: Long): String? {
        return lockableDatabase.execute(false) { db ->
            db.query(
                "folders",
                arrayOf("server_id"),
                "id = ?",
                arrayOf(folderId.toString()),
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }
    }

    fun getMessageCount(folderId: Long): Int {
        return lockableDatabase.execute(false) { db ->
            db.rawQuery(
                "SELECT COUNT(id) FROM messages WHERE empty = 0 AND deleted = 0 AND folder_id = ?",
                arrayOf(folderId.toString()),
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        }
    }

    fun getUnreadMessageCount(folderId: Long): Int {
        return lockableDatabase.execute(false) { db ->
            db.rawQuery(
                "SELECT COUNT(id) FROM messages WHERE empty = 0 AND deleted = 0 AND read = 0 AND folder_id = ?",
                arrayOf(folderId.toString()),
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        }
    }

    fun getUnreadMessageCount(conditions: ConditionsTreeNode?): Int {
        return getMessageCount(condition = "messages.read = 0", conditions)
    }

    fun getStarredMessageCount(conditions: ConditionsTreeNode?): Int {
        return getMessageCount(condition = "messages.flagged = 1", conditions)
    }

    private fun getMessageCount(condition: String, extraConditions: ConditionsTreeNode?): Int {
        val whereBuilder = StringBuilder()
        val queryArgs = mutableListOf<String>()
        SqlQueryBuilder.buildWhereClause(extraConditions, whereBuilder, queryArgs)

        val where = if (whereBuilder.isNotEmpty()) "AND ($whereBuilder)" else ""
        val selectionArgs = queryArgs.toTypedArray()

        val query =
            """
SELECT COUNT(messages.id)
FROM messages
JOIN folders ON (folders.id = messages.folder_id)
WHERE (messages.empty = 0 AND messages.deleted = 0 AND $condition) $where
            """

        return lockableDatabase.execute(false) { db ->
            db.rawQuery(query, selectionArgs).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        }
    }

    fun hasMoreMessages(folderId: Long): MoreMessages {
        return getFolder(folderId) { it.moreMessages } ?: throw FolderNotFoundException(folderId)
    }
}

private class CursorFolderAccessor(val cursor: Cursor) : FolderDetailsAccessor {
    override val id: Long
        get() = cursor.getLong(0)

    override val name: String
        get() = cursor.getString(1)

    override val type: FolderType
        get() = cursor.getString(2).toFolderType()

    override val serverId: String?
        get() = cursor.getString(3)

    override val isLocalOnly: Boolean
        get() = cursor.getInt(4) == 1

    override val isInTopGroup: Boolean
        get() = cursor.getInt(5) == 1

    override val isIntegrate: Boolean
        get() = cursor.getInt(6) == 1

    override val isSyncEnabled: Boolean
        get() = cursor.getInt(7) == 1

    override val isVisible: Boolean
        get() = cursor.getInt(8) == 1

    override val isNotificationsEnabled: Boolean
        get() = cursor.getInt(9) == 1

    override val isPushEnabled: Boolean
        get() = cursor.getInt(10) == 1

    override val visibleLimit: Int
        get() = cursor.getInt(11)

    override val moreMessages: MoreMessages
        get() = MoreMessages.fromDatabaseName(cursor.getString(12))

    override val lastChecked: Long?
        get() = cursor.getLongOrNull(13)

    override val unreadMessageCount: Int
        get() = cursor.getInt(14)

    override val starredMessageCount: Int
        get() = cursor.getInt(15)

    override fun serverIdOrThrow(): String {
        return serverId ?: error("No server ID found for folder '$name' ($id)")
    }
}

private val FOLDER_COLUMNS = arrayOf(
    "id",
    "name",
    "type",
    "server_id",
    "local_only",
    "top_group",
    "integrate",
    "sync_enabled",
    "visible",
    "notifications_enabled",
    "push_enabled",
    "visible_limit",
    "more_messages",
    "last_updated",
)
