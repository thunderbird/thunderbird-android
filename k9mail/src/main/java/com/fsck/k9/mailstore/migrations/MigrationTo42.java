package com.fsck.k9.mailstore.migrations;


import java.util.List;

import android.os.SystemClock;

import com.fsck.k9.mail.Folder;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.preferences.Storage;
import com.fsck.k9.preferences.StorageEditor;
import timber.log.Timber;


class MigrationTo42 {
    public static void from41MoveFolderPreferences(MigrationsHelper migrationsHelper) {
        try {
            LocalStore localStore = migrationsHelper.getLocalStore();
            Storage storage = migrationsHelper.getStorage();

            long startTime = SystemClock.elapsedRealtime();
            StorageEditor editor = storage.edit();

            List<? extends Folder > folders = localStore.getFolders(true);
            for (Folder folder : folders) {
                if (folder instanceof LocalFolder) {
                    LocalFolder lFolder = (LocalFolder)folder;
                    lFolder.save(editor);
                }
            }

            editor.commit();
            long endTime = SystemClock.elapsedRealtime();
            Timber.i("Putting folder preferences for %d folders back into Preferences took %d ms",
                    folders.size(), endTime - startTime);
        } catch (Exception e) {
            Timber.e(e, "Could not replace Preferences in upgrade from DB_VERSION 41");
        }
    }
}
