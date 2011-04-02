package com.fsck.k9.preferences;

import java.io.PrintWriter;

public class StorageExporterEncryptedKeyValue extends BaseExporter implements IStorageExporter {
    K9Krypto krypto = null;
   
    @Override
    public boolean needsKey() {
        return true;
    }

    @Override
    public void output(PrintWriter pf, String key, String value) throws StorageImportExportException {
        try {
            String keyEnc = krypto.encrypt(key);
            String valueEnc = krypto.encrypt(value);
            String output = keyEnc + ":" + valueEnc;
        //  Log.i(K9.LOG_TAG, "For key " + key + ", output is " + output);
            pf.println(output);
        }
        catch (Exception e) {
            throw new StorageImportExportException("Unable to encrypt and write key/value", e);
        }
    }

    @Override
    public void initialize(String encryptionKey) throws StorageImportExportException {
        try {
            krypto = new K9Krypto(encryptionKey, K9Krypto.MODE.ENCRYPT);
        }
        catch (Exception e) {
            throw new StorageImportExportException("Unable to initialize encryption", e);
        }
    }

    @Override
    public void finish(PrintWriter pf) throws StorageImportExportException {
        // Nothing to do
    }
}
