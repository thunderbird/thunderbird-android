package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;


class MigrationTo61 {
    static void addKeywords(SQLiteDatabase db) {
        addKeywordTagMap();
    }

    private static void addKeywordTagMap(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE keyword_tag_map (keyword TEXT UNIQUE, tag TEXT)");
    }
}
