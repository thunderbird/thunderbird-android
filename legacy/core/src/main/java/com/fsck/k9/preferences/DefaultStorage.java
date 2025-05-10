package com.fsck.k9.preferences;


import java.util.Collections;
import java.util.Map;

import androidx.annotation.NonNull;
import net.thunderbird.core.preferences.Storage;
import timber.log.Timber;

public class DefaultStorage implements Storage {
    private final Map<String, String> values;

    public DefaultStorage(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean contains(@NonNull String key) {
        return values.containsKey(key);
    }

    @NonNull
    @Override
    public Map<String, String> getAll() {
        return values;
    }

    @Override
    public boolean getBoolean(@NonNull String key, boolean defValue) {
        String val = values.get(key);
        if (val == null) {
            return defValue;
        }
        return Boolean.parseBoolean(val);
    }

    @Override
    public int getInt(@NonNull String key, int defValue) {
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

    @Override
    public long getLong(@NonNull String key, long defValue) {
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

    @NonNull
    @Override
    public String getString(String key, String defValue) {
        String val = values.get(key);
        if (val == null) {
            return defValue;
        }
        return val;
    }
}
