package com.fsck.k9.preferences;

import com.fsck.k9.Preferences;

import android.content.SharedPreferences;

public interface IStorageImporter {
    public abstract int importPreferences(Preferences preferences, SharedPreferences.Editor context, String data, String encryptionKey)  throws StorageImportExportException;
}