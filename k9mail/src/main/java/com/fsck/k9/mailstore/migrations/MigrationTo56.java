package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;


public class MigrationTo56 {
    public static void migratePendingCommands(SQLiteDatabase db) {
        // TODO actually migrate
        db.execSQL("DROP TABLE IF EXISTS pending_commands");
        db.execSQL("CREATE TABLE pending_commands " +
                "(id INTEGER PRIMARY KEY, command TEXT, data TEXT)");
    }
}
