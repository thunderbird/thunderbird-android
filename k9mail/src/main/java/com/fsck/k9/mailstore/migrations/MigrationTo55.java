package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;


public class MigrationTo55 {
    public static void createFtsSearchTable(SQLiteDatabase db, MigrationsHelper migrationsHelper) {
        db.execSQL("CREATE VIRTUAL TABLE messages_fulltext USING fts4 (fulltext)");
    }
}
