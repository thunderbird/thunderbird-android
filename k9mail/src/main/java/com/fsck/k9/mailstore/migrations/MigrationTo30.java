package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;


class MigrationTo30 {
    public static void addDeletedColumn(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE messages ADD deleted INTEGER default 0");
        } catch (SQLiteException e) {
            if (!e.toString().startsWith("duplicate column name: deleted")) {
                throw e;
            }
        }
    }
}
