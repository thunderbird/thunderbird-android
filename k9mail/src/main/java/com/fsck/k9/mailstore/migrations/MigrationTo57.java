package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;


class MigrationTo57 {
    private static final int IN_DATABASE = 1;
    private static final int CHILD_PART_CONTAINS_DATA = 3;

    static void fixDataLocationForMultipartParts(SQLiteDatabase db) {
        db.execSQL("UPDATE message_parts SET data_location = " + CHILD_PART_CONTAINS_DATA + " " +
                "WHERE data_location = " + IN_DATABASE + " AND mime_type LIKE 'multipart/%'");
    }
}
