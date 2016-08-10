
package com.fsck.k9.mailstore;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.helper.UrlEncodingHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mailstore.LocalFolder.DataLocation;
import com.fsck.k9.mailstore.LocalFolder.MoreMessages;
import com.fsck.k9.mailstore.LockableDatabase.DbCallback;
import com.fsck.k9.mailstore.LockableDatabase.WrappedException;
import com.fsck.k9.mailstore.StorageManager.StorageProvider;
import com.fsck.k9.message.extractors.AttachmentCounter;
import com.fsck.k9.message.extractors.MessageFulltextCreator;
import com.fsck.k9.message.extractors.MessagePreviewCreator;
import com.fsck.k9.preferences.Storage;
import com.fsck.k9.provider.EmailProvider;
import com.fsck.k9.provider.EmailProvider.MessageColumns;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchSpecification.Attribute;
import com.fsck.k9.search.SearchSpecification.SearchField;
import com.fsck.k9.search.SqlQueryBuilder;
import org.apache.james.mime4j.codec.Base64InputStream;
import org.apache.james.mime4j.codec.QuotedPrintableInputStream;
import org.apache.james.mime4j.util.MimeUtil;

/**
 * <pre>
 * Implements a SQLite database backed local store for Messages.
 * </pre>
 */
public class LocalStore extends Store implements Serializable {

    private static final long serialVersionUID = -5142141896809423072L;

    static final String[] EMPTY_STRING_ARRAY = new String[0];
    static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * Lock objects indexed by account UUID.
     *
     * @see #getInstance(Account, Context)
     */
    private static ConcurrentMap<String, Object> sAccountLocks = new ConcurrentHashMap<>();

    /**
     * Local stores indexed by UUID because the Uri may change due to migration to/from SD-card.
     */
    private static ConcurrentMap<String, LocalStore> sLocalStores = new ConcurrentHashMap<>();

    /*
     * a String containing the columns getMessages expects to work with
     * in the correct order.
     */
    static String GET_MESSAGES_COLS =
        "subject, sender_list, date, uid, flags, messages.id, to_list, cc_list, " +
        "bcc_list, reply_to_list, attachment_count, internal_date, messages.message_id, " +
        "folder_id, preview, threads.id, threads.root, deleted, read, flagged, answered, " +
        "forwarded, message_part_id, messages.mime_type, preview_type, header ";

    static final String GET_FOLDER_COLS =
        "folders.id, name, visible_limit, last_updated, status, push_state, last_pushed, " +
        "integrate, top_group, poll_class, push_class, display_class, notify_class, more_messages";

    static final int FOLDER_ID_INDEX = 0;
    static final int FOLDER_NAME_INDEX = 1;
    static final int FOLDER_VISIBLE_LIMIT_INDEX = 2;
    static final int FOLDER_LAST_CHECKED_INDEX = 3;
    static final int FOLDER_STATUS_INDEX = 4;
    static final int FOLDER_PUSH_STATE_INDEX = 5;
    static final int FOLDER_LAST_PUSHED_INDEX = 6;
    static final int FOLDER_INTEGRATE_INDEX = 7;
    static final int FOLDER_TOP_GROUP_INDEX = 8;
    static final int FOLDER_SYNC_CLASS_INDEX = 9;
    static final int FOLDER_PUSH_CLASS_INDEX = 10;
    static final int FOLDER_DISPLAY_CLASS_INDEX = 11;
    static final int FOLDER_NOTIFY_CLASS_INDEX = 12;
    static final int MORE_MESSAGES_INDEX = 13;

    static final String[] UID_CHECK_PROJECTION = { "uid" };

    /**
     * Maximum number of UIDs to check for existence at once.
     *
     * @see LocalFolder#extractNewMessages(List)
     */
    static final int UID_CHECK_BATCH_SIZE = 500;

    /**
     * Maximum number of messages to perform flag updates on at once.
     *
     * @see #setFlag(List, Flag, boolean)
     */
    private static final int FLAG_UPDATE_BATCH_SIZE = 500;

    /**
     * Maximum number of threads to perform flag updates on at once.
     *
     * @see #setFlagForThreads(List, Flag, boolean)
     */
    private static final int THREAD_FLAG_UPDATE_BATCH_SIZE = 500;

    public static final int DB_VERSION = 55;


    public static String getColumnNameForFlag(Flag flag) {
        switch (flag) {
            case SEEN: {
                return MessageColumns.READ;
            }
            case FLAGGED: {
                return MessageColumns.FLAGGED;
            }
            case ANSWERED: {
                return MessageColumns.ANSWERED;
            }
            case FORWARDED: {
                return MessageColumns.FORWARDED;
            }
            default: {
                throw new IllegalArgumentException("Flag must be a special column flag");
            }
        }
    }


    protected String uUid = null;

    final Context context;

    LockableDatabase database;

    private ContentResolver mContentResolver;
    private final Account mAccount;
    private final MessagePreviewCreator messagePreviewCreator;
    private final MessageFulltextCreator messageFulltextCreator;
    private final AttachmentCounter attachmentCounter;

    /**
     * local://localhost/path/to/database/uuid.db
     * This constructor is only used by {@link LocalStore#getInstance(Account, Context)}
     * @throws UnavailableStorageException if not {@link StorageProvider#isReady(Context)}
     */
    private LocalStore(final Account account, final Context context) throws MessagingException {
        mAccount = account;
        database = new LockableDatabase(context, account.getUuid(), new StoreSchemaDefinition(this));

        this.context = context;
        mContentResolver = context.getContentResolver();
        database.setStorageProviderId(account.getLocalStorageProviderId());
        uUid = account.getUuid();

        messagePreviewCreator = MessagePreviewCreator.newInstance();
        messageFulltextCreator = MessageFulltextCreator.newInstance();
        attachmentCounter = AttachmentCounter.newInstance();

        database.open();
    }

    /**
     * Get an instance of a local mail store.
     *
     * @throws UnavailableStorageException
     *          if not {@link StorageProvider#isReady(Context)}
     */
    public static LocalStore getInstance(Account account, Context context)
            throws MessagingException {

        String accountUuid = account.getUuid();

        // Create new per-account lock object if necessary
        sAccountLocks.putIfAbsent(accountUuid, new Object());

        // Use per-account locks so DatabaseUpgradeService always knows which account database is
        // currently upgraded.
        synchronized (sAccountLocks.get(accountUuid)) {
            LocalStore store = sLocalStores.get(accountUuid);

            if (store == null) {
                // Creating a LocalStore instance will create or upgrade the database if
                // necessary. This could take some time.
                store = new LocalStore(account, context);

                sLocalStores.put(accountUuid, store);
            }

            return store;
        }
    }

    public static void removeAccount(Account account) {
        try {
            removeInstance(account);
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Failed to reset local store for account " + account.getUuid(), e);
        }
    }

    private static void removeInstance(Account account) {
        String accountUuid = account.getUuid();
        sLocalStores.remove(accountUuid);
    }

    public void switchLocalStorage(final String newStorageProviderId) throws MessagingException {
        database.switchProvider(newStorageProviderId);
    }

    protected Account getAccount() {
        return mAccount;
    }

    protected Storage getStorage() {
        return Preferences.getPreferences(context).getStorage();
    }

    public long getSize() throws MessagingException {

        final StorageManager storageManager = StorageManager.getInstance(context);

        final File attachmentDirectory = storageManager.getAttachmentDirectory(uUid,
                                         database.getStorageProviderId());

        return database.execute(false, new DbCallback<Long>() {
            @Override
            public Long doDbWork(final SQLiteDatabase db) {
                final File[] files = attachmentDirectory.listFiles();
                long attachmentLength = 0;
                if (files != null) {
                    for (File file : files) {
                        if (file.exists()) {
                            attachmentLength += file.length();
                        }
                    }
                }

                final File dbFile = storageManager.getDatabase(uUid, database.getStorageProviderId());
                return dbFile.length() + attachmentLength;
            }
        });
    }

    public void compact() throws MessagingException {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Before compaction size = " + getSize());

        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                db.execSQL("VACUUM");
                return null;
            }
        });
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "After compaction size = " + getSize());
    }


    public void clear() throws MessagingException {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Before prune size = " + getSize());

        deleteAllMessageDataFromDisk();
        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "After prune / before compaction size = " + getSize());

            Log.i(K9.LOG_TAG, "Before clear folder count = " + getFolderCount());
            Log.i(K9.LOG_TAG, "Before clear message count = " + getMessageCount());

            Log.i(K9.LOG_TAG, "After prune / before clear size = " + getSize());
        }

        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) {
                // We don't care about threads of deleted messages, so delete the whole table.
                db.delete("threads", null, null);

                // Don't delete deleted messages. They are essentially placeholders for UIDs of messages that have
                // been deleted locally.
                db.delete("messages", "deleted = 0", null);
                return null;
            }
        });

        compact();

        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "After clear message count = " + getMessageCount());

            Log.i(K9.LOG_TAG, "After clear size = " + getSize());
        }
    }

    public int getMessageCount() throws MessagingException {
        return database.execute(false, new DbCallback<Integer>() {
            @Override
            public Integer doDbWork(final SQLiteDatabase db) {
                Cursor cursor = null;
                try {
                    cursor = db.rawQuery("SELECT COUNT(*) FROM messages", null);
                    cursor.moveToFirst();
                    return cursor.getInt(0);   // message count
                } finally {
                    Utility.closeQuietly(cursor);
                }
            }
        });
    }

    public int getFolderCount() throws MessagingException {
        return database.execute(false, new DbCallback<Integer>() {
            @Override
            public Integer doDbWork(final SQLiteDatabase db) {
                Cursor cursor = null;
                try {
                    cursor = db.rawQuery("SELECT COUNT(*) FROM folders", null);
                    cursor.moveToFirst();
                    return cursor.getInt(0);        // folder count
                } finally {
                    Utility.closeQuietly(cursor);
                }
            }
        });
    }

    @Override
    public LocalFolder getFolder(String name) {
        return new LocalFolder(this, name);
    }

    // TODO this takes about 260-300ms, seems slow.
    @Override
    public List<LocalFolder> getPersonalNamespaces(boolean forceListAll) throws MessagingException {
        final List<LocalFolder> folders = new LinkedList<>();
        try {
            database.execute(false, new DbCallback < List <? extends Folder >> () {
                @Override
                public List <? extends Folder > doDbWork(final SQLiteDatabase db) throws WrappedException {
                    Cursor cursor = null;

                    try {
                        cursor = db.rawQuery("SELECT " + GET_FOLDER_COLS + " FROM folders " +
                                "ORDER BY name ASC", null);
                        while (cursor.moveToNext()) {
                            if (cursor.isNull(FOLDER_ID_INDEX)) {
                                continue;
                            }
                            String folderName = cursor.getString(FOLDER_NAME_INDEX);
                            LocalFolder folder = new LocalFolder(LocalStore.this, folderName);
                            folder.open(cursor);

                            folders.add(folder);
                        }
                        return folders;
                    } catch (MessagingException e) {
                        throw new WrappedException(e);
                    } finally {
                        Utility.closeQuietly(cursor);
                    }
                }
            });
        } catch (WrappedException e) {
            throw(MessagingException) e.getCause();
        }
        return folders;
    }

    @Override
    public void checkSettings() throws MessagingException {
    }

    public void delete() throws UnavailableStorageException {
        database.delete();
    }

    public void recreate() throws UnavailableStorageException {
        database.recreate();
    }

    private void deleteAllMessageDataFromDisk() throws MessagingException {
        markAllMessagePartsDataAsMissing();
        deleteAllMessagePartsDataFromDisk();
    }

    private void markAllMessagePartsDataAsMissing() throws MessagingException {
        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                ContentValues cv = new ContentValues();
                cv.put("data_location", DataLocation.MISSING);
                db.update("message_parts", cv, null, null);

                return null;
            }
        });
    }

    private void deleteAllMessagePartsDataFromDisk() {
        final StorageManager storageManager = StorageManager.getInstance(context);
        File attachmentDirectory = storageManager.getAttachmentDirectory(uUid, database.getStorageProviderId());
        File[] files = attachmentDirectory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.exists() && !file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    public void resetVisibleLimits(int visibleLimit) throws MessagingException {
        final ContentValues cv = new ContentValues();
        cv.put("visible_limit", Integer.toString(visibleLimit));
        cv.put("more_messages", MoreMessages.UNKNOWN.getDatabaseName());
        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                db.update("folders", cv, null, null);
                return null;
            }
        });
    }

    public List<PendingCommand> getPendingCommands() throws MessagingException {
        return database.execute(false, new DbCallback<List<PendingCommand>>() {
            @Override
            public List<PendingCommand> doDbWork(final SQLiteDatabase db) throws WrappedException {
                Cursor cursor = null;
                try {
                    cursor = db.query("pending_commands",
                                      new String[] { "id", "command", "arguments" },
                                      null,
                                      null,
                                      null,
                                      null,
                                      "id ASC");
                    List<PendingCommand> commands = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        PendingCommand command = new PendingCommand();
                        command.mId = cursor.getLong(0);
                        command.command = cursor.getString(1);
                        String arguments = cursor.getString(2);
                        command.arguments = arguments.split(",");
                        for (int i = 0; i < command.arguments.length; i++) {
                            command.arguments[i] = Utility.fastUrlDecode(command.arguments[i]);
                        }
                        commands.add(command);
                    }
                    return commands;
                } finally {
                    Utility.closeQuietly(cursor);
                }
            }
        });
    }

    public void addPendingCommand(PendingCommand command) throws MessagingException {
        for (int i = 0; i < command.arguments.length; i++) {
            command.arguments[i] = UrlEncodingHelper.encodeUtf8(command.arguments[i]);
        }
        final ContentValues cv = new ContentValues();
        cv.put("command", command.command);
        cv.put("arguments", Utility.combine(command.arguments, ','));
        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                db.insert("pending_commands", "command", cv);
                return null;
            }
        });
    }

    public void removePendingCommand(final PendingCommand command) throws MessagingException {
        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                db.delete("pending_commands", "id = ?", new String[] { Long.toString(command.mId) });
                return null;
            }
        });
    }

    public void removePendingCommands() throws MessagingException {
        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                db.delete("pending_commands", null, null);
                return null;
            }
        });
    }

    public static class PendingCommand {
        private long mId;
        public String command;
        public String[] arguments;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(command);
            sb.append(": ");
            for (String argument : arguments) {
                sb.append(", ");
                sb.append(argument);
                //sb.append("\n");
            }
            return sb.toString();
        }
    }

    @Override
    public boolean isMoveCapable() {
        return true;
    }

    @Override
    public boolean isCopyCapable() {
        return true;
    }

    public List<LocalMessage> searchForMessages(MessageRetrievalListener<LocalMessage> retrievalListener,
                                        LocalSearch search) throws MessagingException {

        StringBuilder query = new StringBuilder();
        List<String> queryArgs = new ArrayList<>();
        SqlQueryBuilder.buildWhereClause(mAccount, search.getConditions(), query, queryArgs);

        // Avoid "ambiguous column name" error by prefixing "id" with the message table name
        String where = SqlQueryBuilder.addPrefixToSelection(new String[] { "id" },
                "messages.", query.toString());

        String[] selectionArgs = queryArgs.toArray(new String[queryArgs.size()]);

        String sqlQuery = "SELECT " + GET_MESSAGES_COLS + "FROM messages " +
                "LEFT JOIN threads ON (threads.message_id = messages.id) " +
                "LEFT JOIN message_parts ON (message_parts.id = messages.message_part_id) " +
                "LEFT JOIN folders ON (folders.id = messages.folder_id) WHERE " +
                "(empty = 0 AND deleted = 0)" +
                ((!TextUtils.isEmpty(where)) ? " AND (" + where + ")" : "") +
                " ORDER BY date DESC";

        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "Query = " + sqlQuery);
        }

        return getMessages(retrievalListener, null, sqlQuery, selectionArgs);
    }

    /*
     * Given a query string, actually do the query for the messages and
     * call the MessageRetrievalListener for each one
     */
    List<LocalMessage> getMessages(
        final MessageRetrievalListener<LocalMessage> listener,
        final LocalFolder folder,
        final String queryString, final String[] placeHolders
    ) throws MessagingException {
        final List<LocalMessage> messages = new ArrayList<>();
        final int j = database.execute(false, new DbCallback<Integer>() {
            @Override
            public Integer doDbWork(final SQLiteDatabase db) throws WrappedException {
                Cursor cursor = null;
                int i = 0;
                try {
                    cursor = db.rawQuery(queryString + " LIMIT 10", placeHolders);

                    while (cursor.moveToNext()) {
                        LocalMessage message = new LocalMessage(LocalStore.this, null, folder);
                        message.populateFromGetMessageCursor(cursor);

                        messages.add(message);
                        if (listener != null) {
                            listener.messageFinished(message, i, -1);
                        }
                        i++;
                    }
                    cursor.close();
                    cursor = db.rawQuery(queryString + " LIMIT -1 OFFSET 10", placeHolders);

                    while (cursor.moveToNext()) {
                        LocalMessage message = new LocalMessage(LocalStore.this, null, folder);
                        message.populateFromGetMessageCursor(cursor);

                        messages.add(message);
                        if (listener != null) {
                            listener.messageFinished(message, i, -1);
                        }
                        i++;
                    }
                } catch (Exception e) {
                    Log.d(K9.LOG_TAG, "Got an exception", e);
                } finally {
                    Utility.closeQuietly(cursor);
                }
                return i;
            }
        });
        if (listener != null) {
            listener.messagesFinished(j);
        }

        return Collections.unmodifiableList(messages);

    }

    public List<LocalMessage> getMessagesInThread(final long rootId) throws MessagingException {
        String rootIdString = Long.toString(rootId);

        LocalSearch search = new LocalSearch();
        search.and(SearchField.THREAD_ID, rootIdString, Attribute.EQUALS);

        return searchForMessages(null, search);
    }

    public AttachmentInfo getAttachmentInfo(final String attachmentId) throws MessagingException {
        return database.execute(false, new DbCallback<AttachmentInfo>() {
            @Override
            public AttachmentInfo doDbWork(final SQLiteDatabase db) throws WrappedException {
                Cursor cursor = db.query("message_parts",
                        new String[] { "display_name", "decoded_body_size", "mime_type" },
                        "id = ?",
                        new String[] { attachmentId },
                        null, null, null);
                try {
                    if (!cursor.moveToFirst()) {
                        return null;
                    }
                    String name = cursor.getString(0);
                    long size = cursor.getLong(1);
                    String mimeType = cursor.getString(2);

                    final AttachmentInfo attachmentInfo = new AttachmentInfo();
                    attachmentInfo.name = name;
                    attachmentInfo.size = size;
                    attachmentInfo.type = mimeType;

                    return attachmentInfo;
                } finally {
                    cursor.close();
                }
            }
        });
    }

    @Nullable
    public InputStream getAttachmentInputStream(final String attachmentId) throws MessagingException {
        return database.execute(false, new DbCallback<InputStream>() {
            @Override
            public InputStream doDbWork(final SQLiteDatabase db) throws WrappedException {
                Cursor cursor = db.query("message_parts",
                        new String[] { "data_location", "data", "encoding" },
                        "id = ?",
                        new String[] { attachmentId },
                        null, null, null);
                try {
                    if (!cursor.moveToFirst()) {
                        return null;
                    }

                    int location = cursor.getInt(0);
                    String encoding = cursor.getString(2);

                    InputStream rawInputStream = getRawAttachmentInputStream(cursor, location, attachmentId);
                    return getDecodingInputStream(rawInputStream, encoding);
                } finally {
                    cursor.close();
                }
            }
        });
    }

    @Nullable
    private InputStream getRawAttachmentInputStream(Cursor cursor, int location, String attachmentId) {
        switch (location) {
            case DataLocation.IN_DATABASE: {
                byte[] data = cursor.getBlob(1);
                return new ByteArrayInputStream(data);
            }
            case DataLocation.ON_DISK: {
                File file = getAttachmentFile(attachmentId);
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    return null;
                }
            }
            default: {
                throw new IllegalStateException("No attachment data available");
            }
        }
    }

    @Nullable
    InputStream getDecodingInputStream(@Nullable final InputStream rawInputStream, @Nullable String encoding) {
        if (rawInputStream == null) {
            return null;
        }

        if (MimeUtil.ENC_BASE64.equals(encoding)) {
            return new Base64InputStream(rawInputStream) {
                @Override
                public void close() throws IOException {
                    super.close();
                    rawInputStream.close();
                }
            };
        }
        if (MimeUtil.ENC_QUOTED_PRINTABLE.equals(encoding)) {
            return new QuotedPrintableInputStream(rawInputStream) {
                @Override
                public void close() throws IOException {
                    super.close();
                    rawInputStream.close();
                }
            };
        }

        return rawInputStream;
    }

    File getAttachmentFile(String attachmentId) {
        final StorageManager storageManager = StorageManager.getInstance(context);
        final File attachmentDirectory = storageManager.getAttachmentDirectory(uUid, database.getStorageProviderId());
        return new File(attachmentDirectory, attachmentId);
    }

    public static class AttachmentInfo {
        public String name;
        public long size;
        public String type;
    }

    public void createFolders(final List<LocalFolder> foldersToCreate, final int visibleLimit) throws MessagingException {
        database.execute(true, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws WrappedException {
                for (LocalFolder folder : foldersToCreate) {
                    String name = folder.getName();
                    final  LocalFolder.PreferencesHolder prefHolder = folder.new PreferencesHolder();

                    // When created, special folders should always be displayed
                    // inbox should be integrated
                    // and the inbox and drafts folders should be syncced by default
                    if (mAccount.isSpecialFolder(name)) {
                        prefHolder.inTopGroup = true;
                        prefHolder.displayClass = LocalFolder.FolderClass.FIRST_CLASS;
                        if (name.equalsIgnoreCase(mAccount.getInboxFolderName())) {
                            prefHolder.integrate = true;
                            prefHolder.notifyClass = LocalFolder.FolderClass.FIRST_CLASS;
                            prefHolder.pushClass = LocalFolder.FolderClass.FIRST_CLASS;
                        } else {
                            prefHolder.pushClass = LocalFolder.FolderClass.INHERITED;

                        }
                        if (name.equalsIgnoreCase(mAccount.getInboxFolderName()) ||
                                name.equalsIgnoreCase(mAccount.getDraftsFolderName())) {
                            prefHolder.syncClass = LocalFolder.FolderClass.FIRST_CLASS;
                        } else {
                            prefHolder.syncClass = LocalFolder.FolderClass.NO_CLASS;
                        }
                    }
                    folder.refresh(name, prefHolder);   // Recover settings from Preferences

                    db.execSQL("INSERT INTO folders (name, visible_limit, top_group, display_class, poll_class, notify_class, push_class, integrate) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {
                                   name,
                                   visibleLimit,
                                   prefHolder.inTopGroup ? 1 : 0,
                                   prefHolder.displayClass.name(),
                                   prefHolder.syncClass.name(),
                                   prefHolder.notifyClass.name(),
                                   prefHolder.pushClass.name(),
                                   prefHolder.integrate ? 1 : 0,
                               });

                }
                return null;
            }
        });
    }


    static String serializeFlags(Iterable<Flag> flags) {
        List<Flag> extraFlags = new ArrayList<>();

        for (Flag flag : flags) {
            switch (flag) {
                case DELETED:
                case SEEN:
                case FLAGGED:
                case ANSWERED:
                case FORWARDED: {
                    break;
                }
                default: {
                    extraFlags.add(flag);
                }
            }
        }

        return Utility.combine(extraFlags, ',').toUpperCase(Locale.US);
    }

    // TODO: database should not be exposed!
    public LockableDatabase getDatabase() {
        return database;
    }

    public MessagePreviewCreator getMessagePreviewCreator() {
        return messagePreviewCreator;
    }

    public MessageFulltextCreator getMessageFulltextCreator() {
        return messageFulltextCreator;
    }

    public AttachmentCounter getAttachmentCounter() {
        return attachmentCounter;
    }

    void notifyChange() {
        Uri uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI, "account/" + uUid + "/messages");
        mContentResolver.notifyChange(uri, null);
    }

    /**
     * Split database operations with a large set of arguments into multiple SQL statements.
     *
     * <p>
     * At the time of this writing (2012-12-06) SQLite only supports around 1000 arguments. That's
     * why we have to split SQL statements with a large set of arguments into multiple SQL
     * statements each working on a subset of the arguments.
     * </p>
     *
     * @param selectionCallback
     *         Supplies the argument set and the code to query/update the database.
     * @param batchSize
     *         The maximum size of the selection set in each SQL statement.
     *
     * @throws MessagingException
     */
    public void doBatchSetSelection(final BatchSetSelection selectionCallback, final int batchSize)
            throws MessagingException {

        final List<String> selectionArgs = new ArrayList<>();
        int start = 0;

        while (start < selectionCallback.getListSize()) {
            final StringBuilder selection = new StringBuilder();

            selection.append(" IN (");

            int count = Math.min(selectionCallback.getListSize() - start, batchSize);

            for (int i = start, end = start + count; i < end; i++) {
                if (i > start) {
                    selection.append(",?");
                } else {
                    selection.append("?");
                }

                selectionArgs.add(selectionCallback.getListItem(i));
            }

            selection.append(")");

            try {
                database.execute(true, new DbCallback<Void>() {
                    @Override
                    public Void doDbWork(final SQLiteDatabase db) throws WrappedException,
                            UnavailableStorageException {

                        selectionCallback.doDbWork(db, selection.toString(),
                                selectionArgs.toArray(new String[selectionArgs.size()]));

                        return null;
                    }
                });

                selectionCallback.postDbWork();

            } catch (WrappedException e) {
                throw(MessagingException) e.getCause();
            }

            selectionArgs.clear();
            start += count;
        }
    }

    /**
     * Defines the behavior of {@link LocalStore#doBatchSetSelection(BatchSetSelection, int)}.
     */
    public interface BatchSetSelection {
        /**
         * @return The size of the argument list.
         */
        int getListSize();

        /**
         * Get a specific item of the argument list.
         *
         * @param index
         *         The index of the item.
         *
         * @return Item at position {@code i} of the argument list.
         */
        String getListItem(int index);

        /**
         * Execute the SQL statement.
         *
         * @param db
         *         Use this {@link SQLiteDatabase} instance for your SQL statement.
         * @param selectionSet
         *         A partial selection string containing place holders for the argument list, e.g.
         *         {@code " IN (?,?,?)"} (starts with a space).
         * @param selectionArgs
         *         The current subset of the argument list.
         * @throws UnavailableStorageException
         */
        void doDbWork(SQLiteDatabase db, String selectionSet, String[] selectionArgs)
                throws UnavailableStorageException;

        /**
         * This will be executed after each invocation of
         * {@link #doDbWork(SQLiteDatabase, String, String[])} (after the transaction has been
         * committed).
         */
        void postDbWork();
    }

    /**
     * Change the state of a flag for a list of messages.
     *
     * <p>
     * The goal of this method is to be fast. Currently this means using as few SQL UPDATE
     * statements as possible.
     *
     * @param messageIds
     *         A list of primary keys in the "messages" table.
     * @param flag
     *         The flag to change. This must be a flag with a separate column in the database.
     * @param newState
     *         {@code true}, if the flag should be set. {@code false}, otherwise.
     *
     * @throws MessagingException
     */
    public void setFlag(final List<Long> messageIds, final Flag flag, final boolean newState)
            throws MessagingException {

        final ContentValues cv = new ContentValues();
        cv.put(getColumnNameForFlag(flag), newState);

        doBatchSetSelection(new BatchSetSelection() {

            @Override
            public int getListSize() {
                return messageIds.size();
            }

            @Override
            public String getListItem(int index) {
                return Long.toString(messageIds.get(index));
            }

            @Override
            public void doDbWork(SQLiteDatabase db, String selectionSet, String[] selectionArgs)
                    throws UnavailableStorageException {

                db.update("messages", cv, "empty = 0 AND id" + selectionSet,
                        selectionArgs);
            }

            @Override
            public void postDbWork() {
                notifyChange();
            }
        }, FLAG_UPDATE_BATCH_SIZE);
    }

    /**
     * Change the state of a flag for a list of threads.
     *
     * <p>
     * The goal of this method is to be fast. Currently this means using as few SQL UPDATE
     * statements as possible.
     *
     * @param threadRootIds
     *         A list of root thread IDs.
     * @param flag
     *         The flag to change. This must be a flag with a separate column in the database.
     * @param newState
     *         {@code true}, if the flag should be set. {@code false}, otherwise.
     *
     * @throws MessagingException
     */
    public void setFlagForThreads(final List<Long> threadRootIds, Flag flag, final boolean newState)
            throws MessagingException {

        final String flagColumn = getColumnNameForFlag(flag);

        doBatchSetSelection(new BatchSetSelection() {

            @Override
            public int getListSize() {
                return threadRootIds.size();
            }

            @Override
            public String getListItem(int index) {
                return Long.toString(threadRootIds.get(index));
            }

            @Override
            public void doDbWork(SQLiteDatabase db, String selectionSet, String[] selectionArgs)
                    throws UnavailableStorageException {

                db.execSQL("UPDATE messages SET " + flagColumn + " = " + ((newState) ? "1" : "0") +
                        " WHERE id IN (" +
                        "SELECT m.id FROM threads t " +
                        "LEFT JOIN messages m ON (t.message_id = m.id) " +
                        "WHERE m.empty = 0 AND m.deleted = 0 " +
                        "AND t.root" + selectionSet + ")",
                        selectionArgs);
            }

            @Override
            public void postDbWork() {
                notifyChange();
            }
        }, THREAD_FLAG_UPDATE_BATCH_SIZE);
    }

    /**
     * Get folder name and UID for the supplied messages.
     *
     * @param messageIds
     *         A list of primary keys in the "messages" table.
     * @param threadedList
     *         If this is {@code true}, {@code messageIds} contains the thread IDs of the messages
     *         at the root of a thread. In that case return UIDs for all messages in these threads.
     *         If this is {@code false} only the UIDs for messages in {@code messageIds} are
     *         returned.
     *
     * @return The list of UIDs for the messages grouped by folder name.
     *
     * @throws MessagingException
     */
    public Map<String, List<String>> getFoldersAndUids(final List<Long> messageIds,
            final boolean threadedList) throws MessagingException {

        final Map<String, List<String>> folderMap = new HashMap<>();

        doBatchSetSelection(new BatchSetSelection() {

            @Override
            public int getListSize() {
                return messageIds.size();
            }

            @Override
            public String getListItem(int index) {
                return Long.toString(messageIds.get(index));
            }

            @Override
            public void doDbWork(SQLiteDatabase db, String selectionSet, String[] selectionArgs)
                    throws UnavailableStorageException {

                if (threadedList) {
                    String sql = "SELECT m.uid, f.name " +
                            "FROM threads t " +
                            "LEFT JOIN messages m ON (t.message_id = m.id) " +
                            "LEFT JOIN folders f ON (m.folder_id = f.id) " +
                            "WHERE m.empty = 0 AND m.deleted = 0 " +
                            "AND t.root" + selectionSet;

                    getDataFromCursor(db.rawQuery(sql, selectionArgs));

                } else {
                    String sql =
                            "SELECT m.uid, f.name " +
                            "FROM messages m " +
                            "LEFT JOIN folders f ON (m.folder_id = f.id) " +
                            "WHERE m.empty = 0 AND m.id" + selectionSet;

                    getDataFromCursor(db.rawQuery(sql, selectionArgs));
                }
            }

            private void getDataFromCursor(Cursor cursor) {
                try {
                    while (cursor.moveToNext()) {
                        String uid = cursor.getString(0);
                        String folderName = cursor.getString(1);

                        List<String> uidList = folderMap.get(folderName);
                        if (uidList == null) {
                            uidList = new ArrayList<>();
                            folderMap.put(folderName, uidList);
                        }

                        uidList.add(uid);
                    }
                } finally {
                    cursor.close();
                }
            }

            @Override
            public void postDbWork() {
                notifyChange();

            }
        }, UID_CHECK_BATCH_SIZE);

        return folderMap;
    }
}
