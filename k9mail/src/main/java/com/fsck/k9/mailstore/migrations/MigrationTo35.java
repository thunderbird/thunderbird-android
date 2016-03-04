package com.fsck.k9.mailstore.migrations;


import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.fsck.k9.K9;


class MigrationTo35 {
    public static void updateRemoveXNoSeenInfoFlag(SQLiteDatabase db) {
        try {
            db.execSQL("update messages set flags = replace(flags, 'X_NO_SEEN_INFO', 'X_BAD_FLAG')");
        } catch (SQLiteException e) {
            Log.e(K9.LOG_TAG, "Unable to get rid of obsolete flag X_NO_SEEN_INFO", e);
        }
    }
}
