package com.fsck.k9.preferences;


import java.net.URI;
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

import com.fsck.k9.helper.UrlEncodingHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.filter.Base64;
import timber.log.Timber;

public class Storage {
    private static ConcurrentMap<Context, Storage> storages =
        new ConcurrentHashMap<Context, Storage>();

    private volatile ConcurrentMap<String, String> storage = new ConcurrentHashMap<String, String>();

    private int DB_VERSION = 2;
    private String DB_NAME = "preferences_storage";

    private ThreadLocal<ConcurrentMap<String, String>> workingStorage
    = new ThreadLocal<ConcurrentMap<String, String>>();
    private ThreadLocal<SQLiteDatabase> workingDB =
        new ThreadLocal<SQLiteDatabase>();
    private ThreadLocal<List<String>> workingChangedKeys = new ThreadLocal<List<String>>();


    private Context context = null;

    private SQLiteDatabase openDB() {
        SQLiteDatabase mDb = context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);

        if (mDb.getVersion() == 1) {
            Timber.i("Updating preferences to urlencoded username/password");

            String accountUuids = readValue(mDb, "accountUuids");
            if (accountUuids != null && accountUuids.length() != 0) {
                String[] uuids = accountUuids.split(",");
                for (String uuid : uuids) {
                    try {
                        String storeUriStr = Base64.decode(readValue(mDb, uuid + ".storeUri"));
                        String transportUriStr = Base64.decode(readValue(mDb, uuid + ".transportUri"));

                        URI uri = new URI(transportUriStr);
                        String newUserInfo = null;
                        if (transportUriStr != null) {
                            String[] userInfoParts = uri.getUserInfo().split(":");

                            String usernameEnc = UrlEncodingHelper.encodeUtf8(userInfoParts[0]);
                            String passwordEnc = "";
                            String authType = "";
                            if (userInfoParts.length > 1) {
                                passwordEnc = ":" + UrlEncodingHelper.encodeUtf8(userInfoParts[1]);
                            }
                            if (userInfoParts.length > 2) {
                                authType = ":" + userInfoParts[2];
                            }

                            newUserInfo = usernameEnc + passwordEnc + authType;
                        }

                        if (newUserInfo != null) {
                            URI newUri = new URI(uri.getScheme(), newUserInfo, uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
                            String newTransportUriStr = Base64.encode(newUri.toString());
                            writeValue(mDb, uuid + ".transportUri", newTransportUriStr);
                        }

                        uri = new URI(storeUriStr);
                        newUserInfo = null;
                        if (storeUriStr.startsWith("imap")) {
                            String[] userInfoParts = uri.getUserInfo().split(":");
                            if (userInfoParts.length == 2) {
                                String usernameEnc = UrlEncodingHelper.encodeUtf8(userInfoParts[0]);
                                String passwordEnc = UrlEncodingHelper.encodeUtf8(userInfoParts[1]);

                                newUserInfo = usernameEnc + ":" + passwordEnc;
                            } else {
                                String authType = userInfoParts[0];
                                String usernameEnc = UrlEncodingHelper.encodeUtf8(userInfoParts[1]);
                                String passwordEnc = UrlEncodingHelper.encodeUtf8(userInfoParts[2]);

                                newUserInfo = authType + ":" + usernameEnc + ":" + passwordEnc;
                            }
                        } else if (storeUriStr.startsWith("pop3")) {
                            String[] userInfoParts = uri.getUserInfo().split(":", 2);
                            String usernameEnc = UrlEncodingHelper.encodeUtf8(userInfoParts[0]);

                            String passwordEnc = "";
                            if (userInfoParts.length > 1) {
                                passwordEnc = ":" + UrlEncodingHelper.encodeUtf8(userInfoParts[1]);
                            }

                            newUserInfo = usernameEnc + passwordEnc;
                        } else if (storeUriStr.startsWith("webdav")) {
                            String[] userInfoParts = uri.getUserInfo().split(":", 2);
                            String usernameEnc = UrlEncodingHelper.encodeUtf8(userInfoParts[0]);

                            String passwordEnc = "";
                            if (userInfoParts.length > 1) {
                                passwordEnc = ":" + UrlEncodingHelper.encodeUtf8(userInfoParts[1]);
                            }

                            newUserInfo = usernameEnc + passwordEnc;
                        }

                        if (newUserInfo != null) {
                            URI newUri = new URI(uri.getScheme(), newUserInfo, uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
                            String newStoreUriStr = Base64.encode(newUri.toString());
                            writeValue(mDb, uuid + ".storeUri", newStoreUriStr);
                        }
                    } catch (Exception e) {
                        Timber.e(e, "ooops");
                    }
                }
            }

            mDb.setVersion(DB_VERSION);
        }

        if (mDb.getVersion() != DB_VERSION) {
            Timber.i("Creating Storage database");
            mDb.execSQL("DROP TABLE IF EXISTS preferences_storage");
            mDb.execSQL("CREATE TABLE preferences_storage " +
                        "(primkey TEXT PRIMARY KEY ON CONFLICT REPLACE, value TEXT)");
            mDb.setVersion(DB_VERSION);
        }
        return mDb;
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
        ConcurrentMap<String, String> newStorage = new ConcurrentHashMap<String, String>();
        newStorage.putAll(storage);
        workingStorage.set(newStorage);

        SQLiteDatabase mDb = openDB();
        workingDB.set(mDb);

        List<String> changedKeys = new ArrayList<String>();
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
}
