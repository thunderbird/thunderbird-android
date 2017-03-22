package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;


class MigrationTo34 {
    public static void addFlaggedCountColumn(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE folders ADD flagged_count INTEGER default 0");
        } catch (SQLiteException e) {
            if (!e.getMessage().startsWith("duplicate column name: flagged_count")) {
                throw e;
            }
        }
    }
}
