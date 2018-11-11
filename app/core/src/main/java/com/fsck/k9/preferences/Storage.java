package com.fsck.k9.preferences;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.SystemClock;

import com.fsck.k9.helper.Utility;
import com.fsck.k9.preferences.migrations.StorageMigrations;
import com.fsck.k9.preferences.migrations.StorageMigrationsHelper;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

public class Storage {
    private static ConcurrentMap<Context, Storage> storages = new ConcurrentHashMap<>();

    private volatile ConcurrentMap<String, String> storage = new ConcurrentHashMap<>();

    private static final int DB_VERSION = 2;
    private static final String DB_NAME = "preferences_storage";

    private ThreadLocal<ConcurrentMap<String, String>> workingStorage = new ThreadLocal<>();
    private ThreadLocal<SQLiteDatabase> workingDB = new ThreadLocal<>();
    private ThreadLocal<List<String>> workingChangedKeys = new ThreadLocal<>();


    private Context context = null;

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

    public static Storage getStorage(Context context) {
        Storage tmpStorage = storages.get(context);
        if (tmpStorage != null) {
            Timber.d("Returning already existing Storage");
            return tmpStorage;
        } else {
            Timber.d("Creating provisional storage");
            tmpStorage = new Storage(context);
            Storage oldStorage = storages.putIfAbsent(context, tmpStorage);
            if (oldStorage != null) {
                Timber.d("Another thread beat us to creating the Storage, returning that one");
                return oldStorage;
            } else {
                Timber.d("Returning the Storage we created");
                return tmpStorage;
            }
        }
    }

    private void loadValues() {
        long startTime = SystemClock.elapsedRealtime();
        Timber.i("Loading preferences from DB into Storage");
        Cursor cursor = null;
        SQLiteDatabase mDb = null;
        try {
            mDb = openDB();

            cursor = mDb.rawQuery("SELECT primkey, value FROM preferences_storage", null);
            while (cursor.moveToNext()) {
                String key = cursor.getString(0);
                String value = cursor.getString(1);
                Timber.d("Loading key '%s', value = '%s'", key, value);
                storage.put(key, value);
            }
        } finally {
            Utility.closeQuietly(cursor);
            if (mDb != null) {
                mDb.close();
            }
            long endTime = SystemClock.elapsedRealtime();
            Timber.i("Preferences load took %d ms", endTime - startTime);
        }
    }

    private Storage(Context context) {
        this.context = context;
        loadValues();
    }

    private void keyChange(String key) {
        List<String> changedKeys = workingChangedKeys.get();
        if (!changedKeys.contains(key)) {
            changedKeys.add(key);
        }
    }

    void put(Map<String, String> insertables) {
        String sql = "INSERT INTO preferences_storage (primkey, value) VALUES (?, ?)";
        SQLiteStatement stmt = workingDB.get().compileStatement(sql);

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
        workingStorage.get().put(key, value);

        keyChange(key);
    }

    void remove(String key) {
        workingDB.get().delete("preferences_storage", "primkey = ?", new String[] { key });
        workingStorage.get().remove(key);

        keyChange(key);
    }

    void doInTransaction(Runnable dbWork) {
        ConcurrentMap<String, String> newStorage = new ConcurrentHashMap<>(storage);
        workingStorage.set(newStorage);

        SQLiteDatabase mDb = openDB();
        workingDB.set(mDb);

        List<String> changedKeys = new ArrayList<>();
        workingChangedKeys.set(changedKeys);

        mDb.beginTransaction();
        try {
            dbWork.run();
            mDb.setTransactionSuccessful();
            storage = newStorage;
        } finally {
            workingDB.remove();
            workingStorage.remove();
            workingChangedKeys.remove();
            mDb.endTransaction();
            mDb.close();
        }
    }

    public boolean isEmpty() {
        return storage.isEmpty();
    }

    public boolean contains(String key) {
        // TODO this used to be ConcurrentHashMap#contains which is
        // actually containsValue. But looking at the usage of this method,
        // it's clear that containsKey is what's intended. Investigate if this
        // was a bug previously. Looks like it was only used once, when upgrading
        return storage.containsKey(key);
    }

    public StorageEditor edit() {
        return new StorageEditor(this);
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
        ContentValues cv = new ContentValues();
        cv.put("primkey", key);
        cv.put("value", value);

        long result = mDb.insert("preferences_storage", "primkey", cv);

        if (result == -1) {
            Timber.e("Error writing key '%s', value = '%s'", key, value);
        }
    }

    private StorageMigrationsHelper migrationsHelper = new StorageMigrationsHelper() {
        @Override
        public void writeValue(@NotNull SQLiteDatabase db, @NotNull String key, String value) {
            Storage.this.writeValue(db, key, value);
        }

        @Override
        public String readValue(@NotNull SQLiteDatabase db, @NotNull String key) {
            return Storage.this.readValue(db, key);
        }
    };
}
