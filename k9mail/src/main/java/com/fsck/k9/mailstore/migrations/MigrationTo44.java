package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;


class MigrationTo44 {
    public static void addMessagesThreadingColumns(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE messages ADD thread_root INTEGER");
            db.execSQL("ALTER TABLE messages ADD thread_parent INTEGER");
            db.execSQL("ALTER TABLE messages ADD normalized_subject_hash INTEGER");
            db.execSQL("ALTER TABLE messages ADD empty INTEGER");
        } catch (SQLiteException e) {
            if (!e.getMessage().startsWith("duplicate column name:")) {
                throw e;
            }
        }
    }
}
