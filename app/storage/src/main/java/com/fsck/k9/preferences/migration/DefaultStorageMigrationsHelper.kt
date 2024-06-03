package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import app.k9mail.core.android.common.database.getStringOrThrow
import app.k9mail.core.android.common.database.map
import timber.log.Timber

class DefaultStorageMigrationsHelper : StorageMigrationsHelper {
    override fun readAllValues(db: SQLiteDatabase): Map<String, String> {
        return db.query(TABLE_NAME, arrayOf(KEY_COLUMN, VALUE_COLUMN), null, null, null, null, null).use {
            it.map { cursor ->
                val key = cursor.getStringOrThrow(KEY_COLUMN)
                val value = cursor.getStringOrThrow(VALUE_COLUMN)
                Timber.d("Loading key '%s', value = '%s'", key, value)

                key to value
            }
        }.toMap()
    }

    override fun readValue(db: SQLiteDatabase, key: String): String? {
        return db.query(
            TABLE_NAME,
            arrayOf(VALUE_COLUMN),
            "$KEY_COLUMN = ?",
            arrayOf(key),
            null,
            null,
            null,
        ).use { cursor ->
            if (cursor.moveToNext()) {
                cursor.getStringOrThrow(VALUE_COLUMN).also { value ->
                    Timber.d("Loading key '%s', value = '%s'", key, value)
                }
            } else {
                null
            }
        }
    }

    override fun writeValue(db: SQLiteDatabase, key: String, value: String?) {
        if (value == null) {
            db.delete(TABLE_NAME, "$KEY_COLUMN = ?", arrayOf(key))
            return
        }

        val values = contentValuesOf(
            KEY_COLUMN to key,
            VALUE_COLUMN to value,
        )

        val result = db.update(TABLE_NAME, values, "$KEY_COLUMN = ?", arrayOf(key))

        if (result == -1) {
            Timber.e("Error writing key '%s', value = '%s'", key, value)
        }
    }

    override fun insertValue(db: SQLiteDatabase, key: String, value: String?) {
        if (value == null) {
            return
        }

        val values = contentValuesOf(
            KEY_COLUMN to key,
            VALUE_COLUMN to value,
        )

        val result = db.insert(TABLE_NAME, null, values)

        if (result == -1L) {
            Timber.e("Error writing key '%s', value = '%s'", key, value)
        }
    }

    companion object {
        private const val TABLE_NAME = "preferences_storage"
        private const val KEY_COLUMN = "primkey"
        private const val VALUE_COLUMN = "value"
    }
}
