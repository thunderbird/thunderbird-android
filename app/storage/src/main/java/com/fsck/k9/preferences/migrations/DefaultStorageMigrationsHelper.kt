package com.fsck.k9.preferences.migrations

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.helper.Utility
import timber.log.Timber

class DefaultStorageMigrationsHelper : StorageMigrationsHelper {
    override fun readAllValues(db: SQLiteDatabase): Map<String, String> {
        val loadedValues = HashMap<String, String>()
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("SELECT primkey, value FROM preferences_storage", null)
            while (cursor.moveToNext()) {
                val key = cursor.getString(0)
                val value = cursor.getString(1)
                Timber.d("Loading key '%s', value = '%s'", key, value)
                loadedValues[key] = value
            }
        } finally {
            Utility.closeQuietly(cursor)
        }

        return loadedValues
    }

    override fun readValue(db: SQLiteDatabase, key: String): String? {
        var cursor: Cursor? = null
        var value: String? = null
        try {
            cursor = db.query(
                "preferences_storage",
                arrayOf("value"),
                "primkey = ?",
                arrayOf(key),
                null,
                null,
                null
            )

            if (cursor.moveToNext()) {
                value = cursor.getString(0)
                Timber.d("Loading key '%s', value = '%s'", key, value)
            }
        } finally {
            Utility.closeQuietly(cursor)
        }

        return value
    }

    override fun writeValue(db: SQLiteDatabase, key: String, value: String?) {
        if (value == null) {
            db.delete("preferences_storage", "primkey = ?", arrayOf(key))
            return
        }

        val cv = ContentValues()
        cv.put("primkey", key)
        cv.put("value", value)

        val result = db.update("preferences_storage", cv, "primkey = ?", arrayOf(key)).toLong()

        if (result == -1L) {
            Timber.e("Error writing key '%s', value = '%s'", key, value)
        }
    }

    override fun insertValue(db: SQLiteDatabase, key: String, value: String?) {
        if (value == null) {
            return
        }

        val cv = ContentValues()
        cv.put("primkey", key)
        cv.put("value", value)

        val result = db.insert("preferences_storage", null, cv)

        if (result == -1L) {
            Timber.e("Error writing key '%s', value = '%s'", key, value)
        }
    }
}
