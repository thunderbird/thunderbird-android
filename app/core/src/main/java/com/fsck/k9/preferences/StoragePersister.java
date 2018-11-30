package com.fsck.k9.preferences;


import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.SystemClock;
import android.support.annotation.CheckResult;

import com.fsck.k9.helper.Utility;
import com.fsck.k9.preferences.migrations.StorageMigrations;
import com.fsck.k9.preferences.migrations.StorageMigrationsHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import timber.log.Timber;


public class StoragePersister {
    private static final int DB_VERSION = 4;
    private static final String DB_NAME = "preferences_storage";

    private final Context context;

    public StoragePersister(Context context) {
        this.context = context;
    }

    private SQLiteDatabase openDB() {
        SQLiteDatabase db = context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);

        db.beginTransaction();
        try {
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
            workingDb.setTransactionSuccessful();

            operationCallback.onPersistTransactionSuccess(workingStorage);
        } finally {
            workingDb.endTransaction();
            workingDb.close();
        }
    }

    static class StoragePersistOperations {
        private SQLiteDatabase workingDB;
        private Map<String, String> workingStorage;

        private StoragePersistOperations(Map<String, String> workingStorage, SQLiteDatabase workingDb) {
            this.workingDB = workingDb;
            this.workingStorage = workingStorage;
        }

        void put(Map<String, String> insertables) {
            String sql = "INSERT INTO preferences_storage (primkey, value) VALUES (?, ?)";
            SQLiteStatement stmt = workingDB.compileStatement(sql);

            for (Map.Entry<String, String> entry : insertables.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                stmt.bindString(1, key);
                stmt.bindString(2, value);
                stmt.execute();
                stmt.clearBindings();
                liveUpdate(key, value);
            }
            stmt.close();
        }

        private void liveUpdate(String key, String value) {
            workingStorage.put(key, value);
        }

        void remove(String key) {
            workingDB.delete("preferences_storage", "primkey = ?", new String[] { key });
            workingStorage.remove(key);
        }
    }

    interface StoragePersistOperationCallback {
        void beforePersistTransaction(Map<String, String> workingStorage);
        void persist(StoragePersistOperations ops);
        void onPersistTransactionSuccess(Map<String, String> workingStorage);
    }

    @CheckResult
    public Map<String, String> loadValues() {
        long startTime = SystemClock.elapsedRealtime();
        Timber.i("Loading preferences from DB into Storage");
        HashMap<String, String> loadedValues = new HashMap<>();
        Cursor cursor = null;
        SQLiteDatabase mDb = null;
        try {
            mDb = openDB();

            cursor = mDb.rawQuery("SELECT primkey, value FROM preferences_storage", null);
            while (cursor.moveToNext()) {
                String key = cursor.getString(0);
                String value = cursor.getString(1);
                Timber.d("Loading key '%s', value = '%s'", key, value);
                loadedValues.put(key, value);
            }
        } finally {
            Utility.closeQuietly(cursor);
            if (mDb != null) {
                mDb.close();
            }
            long endTime = SystemClock.elapsedRealtime();
            Timber.i("Preferences load took %d ms", endTime - startTime);
        }

        return loadedValues;
    }

    private String readValue(SQLiteDatabase mDb, String key) {
        Cursor cursor = null;
        String value = null;
        try {
            cursor = mDb.query(
                    "preferences_storage",
                    new String[] {"value"},
                    "primkey = ?",
                    new String[] {key},
                    null,
                    null,
                    null);

            if (cursor.moveToNext()) {
                value = cursor.getString(0);
                Timber.d("Loading key '%s', value = '%s'", key, value);
            }
        } finally {
            Utility.closeQuietly(cursor);
        }

        return value;
    }

    private void writeValue(SQLiteDatabase mDb, String key, String value) {
        if (value == null) {
            mDb.delete("preferences_storage", "primkey = ?", new String[] { key });
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put("primkey", key);
        cv.put("value", value);

        long result = mDb.update("preferences_storage", cv, "primkey = ?", new String[] { key });

        if (result == -1) {
            Timber.e("Error writing key '%s', value = '%s'", key, value);
        }
    }

    private void insertValue(SQLiteDatabase mDb, String key, String value) {
        if (value == null) {
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put("primkey", key);
        cv.put("value", value);

        long result = mDb.insert("preferences_storage", null, cv);

        if (result == -1) {
            Timber.e("Error writing key '%s', value = '%s'", key, value);
        }
    }

    private StorageMigrationsHelper migrationsHelper = new StorageMigrationsHelper() {
        @Override
        public void writeValue(@NotNull SQLiteDatabase db, @NotNull String key, String value) {
            StoragePersister.this.writeValue(db, key, value);
        }

        @Override
        public String readValue(@NotNull SQLiteDatabase db, @NotNull String key) {
            return StoragePersister.this.readValue(db, key);
        }

        @Override
        public void insertValue(@NotNull SQLiteDatabase db, @NotNull String key, @Nullable String value) {
            StoragePersister.this.insertValue(db, key, value);
        }
    };
}
