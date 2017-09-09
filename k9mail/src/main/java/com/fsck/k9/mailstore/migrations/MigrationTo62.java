
package com.fsck.k9.mailstore.migrations;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


class MigrationTo62 {

    static void addFolderParentRemoteId(SQLiteDatabase db) {
        if (!columnExists(db, "folders", "parentRemoteId")) {
            db.execSQL("ALTER TABLE folders ADD parentRemoteId TEXT");
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
