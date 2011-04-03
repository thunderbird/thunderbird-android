package com.fsck.k9.preferences;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.R;


public class StorageFormat {
    // Never, ever re-use these numbers!
    public static final String ENCRYPTED_KEY_VALUE = "1";  // This format is obsolete but retained for importing old files
    public static final String ENCRYPTED_BLOB = "2";
    public static final String ENCRYPTED_URL_ENCODED = "3";

    public static Map<String, StorageFormat> storageFormatMap = new HashMap<String, StorageFormat>();
    public static List<String> presentableVersions = new LinkedList<String>();
    static {
        storageFormatMap.put(ENCRYPTED_KEY_VALUE, new StorageFormat(StorageImporterEncryptedKeyValue.class, StorageExporterObsolete.class, null));
        storageFormatMap.put(ENCRYPTED_BLOB, new StorageFormat(StorageImporterEncryptedBlob.class, StorageExporterEncryptedBlob.class, R.string.settings_format_encrypted));
        storageFormatMap.put(ENCRYPTED_URL_ENCODED, new StorageFormat(StorageImporterUrlEncoded.class, StorageExporterUrlEncoded.class, null /*R.string.settings_format_unencrypted */));  // Uncomment resource id when made release-ready
        
        for (Map.Entry<String, StorageFormat> entry : storageFormatMap.entrySet()) {
            if (entry.getValue().presentableResource != null) {
                presentableVersions.add(entry.getKey());
            }
        }
    }
    
    public static List<String> getPresentableFormats()
    {
        return presentableVersions;
    }
    
    public static Integer getPresentableResource(String storageFormat) {
        StorageFormat storageVersion = storageFormatMap.get(storageFormat);
        if (storageVersion == null) {
            return null;
        }
        return storageVersion.presentableResource;
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

    public static Boolean exportNeedsKey(String storageFormat) {
        StorageFormat storageVersion = storageFormatMap.get(storageFormat);
        if (storageVersion == null) {
            return null;
        }
        try {
            Method method = storageVersion.exporterClass.getMethod("needsKey");
            if (method == null) {
                return null;
            }
            Object obj = method.invoke(null);
            if (obj != null && obj instanceof Boolean) {
                return (Boolean)obj;
            }
        }
        catch (Exception e) {
            Log.e(K9.LOG_TAG, "Unable to reflectively invoke needsKey on exporter for format " + storageFormat);
            return null;
        }
        return null;
    }


    private final Class <? extends IStorageImporter > importerClass;
    private final Class <? extends IStorageExporter > exporterClass;
    private final Integer presentableResource;

    private StorageFormat(Class <? extends IStorageImporter > imclass, Class <? extends IStorageExporter > exclass, Integer present) {
        importerClass = imclass;
        exporterClass = exclass;
        presentableResource = present;
    }
}
