package com.fsck.k9.helper;

import java.util.List;

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
    protected final List<Cursor> mCursors;

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
     * Used to cache the value of {@link #getCount()}
     */
    private int mCount = -1;


    /**
     * Constructor
     *
     * @param cursors
     *         The list of cursors this {@code MultiCursor} should combine.
     */
    public MergeCursor(List<Cursor> cursors) {
        mCursors = cursors;
        mActiveCursorIndex = 0;
        mActiveCursor = cursors.get(0);
    }

    @Override
    public void close() {
        for (Cursor cursor : mCursors) {
            cursor.close();
        }
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        mActiveCursor.copyStringToBuffer(columnIndex, buffer);
    }

    @Override
    public void deactivate() {
        for (Cursor cursor : mCursors) {
            cursor.deactivate();
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
                count += cursor.getCount();
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
        int pos = 0;
        for (int i = 0; i < mActiveCursorIndex; i++) {
            pos += mCursors.get(i).getCount();
        }

        return pos + mActiveCursor.getPosition();
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

    @Override
    public boolean isAfterLast() {
        if (mActiveCursorIndex == mCursors.size() - 1) {
            return mActiveCursor.isAfterLast();
        }

        return false;
    }

    @Override
    public boolean isBeforeFirst() {
        if (mActiveCursorIndex == 0) {
            return mActiveCursor.isBeforeFirst();
        }

        return false;
    }

    @Override
    public boolean isClosed() {
        return mActiveCursor.isClosed();
    }

    @Override
    public boolean isFirst() {
        if (mActiveCursorIndex == 0) {
            return mActiveCursor.isFirst();
        }

        return false;
    }

    @Override
    public boolean isLast() {
        if (mActiveCursorIndex == mCursors.size() - 1) {
            return mActiveCursor.isLast();
        }

        return false;
    }

    @Override
    public boolean isNull(int columnIndex) {
        return mActiveCursor.isNull(columnIndex);
    }

    @Override
    public boolean move(int offset) {
        int ofs = offset;
        int pos = mActiveCursor.getPosition();
        if (offset >= 0) {
            while (pos + ofs > mActiveCursor.getCount() &
                    mActiveCursorIndex < mCursors.size() - 1) {

                // Adjust the "move offset"
                ofs -= mActiveCursor.getCount() - pos;

                // Move to the next cursor
                mActiveCursor = mCursors.get(++mActiveCursorIndex);

                // Move the new cursor to the first position
                mActiveCursor.moveToFirst();
                pos = 0;
            }
        } else {
            while (pos + ofs < 0 && mActiveCursorIndex > 0) {
                // Adjust the "move offset"
                ofs += pos;

                // Move to the next cursor
                mActiveCursor = mCursors.get(--mActiveCursorIndex);

                // Move the new cursor to the first position
                mActiveCursor.moveToLast();
                pos = mActiveCursor.getPosition();
            }
        }

        return mActiveCursor.move(ofs);
    }

    @Override
    public boolean moveToFirst() {
        mActiveCursorIndex = 0;
        mActiveCursor = mCursors.get(mActiveCursorIndex);
        return mActiveCursor.moveToFirst();
    }

    @Override
    public boolean moveToLast() {
        mActiveCursorIndex = mCursors.size() - 1;
        mActiveCursor = mCursors.get(mActiveCursorIndex);
        return mActiveCursor.moveToLast();
    }

    @Override
    public boolean moveToNext() {
        return move(1);
    }

    @Override
    public boolean moveToPosition(int position) {
        // Start at the beginning
        mActiveCursorIndex = 0;
        mActiveCursor = mCursors.get(mActiveCursorIndex);

        int pos = position;
        while (pos > mActiveCursor.getCount() - 1 &&
                mActiveCursorIndex < mCursors.size() - 1) {

            // Adjust the position
            pos -= mActiveCursor.getCount();

            // Move to the next cursor
            mActiveCursor = mCursors.get(++mActiveCursorIndex);
        }

        return mActiveCursor.moveToPosition(pos);
    }

    @Override
    public boolean moveToPrevious() {
        return move(-1);
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
