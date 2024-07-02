package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase

internal object MigrationTo64 {
    @JvmStatic
    fun addExtraValuesTables(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE account_extra_values (" +
                "name TEXT NOT NULL PRIMARY KEY, " +
                "value_text TEXT, " +
                "value_integer INTEGER " +
                ")",
        )

        db.execSQL(
            "CREATE TABLE folder_extra_values (" +
                "folder_id INTEGER NOT NULL, " +
                "name TEXT NOT NULL, " +
                "value_text TEXT, " +
                "value_integer INTEGER, " +
                "PRIMARY KEY (folder_id, name)" +
                ")",
        )

        db.execSQL(
            "CREATE TRIGGER delete_folder_extra_values " +
                "BEFORE DELETE ON folders " +
                "BEGIN " +
                "DELETE FROM folder_extra_values WHERE old.id = folder_id; " +
                "END;",
        )
    }
}
