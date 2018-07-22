package com.fsck.k9.mailstore

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getStringOrNull
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendStorage

class K9BackendStorage(
        private val preferences: Preferences,
        private val account: Account,
        private val localStore: LocalStore
) : BackendStorage {
    private val database = localStore.database


    override fun getFolder(folderServerId: String): BackendFolder {
        return K9BackendFolder(preferences, account, localStore, folderServerId)
    }

    override fun getExtraString(name: String): String? {
        return database.execute(false) { db ->
            val cursor = db.query(
                    "account_extra_values",
                    arrayOf("value_string"),
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
                put("value_string", value)
            }
            db.update("account_extra_values", contentValues, "name = ?", arrayOf(name))
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
                put("value_integer", value)
            }
            db.update("account_extra_values", contentValues, "name = ?", arrayOf(name))
        }
    }

    private fun Cursor.getLongOrNull(columnIndex: Int): Long? = if (isNull(columnIndex)) null else getLong(columnIndex)
}
