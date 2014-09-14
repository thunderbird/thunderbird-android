package com.fsck.k9.helper;

import java.util.Comparator;

import android.database.Cursor;


public class MergeCursorWithUniqueId extends MergeCursor {
    private static final int SHIFT = 48;
    private static final long MAX_ID = (1L << SHIFT) - 1;
    private static final long MAX_CURSORS = 1L << (63 - SHIFT);

    private int mColumnCount = -1;
    private int mIdColumnIndex = -1;


    public MergeCursorWithUniqueId(Cursor[] cursors, Comparator<Cursor> comparator) {
        super(cursors, comparator);

        if (cursors.length > MAX_CURSORS) {
            throw new IllegalArgumentException("This class only supports up to " +
                    MAX_CURSORS + " cursors");
        }
    }

    @Override
    public int getColumnCount() {
        if (mColumnCount == -1) {
            mColumnCount = super.getColumnCount();
        }

        return mColumnCount + 1;
    }

    @Override
    public int getColumnIndex(String columnName) {
        if ("_id".equals(columnName)) {
            return getUniqueIdColumnIndex();
        }

        return super.getColumnIndexOrThrow(columnName);
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        if ("_id".equals(columnName)) {
            return getUniqueIdColumnIndex();
        }

        return super.getColumnIndexOrThrow(columnName);
    }

    @Override
    public long getLong(int columnIndex) {
        if (columnIndex == getUniqueIdColumnIndex()) {
            long id = getPerCursorId();
            if (id > MAX_ID) {
                throw new RuntimeException("Sorry, " + this.getClass().getName() +
                        " can only handle '_id' values up to " + SHIFT + " bits.");
            }

            return (((long) mActiveCursorIndex) << SHIFT) + id;
        }

        return super.getLong(columnIndex);
    }

    protected int getUniqueIdColumnIndex() {
        if (mColumnCount == -1) {
            mColumnCount = super.getColumnCount();
        }

        return mColumnCount;
    }

    protected long getPerCursorId() {
        if (mIdColumnIndex == -1) {
            mIdColumnIndex = super.getColumnIndexOrThrow("_id");
        }

        return super.getLong(mIdColumnIndex);
    }
}
