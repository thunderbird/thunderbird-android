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
    protected final Cursor[] mCursors;

    /**
     * The currently active cursor.
     */
    protected Cursor mActiveCursor;

    /**
     * The index of the currently active cursor in {@link #mCursors}.
     *
     * @see #mActiveCursor
     */
    protected int mActiveCursorIndex;

    /**
     * The cursor's current position.
     */
    protected int mPosition;

    /**
     * Used to cache the value of {@link #getCount()}.
     */
    private int mCount = -1;

    /**
     * The comparator that is used to decide how the individual cursors are merged.
     */
    private final Comparator<Cursor> mComparator;


    /**
     * Constructor
     *
     * @param cursors
     *         The list of cursors this {@code MultiCursor} should combine.
     * @param comparator
     *         A comparator that is used to decide in what order the individual cursors are merged.
     */
    public MergeCursor(Cursor[] cursors, Comparator<Cursor> comparator) {
        mCursors = cursors.clone();
        mComparator = comparator;

        resetCursors();
    }

    private void resetCursors() {
        mActiveCursorIndex = -1;
        mActiveCursor = null;
        mPosition = -1;

        for (int i = 0, len = mCursors.length; i < len; i++) {
            Cursor cursor = mCursors[i];
            if (cursor != null) {
                cursor.moveToPosition(-1);

                if (mActiveCursor == null) {
                    mActiveCursorIndex = i;
                    mActiveCursor = mCursors[mActiveCursorIndex];
                }
            }
        }
    }

    @Override
    public void close() {
        for (Cursor cursor : mCursors) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        mActiveCursor.copyStringToBuffer(columnIndex, buffer);
    }

    @Override
    public void deactivate() {
        for (Cursor cursor : mCursors) {
            if (cursor != null) {
                cursor.deactivate();
            }
        }
    }

    @Override
    public byte[] getBlob(int columnIndex) {
        return mActiveCursor.getBlob(columnIndex);
    }

    @Override
    public int getColumnCount() {
        return mActiveCursor.getColumnCount();
    }

    @Override
    public int getColumnIndex(String columnName) {
        return mActiveCursor.getColumnIndex(columnName);
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        return mActiveCursor.getColumnIndexOrThrow(columnName);
    }

    @Override
    public String getColumnName(int columnIndex) {
        return mActiveCursor.getColumnName(columnIndex);
    }

    @Override
    public String[] getColumnNames() {
        return mActiveCursor.getColumnNames();
    }

    @Override
    public int getCount() {
        // CursorLoaders seem to call getCount() a lot. So we're caching the aggregated count.
        if (mCount == -1) {
            int count = 0;
            for (Cursor cursor : mCursors) {
                if (cursor != null) {
                    count += cursor.getCount();
                }
            }

            mCount = count;
        }

        return mCount;
    }

    @Override
    public double getDouble(int columnIndex) {
        return mActiveCursor.getDouble(columnIndex);
    }

    @Override
    public float getFloat(int columnIndex) {
        return mActiveCursor.getFloat(columnIndex);
    }

    @Override
    public int getInt(int columnIndex) {
        return mActiveCursor.getInt(columnIndex);
    }

    @Override
    public long getLong(int columnIndex) {
        return mActiveCursor.getLong(columnIndex);
    }

    @Override
    public int getPosition() {
        return mPosition;
    }

    @Override
    public short getShort(int columnIndex) {
        return mActiveCursor.getShort(columnIndex);
    }

    @Override
    public String getString(int columnIndex) {
        return mActiveCursor.getString(columnIndex);
    }

    @Override
    public int getType(int columnIndex) {
        return mActiveCursor.getType(columnIndex);
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        return mActiveCursor.getWantsAllOnMoveCalls();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void setExtras(Bundle extras) {
        mActiveCursor.setExtras(extras);
    }

    @Override
    public boolean isAfterLast() {
        int count = getCount();
        if (count == 0) {
            return true;
        }

        return (mPosition == count);
    }

    @Override
    public boolean isBeforeFirst() {
        if (getCount() == 0) {
            return true;
        }

        return (mPosition == -1);
    }

    @Override
    public boolean isClosed() {
        return mActiveCursor.isClosed();
    }

    @Override
    public boolean isFirst() {
        if (getCount() == 0) {
            return false;
        }

        return (mPosition == 0);
    }

    @Override
    public boolean isLast() {
        int count = getCount();
        if (count == 0) {
            return false;
        }

        return (mPosition == (count - 1));
    }

    @Override
    public boolean isNull(int columnIndex) {
        return mActiveCursor.isNull(columnIndex);
    }

    @Override
    public boolean move(int offset) {
        return moveToPosition(mPosition + offset);
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
        if (mPosition == count) {
            return false;
        }

        if (mPosition == count - 1) {
            mActiveCursor.moveToNext();
            mPosition++;
            return false;
        }

        int smallest = -1;
        for (int i = 0, len = mCursors.length; i < len; i++) {
            if (mCursors[i] == null || mCursors[i].getCount() == 0 || mCursors[i].isLast()) {
                continue;
            }

            if (smallest == -1) {
                smallest = i;
                mCursors[smallest].moveToNext();
                continue;
            }

            Cursor left = mCursors[smallest];
            Cursor right = mCursors[i];

            right.moveToNext();

            int result = mComparator.compare(left, right);
            if (result > 0) {
                smallest = i;
                left.moveToPrevious();
            } else {
                right.moveToPrevious();
            }
        }

        mPosition++;
        if (smallest != -1) {
            mActiveCursorIndex = smallest;
            mActiveCursor = mCursors[mActiveCursorIndex];
        }

        return true;
    }

    @Override
    public boolean moveToPosition(int position) {
        // Make sure position isn't past the end of the cursor
        final int count = getCount();
        if (position >= count) {
            mPosition = count;
            return false;
        }

        // Make sure position isn't before the beginning of the cursor
        if (position < 0) {
            mPosition = -1;
            return false;
        }

        // Check for no-op moves, and skip the rest of the work for them
        if (position == mPosition) {
            return true;
        }

        if (position > mPosition) {
            for (int i = 0, end = position - mPosition; i < end; i++) {
                if (!moveToNext()) {
                    return false;
                }
            }
        } else {
            for (int i = 0, end = mPosition - position; i < end; i++) {
                if (!moveToPrevious()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean moveToPrevious() {
        if (mPosition < 0) {
            return false;
        }

        mActiveCursor.moveToPrevious();

        if (mPosition == 0) {
            mPosition = -1;
            return false;
        }

        int greatest = -1;
        for (int i = 0, len = mCursors.length; i < len; i++) {
            if (mCursors[i] == null || mCursors[i].isBeforeFirst()) {
                continue;
            }

            if (greatest == -1) {
                greatest = i;
                continue;
            }

            Cursor left = mCursors[greatest];
            Cursor right = mCursors[i];

            int result = mComparator.compare(left, right);
            if (result <= 0) {
                greatest = i;
            }
        }

        mPosition--;
        if (greatest != -1) {
            mActiveCursorIndex = greatest;
            mActiveCursor = mCursors[mActiveCursorIndex];
        }

        return true;
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        for (Cursor cursor : mCursors) {
            cursor.registerContentObserver(observer);
        }
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        for (Cursor cursor : mCursors) {
            cursor.registerDataSetObserver(observer);
        }
    }

    @Deprecated
    @Override
    public boolean requery() {
        boolean success = true;
        for (Cursor cursor : mCursors) {
            success &= cursor.requery();
        }

        return success;
    }

    @Override
    public void setNotificationUri(ContentResolver cr, Uri uri) {
        for (Cursor cursor : mCursors) {
            cursor.setNotificationUri(cr, uri);
        }
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        for (Cursor cursor : mCursors) {
            cursor.unregisterContentObserver(observer);
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        for (Cursor cursor : mCursors) {
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
