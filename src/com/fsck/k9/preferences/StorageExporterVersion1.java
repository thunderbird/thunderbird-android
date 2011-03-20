package com.fsck.k9.preferences;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;

public class StorageExporterVersion1 implements IStorageExporter {
    public void exportPreferences(Context context, String uuid, OutputStream os, String encryptionKey) throws StorageImportExportException {
        try {
            Log.i(K9.LOG_TAG, "Exporting preferences for account " + uuid + " to OutputStream");
            K9Krypto krypto = new K9Krypto(encryptionKey, K9Krypto.MODE.ENCRYPT);
            OutputStreamWriter sw = new OutputStreamWriter(os);
            PrintWriter pf = new PrintWriter(sw);
            long keysEvaluated = 0;
            long keysExported = 0;
            pf.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");

            pf.print("<k9settings version=\"1\"");
            pf.println(">");

            Preferences preferences = Preferences.getPreferences(context);
            SharedPreferences storage = preferences.getPreferences();

            Account[] accounts = preferences.getAccounts();
            Set<String> accountUuids = new HashSet<String>();
            for (Account account : accounts) {
                accountUuids.add(account.getUuid());
            }

            Map < String, ? extends Object > prefs = storage.getAll();
            for (Map.Entry < String, ? extends Object > entry : prefs.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().toString();
                //Log.i(K9.LOG_TAG, "Evaluating key " + key);
                keysEvaluated++;
                if (uuid != null) {
                    String[] comps = key.split("\\.");
                    String keyUuid = comps[0];
                    //Log.i(K9.LOG_TAG, "Got key uuid " + keyUuid);
                    if (uuid.equals(keyUuid) == false) {
                        //Log.i(K9.LOG_TAG, "Skipping key " + key + " which is for another account or global");
                        continue;
                    }
                } else {
                    String[] comps = key.split("\\.");
                    if (comps.length > 1) {
                        String keyUuid = comps[0];
                        if (accountUuids.contains(keyUuid) == false) {
                            //Log.i(K9.LOG_TAG, "Skipping key " + key + " which is not for any current account");
                            continue;
                        }
                    }
                }
                String keyEnc = krypto.encrypt(key);
                String valueEnc = krypto.encrypt(value);
                String output = keyEnc + ":" + valueEnc;
                //Log.i(K9.LOG_TAG, "For key " + key + ", output is " + output);
                pf.println(output);
                keysExported++;

            }

            pf.println("</k9settings>");
            pf.flush();

            Log.i(K9.LOG_TAG, "Exported " + keysExported + " settings of " + keysEvaluated
                  + " total for preferences for account " + uuid);
        } catch (Exception e) {
            throw new StorageImportExportException("Unable to encrypt settings", e);
        }
    }

    @Override
    public boolean needsKey()
    {
        return true;
    }
}
