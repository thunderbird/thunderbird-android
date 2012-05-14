package com.fsck.k9.preferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.preferences.StorageImporter.ImportElement;

public abstract class BaseStorageImporter implements IStorageImporter {
    /**
     *  We translate UUIDs in the import file into new UUIDs in the local instance for the following reasons:
     *  1) Accidentally importing the same file twice cannot damage settings in an existing account.
     *     (Say, an account that was imported two months ago and has since had significant settings changes.)
     *  2) Importing a single file multiple times allows for creating multiple accounts from the same template.
     *  3) Exporting an account and importing back into the same instance is a poor-man's account copy (until a real
     *     copy function is created, if ever)
     *     
     *  Reason number 1 is sufficient.  Reasons 2 & 3 are incidental but useful.
     */
    Map<String, String> uuidMapping = new HashMap<String, String>();
    List<Integer> accountNumbers = null;
    String accountUuids = null;

    int settingsImported = 0;
    int numAccounts = 0;
    
    public int importPreferences(Preferences preferences, SharedPreferences.Editor editor, ImportElement dataset, String encryptionKey) throws StorageImportExportException {
        try {
            accountNumbers = Account.getExistingAccountNumbers(preferences);

            accountUuids = preferences.getPreferences().getString("accountUuids", null);

            Log.i(K9.LOG_TAG, "Existing accountNumbers = " + accountNumbers);
            
            importPreferences(editor, dataset, encryptionKey);

            editor.putString("accountUuids", accountUuids);
            Log.i(K9.LOG_TAG, "Imported " + settingsImported + " settings and " + numAccounts + " accounts");
            return numAccounts;
        } catch (Exception e) {
            throw new StorageImportExportException("Unable to import settings", e);
        }
    }
    
    public abstract void importPreferences(Editor editor, ImportElement dataset, String encryptionKey) throws StorageImportExportException ;

    public void incorporate(SharedPreferences.Editor editor, String key, String value) {
        if ("defaultAccountUuid".equals(key))
        {
            Log.i(K9.LOG_TAG, "Skipping import of \"defaultAccountUuid\"");
            return;
        }
        String[] keyParts = key.split("\\.");
        if (keyParts.length > 1) {
            String oldUuid = keyParts[0];
            String newUuid = uuidMapping.get(oldUuid);
            if (newUuid == null) {
                newUuid = UUID.randomUUID().toString();
                uuidMapping.put(oldUuid, newUuid);

                Log.i(K9.LOG_TAG, "Mapping oldUuid " + oldUuid + " to newUuid " + newUuid);
            }
            keyParts[0] = newUuid;
            if ("accountNumber".equals(keyParts[1])) {
                int accountNumber = Account.findNewAccountNumber(accountNumbers);
                Log.i(K9.LOG_TAG, "Assigning new account number " + accountNumber);
                accountNumbers.add(accountNumber);
                value = Integer.toString(accountNumber);
                accountUuids += (accountUuids.length() != 0 ? "," : "") + newUuid;
                numAccounts++;
            }
            StringBuilder builder = new StringBuilder();
            for (String part : keyParts) {
                if (builder.length() > 0) {
                    builder.append(".");
                }
                builder.append(part);
            }
            key = builder.toString();
        }
        //Log.i(K9.LOG_TAG, "Setting " + key + " = " + value);
        settingsImported++;
        editor.putString(key, value);
    }

    @Override
    public boolean needsKey() {
        return true;
    }
}
