package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;


class MigrationTo54 {
    public static void addPreviewTypeColumn(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE messages ADD preview_type TEXT default \"none\"");
        db.execSQL("UPDATE messages SET preview_type = 'text' WHERE preview IS NOT NULL");
    }
}
