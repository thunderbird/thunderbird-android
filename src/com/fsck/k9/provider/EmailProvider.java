package com.fsck.k9.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.cache.EmailProviderCacheCursor;
import com.fsck.k9.helper.StringUtils;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.LockableDatabase;
import com.fsck.k9.mail.store.LockableDatabase.DbCallback;
import com.fsck.k9.mail.store.LockableDatabase.WrappedException;
import com.fsck.k9.mail.store.UnavailableStorageException;
import com.fsck.k9.search.SqlQueryBuilder;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * Content Provider used to display the message list etc.
 *
 * <p>
 * For now this content provider is for internal use only. In the future we may allow third-party
 * apps to access K-9 Mail content using this content provider.
 * </p>
 */
/*
 * TODO:
 * - add support for account list and folder list
 */
public class EmailProvider extends ContentProvider {
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    public static final String AUTHORITY = "com.fsck.k9.provider.email";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);


    /*
     * Constants that are used for the URI matching.
     */
    private static final int MESSAGE_BASE = 0;
    private static final int MESSAGES = MESSAGE_BASE;
    private static final int MESSAGES_THREADED = MESSAGE_BASE + 1;
    private static final int MESSAGES_THREAD = MESSAGE_BASE + 2;

    private static final int STATS_BASE = 100;
    private static final int STATS = STATS_BASE;


    private static final String MESSAGES_TABLE = "messages";

    private static final Map<String, String> THREAD_AGGREGATION_FUNCS = new HashMap<String, String>();
    static {
        THREAD_AGGREGATION_FUNCS.put(MessageColumns.DATE, "MAX");
        THREAD_AGGREGATION_FUNCS.put(MessageColumns.INTERNAL_DATE, "MAX");
        THREAD_AGGREGATION_FUNCS.put(MessageColumns.ATTACHMENT_COUNT, "SUM");
        THREAD_AGGREGATION_FUNCS.put(MessageColumns.READ, "MIN");
        THREAD_AGGREGATION_FUNCS.put(MessageColumns.FLAGGED, "MAX");
        THREAD_AGGREGATION_FUNCS.put(MessageColumns.ANSWERED, "MIN");
        THREAD_AGGREGATION_FUNCS.put(MessageColumns.FORWARDED, "MIN");
    }

    private static final String[] FIXUP_MESSAGES_COLUMNS = {
        MessageColumns.ID
    };

    private static final String[] FIXUP_AGGREGATED_MESSAGES_COLUMNS = {
        MessageColumns.DATE,
        MessageColumns.INTERNAL_DATE,
        MessageColumns.ATTACHMENT_COUNT,
        MessageColumns.READ,
        MessageColumns.FLAGGED,
        MessageColumns.ANSWERED,
        MessageColumns.FORWARDED
    };

    private static final String FOLDERS_TABLE = "folders";

    private static final String[] FOLDERS_COLUMNS = {
        FolderColumns.ID,
        FolderColumns.NAME,
        FolderColumns.LAST_UPDATED,
        FolderColumns.UNREAD_COUNT,
        FolderColumns.VISIBLE_LIMIT,
        FolderColumns.STATUS,
        FolderColumns.PUSH_STATE,
        FolderColumns.LAST_PUSHED,
        FolderColumns.FLAGGED_COUNT,
        FolderColumns.INTEGRATE,
        FolderColumns.TOP_GROUP,
        FolderColumns.POLL_CLASS,
        FolderColumns.PUSH_CLASS,
        FolderColumns.DISPLAY_CLASS
    };

    private static final String THREADS_TABLE = "threads";

    static {
        UriMatcher matcher = sUriMatcher;

        matcher.addURI(AUTHORITY, "account/*/messages", MESSAGES);
        matcher.addURI(AUTHORITY, "account/*/messages/threaded", MESSAGES_THREADED);
        matcher.addURI(AUTHORITY, "account/*/thread/#", MESSAGES_THREAD);

        matcher.addURI(AUTHORITY, "account/*/stats", STATS);
    }

    public interface SpecialColumns {
        public static final String ACCOUNT_UUID = "account_uuid";

        public static final String THREAD_COUNT = "thread_count";

        public static final String FOLDER_NAME = "name";
        public static final String INTEGRATE = "integrate";
    }

    public interface MessageColumns {
        public static final String ID = "id";
        public static final String UID = "uid";
        public static final String INTERNAL_DATE = "internal_date";
        public static final String SUBJECT = "subject";
        public static final String DATE = "date";
        public static final String MESSAGE_ID = "message_id";
        public static final String SENDER_LIST = "sender_list";
        public static final String TO_LIST = "to_list";
        public static final String CC_LIST = "cc_list";
        public static final String BCC_LIST = "bcc_list";
        public static final String REPLY_TO_LIST = "reply_to_list";
        public static final String FLAGS = "flags";
        public static final String ATTACHMENT_COUNT = "attachment_count";
        public static final String FOLDER_ID = "folder_id";
        public static final String PREVIEW = "preview";
        public static final String READ = "read";
        public static final String FLAGGED = "flagged";
        public static final String ANSWERED = "answered";
        public static final String FORWARDED = "forwarded";
    }

    private interface InternalMessageColumns extends MessageColumns {
        public static final String DELETED = "deleted";
        public static final String EMPTY = "empty";
        public static final String TEXT_CONTENT = "text_content";
        public static final String HTML_CONTENT = "html_content";
        public static final String MIME_TYPE = "mime_type";
    }

    public interface FolderColumns {
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String LAST_UPDATED = "last_updated";
        public static final String UNREAD_COUNT = "unread_count";
        public static final String VISIBLE_LIMIT = "visible_limit";
        public static final String STATUS = "status";
        public static final String PUSH_STATE = "push_state";
        public static final String LAST_PUSHED = "last_pushed";
        public static final String FLAGGED_COUNT = "flagged_count";
        public static final String INTEGRATE = "integrate";
        public static final String TOP_GROUP = "top_group";
        public static final String POLL_CLASS = "poll_class";
        public static final String PUSH_CLASS = "push_class";
        public static final String DISPLAY_CLASS = "display_class";
    }

    public interface ThreadColumns {
        public static final String ID = "id";
        public static final String MESSAGE_ID = "message_id";
        public static final String ROOT = "root";
        public static final String PARENT = "parent";
    }

    public interface StatsColumns {
        public static final String UNREAD_COUNT = "unread_count";
        public static final String FLAGGED_COUNT = "flagged_count";
    }

    private static final String[] STATS_DEFAULT_PROJECTION = {
            StatsColumns.UNREAD_COUNT,
            StatsColumns.FLAGGED_COUNT
    };


    private Preferences mPreferences;


    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {

        int match = sUriMatcher.match(uri);
        if (match < 0) {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        ContentResolver contentResolver = getContext().getContentResolver();
        Cursor cursor = null;
        switch (match) {
            case MESSAGES:
            case MESSAGES_THREADED:
            case MESSAGES_THREAD: {
                List<String> segments = uri.getPathSegments();
                String accountUuid = segments.get(1);

                List<String> dbColumnNames = new ArrayList<String>(projection.length);
                Map<String, String> specialColumns = new HashMap<String, String>();
                for (String columnName : projection) {
                    if (SpecialColumns.ACCOUNT_UUID.equals(columnName)) {
                        specialColumns.put(SpecialColumns.ACCOUNT_UUID, accountUuid);
                    } else {
                        dbColumnNames.add(columnName);
                    }
                }

                String[] dbProjection = dbColumnNames.toArray(new String[0]);

                if (match == MESSAGES) {
                    cursor = getMessages(accountUuid, dbProjection, selection, selectionArgs,
                            sortOrder);
                } else if (match == MESSAGES_THREADED) {
                    cursor = getThreadedMessages(accountUuid, dbProjection, selection,
                            selectionArgs, sortOrder);
                } else if (match == MESSAGES_THREAD) {
                    String threadId = segments.get(3);
                    cursor = getThread(accountUuid, dbProjection, threadId, sortOrder);
                } else {
                    throw new RuntimeException("Not implemented");
                }

                Uri notificationUri = Uri.withAppendedPath(CONTENT_URI, "account/" + accountUuid +
                        "/messages");
                cursor.setNotificationUri(contentResolver, notificationUri);

                cursor = new SpecialColumnsCursor(new IdTrickeryCursor(cursor), projection,
                        specialColumns);

                cursor = new EmailProviderCacheCursor(accountUuid, cursor, getContext());
                break;
            }
            case STATS: {
                List<String> segments = uri.getPathSegments();
                String accountUuid = segments.get(1);

                cursor = getAccountStats(accountUuid, projection, selection, selectionArgs);

                Uri notificationUri = Uri.withAppendedPath(CONTENT_URI, "account/" + accountUuid +
                        "/messages");

                cursor.setNotificationUri(contentResolver, notificationUri);
                break;
            }
        }

        return cursor;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new RuntimeException("not implemented yet");
    }

    protected Cursor getMessages(String accountUuid, final String[] projection,
            final String selection, final String[] selectionArgs, final String sortOrder) {

        Account account = getAccount(accountUuid);
        LockableDatabase database = getDatabase(account);

        try {
            return database.execute(false, new DbCallback<Cursor>() {
                @Override
                public Cursor doDbWork(SQLiteDatabase db) throws WrappedException,
                        UnavailableStorageException {

                    String where;
                    if (StringUtils.isNullOrEmpty(selection)) {
                        where = InternalMessageColumns.DELETED + "=0 AND (" +
                                InternalMessageColumns.EMPTY + " IS NULL OR " +
                                InternalMessageColumns.EMPTY + "!=1)";
                    } else {
                        where = "(" + selection + ") AND " +
                                InternalMessageColumns.DELETED + "=0 AND (" +
                                InternalMessageColumns.EMPTY + " IS NULL OR " +
                                InternalMessageColumns.EMPTY + "!=1)";
                    }

                    final Cursor cursor;
                    if (Utility.arrayContainsAny(projection, (Object[]) FOLDERS_COLUMNS)) {
                        StringBuilder query = new StringBuilder();
                        query.append("SELECT ");
                        boolean first = true;
                        for (String columnName : projection) {
                            if (!first) {
                                query.append(",");
                            } else {
                                first = false;
                            }

                            if (MessageColumns.ID.equals(columnName)) {
                                query.append("m.");
                                query.append(MessageColumns.ID);
                                query.append(" AS ");
                                query.append(MessageColumns.ID);
                            } else {
                                query.append(columnName);
                            }
                        }

                        query.append(" FROM messages m " +
                                "JOIN threads t ON (t.message_id = m.id) " +
                                "LEFT JOIN folders f ON (m.folder_id = f.id) " +
                                "WHERE ");
                        query.append(SqlQueryBuilder.addPrefixToSelection(FIXUP_MESSAGES_COLUMNS,
                                "m.", where));
                        query.append(" ORDER BY ");
                        query.append(SqlQueryBuilder.addPrefixToSelection(FIXUP_MESSAGES_COLUMNS,
                                "m.", sortOrder));

                        cursor = db.rawQuery(query.toString(), selectionArgs);
                    } else {
                        cursor = db.query(MESSAGES_TABLE, projection, where, selectionArgs, null,
                                null, sortOrder);
                    }

                    return cursor;
                }
            });
        } catch (UnavailableStorageException e) {
            throw new RuntimeException("Storage not available", e);
        }
    }

    protected Cursor getThreadedMessages(String accountUuid, final String[] projection,
            final String selection, final String[] selectionArgs, final String sortOrder) {

        Account account = getAccount(accountUuid);
        LockableDatabase database = getDatabase(account);

        try {
            return database.execute(false, new DbCallback<Cursor>() {
                @Override
                public Cursor doDbWork(SQLiteDatabase db) throws WrappedException,
                        UnavailableStorageException {

                    StringBuilder query = new StringBuilder();

                    query.append("SELECT ");
                    boolean first = true;
                    for (String columnName : projection) {
                        if (!first) {
                            query.append(",");
                        } else {
                            first = false;
                        }

                        final String aggregationFunc = THREAD_AGGREGATION_FUNCS.get(columnName);

                        if (MessageColumns.ID.equals(columnName)) {
                            query.append("m." + MessageColumns.ID + " AS " + MessageColumns.ID);
                        } else if (aggregationFunc != null) {
                            query.append("a.");
                            query.append(columnName);
                            query.append(" AS ");
                            query.append(columnName);
                        } else {
                            query.append(columnName);
                        }
                    }

                    query.append(" FROM (");

                    createThreadedSubQuery(projection, selection, selectionArgs, query);

                    query.append(") a ");

                    query.append("LEFT JOIN " + THREADS_TABLE + " t " +
                            "ON (t." + ThreadColumns.ROOT + " = a.thread_root) " +
                            "LEFT JOIN " + MESSAGES_TABLE + " m " +
                            "ON (m." + MessageColumns.ID + " = t." + ThreadColumns.MESSAGE_ID +
                            ") ");

                    if (Utility.arrayContainsAny(projection, (Object[]) FOLDERS_COLUMNS)) {
                        query.append("LEFT JOIN " + FOLDERS_TABLE + " f " +
                                "ON (m." + MessageColumns.FOLDER_ID + " = f." + FolderColumns.ID +
                                ") ");
                    }

                    query.append("WHERE m." + MessageColumns.DATE + " = a." + MessageColumns.DATE);
                    if (!StringUtils.isNullOrEmpty(sortOrder)) {
                        query.append(" ORDER BY ");
                        query.append(SqlQueryBuilder.addPrefixToSelection(
                                FIXUP_AGGREGATED_MESSAGES_COLUMNS, "a.", sortOrder));
                    }

                    return db.rawQuery(query.toString(), selectionArgs);
                }
            });
        } catch (UnavailableStorageException e) {
            throw new RuntimeException("Storage not available", e);
        }
    }

    private void createThreadedSubQuery(String[] projection, String selection,
            String[] selectionArgs, StringBuilder query) {

        query.append("SELECT t." + ThreadColumns.ROOT + " AS thread_root");
        for (String columnName : projection) {
            String aggregationFunc = THREAD_AGGREGATION_FUNCS.get(columnName);

            if (SpecialColumns.THREAD_COUNT.equals(columnName)) {
                query.append(",COUNT(t." + ThreadColumns.ROOT + ") AS " +
                        SpecialColumns.THREAD_COUNT);
            } else if (aggregationFunc != null) {
                query.append(",");
                query.append(aggregationFunc);
                query.append("(");
                query.append(columnName);
                query.append(") AS ");
                query.append(columnName);
            } else {
                // Skip
            }
        }

        query.append(
                " FROM " + MESSAGES_TABLE + " m " +
                "LEFT JOIN " + THREADS_TABLE + " t " +
                "ON (t." + ThreadColumns.MESSAGE_ID + " = m." + MessageColumns.ID + ")");

        if (Utility.arrayContainsAny(projection, (Object[]) FOLDERS_COLUMNS)) {
            query.append("LEFT JOIN " + FOLDERS_TABLE + " f " +
                    "ON (m." + MessageColumns.FOLDER_ID + " = f." + FolderColumns.ID +
                    ")");
        }

        query.append(" WHERE " +
                "(" + InternalMessageColumns.DELETED + " = 0 AND " +
                "(" + InternalMessageColumns.EMPTY + " IS NULL OR " +
                InternalMessageColumns.EMPTY + " != 1))");


        if (!StringUtils.isNullOrEmpty(selection)) {
            query.append(" AND (");
            query.append(selection);
            query.append(")");
        }

        query.append(" GROUP BY t." + ThreadColumns.ROOT);
    }

    protected Cursor getThread(String accountUuid, final String[] projection, final String threadId,
            final String sortOrder) {

        Account account = getAccount(accountUuid);
        LockableDatabase database = getDatabase(account);

        try {
            return database.execute(false, new DbCallback<Cursor>() {
                @Override
                public Cursor doDbWork(SQLiteDatabase db) throws WrappedException,
                        UnavailableStorageException {

                    StringBuilder query = new StringBuilder();
                    query.append("SELECT ");
                    boolean first = true;
                    for (String columnName : projection) {
                        if (!first) {
                            query.append(",");
                        } else {
                            first = false;
                        }

                        if (MessageColumns.ID.equals(columnName)) {
                            query.append("m." + MessageColumns.ID + " AS " + MessageColumns.ID);
                        } else {
                            query.append(columnName);
                        }
                    }

                    query.append(" FROM " + THREADS_TABLE + " t JOIN " + MESSAGES_TABLE + " m " +
                            "ON (m." + MessageColumns.ID + " = t." + ThreadColumns.MESSAGE_ID +
                            ") ");

                    if (Utility.arrayContainsAny(projection, (Object[]) FOLDERS_COLUMNS)) {
                        query.append("LEFT JOIN " + FOLDERS_TABLE + " f " +
                                "ON (m." + MessageColumns.FOLDER_ID +  " = f." + FolderColumns.ID +
                                ") ");
                    }

                    query.append("WHERE " +
                            ThreadColumns.ROOT + " = ? AND " +
                            InternalMessageColumns.DELETED + " = 0 AND (" +
                            InternalMessageColumns.EMPTY + " IS NULL OR " +
                            InternalMessageColumns.EMPTY + " != 1)");

                    query.append(" ORDER BY ");
                    query.append(SqlQueryBuilder.addPrefixToSelection(FIXUP_MESSAGES_COLUMNS,
                            "m.", sortOrder));

                    return db.rawQuery(query.toString(), new String[] { threadId });
                }
            });
        } catch (UnavailableStorageException e) {
            throw new RuntimeException("Storage not available", e);
        }
    }

    private Cursor getAccountStats(String accountUuid, String[] columns,
            final String selection, final String[] selectionArgs) {

        Account account = getAccount(accountUuid);
        LockableDatabase database = getDatabase(account);

        // Use default projection if none was given
        String[] sourceProjection = (columns == null) ? STATS_DEFAULT_PROJECTION : columns;

        // Create SQL query string
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");

        // Append projection for the database query
        // e.g. "SUM(read=0) AS unread_count, SUM(flagged) AS flagged_count"
        boolean first = true;
        for (String columnName : sourceProjection) {
            if (!first) {
                sql.append(',');
            } else {
                first = false;
            }

            if (StatsColumns.UNREAD_COUNT.equals(columnName)) {
                sql.append("SUM(" + MessageColumns.READ + "=0) AS " + StatsColumns.UNREAD_COUNT);
            } else if (StatsColumns.FLAGGED_COUNT.equals(columnName)) {
                sql.append("SUM(" + MessageColumns.FLAGGED + ") AS " + StatsColumns.FLAGGED_COUNT);
            } else {
                throw new IllegalArgumentException("Column name not allowed: " + columnName);
            }
        }

        // Table selection
        sql.append(" FROM messages");

        if (StringUtils.containsAny(selection, FOLDERS_COLUMNS)) {
            sql.append(" JOIN folders ON (folders.id = messages.folder_id)");
        }

        // WHERE clause
        sql.append(" WHERE (deleted=0 AND (empty IS NULL OR empty!=1))");
        if (!StringUtils.isNullOrEmpty(selection)) {
            sql.append(" AND (");
            sql.append(selection);
            sql.append(")");
        }

        // Query the database and return the result cursor
        try {
            return database.execute(false, new DbCallback<Cursor>() {
                @Override
                public Cursor doDbWork(SQLiteDatabase db) throws WrappedException,
                        UnavailableStorageException {

                    return db.rawQuery(sql.toString(), selectionArgs);
                }
            });
        }  catch (UnavailableStorageException e) {
            throw new RuntimeException("Storage not available", e);
        }
    }

    private Account getAccount(String accountUuid) {
        if (mPreferences == null) {
            Context appContext = getContext().getApplicationContext();
            mPreferences = Preferences.getPreferences(appContext);
        }

        Account account = mPreferences.getAccount(accountUuid);

        if (account == null) {
            throw new IllegalArgumentException("Unknown account: " + accountUuid);
        }

        return account;
    }

    private LockableDatabase getDatabase(Account account) {
        LocalStore localStore;
        try {
            localStore = account.getLocalStore();
        } catch (MessagingException e) {
            throw new RuntimeException("Couldn't get LocalStore", e);
        }

        return localStore.getDatabase();
    }

    /**
     * This class is needed to make {@link CursorAdapter} work with our database schema.
     *
     * <p>
     * {@code CursorAdapter} requires a column named {@code "_id"} containing a stable id. We use
     * the column name {@code "id"} as primary key in all our tables. So this {@link CursorWrapper}
     * maps all queries for {@code "_id"} to {@code "id"}.
     * </p><p>
     * Please note that this only works for the returned {@code Cursor}. When querying the content
     * provider you still need to use {@link MessageColumns#ID}.
     * </p>
     */
    static class IdTrickeryCursor extends CursorWrapper {
        public IdTrickeryCursor(Cursor cursor) {
            super(cursor);
        }

        @Override
        public int getColumnIndex(String columnName) {
            if ("_id".equals(columnName)) {
                return super.getColumnIndex("id");
            }

            return super.getColumnIndex(columnName);
        }

        @Override
        public int getColumnIndexOrThrow(String columnName) {
            if ("_id".equals(columnName)) {
                return super.getColumnIndexOrThrow("id");
            }

            return super.getColumnIndexOrThrow(columnName);
        }
    }

    static class SpecialColumnsCursor extends CursorWrapper {
        private int[] mColumnMapping;
        private String[] mSpecialColumnValues;
        private String[] mColumnNames;

        public SpecialColumnsCursor(Cursor cursor, String[] allColumnNames,
                Map<String, String> specialColumns) {
            super(cursor);

            mColumnNames = allColumnNames;
            mColumnMapping = new int[allColumnNames.length];
            mSpecialColumnValues = new String[specialColumns.size()];
            for (int i = 0, columnIndex = 0, specialColumnCount = 0, len = allColumnNames.length;
                    i < len; i++) {

                String columnName = allColumnNames[i];

                if (specialColumns.containsKey(columnName)) {
                    // This is a special column name, so save the value in mSpecialColumnValues
                    mSpecialColumnValues[specialColumnCount] = specialColumns.get(columnName);

                    // Write the index into mSpecialColumnValues negated into mColumnMapping
                    mColumnMapping[i] = -(specialColumnCount + 1);
                    specialColumnCount++;
                } else {
                    mColumnMapping[i] = columnIndex++;
                }
            }
        }

        @Override
        public byte[] getBlob(int columnIndex) {
            int realColumnIndex = mColumnMapping[columnIndex];
            if (realColumnIndex < 0) {
                throw new RuntimeException("Special column can only be retrieved as string.");
            }

            return super.getBlob(realColumnIndex);
        }

        @Override
        public int getColumnCount() {
            return mColumnMapping.length;
        }

        @Override
        public int getColumnIndex(String columnName) {
            for (int i = 0, len = mColumnNames.length; i < len; i++) {
                if (mColumnNames[i].equals(columnName)) {
                    return i;
                }
            }

            return super.getColumnIndex(columnName);
        }

        @Override
        public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
            int index = getColumnIndex(columnName);

            if (index == -1) {
                throw new IllegalArgumentException("Unknown column name");
            }

            return index;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return mColumnNames[columnIndex];
        }

        @Override
        public String[] getColumnNames() {
            return mColumnNames.clone();
        }

        @Override
        public double getDouble(int columnIndex) {
            int realColumnIndex = mColumnMapping[columnIndex];
            if (realColumnIndex < 0) {
                throw new RuntimeException("Special column can only be retrieved as string.");
            }

            return super.getDouble(realColumnIndex);
        }

        @Override
        public float getFloat(int columnIndex) {
            int realColumnIndex = mColumnMapping[columnIndex];
            if (realColumnIndex < 0) {
                throw new RuntimeException("Special column can only be retrieved as string.");
            }

            return super.getFloat(realColumnIndex);
        }

        @Override
        public int getInt(int columnIndex) {
            int realColumnIndex = mColumnMapping[columnIndex];
            if (realColumnIndex < 0) {
                throw new RuntimeException("Special column can only be retrieved as string.");
            }

            return super.getInt(realColumnIndex);
        }

        @Override
        public long getLong(int columnIndex) {
            int realColumnIndex = mColumnMapping[columnIndex];
            if (realColumnIndex < 0) {
                throw new RuntimeException("Special column can only be retrieved as string.");
            }

            return super.getLong(realColumnIndex);
        }

        @Override
        public short getShort(int columnIndex) {
            int realColumnIndex = mColumnMapping[columnIndex];
            if (realColumnIndex < 0) {
                throw new RuntimeException("Special column can only be retrieved as string.");
            }

            return super.getShort(realColumnIndex);
        }

        @Override
        public String getString(int columnIndex) {
            int realColumnIndex = mColumnMapping[columnIndex];
            if (realColumnIndex < 0) {
                return mSpecialColumnValues[-realColumnIndex - 1];
            }

            return super.getString(realColumnIndex);
        }

        @TargetApi(11)
        @Override
        public int getType(int columnIndex) {
            int realColumnIndex = mColumnMapping[columnIndex];
            if (realColumnIndex < 0) {
                return FIELD_TYPE_STRING;
            }

            return super.getType(realColumnIndex);
        }

        @Override
        public boolean isNull(int columnIndex) {
            int realColumnIndex = mColumnMapping[columnIndex];
            if (realColumnIndex < 0) {
                return (mSpecialColumnValues[-realColumnIndex - 1] == null);
            }

            return super.isNull(realColumnIndex);
        }
    }
}
