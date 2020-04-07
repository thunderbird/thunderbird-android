package com.fsck.k9.storage.migrations;


import android.database.sqlite.SQLiteDatabase;

import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.MigrationsHelper;


public class Migrations {
    @SuppressWarnings("fallthrough")
    public static void upgradeDatabase(SQLiteDatabase db, MigrationsHelper migrationsHelper) {
        boolean shouldBuildFtsTable = false;
        switch (db.getVersion()) {
            case 61:
                MigrationTo62.addServerIdColumnToFoldersTable(db);
            case 63:
                MigrationTo64.addExtraValuesTables(db);
            case 64:
                MigrationTo65.addLocalOnlyColumnToFoldersTable(db, migrationsHelper);
            case 65:
                MigrationTo66.addEncryptionTypeColumnToMessagesTable(db);
            case 66:
                MigrationTo67.addTypeColumnToFoldersTable(db, migrationsHelper);
            case 67:
                MigrationTo68.addOutboxStateTable(db);
            case 68:
                new MigrationTo69(db).createPendingDelete();
            case 69:
                new MigrationTo70(db).removePushState();
        }

        if (shouldBuildFtsTable) {
            buildFtsTable(db, migrationsHelper);
        }
    }

    private static void buildFtsTable(SQLiteDatabase db, MigrationsHelper migrationsHelper) {
        LocalStore localStore = migrationsHelper.getLocalStore();
        FullTextIndexer fullTextIndexer = new FullTextIndexer(localStore, db);
        fullTextIndexer.indexAllMessages();
    }
}
