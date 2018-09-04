package com.fsck.k9.storage.migrations;


import android.database.sqlite.SQLiteDatabase;


class MigrationTo53 {
    public static void removeNullValuesFromEmptyColumnInMessagesTable(SQLiteDatabase db) {
        db.execSQL("UPDATE messages SET empty = 0 WHERE empty IS NULL");
    }
}
