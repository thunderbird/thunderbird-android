package com.fsck.k9.preferences

import android.database.sqlite.SQLiteDatabase

private const val TABLE_NAME = "preferences_storage"
private const val PRIMARY_KEY_COLUMN = "primkey"
private const val VALUE_COLUMN = "value"

fun createPreferencesDatabase(): SQLiteDatabase {
    val database = SQLiteDatabase.create(null)

    database.execSQL(
        """
        CREATE TABLE $TABLE_NAME (
          $PRIMARY_KEY_COLUMN TEXT PRIMARY KEY ON CONFLICT REPLACE,
          $VALUE_COLUMN TEXT
        )
        """.trimIndent(),
    )

    return database
}
