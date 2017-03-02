package com.fsck.k9.mailstore.migrations;


import java.util.List;

import timber.log.Timber;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.preferences.Storage;
import com.fsck.k9.preferences.StorageEditor;


class MigrationTo42 {
    public static void from41MoveFolderPreferences(MigrationsHelper migrationsHelper) {
        try {
            LocalStore localStore = migrationsHelper.getLocalStore();
            Storage storage = migrationsHelper.getStorage();

            long startTime = System.currentTimeMillis();
            StorageEditor editor = storage.edit();

            List<? extends Folder > folders = localStore.getPersonalNamespaces(true);
            for (Folder folder : folders) {
                if (folder instanceof LocalFolder) {
                    LocalFolder lFolder = (LocalFolder)folder;
                    lFolder.save(editor);
                }
            }

            editor.commit();
            long endTime = System.currentTimeMillis();
            Timber.i("Putting folder preferences for " + folders.size() +
                    " folders back into Preferences took " + (endTime - startTime) + " ms");
        } catch (Exception e) {
            Timber.e(e, "Could not replace Preferences in upgrade from DB_VERSION 41");
        }
    }
}
