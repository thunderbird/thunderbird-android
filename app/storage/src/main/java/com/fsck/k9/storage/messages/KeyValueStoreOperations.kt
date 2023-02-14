package com.fsck.k9.storage.messages

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.fsck.k9.mailstore.LockableDatabase

internal class KeyValueStoreOperations(private val lockableDatabase: LockableDatabase) {
    fun getExtraString(name: String): String? {
        return lockableDatabase.execute(false) { db ->
            db.query(
                "account_extra_values",
                arrayOf("value_text"),
                "name = ?",
                arrayOf(name),
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getStringOrNull(0)
                } else {
                    null
                }
            }
        }
    }

    fun setExtraString(name: String, value: String) {
        lockableDatabase.execute(false) { db ->
            val contentValues = ContentValues().apply {
                put("name", name)
                put("value_text", value)
            }
            db.insertWithOnConflict("account_extra_values", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun getExtraNumber(name: String): Long? {
        return lockableDatabase.execute(false) { db ->
            db.query(
                "account_extra_values",
                arrayOf("value_integer"),
                "name = ?",
                arrayOf(name),
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLongOrNull(0)
                } else {
                    null
                }
            }
        }
    }

    fun setExtraNumber(name: String, value: Long) {
        lockableDatabase.execute(false) { db ->
            val contentValues = ContentValues().apply {
                put("name", name)
                put("value_integer", value)
            }
            db.insertWithOnConflict("account_extra_values", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun getFolderExtraString(folderId: Long, name: String): String? {
        return lockableDatabase.execute(false) { db ->
            db.query(
                "folder_extra_values",
                arrayOf("value_text"),
                "name = ? AND folder_id = ?",
                arrayOf(name, folderId.toString()),
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getStringOrNull(0)
                } else {
                    null
                }
            }
        }
    }

    fun setFolderExtraString(folderId: Long, name: String, value: String?) {
        lockableDatabase.execute(false) { db ->
            val contentValues = ContentValues().apply {
                put("folder_id", folderId)
                put("name", name)
                put("value_text", value)
            }
            db.insertWithOnConflict("folder_extra_values", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun getFolderExtraNumber(folderId: Long, name: String): Long? {
        return lockableDatabase.execute(false) { db ->
            db.query(
                "folder_extra_values",
                arrayOf("value_integer"),
                "name = ? AND folder_id = ?",
                arrayOf(name, folderId.toString()),
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLongOrNull(0)
                } else {
                    null
                }
            }
        }
    }

    fun setFolderExtraNumber(folderId: Long, name: String, value: Long) {
        lockableDatabase.execute(false) { db ->
            val contentValues = ContentValues().apply {
                put("folder_id", folderId)
                put("name", name)
                put("value_integer", value)
            }
            db.insertWithOnConflict("folder_extra_values", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }
}
