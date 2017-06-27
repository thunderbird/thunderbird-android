package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;


class MigrationTo61 {
    public static void foldersAddHighestModSeqColumn(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE folders ADD highest_mod_seq INTEGER default 0");
        } catch (SQLiteException e) {
            if (!e.getMessage().startsWith("duplicate column name:")) {
                throw e;
            }
        }
    }
}
