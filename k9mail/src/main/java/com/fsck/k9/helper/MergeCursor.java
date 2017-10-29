/*
 * Copyright (C) 2012 The K-9 Dog Walkers
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fsck.k9.helper;

import java.util.Comparator;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;


/**
 * This class can be used to combine multiple {@link Cursor}s into one.
 */
public class MergeCursor implements Cursor {
    /**
     * List of the cursors combined in this object.
     */
    private final Cursor[] cursors;

    /**
     * The currently active cursor.
     */
    private Cursor activeCursor;

    /**
     * The index of the currently active cursor in {@link #cursors}.
     *
     * @see #activeCursor
     */
    protected int activeCursorIndex;

    /**
     * The cursor's current position.
     */
    protected int position;

    /**
     * Used to cache the value of {@link #getCount()}.
     */
    private int count = -1;

    /**
     * The comparator that is used to decide how the individual cursors are merged.
     */
    private final Comparator<Cursor> comparator;


    /**
     * Constructor
     *
     * @param cursors
     *         The list of cursors this {@code MultiCursor} should combine.
     * @param comparator
     *         A comparator that is used to decide in what order the individual cursors are merged.
     */
    public MergeCursor(Cursor[] cursors, Comparator<Cursor> comparator) {
        this.cursors = cursors.clone();
        this.comparator = comparator;

        resetCursors();
    }

    private void resetCursors() {
        activeCursorIndex = -1;
        activeCursor = null;
        position = -1;

        for (int i = 0, len = cursors.length; i < len; i++) {
            Cursor cursor = cursors[i];
            if (cursor != null) {
                cursor.moveToPosition(-1);

                if (activeCursor == null) {
                    activeCursorIndex = i;
                    activeCursor = cursors[activeCursorIndex];
                }
            }
        }
    }

    @Override
    public void close() {
        for (Cursor cursor : cursors) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        activeCursor.copyStringToBuffer(columnIndex, buffer);
    }

    @Override
    public void deactivate() {
        for (Cursor cursor : cursors) {
            if (cursor != null) {
                cursor.deactivate();
            }
        }
    }

    @Override
    public byte[] getBlob(int columnIndex) {
        return activeCursor.getBlob(columnIndex);
    }

    @Override
    public int getColumnCount() {
        return activeCursor.getColumnCount();
    }

    @Override
    public int getColumnIndex(String columnName) {
        return activeCursor.getColumnIndex(columnName);
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        return activeCursor.getColumnIndexOrThrow(columnName);
    }

    @Override
    public String getColumnName(int columnIndex) {
        return activeCursor.getColumnName(columnIndex);
    }

    @Override
    public String[] getColumnNames() {
        return activeCursor.getColumnNames();
    }

    @Override
    public int getCount() {
        // CursorLoaders seem to call getCount() a lot. So we're caching the aggregated count.
        if (count == -1) {
            int count = 0;
            for (Cursor cursor : cursors) {
                if (cursor != null) {
                    count += cursor.getCount();
                }
            }

            this.count = count;
        }

        return count;
    }

    @Override
    public double getDouble(int columnIndex) {
        return activeCursor.getDouble(columnIndex);
    }

    @Override
    public float getFloat(int columnIndex) {
        return activeCursor.getFloat(columnIndex);
    }

    @Override
    public int getInt(int columnIndex) {
        return activeCursor.getInt(columnIndex);
    }

    @Override
    public long getLong(int columnIndex) {
        return activeCursor.getLong(columnIndex);
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public short getShort(int columnIndex) {
        return activeCursor.getShort(columnIndex);
    }

    @Override
    public String getString(int columnIndex) {
        return activeCursor.getString(columnIndex);
    }

    @Override
    public int getType(int columnIndex) {
        return activeCursor.getType(columnIndex);
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        return activeCursor.getWantsAllOnMoveCalls();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void setExtras(Bundle extras) {
        activeCursor.setExtras(extras);
    }

    @Override
    public boolean isAfterLast() {
        int count = getCount();
        if (count == 0) {
            return true;
        }

        return (position == count);
    }

    @Override
    public boolean isBeforeFirst() {
        if (getCount() == 0) {
            return true;
        }

        return (position == -1);
    }

    @Override
    public boolean isClosed() {
        return activeCursor.isClosed();
    }

    @Override
    public boolean isFirst() {
        if (getCount() == 0) {
            return false;
        }

        return (position == 0);
    }

    @Override
    public boolean isLast() {
        int count = getCount();
        if (count == 0) {
            return false;
        }

        return (position == (count - 1));
    }

    @Override
    public boolean isNull(int columnIndex) {
        return activeCursor.isNull(columnIndex);
    }

    @Override
    public boolean move(int offset) {
        return moveToPosition(position + offset);
    }

    @Override
    public boolean moveToFirst() {
        return moveToPosition(0);
    }

    @Override
    public boolean moveToLast() {
        return moveToPosition(getCount() - 1);
    }

    @Override
    public boolean moveToNext() {
        int count = getCount();
        if (position == count) {
            return false;
        }

        if (position == count - 1) {
            activeCursor.moveToNext();
            position++;
            return false;
        }

        int smallest = -1;
        for (int i = 0, len = cursors.length; i < len; i++) {
            if (cursors[i] == null || cursors[i].getCount() == 0 || cursors[i].isLast()) {
                continue;
            }

            if (smallest == -1) {
                smallest = i;
                cursors[smallest].moveToNext();
                continue;
            }

            Cursor left = cursors[smallest];
            Cursor right = cursors[i];

            right.moveToNext();

            int result = comparator.compare(left, right);
            if (result > 0) {
                smallest = i;
                left.moveToPrevious();
            } else {
                right.moveToPrevious();
            }
        }

        position++;
        if (smallest != -1) {
            activeCursorIndex = smallest;
            activeCursor = cursors[activeCursorIndex];
        }

        return true;
    }

    @Override
    public boolean moveToPosition(int position) {
        // Make sure position isn't past the end of the cursor
        final int count = getCount();
        if (position >= count) {
            this.position = count;
            return false;
        }

        // Make sure position isn't before the beginning of the cursor
        if (position < 0) {
            this.position = -1;
            return false;
        }

        // Check for no-op moves, and skip the rest of the work for them
        if (position == this.position) {
            return true;
        }

        if (position > this.position) {
            for (int i = 0, end = position - this.position; i < end; i++) {
                if (!moveToNext()) {
                    return false;
                }
            }
        } else {
            for (int i = 0, end = this.position - position; i < end; i++) {
                if (!moveToPrevious()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean moveToPrevious() {
        if (position < 0) {
            return false;
        }

        activeCursor.moveToPrevious();

        if (position == 0) {
            position = -1;
            return false;
        }

        int greatest = -1;
        for (int i = 0, len = cursors.length; i < len; i++) {
            if (cursors[i] == null || cursors[i].isBeforeFirst()) {
                continue;
            }

            if (greatest == -1) {
                greatest = i;
                continue;
            }

            Cursor left = cursors[greatest];
            Cursor right = cursors[i];

            int result = comparator.compare(left, right);
            if (result <= 0) {
                greatest = i;
            }
        }

        position--;
        if (greatest != -1) {
            activeCursorIndex = greatest;
            activeCursor = cursors[activeCursorIndex];
        }

        return true;
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        for (Cursor cursor : cursors) {
            cursor.registerContentObserver(observer);
        }
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        for (Cursor cursor : cursors) {
            cursor.registerDataSetObserver(observer);
        }
    }

    @Deprecated
    @Override
    public boolean requery() {
        boolean success = true;
        for (Cursor cursor : cursors) {
            success &= cursor.requery();
        }

        return success;
    }

    @Override
    public void setNotificationUri(ContentResolver cr, Uri uri) {
        for (Cursor cursor : cursors) {
            cursor.setNotificationUri(cr, uri);
        }
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        for (Cursor cursor : cursors) {
            cursor.unregisterContentObserver(observer);
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        for (Cursor cursor : cursors) {
            cursor.unregisterDataSetObserver(observer);
        }
    }

    @Override
    public Bundle getExtras() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Bundle respond(Bundle extras) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Uri getNotificationUri() {
        return null;
    }
}
