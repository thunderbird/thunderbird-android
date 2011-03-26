package com.fsck.k9.preferences;

import java.util.HashMap;
import java.util.Map;


public class StorageFormat {
    // Never, ever re-use these numbers!
    public static final String ENCRYPTED_XML_FILE = "1";

    public static Map<String, StorageFormat> versionMap = new HashMap<String, StorageFormat>();
    static {
        versionMap.put(ENCRYPTED_XML_FILE, new StorageFormat(StorageImporterVersion1.class, StorageExporterVersion1.class, true));
    }

    public static IStorageImporter createImporter(String version) throws InstantiationException, IllegalAccessException {
        StorageFormat storageVersion = versionMap.get(version);
        if (storageVersion == null) {
            return null;
        }
        return storageVersion.importerClass.newInstance();
    }

    public static IStorageExporter createExporter(String version) throws InstantiationException, IllegalAccessException {
        StorageFormat storageVersion = versionMap.get(version);
        if (storageVersion == null) {
            return null;
        }
        return storageVersion.exporterClass.newInstance();
    }

    public static Boolean needsKey(String version) {
        StorageFormat storageVersion = versionMap.get(version);
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
