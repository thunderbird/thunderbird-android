package com.fsck.k9.preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;

import android.app.Activity;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.ExportListener;

public class StorageExporter {
    private static void exportPreferences(Activity activity, String storageFormat, boolean includeGlobals, Set<String> accountUuids, String fileName, OutputStream os, String encryptionKey, final ExportListener listener)  {
        try {
            IStorageExporter storageExporter = StorageFormat.createExporter(storageFormat);
            if (storageExporter == null) {
                throw new StorageImportExportException(activity.getString(R.string.settings_unknown_version, storageFormat), null);
            }
            if (storageExporter.needsKey() && encryptionKey == null) {
                throw new StorageImportExportException("Encryption key required, but none supplied");
            } else {
                finishExport(activity, storageFormat, storageExporter, includeGlobals, accountUuids, fileName, os, encryptionKey, listener);
            }
        }

        catch (Exception e) {
            if (listener != null) {
                listener.failure(e.getLocalizedMessage(), e);
            }
        }
    }

    public static void exportPreferences(Activity activity, String storageFormat, boolean includeGlobals, Set<String> accountUuids, String fileName, String encryptionKey, final ExportListener listener) throws StorageImportExportException {
        exportPreferences(activity, storageFormat, includeGlobals, accountUuids, fileName, null, encryptionKey, listener);
    }

    private static void finishExport(Activity activity, String storageFormat, IStorageExporter storageExporter, boolean includeGlobals, Set<String> accountUuids, String fileName, OutputStream os, String encryptionKey, ExportListener listener) throws StorageImportExportException {
        boolean needToClose = false;
        if (listener != null) {
            listener.started();
        }
        try {
            // This needs to be after the password prompt.  If the user cancels the password, we do not want
            // to create the file needlessly
            if (os == null && fileName != null) {
                needToClose = true;
                File outFile = new File(fileName);
                os = new FileOutputStream(outFile);
            }
            if (os != null) {

                OutputStreamWriter sw = new OutputStreamWriter(os);
                PrintWriter pf = new PrintWriter(sw);
                pf.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");

                pf.println("<k9settings version=\"" + storageFormat + "\">");
                pf.flush();

                storageExporter.exportPreferences(activity, includeGlobals, accountUuids, os, encryptionKey);

                pf.println("</k9settings>");
                pf.flush();
                if (listener != null) {
                    if (fileName != null) {
                        listener.success(fileName);
                    } else {
                        listener.success();
                    }
                }
            } else {
                throw new StorageImportExportException("Internal error; no fileName or OutputStream", null);
            }
        } catch (Exception e) {
            throw new StorageImportExportException(e.getLocalizedMessage(), e);
        } finally {
            if (needToClose && os != null) {
                try {
                    os.close();
                } catch (Exception e) {
                    Log.w(K9.LOG_TAG, "Unable to close OutputStream", e);
                }
            }
        }

    }

}
