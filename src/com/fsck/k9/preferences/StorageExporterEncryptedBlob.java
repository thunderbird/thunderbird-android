package com.fsck.k9.preferences;

import java.io.PrintWriter;

import org.apache.commons.codec.binary.Base64;

public class StorageExporterEncryptedBlob extends BaseStorageExporter implements IStorageExporter {
    K9Krypto krypto = null;
    StringBuilder builder = null;
    Base64 mBase64 = null;
   
    @Override
    public boolean needsKey() {
        return true;
    }

    @Override
    public void output(PrintWriter pf, String key, String value) throws StorageImportExportException {
        try {
            byte[] encodedKey = mBase64.encode(key.getBytes());
            byte[] encodedValue = mBase64.encode(value.getBytes());
            builder.append(new String(encodedKey));
            builder.append(":");
            builder.append(new String(encodedValue));
            builder.append("\n");
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
        builder = new StringBuilder();
        mBase64 = new Base64();
    }

    @Override
    public void finish(PrintWriter pf) throws StorageImportExportException {
        try {
            String encryptedSettings = krypto.encrypt(builder.toString());
            pf.print(encryptedSettings);
        } 
        catch (Exception e) {
            throw new StorageImportExportException("Unable to encrypt settings", e);
        } 
    }
}
