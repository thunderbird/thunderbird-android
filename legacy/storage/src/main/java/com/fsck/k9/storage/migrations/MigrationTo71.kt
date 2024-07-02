package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase

internal class MigrationTo71(private val db: SQLiteDatabase) {
    fun cleanUpFolderClass() {
        db.execSQL(
            "UPDATE folders SET poll_class = 'NO_CLASS' " +
                "WHERE poll_class NOT IN ('NO_CLASS', 'INHERITED', 'FIRST_CLASS', 'SECOND_CLASS')",
        )
        db.execSQL(
            "UPDATE folders SET push_class = 'NO_CLASS' " +
                "WHERE push_class NOT IN ('NO_CLASS', 'INHERITED', 'FIRST_CLASS', 'SECOND_CLASS')",
        )
        db.execSQL(
            "UPDATE folders SET display_class = 'NO_CLASS' " +
                "WHERE display_class NOT IN ('NO_CLASS', 'INHERITED', 'FIRST_CLASS', 'SECOND_CLASS')",
        )
        db.execSQL(
            "UPDATE folders SET notify_class = 'NO_CLASS' " +
                "WHERE notify_class NOT IN ('NO_CLASS', 'INHERITED', 'FIRST_CLASS', 'SECOND_CLASS')",
        )
    }
}
