package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;


class MigrationTo59 {
    static void addMissingIndexes(SQLiteDatabase db) {
        addMessageCompositeIndex(db);
        addMessageEmptyIndex(db);
        addMessageFlaggedIndex(db);
        addMessageFolderIdDeletedDateIndex(db);
        addMessageReadIndex(db);
        addMessageUidIndex(db);
        addMessageReadIndex(db);
    }

    private static void addMessageCompositeIndex(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS msg_composite ON messages (deleted, empty,folder_id,flagged,read)");
    }

    private static void addMessageEmptyIndex(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS msg_empty ON messages (empty)");
    }

    private static void addMessageFlaggedIndex(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS msg_flagged ON messages (flagged)");
    }

    private static void addMessageFolderIdDeletedDateIndex(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS msg_folder_id_deleted_date ON messages (folder_id,deleted,internal_date)");
    }

    private static void addMessageReadIndex(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS msg_read ON messages (read)");
    }

    private static void addMessageUidIndex(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS msg_uid ON messages (uid, folder_id)");
    }
}
