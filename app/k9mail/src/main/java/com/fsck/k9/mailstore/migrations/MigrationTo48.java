package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;


class MigrationTo48 {
    public static void updateThreadsSetRootWhereNull(SQLiteDatabase db) {
        db.execSQL("UPDATE threads SET root=id WHERE root IS NULL");

        db.execSQL("CREATE TRIGGER set_thread_root " +
                "AFTER INSERT ON threads " +
                "BEGIN " +
                "UPDATE threads SET root=id WHERE root IS NULL AND ROWID = NEW.ROWID; " +
                "END");
    }
}
