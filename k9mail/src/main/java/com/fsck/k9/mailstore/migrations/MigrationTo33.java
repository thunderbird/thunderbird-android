package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;


class MigrationTo33 {
    public static void addPreviewColumn(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE messages ADD preview TEXT");
        } catch (SQLiteException e) {
            if (!e.toString().startsWith("duplicate column name: preview")) {
                throw e;
            }
        }
    }
}
