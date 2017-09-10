package com.fsck.k9.mailstore.migrations;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


class MigrationTo61 {

    static void addFolderRemoteId(SQLiteDatabase db) {
        if (!columnExists(db, "folders", "remoteId")) {
            db.execSQL("ALTER TABLE folders ADD remoteId TEXT");
            db.execSQL("UPDATE folders SET remoteId = name");
            db.execSQL("DROP INDEX IF EXISTS folder_name");
            db.execSQL("CREATE INDEX IF NOT EXISTS folder_remoteId ON folders (remoteId)");
        }
    }

    private static boolean columnExists(SQLiteDatabase db, String table, String columnName) {
        Cursor columnCursor = db.rawQuery("PRAGMA table_info(" + table + ")", null);
        boolean foundColumn = false;
        while (columnCursor.moveToNext()) {
            String currentColumnName = columnCursor.getString(1);
            if (currentColumnName.equals(columnName)) {
                foundColumn = true;
                break;
            }
        }
        columnCursor.close();
        return foundColumn;
    }
}
