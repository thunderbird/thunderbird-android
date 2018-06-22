package com.fsck.k9.cache;

import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.provider.EmailProvider.MessageColumns;
import com.fsck.k9.provider.EmailProvider.ThreadColumns;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;

/**
 * A {@link CursorWrapper} that utilizes {@link EmailProviderCache}.
 */
public class EmailProviderCacheCursor extends CursorWrapper {
    private EmailProviderCache mCache;
    private List<Integer> mHiddenRows = new ArrayList<Integer>();
    private int mMessageIdColumn;
    private int mFolderIdColumn;
    private int mThreadRootColumn;

    /**
     * The cursor's current position.
     *
     * Note: This is only used when {@link #mHiddenRows} isn't empty.
     */
    private int mPosition;


    public EmailProviderCacheCursor(String accountUuid, Cursor cursor, Context context) {
        super(cursor);

        mCache = EmailProviderCache.getCache(accountUuid, context);

        mMessageIdColumn = cursor.getColumnIndex(MessageColumns.ID);
        mFolderIdColumn = cursor.getColumnIndex(MessageColumns.FOLDER_ID);
        mThreadRootColumn = cursor.getColumnIndex(ThreadColumns.ROOT);

        if (mMessageIdColumn == -1 || mFolderIdColumn == -1 || mThreadRootColumn == -1) {
            throw new IllegalArgumentException("The supplied cursor needs to contain the " +
                    "following columns: " + MessageColumns.ID + ", " + MessageColumns.FOLDER_ID +
                    ", " + ThreadColumns.ROOT);
        }

        while (cursor.moveToNext()) {
            long messageId = cursor.getLong(mMessageIdColumn);
            long folderId = cursor.getLong(mFolderIdColumn);
            if (mCache.isMessageHidden(messageId, folderId)) {
                mHiddenRows.add(cursor.getPosition());
            }
        }

        // Reset the cursor position
        cursor.moveToFirst();
        cursor.moveToPrevious();
    }

    @Override
    public int getInt(int columnIndex) {
        long messageId = getLong(mMessageIdColumn);
        long threadRootId = getLong(mThreadRootColumn);

        String columnName = getColumnName(columnIndex);
        String value = mCache.getValueForMessage(messageId, columnName);

        if (value != null) {
            return Integer.parseInt(value);
        }

        value = mCache.getValueForThread(threadRootId, columnName);
        if (value != null) {
            return Integer.parseInt(value);
        }

        return super.getInt(columnIndex);
    }

    @Override
    public int getCount() {
        return super.getCount() - mHiddenRows.size();
    }

    @Override
    public boolean moveToFirst() {
        return moveToPosition(0);
    }

    @Override
    public boolean moveToLast() {
        return moveToPosition(getCount());
    }

    @Override
    public boolean moveToNext() {
        return moveToPosition(getPosition() + 1);
    }

    @Override
    public boolean moveToPrevious() {
        return moveToPosition(getPosition() - 1);
    }

    @Override
    public boolean move(int offset) {
        return moveToPosition(getPosition() + offset);
    }

    @Override
    public boolean moveToPosition(int position) {
        if (mHiddenRows.isEmpty()) {
            return super.moveToPosition(position);
        }

        mPosition = position;
        int newPosition = position;
        for (int hiddenRow : mHiddenRows) {
            if (hiddenRow > newPosition) {
                break;
            }
            newPosition++;
        }

        return super.moveToPosition(newPosition);
    }

    @Override
    public int getPosition() {
        if (mHiddenRows.isEmpty()) {
            return super.getPosition();
        }

        return mPosition;
    }

    @Override
    public boolean isLast() {
        if (mHiddenRows.isEmpty()) {
            return super.isLast();
        }

        return (mPosition == getCount() - 1);
    }
}
