package com.fsck.k9.preferences.migrations;


import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.fsck.k9.helper.Utility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import timber.log.Timber;


public class DefaultStorageMigrationsHelper implements StorageMigrationsHelper {
    @NotNull
    @Override
    public Map<String, String> readAllValues(@NotNull SQLiteDatabase db) {
        HashMap<String, String> loadedValues = new HashMap<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT primkey, value FROM preferences_storage", null);
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

    @Override
    public String readValue(@NotNull SQLiteDatabase db, @NotNull String key) {
        Cursor cursor = null;
        String value = null;
        try {
            cursor = db.query(
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

    @Override
    public void writeValue(@NotNull SQLiteDatabase db, @NotNull String key, String value) {
        if (value == null) {
            db.delete("preferences_storage", "primkey = ?", new String[] { key });
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put("primkey", key);
        cv.put("value", value);

        long result = db.update("preferences_storage", cv, "primkey = ?", new String[] { key });

        if (result == -1) {
            Timber.e("Error writing key '%s', value = '%s'", key, value);
        }
    }

    @Override
    public void insertValue(@NotNull SQLiteDatabase db, @NotNull String key, @Nullable String value) {
        if (value == null) {
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put("primkey", key);
        cv.put("value", value);

        long result = db.insert("preferences_storage", null, cv);

        if (result == -1) {
            Timber.e("Error writing key '%s', value = '%s'", key, value);
        }
    }
}
