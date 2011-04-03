package com.fsck.k9.preferences;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;

public abstract class BaseStorageExporter {
    public void exportPreferences(Context context, boolean includeGlobals, Set<String> accountUuids, OutputStream os, String encryptionKey) throws StorageImportExportException {
        try {
            initialize(encryptionKey);
            OutputStreamWriter sw = new OutputStreamWriter(os);
            PrintWriter pf = new PrintWriter(sw);
            long keysEvaluated = 0;
            long keysExported = 0;
            
            Preferences preferences = Preferences.getPreferences(context);
            SharedPreferences storage = preferences.getPreferences();

            if (accountUuids == null) {
                accountUuids = Collections.emptySet();
            }
            Log.i(K9.LOG_TAG, "Exporting preferences for " + accountUuids.size() + " accounts and "
                    + (includeGlobals ? "" : "not ") + " globals");

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
                output(pf, key, value);
                
                keysExported++;

            }
            finish(pf);
            pf.flush();

            Log.i(K9.LOG_TAG, "Exported " + keysExported + " of " + keysEvaluated + " settings.");
        } catch (Exception e) {
            throw new StorageImportExportException("Unable to encrypt settings", e);
        }
    }
    
    public abstract void initialize(String encryptionKey) throws StorageImportExportException;
    
    public abstract void finish(PrintWriter pf) throws StorageImportExportException;
    
    public abstract void output(PrintWriter pf, String key, String value) throws StorageImportExportException;
    
}
