package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;


class MigrationTo45 {
    public static void changeThreadingIndexes(SQLiteDatabase db) {
        try {
            db.execSQL("DROP INDEX IF EXISTS msg_empty");
            db.execSQL("CREATE INDEX IF NOT EXISTS msg_empty ON messages (empty)");

            db.execSQL("DROP INDEX IF EXISTS msg_thread_root");
            db.execSQL("CREATE INDEX IF NOT EXISTS msg_thread_root ON messages (thread_root)");

            db.execSQL("DROP INDEX IF EXISTS msg_thread_parent");
            db.execSQL("CREATE INDEX IF NOT EXISTS msg_thread_parent ON messages (thread_parent)");
        } catch (SQLiteException e) {
            if (!e.getMessage().startsWith("duplicate column name:")) {
                throw e;
            }
        }
    }
}
