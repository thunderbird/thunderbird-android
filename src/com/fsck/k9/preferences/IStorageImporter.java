package com.fsck.k9.preferences;

import com.fsck.k9.Preferences;
import com.fsck.k9.preferences.StorageImporter.ImportElement;

import android.content.SharedPreferences;

public interface IStorageImporter {
    public boolean needsKey();
    public abstract int importPreferences(Preferences preferences, SharedPreferences.Editor context, ImportElement dataset, String encryptionKey)  throws StorageImportExportException;
}