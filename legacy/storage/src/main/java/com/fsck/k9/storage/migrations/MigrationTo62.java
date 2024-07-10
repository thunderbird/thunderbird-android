package com.fsck.k9.storage.migrations;


import android.database.sqlite.SQLiteDatabase;


class MigrationTo62 {
    public static void addServerIdColumnToFoldersTable(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE folders ADD server_id TEXT");
        db.execSQL("UPDATE folders SET server_id = name");

        db.execSQL("DROP INDEX IF EXISTS folder_name");
        db.execSQL("CREATE INDEX folder_server_id ON folders (server_id)");
    }
}
