package com.fsck.k9.preferences;

import java.io.OutputStream;

import android.content.Context;

public interface IStorageExporter
{
    public boolean needsKey();
    public void exportPreferences(Context context, String uuid, OutputStream os, String encryptionKey) throws StorageImportExportException;
}