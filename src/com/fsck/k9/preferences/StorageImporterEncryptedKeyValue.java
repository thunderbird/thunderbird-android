package com.fsck.k9.preferences;

import java.io.BufferedReader;
import java.io.StringReader;

import android.content.SharedPreferences;

import com.fsck.k9.preferences.StorageImporter.ImportElement;

public class StorageImporterEncryptedKeyValue extends BaseStorageImporter implements IStorageImporter {
    public void importPreferences(SharedPreferences.Editor editor, ImportElement dataset, String encryptionKey) throws StorageImportExportException {
        try {

            String data = dataset.data.toString();
           
            StringReader sr = new StringReader(data);
            BufferedReader br = new BufferedReader(sr);
            String line = null;
            K9Krypto krypto = new K9Krypto(encryptionKey, K9Krypto.MODE.DECRYPT);
            do {
                line = br.readLine();
                if (line != null) {
                    //Log.i(K9.LOG_TAG, "Got line " + line);
                    String[] comps = line.split(":");
                    if (comps.length > 1) {
                        String keyEnc = comps[0];
                        String valueEnc = comps[1];
                        String key = krypto.decrypt(keyEnc);
                        String value = krypto.decrypt(valueEnc);
                        
                        incorporate(editor, key, value);
                    }
                }
            } while (line != null);
        } catch (Exception e) {
            throw new StorageImportExportException("Unable to decrypt settings", e);
        }
    }

    @Override
    public boolean needsKey() {
        return true;
    }
}
