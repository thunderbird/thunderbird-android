package com.fsck.k9.preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
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
            exportPreferences(context, os, includeGlobals, accountUuids);

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
            Set<String> accountUuids) throws StorageImportExportException  {

        try {
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(os, "UTF-8");

            serializer.startDocument(null, Boolean.valueOf(true));

            // Output with indentation
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

            // Root tag
            serializer.startTag(null, "k9settings");
            serializer.attribute(null, "version", "x");

            Log.i(K9.LOG_TAG, "Exporting preferences");
            long keysEvaluated = 0;
            long keysExported = 0;

            Preferences preferences = Preferences.getPreferences(context);
            SharedPreferences storage = preferences.getPreferences();

            if (accountUuids == null) {
                Account[] accounts = preferences.getAccounts();
                accountUuids = new HashSet<String>();
                for (Account account : accounts) {
                    accountUuids.add(account.getUuid());
                }
            }

            Map < String, ? extends Object > prefs = storage.getAll();
            for (Map.Entry < String, ? extends Object > entry : prefs.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().toString();
                //Log.i(K9.LOG_TAG, "Evaluating key " + key);
                keysEvaluated++;
                String[] comps = key.split("\\.");
                if (comps.length > 1) {
                    String keyUuid = comps[0];
                    if (accountUuids.contains(keyUuid) == false) {
                        //Log.i(K9.LOG_TAG, "Skipping key " + key + " which is not for any current account");
                        continue;
                    }
                } else if (!includeGlobals) {
                    // Skip global config entries if the user didn't request them
                        continue;
                }
                serializer.startTag(null, "value");
                serializer.attribute(null, "key", key);
                serializer.text(value);
                serializer.endTag(null, "value");
                //Log.i(K9.LOG_TAG, "For key " + key + ", output is " + output);
                keysExported++;

            }

            Log.i(K9.LOG_TAG, "Exported " + keysExported + " of " + keysEvaluated + " settings.");

            serializer.endTag(null, "k9settings");
            serializer.endDocument();
            serializer.flush();

        } catch (Exception e) {
            throw new StorageImportExportException(e.getLocalizedMessage(), e);
        }
    }
}
