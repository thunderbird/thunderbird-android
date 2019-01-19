package com.fsck.k9.preferences;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class Storage {
    private volatile Map<String, String> storage = Collections.emptyMap();

    public Storage() { }

    public boolean isEmpty() {
        return storage.isEmpty();
    }

    public boolean contains(String key) {
        return storage.containsKey(key);
    }

    public Map<String, String> getAll() {
        return storage;
    }

    public boolean getBoolean(String key, boolean defValue) {
        String val = storage.get(key);
        if (val == null) {
            return defValue;
        }
        return Boolean.parseBoolean(val);
    }

    public int getInt(String key, int defValue) {
        String val = storage.get(key);
        if (val == null) {
            return defValue;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException nfe) {
            Timber.e(nfe, "Could not parse int");
            return defValue;
        }
    }

    public long getLong(String key, long defValue) {
        String val = storage.get(key);
        if (val == null) {
            return defValue;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException nfe) {
            Timber.e(nfe, "Could not parse long");
            return defValue;
        }
    }

    public String getString(String key, String defValue) {
        String val = storage.get(key);
        if (val == null) {
            return defValue;
        }
        return val;
    }

    public void replaceAll(Map<String, String> workingStorage) {
        storage = new HashMap<>(workingStorage);
    }
}
