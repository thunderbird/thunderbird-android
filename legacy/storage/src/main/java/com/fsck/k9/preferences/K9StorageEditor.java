package com.fsck.k9.preferences;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import com.fsck.k9.preferences.K9StoragePersister.StoragePersistOperationCallback;
import com.fsck.k9.preferences.K9StoragePersister.StoragePersistOperations;
import net.thunderbird.core.logging.Logger;
import net.thunderbird.core.preference.storage.InMemoryStorage;
import net.thunderbird.core.preference.storage.Storage;
import net.thunderbird.core.preference.storage.StorageEditor;
import net.thunderbird.core.preference.storage.StorageUpdater;

public class K9StorageEditor implements StorageEditor {
    private StorageUpdater storageUpdater;
    private K9StoragePersister storagePersister;

    private Logger logger;

    private Map<String, String> changes = new HashMap<>();
    private List<String> removals = new ArrayList<>();


    public K9StorageEditor(
        StorageUpdater storageUpdater,
        K9StoragePersister storagePersister,
        Logger logger
    ) {
        this.storageUpdater = storageUpdater;
        this.storagePersister = storagePersister;
        this.logger = logger;
    }

    @Override
    public boolean commit() {
        try {
            storageUpdater.updateStorage(this::commitChanges);
            return true;
        } catch (Exception e) {
            logger.error(null, e, () -> "Failed to save preferences");
            return false;
        }
    }

    private Storage commitChanges(Storage storage) {
        long startTime = SystemClock.elapsedRealtime();
        logger.info(null, null, () -> "Committing preference changes");

        Map<String, String> newValues = new HashMap<>();
        Map<String, String> oldValues = storage.getAll();
        StoragePersistOperationCallback committer = new StoragePersistOperationCallback() {
            @Override
            public void beforePersistTransaction(Map<String, String> workingStorage) {
                workingStorage.putAll(oldValues);
            }

            @Override
            public void persist(StoragePersistOperations ops) {
                for (String removeKey : removals) {
                    ops.remove(removeKey);
                }
                for (Entry<String, String> entry : changes.entrySet()) {
                    String key = entry.getKey();
                    String newValue = entry.getValue();
                    String oldValue = oldValues.get(key);
                    if (removals.contains(key) || !newValue.equals(oldValue)) {
                        ops.put(key, newValue);
                    }
                }
            }

            @Override
            public void onPersistTransactionSuccess(Map<String, String> workingStorage) {
                newValues.putAll(workingStorage);
            }
        };
        storagePersister.doInTransaction(committer);
        long endTime = SystemClock.elapsedRealtime();
        logger.info(null, null, () -> String.format("Preferences commit took %d ms", endTime - startTime));

        return new InMemoryStorage(
            newValues,
            logger
        );

    }

    @NonNull
    @Override
    public StorageEditor putBoolean(String key,
        boolean value) {
        changes.put(key, "" + value);
        return this;
    }

    @NonNull
    @Override
    public StorageEditor putInt(String key, int value) {
        changes.put(key, "" + value);
        return this;
    }

    @NonNull
    @Override
    public StorageEditor putLong(String key, long value) {
        changes.put(key, "" + value);
        return this;
    }

    @NonNull
    @Override
    public StorageEditor putString(String key, String value) {
        if (value == null) {
            remove(key);
        } else {
            changes.put(key, value);
        }
        return this;
    }

    @NonNull
    @Override
    public StorageEditor remove(String key) {
        removals.add(key);
        return this;
    }
}
