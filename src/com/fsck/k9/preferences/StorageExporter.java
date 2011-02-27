package com.fsck.k9.preferences;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class StorageExporter {
    //public static String VALIDITY = "K-9MailExport";  // Does outputting a fixed string in a known location make the encrypted data easier to break?
    public static void exportPreferences(Context context, String uuid, String fileName, String encryptionKey) throws StorageImportExportException {
        try {
            Base64 base64 = new Base64();
            File outFile = new File(fileName);
            PrintWriter pf = new PrintWriter(outFile);
            long keysEvaluated = 0;
            long keysExported = 0;
            pf.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");

            //  String testval = SimpleCrypto.encrypt(encryptionKey, VALIDITY);

            pf.print("<k9settings version=\"1\"");
            //pf.print(" validity=\"" + testval + "\"");
            pf.println(">");
            Log.i(K9.LOG_TAG, "Exporting preferences for account " + uuid + " to file " + fileName);

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
                            Log.i(K9.LOG_TAG, "Skipping key " + key + " which is not for any current account");
                            continue;
                        }
                    }
                }
                String keyEnc = SimpleCrypto.encrypt(encryptionKey, key, base64);
                String valueEnc = SimpleCrypto.encrypt(encryptionKey, value, base64);
                String output = keyEnc + ":" + valueEnc;
                //Log.i(K9.LOG_TAG, "For key " + key + ", output is " + output);
                pf.println(output);
                keysExported++;

            }

            pf.println("</k9settings>");
            pf.close();

            Log.i(K9.LOG_TAG, "Exported " + keysExported + " settings of " + keysEvaluated
                  + " total for preferences for account " + uuid + " to file " + fileName + " which is size " + outFile.length());
        } catch (IOException ie) {
            throw new StorageImportExportException("Unable to export settings", ie);
        } catch (Exception e) {
            throw new StorageImportExportException("Unable to encrypt settings", e);
        }
    }
}
