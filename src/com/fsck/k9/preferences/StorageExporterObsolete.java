package com.fsck.k9.preferences;

import java.io.OutputStream;
import java.util.Set;

import com.fsck.k9.R;

import android.content.Context;

public class StorageExporterObsolete implements IStorageExporter
{

    @Override
    public void exportPreferences(Context context, boolean includeGlobals,
            Set<String> accountUuids, OutputStream os, String encryptionKey)
            throws StorageImportExportException
    {
        throw new StorageImportExportException(context.getString(R.string.settings_format_obsolete));
    }

    @Override
    public boolean needsKey()
    {
        return false;
    }

}
