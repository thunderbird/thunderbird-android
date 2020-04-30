package com.fsck.k9.mailstore

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getStringOrNull
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendFolderUpdater
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.FolderType as RemoteFolderType

class K9BackendStorage(
    private val preferences: Preferences,
    private val account: Account,
    private val localStore: LocalStore,
    private val listeners: List<BackendFoldersRefreshListener>
) : BackendStorage {
    private val database = localStore.database

    override fun getFolder(folderServerId: String): BackendFolder {
        return K9BackendFolder(preferences, account, localStore, folderServerId)
    }

    override fun getFolderServerIds(): List<String> {
        return database.query("folders", arrayOf("server_id"), "local_only = 0") { cursor ->
            val folderServerIds = mutableListOf<String>()
            while (cursor.moveToNext()) {
                folderServerIds.add(cursor.getString(0))
            }

            folderServerIds
        }
    }

    override fun createFolderUpdater(): BackendFolderUpdater {
        return K9BackendFolderUpdater()
    }

    override fun getExtraString(name: String): String? {
        return database.execute(false) { db ->
            val cursor = db.query(
                    "account_extra_values",
                    arrayOf("value_text"),
                    "name = ?",
                    arrayOf(name),
                    null, null, null)
            cursor.use {
                if (it.moveToFirst()) {
                    it.getStringOrNull(0)
                } else {
                    null
                }
            }
        }
    }

    override fun setExtraString(name: String, value: String) {
        database.execute(false) { db ->
            val contentValues = ContentValues().apply {
                put("name", name)
                put("value_text", value)
            }
            db.insertWithOnConflict("account_extra_values", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    override fun getExtraNumber(name: String): Long? {
        return database.execute(false) { db ->
            val cursor = db.query(
                    "account_extra_values",
                    arrayOf("value_integer"),
                    "name = ?",
                    arrayOf(name),
                    null, null, null)
            cursor.use {
                if (it.moveToFirst()) {
                    it.getLongOrNull(0)
                } else {
                    null
                }
            }
        }
    }

    override fun setExtraNumber(name: String, value: Long) {
        database.execute(false) { db ->
            val contentValues = ContentValues().apply {
                put("name", name)
                put("value_integer", value)
            }
            db.insertWithOnConflict("account_extra_values", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    private fun Cursor.getLongOrNull(columnIndex: Int): Long? = if (isNull(columnIndex)) null else getLong(columnIndex)

    private inner class K9BackendFolderUpdater : BackendFolderUpdater {
        init {
            listeners.forEach { it.onBeforeFolderListRefresh() }
        }

        override fun createFolders(folders: List<FolderInfo>) {
            if (folders.isEmpty()) return

            val localFolders = folders.map { localStore.getFolder(it.serverId, it.name, it.type) }
            localStore.createFolders(localFolders, account.displayCount)
        }

        override fun deleteFolders(folderServerIds: List<String>) {
            folderServerIds.asSequence()
                .map { localStore.getFolder(it) }
                .forEach { it.delete() }
        }

        override fun changeFolder(folderServerId: String, name: String, type: RemoteFolderType) {
            database.execute(false) { db ->
                val values = ContentValues().apply {
                    put("name", name)
                    put("type", type.toDatabaseFolderType())
                }

                db.update("folders", values, "server_id = ?", arrayOf(folderServerId))
            }
        }

        override fun close() {
            listeners.forEach { it.onAfterFolderListRefresh() }
        }
    }
}
