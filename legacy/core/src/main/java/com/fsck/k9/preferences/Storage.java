package com.fsck.k9.preferences;


import java.util.Collections;
import java.util.Map;

import timber.log.Timber;

public class Storage {
    private final Map<String, String> values;

    public Storage(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }

    public Map<String, String> getAll() {
        return values;
    }

    public boolean getBoolean(String key, boolean defValue) {
        String val = values.get(key);
        if (val == null) {
            return defValue;
        }
        return Boolean.parseBoolean(val);
    }

    public int getInt(String key, int defValue) {
        String val = values.get(key);
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
        String val = values.get(key);
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
        String val = values.get(key);
        if (val == null) {
            return defValue;
        }
        return val;
    }
}
