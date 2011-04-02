package com.fsck.k9.preferences;

import java.util.HashMap;
import java.util.Map;


public class StorageFormat {
    // Never, ever re-use these numbers!
    public static final String ENCRYPTED_KEY_VALUE = "1";
    public static final String ENCRYPTED_BLOB = "2";

    public static Map<String, StorageFormat> storageFormatMap = new HashMap<String, StorageFormat>();
    static {
        storageFormatMap.put(ENCRYPTED_KEY_VALUE, new StorageFormat(StorageImporterEncryptedKeyValue.class, StorageExporterEncryptedKeyValue.class, true));
        storageFormatMap.put(ENCRYPTED_BLOB, new StorageFormat(StorageImporterEncryptedBlob.class, StorageExporterEncryptedBlob.class, true));
    }

    public static IStorageImporter createImporter(String storageFormat) throws InstantiationException, IllegalAccessException {
        StorageFormat storageVersion = storageFormatMap.get(storageFormat);
        if (storageVersion == null) {
            return null;
        }
        return storageVersion.importerClass.newInstance();
    }

    public static IStorageExporter createExporter(String storageFormat) throws InstantiationException, IllegalAccessException {
        StorageFormat storageVersion = storageFormatMap.get(storageFormat);
        if (storageVersion == null) {
            return null;
        }
        return storageVersion.exporterClass.newInstance();
    }

    public static Boolean needsKey(String storageFormat) {
        StorageFormat storageVersion = storageFormatMap.get(storageFormat);
        if (storageVersion == null) {
            return null;
        }
        return storageVersion.needsKey;
    }


    private final Class <? extends IStorageImporter > importerClass;
    private final Class <? extends IStorageExporter > exporterClass;
    private final boolean needsKey;

    private StorageFormat(Class <? extends IStorageImporter > imclass, Class <? extends IStorageExporter > exclass, boolean nk) {
        importerClass = imclass;
        exporterClass = exclass;
        needsKey = nk;
    }
}
