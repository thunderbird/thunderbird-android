package com.fsck.k9.preferences;


import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.preferences.migrations.DefaultStorageMigrationsHelper;
import com.fsck.k9.preferences.migrations.StorageMigrations;
import com.fsck.k9.preferences.migrations.StorageMigrationsHelper;
import timber.log.Timber;


public class K9StoragePersister implements StoragePersister {
    private static final int DB_VERSION = 21;
    private static final String DB_NAME = "preferences_storage";

    private final Context context;
    private final StorageMigrationsHelper migrationsHelper = new DefaultStorageMigrationsHelper();

    public K9StoragePersister(Context context) {
        this.context = context;
    }

    private SQLiteDatabase openDB() {
        SQLiteDatabase db = context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);

        db.beginTransaction();
        try {
            if (db.getVersion() > DB_VERSION) {
                throw new AssertionError("Database downgrades are not supported. " +
                        "Please fix the database '" + DB_NAME + "' manually or clear app data.");
            }

            if (db.getVersion() < 1) {
                createStorageDatabase(db);
            } else {
                StorageMigrations.upgradeDatabase(db, migrationsHelper);
            }

            db.setVersion(DB_VERSION);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (db.getVersion() != DB_VERSION) {
            throw new RuntimeException("Storage database upgrade failed!");
        }

        return db;
    }

    private void createStorageDatabase(SQLiteDatabase db) {
        Timber.i("Creating Storage database");

        db.execSQL("DROP TABLE IF EXISTS preferences_storage");
        db.execSQL("CREATE TABLE preferences_storage " +
                "(primkey TEXT PRIMARY KEY ON CONFLICT REPLACE, value TEXT)");
        db.setVersion(DB_VERSION);
    }

    void doInTransaction(StoragePersistOperationCallback operationCallback) {
        HashMap<String, String> workingStorage = new HashMap<>();
        SQLiteDatabase workingDb = openDB();

        try {
            operationCallback.beforePersistTransaction(workingStorage);

            StoragePersistOperations storagePersistOperations = new StoragePersistOperations(workingStorage, workingDb);
            workingDb.beginTransaction();
            operationCallback.persist(storagePersistOperations);
            storagePersistOperations.close();
            workingDb.setTransactionSuccessful();

            operationCallback.onPersistTransactionSuccess(workingStorage);
        } finally {
            workingDb.endTransaction();
            workingDb.close();
        }
    }

    @NonNull
    @Override
    public StorageEditor createStorageEditor(@NonNull StorageUpdater storageUpdater) {
        return new K9StorageEditor(storageUpdater, this);
    }

    static class StoragePersistOperations {
        private Map<String, String> workingStorage;
        private final SQLiteStatement deleteStatement;
        private final SQLiteStatement insertStatement;

        private StoragePersistOperations(Map<String, String> workingStorage, SQLiteDatabase database) {
            this.workingStorage = workingStorage;

            insertStatement = database.compileStatement(
                    "INSERT INTO preferences_storage (primkey, value) VALUES (?, ?)");
            deleteStatement = database.compileStatement(
                    "DELETE FROM preferences_storage WHERE primkey = ?");
        }

        void put(String key, String value) {
            insertStatement.bindString(1, key);
            insertStatement.bindString(2, value);
            insertStatement.execute();
            insertStatement.clearBindings();

            workingStorage.put(key, value);
        }

        void remove(String key) {
            deleteStatement.bindString(1, key);
            deleteStatement.executeUpdateDelete();
            deleteStatement.clearBindings();

            workingStorage.remove(key);
        }

        private void close() {
            insertStatement.close();
            deleteStatement.close();
        }
    }

    interface StoragePersistOperationCallback {
        void beforePersistTransaction(Map<String, String> workingStorage);
        void persist(StoragePersistOperations ops);
        void onPersistTransactionSuccess(Map<String, String> workingStorage);
    }

    @NonNull
    @Override
    public Storage loadValues() {
        long startTime = SystemClock.elapsedRealtime();
        Timber.i("Loading preferences from DB into Storage");

        try (SQLiteDatabase database = openDB()) {
            return new Storage(readAllValues(database));
        } finally {
            long endTime = SystemClock.elapsedRealtime();
            Timber.i("Preferences load took %d ms", endTime - startTime);
        }
    }

    private Map<String, String> readAllValues(SQLiteDatabase database) {
        HashMap<String, String> loadedValues = new HashMap<>();
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("SELECT primkey, value FROM preferences_storage", null);
            while (cursor.moveToNext()) {
                String key = cursor.getString(0);
                String value = cursor.getString(1);
                Timber.d("Loading key '%s', value = '%s'", key, value);
                loadedValues.put(key, value);
            }
        } finally {
            Utility.closeQuietly(cursor);
        }

        return loadedValues;
    }
}
