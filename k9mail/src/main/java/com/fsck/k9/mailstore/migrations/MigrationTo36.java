package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.fsck.k9.K9;


class MigrationTo36 {
    public static void addAttachmentsContentIdColumn(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE attachments ADD content_id TEXT");
        } catch (SQLiteException e) {
            Log.e(K9.LOG_TAG, "Unable to add content_id column to attachments");
        }
    }
}
