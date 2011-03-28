package com.fsck.k9.preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;

import android.content.Context;
import android.os.Environment;

import com.fsck.k9.helper.Utility;


public class StorageExporter {
    private static final String EXPORT_FILENAME = "settings.k9s";


    public static String exportToFile(Context context, boolean includeGlobals,
            Set<String> accountUuids, String encryptionKey)
            throws StorageImportExportException {

        OutputStream os = null;
        try
        {
            File dir = new File(Environment.getExternalStorageDirectory() + File.separator
                                + context.getPackageName());
            dir.mkdirs();
            File file = Utility.createUniqueFile(dir, EXPORT_FILENAME);
            String fileName = file.getAbsolutePath();
            os = new FileOutputStream(fileName);
            exportPreferences(context, os, includeGlobals, accountUuids, encryptionKey);

            // If all went well, we return the name of the file just written.
            return fileName;
        } catch (Exception e) {
            throw new StorageImportExportException();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ioe) {}
            }
        }
    }

    public static void exportPreferences(Context context, OutputStream os, boolean includeGlobals,
            Set<String> accountUuids, String encryptionKey) throws StorageImportExportException  {

        IStorageExporter storageExporter = new StorageExporterEncryptedXml();
        if (storageExporter.needsKey() && encryptionKey == null) {
            throw new StorageImportExportException("Encryption key required, but none supplied");
        }

        try {
            OutputStreamWriter sw = new OutputStreamWriter(os);
            PrintWriter pf = new PrintWriter(sw);
            pf.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");

            pf.println("<k9settings version=\"" + 1 + "\">");
            pf.flush();

            storageExporter.exportPreferences(context, includeGlobals, accountUuids, os, encryptionKey);

            pf.println("</k9settings>");
            pf.flush();
        } catch (Exception e) {
            throw new StorageImportExportException(e.getLocalizedMessage(), e);
        }
    }
}
