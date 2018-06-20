package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;


class MigrationTo58 {
    static void cleanUpOrphanedData(SQLiteDatabase db) {
        cleanUpFtsTable(db);
        cleanUpMessagePartsTable(db);
    }
    
    private static void cleanUpFtsTable(SQLiteDatabase db) {
        MigrationTo56.cleanUpFtsTable(db);
    }

    private static void cleanUpMessagePartsTable(SQLiteDatabase db) {
        db.execSQL("DELETE FROM message_parts WHERE root NOT IN " +
                "(SELECT message_part_id FROM messages WHERE deleted = 0 AND message_part_id IS NOT NULL)");
    }

    static void createDeleteMessageTrigger(SQLiteDatabase db) {
        db.execSQL("DROP TRIGGER IF EXISTS delete_message");
        db.execSQL("CREATE TRIGGER delete_message " +
                "BEFORE DELETE ON messages " +
                "BEGIN " +
                "DELETE FROM message_parts WHERE root = OLD.message_part_id; " +
                "DELETE FROM messages_fulltext WHERE docid = OLD.id; " +
                "END");
    }
}
