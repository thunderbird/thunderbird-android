package com.fsck.k9.preferences;

import java.util.HashMap;
import java.util.Map;


public class StorageVersioning
{
    public enum STORAGE_VERSION {
        VERSION1(StorageImporterVersion1.class, StorageExporterVersion1.class, true, STORAGE_VERSION_1);
        
        private Class<? extends IStorageImporter> importerClass;
        private Class<? extends IStorageExporter> exporterClass;
        private boolean needsKey;
        private String versionString;
        
        
        private STORAGE_VERSION(Class<? extends IStorageImporter> imclass, Class<? extends IStorageExporter> exclass, boolean nk, String vs) {
            importerClass = imclass;
            exporterClass = exclass;
            needsKey = nk;
            versionString = vs;
            
        }
        public Class<? extends IStorageImporter> getImporterClass() {
            return importerClass;
        }
        public IStorageImporter createImporter() throws InstantiationException, IllegalAccessException {
            IStorageImporter storageImporter = importerClass.newInstance();
            return storageImporter;
        }
        public Class<? extends IStorageExporter> getExporterClass() {
            return exporterClass;
        }
        public IStorageExporter createExporter() throws InstantiationException, IllegalAccessException {
            IStorageExporter storageExporter = exporterClass.newInstance();
            return storageExporter;
        }
        public boolean needsKey() {
            return needsKey;
        }
        public String getVersionString() {
            return versionString;
        }
    }
    
    // Never, ever re-use these numbers!
    private static final String STORAGE_VERSION_1 = "1";
    
    public static Map<String, STORAGE_VERSION> versionMap = new HashMap<String, STORAGE_VERSION>();
    static {
        versionMap.put(STORAGE_VERSION.VERSION1.getVersionString(), STORAGE_VERSION.VERSION1);
    }
    
    public static IStorageImporter createImporter(String version) throws InstantiationException, IllegalAccessException
    {
        STORAGE_VERSION storageVersion = versionMap.get(version);
        if (storageVersion == null)
        {
            return null;
        }
        return storageVersion.createImporter();
    }
    
    public static IStorageExporter createExporter(String version) throws InstantiationException, IllegalAccessException
    {
        STORAGE_VERSION storageVersion = versionMap.get(version);
        if (storageVersion == null)
        {
            return null;
        }
        return storageVersion.createExporter();
    }
    
    public Boolean needsKey(String version)
    {
        STORAGE_VERSION storageVersion = versionMap.get(version);
        if (storageVersion == null)
        {
            return null;
        }
        return storageVersion.needsKey();
    }
    
}
