package com.fsck.k9.storage.migrations;


import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.fsck.k9.Account;
import com.fsck.k9.mail.FolderClass;
import com.fsck.k9.mailstore.MigrationsHelper;


class MigrationTo50 {
    public static void foldersAddNotifyClassColumn(SQLiteDatabase db, MigrationsHelper migrationsHelper) {
        try {
            db.execSQL("ALTER TABLE folders ADD notify_class TEXT default '" +
                    FolderClass.INHERITED.name() + "'");
        } catch (SQLiteException e) {
            if (!e.getMessage().startsWith("duplicate column name:")) {
                throw e;
            }
        }

        ContentValues cv = new ContentValues();
        cv.put("notify_class", FolderClass.FIRST_CLASS.name());

        Account account = migrationsHelper.getAccount();
        db.update("folders", cv, "name = ?", new String[] { account.getInboxFolder() });
    }
}
