package com.fsck.k9.mailstore.migrations;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


class MigrationTo47 {
    public static void createThreadsTable(SQLiteDatabase db) {
        // Create new 'threads' table
        db.execSQL("DROP TABLE IF EXISTS threads");
        db.execSQL("CREATE TABLE threads (" +
                "id INTEGER PRIMARY KEY, " +
                "message_id INTEGER, " +
                "root INTEGER, " +
                "parent INTEGER" +
                ")");

        // Create indices for new table
        db.execSQL("DROP INDEX IF EXISTS threads_message_id");
        db.execSQL("CREATE INDEX IF NOT EXISTS threads_message_id ON threads (message_id)");

        db.execSQL("DROP INDEX IF EXISTS threads_root");
        db.execSQL("CREATE INDEX IF NOT EXISTS threads_root ON threads (root)");

        db.execSQL("DROP INDEX IF EXISTS threads_parent");
        db.execSQL("CREATE INDEX IF NOT EXISTS threads_parent ON threads (parent)");

        // Create entries for all messages in 'threads' table
        db.execSQL("INSERT INTO threads (message_id) SELECT id FROM messages");

        // Copy thread structure from 'messages' table to 'threads'
        Cursor cursor = db.query("messages",
                new String[] { "id", "thread_root", "thread_parent" },
                null, null, null, null, null);
        try {
            ContentValues cv = new ContentValues();
            while (cursor.moveToNext()) {
                cv.clear();
                long messageId = cursor.getLong(0);

                if (!cursor.isNull(1)) {
                    long threadRootMessageId = cursor.getLong(1);
                    db.execSQL("UPDATE threads SET root = (SELECT t.id FROM " +
                                    "threads t WHERE t.message_id = ?) " +
                                    "WHERE message_id = ?",
                            new String[] {
                                    Long.toString(threadRootMessageId),
                                    Long.toString(messageId)
                            });
                }

                if (!cursor.isNull(2)) {
                    long threadParentMessageId = cursor.getLong(2);
                    db.execSQL("UPDATE threads SET parent = (SELECT t.id FROM " +
                                    "threads t WHERE t.message_id = ?) " +
                                    "WHERE message_id = ?",
                            new String[] {
                                    Long.toString(threadParentMessageId),
                                    Long.toString(messageId)
                            });
                }
            }
        } finally {
            cursor.close();
        }

        // Remove indices for old thread-related columns in 'messages' table
        db.execSQL("DROP INDEX IF EXISTS msg_thread_root");
        db.execSQL("DROP INDEX IF EXISTS msg_thread_parent");

        // Clear out old thread-related columns in 'messages'
        ContentValues cv = new ContentValues();
        cv.putNull("thread_root");
        cv.putNull("thread_parent");
        db.update("messages", cv, null, null);
    }
}
