package com.fsck.k9.preferences;

import java.io.BufferedReader;
import java.io.StringReader;

import org.apache.commons.codec.binary.Base64;

import android.content.SharedPreferences;

import com.fsck.k9.preferences.StorageImporter.ImportElement;

public class StorageImporterEncryptedBlob extends BaseStorageImporter implements IStorageImporter {
    public void importPreferences(SharedPreferences.Editor editor, ImportElement dataset, String encryptionKey) throws StorageImportExportException {
        try {

            K9Krypto krypto = new K9Krypto(encryptionKey, K9Krypto.MODE.DECRYPT);
            String data = dataset.data.toString();
            String decryptedData = krypto.decrypt(data);
            StringReader sr = new StringReader(decryptedData);
            BufferedReader br = new BufferedReader(sr);
            String line = null;
            Base64 base64 = new Base64();
           
            do {
                line = br.readLine();
                if (line != null) {
                    String[] comps = line.split(":");
                    if (comps.length > 1) {
                        String keyEnc = comps[0];
                        String valueEnc = comps[1];
                        String key = new String(base64.decode(keyEnc.getBytes()));
                        String value = new String(base64.decode(valueEnc.getBytes()));
                        
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
