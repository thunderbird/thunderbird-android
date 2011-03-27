package com.fsck.k9.preferences;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;

public interface IStorageExporter {
    public boolean needsKey();
    public void exportPreferences(Context context, Set<String> accountUuids, OutputStream os, String encryptionKey) throws StorageImportExportException;
}
