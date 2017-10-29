package com.fsck.k9.cache;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;

import com.fsck.k9.provider.EmailProvider.MessageColumns;
import com.fsck.k9.provider.EmailProvider.ThreadColumns;

/**
 * A {@link CursorWrapper} that utilizes {@link EmailProviderCache}.
 */
public class EmailProviderCacheCursor extends CursorWrapper {
    private EmailProviderCache cache;
    private List<Integer> hiddenRows = new ArrayList<Integer>();
    private int messageIdColumn;
    private int threadRootColumn;

    /**
     * The cursor's current position.
     *
     * Note: This is only used when {@link #hiddenRows} isn't empty.
     */
    private int position;


    public EmailProviderCacheCursor(String accountUuid, Cursor cursor, Context context) {
        super(cursor);

        cache = EmailProviderCache.getCache(accountUuid, context);

        messageIdColumn = cursor.getColumnIndex(MessageColumns.ID);
        int folderIdColumn = cursor.getColumnIndex(MessageColumns.FOLDER_ID);
        threadRootColumn = cursor.getColumnIndex(ThreadColumns.ROOT);

        if (messageIdColumn == -1 || folderIdColumn == -1 || threadRootColumn == -1) {
            throw new IllegalArgumentException("The supplied cursor needs to contain the " +
                    "following columns: " + MessageColumns.ID + ", " + MessageColumns.FOLDER_ID +
                    ", " + ThreadColumns.ROOT);
        }

        while (cursor.moveToNext()) {
            long messageId = cursor.getLong(messageIdColumn);
            long folderId = cursor.getLong(folderIdColumn);
            if (cache.isMessageHidden(messageId, folderId)) {
                hiddenRows.add(cursor.getPosition());
            }
        }

        // Reset the cursor position
        cursor.moveToFirst();
        cursor.moveToPrevious();
    }

    @Override
    public int getInt(int columnIndex) {
        long messageId = getLong(messageIdColumn);
        long threadRootId = getLong(threadRootColumn);

        String columnName = getColumnName(columnIndex);
        String value = cache.getValueForMessage(messageId, columnName);

        if (value != null) {
            return Integer.parseInt(value);
        }

        value = cache.getValueForThread(threadRootId, columnName);
        if (value != null) {
            return Integer.parseInt(value);
        }

        return super.getInt(columnIndex);
    }

    @Override
    public int getCount() {
        return super.getCount() - hiddenRows.size();
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
        if (hiddenRows.isEmpty()) {
            return super.moveToPosition(position);
        }

        this.position = position;
        int newPosition = position;
        for (int hiddenRow : hiddenRows) {
            if (hiddenRow > newPosition) {
                break;
            }
            newPosition++;
        }

        return super.moveToPosition(newPosition);
    }

    @Override
    public int getPosition() {
        if (hiddenRows.isEmpty()) {
            return super.getPosition();
        }

        return position;
    }

    @Override
    public boolean isLast() {
        if (hiddenRows.isEmpty()) {
            return super.isLast();
        }

        return (position == getCount() - 1);
    }
}
