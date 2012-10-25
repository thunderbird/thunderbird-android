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

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
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

    protected int mPosition;

    /**
     * Used to cache the value of {@link #getCount()}
     */
    private int mCount = -1;


    /**
     * Constructor
     *
     * @param cursors
     *         The list of cursors this {@code MultiCursor} should combine.
     */
    public MergeCursor(Cursor[] cursors) {
        mCursors = cursors.clone();

        for (int i = 0, len = mCursors.length; i < len; i++) {
            if (mCursors[i] != null) {
                mActiveCursorIndex = i;
                mActiveCursor = mCursors[mActiveCursorIndex];
            }
        }
        mPosition = -1;
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

    @TargetApi(11)
    @Override
    public int getType(int columnIndex) {
        return mActiveCursor.getType(columnIndex);
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        return mActiveCursor.getWantsAllOnMoveCalls();
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
        return moveToPosition(mPosition + 1);
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

        /* Find the right cursor */
        mActiveCursor = null;
        mActiveCursorIndex = -1;
        mPosition = -1;
        int cursorStartPos = 0;

        for (int i = 0, len = mCursors.length; i < len; i++) {
            if (mCursors[i] == null) {
                continue;
            }

            if (position < (cursorStartPos + mCursors[i].getCount())) {
                mActiveCursorIndex = i;
                mActiveCursor = mCursors[mActiveCursorIndex];
                break;
            }

            cursorStartPos += mCursors[i].getCount();
        }

        /* Move it to the right position */
        if (mActiveCursor != null) {
            boolean success = mActiveCursor.moveToPosition(position - cursorStartPos);
            mPosition = (success) ? position : -1;

            return success;
        }

        return false;
    }

    @Override
    public boolean moveToPrevious() {
        return moveToPosition(mPosition - 1);
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
}
