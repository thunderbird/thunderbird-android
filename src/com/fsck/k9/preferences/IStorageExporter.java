package com.fsck.k9.preferences;

import java.io.OutputStream;
import java.util.Set;

import android.content.Context;

public interface IStorageExporter {
    public boolean needsKey();
    // exportPreferences must be sure to flush all data to the OutputStream before returning
    public void exportPreferences(Context context, boolean includeGlobals, Set<String> accountUuids, OutputStream os, String encryptionKey) throws StorageImportExportException;
}
