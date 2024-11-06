
package com.fsck.k9.mailstore;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import androidx.core.database.CursorKt;
import app.k9mail.legacy.account.Account;
import app.k9mail.legacy.di.DI;
import app.k9mail.legacy.mailstore.MessageListRepository;
import app.k9mail.legacy.mailstore.MoreMessages;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessagingControllerCommands.PendingCommand;
import com.fsck.k9.controller.PendingCommandSerializer;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.FetchProfile.Item;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.FolderType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mailstore.LocalFolder.DataLocation;
import com.fsck.k9.mailstore.LockableDatabase.DbCallback;
import com.fsck.k9.mailstore.LockableDatabase.SchemaDefinition;
import com.fsck.k9.mailstore.StorageManager.InternalStorageProvider;
import com.fsck.k9.message.extractors.AttachmentInfoExtractor;
import app.k9mail.legacy.search.LocalSearch;
import app.k9mail.legacy.search.api.SearchAttribute;
import app.k9mail.legacy.search.api.SearchField;
import com.fsck.k9.search.SqlQueryBuilder;
import kotlinx.datetime.Clock;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.codec.Base64InputStream;
import org.apache.james.mime4j.codec.QuotedPrintableInputStream;
import org.apache.james.mime4j.util.MimeUtil;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSource;
import timber.log.Timber;

/**
 * <pre>
 * Implements a SQLite database backed local store for Messages.
 * </pre>
 */
public class LocalStore {
    static final String[] EMPTY_STRING_ARRAY = new String[0];
    static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /*
     * a String containing the columns getMessages expects to work with
     * in the correct order.
     */
    static final String GET_MESSAGES_COLS =
        "subject, sender_list, date, uid, flags, messages.id, to_list, cc_list, " +
        "bcc_list, reply_to_list, attachment_count, internal_date, messages.message_id, " +
        "folder_id, preview, threads.id, threads.root, deleted, read, flagged, answered, " +
        "forwarded, message_part_id, messages.mime_type, preview_type, header ";

    static final int MSG_INDEX_SUBJECT = 0;
    static final int MSG_INDEX_SENDER_LIST = 1;
    static final int MSG_INDEX_DATE = 2;
    static final int MSG_INDEX_UID = 3;
    static final int MSG_INDEX_FLAGS = 4;
    static final int MSG_INDEX_ID = 5;
    static final int MSG_INDEX_TO = 6;
    static final int MSG_INDEX_CC = 7;
    static final int MSG_INDEX_BCC = 8;
    static final int MSG_INDEX_REPLY_TO = 9;
    static final int MSG_INDEX_ATTACHMENT_COUNT = 10;
    static final int MSG_INDEX_INTERNAL_DATE = 11;
    static final int MSG_INDEX_MESSAGE_ID_HEADER = 12;
    static final int MSG_INDEX_FOLDER_ID = 13;
    static final int MSG_INDEX_PREVIEW = 14;
    static final int MSG_INDEX_THREAD_ID = 15;
    static final int MSG_INDEX_THREAD_ROOT_ID = 16;
    static final int MSG_INDEX_FLAG_DELETED = 17;
    static final int MSG_INDEX_FLAG_READ = 18;
    static final int MSG_INDEX_FLAG_FLAGGED = 19;
    static final int MSG_INDEX_FLAG_ANSWERED = 20;
    static final int MSG_INDEX_FLAG_FORWARDED = 21;
    static final int MSG_INDEX_MESSAGE_PART_ID = 22;
    static final int MSG_INDEX_MIME_TYPE = 23;
    static final int MSG_INDEX_PREVIEW_TYPE = 24;
    static final int MSG_INDEX_HEADER_DATA = 25;

    static final int MSG_INDEX_NOTIFICATION_ID = 26;
    static final int MSG_INDEX_NOTIFICATION_TIMESTAMP = 27;

    static final String GET_FOLDER_COLS =
        "folders.id, name, visible_limit, last_updated, status, " +
        "integrate, top_group, sync_enabled, visible, notifications_enabled, more_messages, server_id, " +
        "local_only, type";

    static final int FOLDER_ID_INDEX = 0;
    static final int FOLDER_NAME_INDEX = 1;
    static final int FOLDER_VISIBLE_LIMIT_INDEX = 2;
    static final int FOLDER_LAST_CHECKED_INDEX = 3;
    static final int FOLDER_STATUS_INDEX = 4;
    static final int FOLDER_INTEGRATE_INDEX = 5;
    static final int FOLDER_TOP_GROUP_INDEX = 6;
    static final int FOLDER_SYNC_ENABLED_INDEX = 7;
    static final int FOLDER_VISIBLE_INDEX = 8;
    static final int FOLDER_NOTIFICATIONS_ENABLED_INDEX = 9;
    static final int MORE_MESSAGES_INDEX = 10;
    static final int FOLDER_SERVER_ID_INDEX = 11;
    static final int LOCAL_ONLY_INDEX = 12;
    static final int TYPE_INDEX = 13;

    static final String[] UID_CHECK_PROJECTION = { "uid" };

    private static final String[] GET_ATTACHMENT_COLS = new String[] { "id", "root", "data_location", "encoding", "data" };

    private static final int ATTACH_PART_ID_INDEX = 0;
    private static final int ATTACH_ROOT_INDEX = 1;
    private static final int ATTACH_LOCATION_INDEX = 2;
    private static final int ATTACH_ENCODING_INDEX = 3;
    private static final int ATTACH_DATA_INDEX = 4;

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

    private final Context context;
    private final PendingCommandSerializer pendingCommandSerializer;
    private final AttachmentInfoExtractor attachmentInfoExtractor;

    private final Account account;
    private final LockableDatabase database;
    private final OutboxStateRepository outboxStateRepository;

    static LocalStore createInstance(Account account, Context context) throws MessagingException {
        return new LocalStore(account, context);
    }

    /**
     * local://localhost/path/to/database/uuid.db
     * This constructor is only used by {@link LocalStoreProvider#getInstance(Account)}
     */
    private LocalStore(final Account account, final Context context) throws MessagingException {
        this.context = context;

        pendingCommandSerializer = PendingCommandSerializer.getInstance();
        attachmentInfoExtractor = DI.get(AttachmentInfoExtractor.class);

        this.account = account;

        SchemaDefinitionFactory schemaDefinitionFactory = DI.get(SchemaDefinitionFactory.class);
        RealMigrationsHelper migrationsHelper = new RealMigrationsHelper();
        SchemaDefinition schemaDefinition = schemaDefinitionFactory.createSchemaDefinition(migrationsHelper);

        database = new LockableDatabase(context, account.getUuid(), schemaDefinition);
        database.open();

        Clock clock = DI.get(Clock.class);
        outboxStateRepository = new OutboxStateRepository(database, clock);
    }

    public static int getDbVersion() {
        SchemaDefinitionFactory schemaDefinitionFactory = DI.get(SchemaDefinitionFactory.class);
        return schemaDefinitionFactory.getDatabaseVersion();
    }

    Account getAccount() {
        return account;
    }

    protected Preferences getPreferences() {
        return Preferences.getPreferences();
    }

    public OutboxStateRepository getOutboxStateRepository() {
        return outboxStateRepository;
    }

    public LocalFolder getFolder(String serverId) {
        return new LocalFolder(this, serverId);
    }

    public LocalFolder getFolder(long folderId) {
        return new LocalFolder(this, folderId);
    }

    public LocalFolder getFolder(String serverId, String name, FolderType type) {
        return new LocalFolder(this, serverId, name, type);
    }

    // TODO this takes about 260-300ms, seems slow.
    public List<LocalFolder> getPersonalNamespaces(boolean forceListAll) throws MessagingException {
        final List<LocalFolder> folders = new LinkedList<>();

        database.execute(false, new DbCallback<List<LocalFolder>>() {
            @Override
            public List<LocalFolder> doDbWork(final SQLiteDatabase db) throws MessagingException {
                Cursor cursor = null;

                try {
                    cursor = db.rawQuery("SELECT " + GET_FOLDER_COLS + " FROM folders " +
                            "ORDER BY name ASC", null);
                    while (cursor.moveToNext()) {
                        if (cursor.isNull(FOLDER_ID_INDEX)) {
                            continue;
                        }
                        long folderId = cursor.getLong(FOLDER_ID_INDEX);
                        LocalFolder folder = new LocalFolder(LocalStore.this, folderId);
                        folder.open(cursor);

                        folders.add(folder);
                    }
                    return folders;
                } finally {
                    Utility.closeQuietly(cursor);
                }
            }
        });

        return folders;
    }

    public void delete() {
        database.delete();
    }

    public void resetVisibleLimits(int visibleLimit) throws MessagingException {
        final ContentValues cv = new ContentValues();
        cv.put("visible_limit", Integer.toString(visibleLimit));
        cv.put("more_messages", MoreMessages.UNKNOWN.getDatabaseName());
        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) {
                db.update("folders", cv, null, null);
                return null;
            }
        });
    }

    public List<PendingCommand> getPendingCommands() throws MessagingException {
        return database.execute(false, new DbCallback<List<PendingCommand>>() {
            @Override
            public List<PendingCommand> doDbWork(final SQLiteDatabase db) {
                Cursor cursor = null;
                try {
                    cursor = db.query("pending_commands",
                                      new String[] { "id", "command", "data" },
                                      null,
                                      null,
                                      null,
                                      null,
                                      "id ASC");
                    List<PendingCommand> commands = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        long databaseId = cursor.getLong(0);
                        String commandName = cursor.getString(1);
                        String data = cursor.getString(2);
                        PendingCommand command = pendingCommandSerializer.unserialize(
                                databaseId, commandName, data);
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
        final ContentValues cv = new ContentValues();
        cv.put("command", command.getCommandName());
        cv.put("data", pendingCommandSerializer.serialize(command));
        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) {
                db.insert("pending_commands", "command", cv);
                return null;
            }
        });
    }

    public void removePendingCommand(final PendingCommand command) throws MessagingException {
        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) {
                db.delete("pending_commands", "id = ?", new String[] { Long.toString(command.databaseId) });
                return null;
            }
        });
    }

    public void removePendingCommands() throws MessagingException {
        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) {
                db.delete("pending_commands", null, null);
                return null;
            }
        });
    }

    public List<LocalMessage> searchForMessages(LocalSearch search) throws MessagingException {
        StringBuilder query = new StringBuilder();
        List<String> queryArgs = new ArrayList<>();
        SqlQueryBuilder.buildWhereClause(search.getConditions(), query, queryArgs);

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

        Timber.d("Query = %s", sqlQuery);

        return getMessages(null, sqlQuery, selectionArgs);
    }

    /*
     * Given a query string, actually do the query for the messages and
     * call the MessageRetrievalListener for each one
     */
    List<LocalMessage> getMessages(LocalFolder folder, String queryString, String[] placeHolders)
            throws MessagingException {
        final List<LocalMessage> messages = new ArrayList<>();
        database.execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) {
                Cursor cursor = null;
                try {
                    cursor = db.rawQuery(queryString + " LIMIT 10", placeHolders);

                    while (cursor.moveToNext()) {
                        LocalMessage message = new LocalMessage(LocalStore.this, null, folder);
                        message.populateFromGetMessageCursor(cursor);

                        messages.add(message);
                    }
                    cursor.close();
                    cursor = db.rawQuery(queryString + " LIMIT -1 OFFSET 10", placeHolders);

                    while (cursor.moveToNext()) {
                        LocalMessage message = new LocalMessage(LocalStore.this, null, folder);
                        message.populateFromGetMessageCursor(cursor);

                        messages.add(message);
                    }
                } catch (Exception e) {
                    Timber.d(e, "Got an exception");
                } finally {
                    Utility.closeQuietly(cursor);
                }

                return null;
            }
        });

        return Collections.unmodifiableList(messages);

    }

    public List<LocalMessage> getMessagesInThread(final long rootId) throws MessagingException {
        String rootIdString = Long.toString(rootId);

        LocalSearch search = new LocalSearch();
        search.and(SearchField.THREAD_ID, rootIdString, SearchAttribute.EQUALS);

        return searchForMessages(search);
    }

    public AttachmentInfo getAttachmentInfo(final String attachmentId) throws MessagingException {
        return database.execute(false, new DbCallback<AttachmentInfo>() {
            @Override
            public AttachmentInfo doDbWork(final SQLiteDatabase db) {
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
    public OpenPgpDataSource getAttachmentDataSource(final String partId) throws MessagingException {
        return new OpenPgpDataSource() {
            @Override
            public void writeTo(OutputStream os) throws IOException {
                writeAttachmentDataToOutputStream(partId, os);
            }
        };
    }

    private void writeAttachmentDataToOutputStream(final String partId, final OutputStream outputStream)
            throws IOException {
        try {
            database.execute(false, new DbCallback<Void>() {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws MessagingException {
                    Cursor cursor = db.query("message_parts",
                            GET_ATTACHMENT_COLS,
                            "id = ?", new String[] { partId },
                            null, null, null);
                    try {
                        writeCursorPartsToOutputStream(db, cursor, outputStream);
                    } catch (IOException e) {
                        throw new MessagingException(e);
                    } finally {
                        Utility.closeQuietly(cursor);
                    }

                    return null;
                }
            });
        } catch (MessagingException e) {
            throw new IOException("Got a MessagingException while writing attachment data!", e);
        }
    }

    private void writeCursorPartsToOutputStream(SQLiteDatabase db, Cursor cursor, OutputStream outputStream)
            throws IOException, MessagingException {
        while (cursor.moveToNext()) {
            String partId = cursor.getString(ATTACH_PART_ID_INDEX);
            int location = cursor.getInt(ATTACH_LOCATION_INDEX);

            if (location == DataLocation.IN_DATABASE || location == DataLocation.ON_DISK) {
                writeSimplePartToOutputStream(partId, cursor, outputStream);
            } else if (location == DataLocation.CHILD_PART_CONTAINS_DATA) {
                writeRawBodyToStream(cursor, db, outputStream);
            }
        }
    }

    private void writeRawBodyToStream(Cursor cursor, SQLiteDatabase db, OutputStream outputStream)
            throws IOException, MessagingException {
        long partId = cursor.getLong(ATTACH_PART_ID_INDEX);
        String rootPart = cursor.getString(ATTACH_ROOT_INDEX);
        LocalMessage message = loadLocalMessageByRootPartId(db, rootPart);

        if (message == null) {
            throw new MessagingException("Unable to find message for attachment!");
        }

        Part part = findPartById(message, partId);
        if (part == null) {
            throw new MessagingException("Unable to find attachment part in associated message (db integrity error?)");
        }

        Body body = part.getBody();
        if (body == null) {
            throw new MessagingException("Attachment part isn't available!");
        }

        body.writeTo(outputStream);
    }

    static Part findPartById(Part searchRoot, long partId) {
        if (searchRoot instanceof LocalMessage) {
            LocalMessage localMessage = (LocalMessage) searchRoot;
            if (localMessage.getMessagePartId() == partId) {
                return localMessage;
            }
        }

        Stack<Part> partStack = new Stack<>();
        partStack.add(searchRoot);

        while (!partStack.empty()) {
            Part part = partStack.pop();

            if (part instanceof LocalPart) {
                LocalPart localBodyPart = (LocalPart) part;
                if (localBodyPart.getPartId() == partId) {
                    return part;
                }
            }

            Body body = part.getBody();
            if (body instanceof Multipart) {
                Multipart innerMultipart = (Multipart) body;
                for (BodyPart innerPart : innerMultipart.getBodyParts()) {
                    partStack.add(innerPart);
                }
            }

            if (body instanceof Part) {
                partStack.add((Part) body);
            }
        }

        return null;
    }

    private LocalMessage loadLocalMessageByRootPartId(SQLiteDatabase db, String rootPart) throws MessagingException {
        Cursor cursor = db.query("messages",
                new String[] { "id" },
                "message_part_id = ?", new String[] { rootPart },
                null, null, null);
        long messageId;
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }

            messageId = cursor.getLong(0);
        } finally {
            Utility.closeQuietly(cursor);
        }

        return loadLocalMessageByMessageId(messageId);
    }

    @Nullable
    private LocalMessage loadLocalMessageByMessageId(long messageId) throws MessagingException {
        Map<Long, List<String>> folderIdsAndUids = getFolderIdsAndUids(Collections.singletonList(messageId), false);
        if (folderIdsAndUids.isEmpty()) {
            return null;
        }

        Map.Entry<Long, List<String>> entry = folderIdsAndUids.entrySet().iterator().next();
        long folderId = entry.getKey();
        String uid = entry.getValue().get(0);

        LocalFolder folder = getFolder(folderId);
        LocalMessage localMessage = folder.getMessage(uid);

        FetchProfile fp = new FetchProfile();
        fp.add(Item.BODY);
        folder.fetch(Collections.singletonList(localMessage), fp, null);

        return localMessage;
    }

    private void writeSimplePartToOutputStream(String partId, Cursor cursor, OutputStream outputStream)
            throws IOException {
        int location = cursor.getInt(ATTACH_LOCATION_INDEX);
        InputStream inputStream = getRawAttachmentInputStream(partId, location, cursor);

        try {
            String encoding = cursor.getString(ATTACH_ENCODING_INDEX);
            inputStream = getDecodingInputStream(inputStream, encoding);
            IOUtils.copy(inputStream, outputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private InputStream getRawAttachmentInputStream(String partId, int location, Cursor cursor)
            throws FileNotFoundException {
        switch (location) {
            case DataLocation.IN_DATABASE: {
                byte[] data = cursor.getBlob(ATTACH_DATA_INDEX);
                return new ByteArrayInputStream(data);
            }
            case DataLocation.ON_DISK: {
                File file = getAttachmentFile(partId);
                return new FileInputStream(file);
            }
            default:
                throw new IllegalStateException("unhandled case");
        }
    }

    InputStream getDecodingInputStream(final InputStream rawInputStream, @Nullable String encoding) {
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
                public void close() {
                    super.close();
                    try {
                        rawInputStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }

        return rawInputStream;
    }

    File getAttachmentFile(String attachmentId) {
        final StorageManager storageManager = StorageManager.getInstance(context);
        final File attachmentDirectory = storageManager.getAttachmentDirectory(
                account.getUuid(), database.getStorageProviderId());
        return new File(attachmentDirectory, attachmentId);
    }

    public static class AttachmentInfo {
        public String name;
        public long size;
        public String type;
    }

    public long createLocalFolder(String folderName, FolderType type) throws MessagingException {
        return database.execute(true, (DbCallback<Long>) db -> {
            ContentValues values = new ContentValues();
            values.put("name", folderName);
            values.putNull("server_id");
            values.put("local_only", 1);
            values.put("type", FolderTypeConverter.toDatabaseFolderType(type));
            values.put("visible_limit", 0);
            values.put("more_messages", MoreMessages.FALSE.getDatabaseName());
            values.put("visible", true);

            return db.insert("folders", null, values);
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

    AttachmentInfoExtractor getAttachmentInfoExtractor() {
        return attachmentInfoExtractor;
    }

    public void notifyChange() {
        MessageListRepository messageListRepository = DI.get(MessageListRepository.class);
        messageListRepository.notifyMessageListChanged(account.getUuid());
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
     */
    private void doBatchSetSelection(final BatchSetSelection selectionCallback, final int batchSize)
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

            database.execute(true, new DbCallback<Void>() {
                @Override
                public Void doDbWork(final SQLiteDatabase db) {

                    selectionCallback.doDbWork(db, selection.toString(),
                            selectionArgs.toArray(new String[selectionArgs.size()]));

                    return null;
                }
            });

            selectionCallback.postDbWork();

            selectionArgs.clear();
            start += count;
        }
    }

    /**
     * Defines the behavior of {@link LocalStore#doBatchSetSelection(BatchSetSelection, int)}.
     */
    interface BatchSetSelection {
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
         */
        void doDbWork(SQLiteDatabase db, String selectionSet, String[] selectionArgs);

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
            public void doDbWork(SQLiteDatabase db, String selectionSet, String[] selectionArgs) {

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
            public void doDbWork(SQLiteDatabase db, String selectionSet, String[] selectionArgs) {

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
     * Get folder ID and UID for the supplied messages.
     *
     * @param messageIds
     *         A list of primary keys in the "messages" table.
     * @param threadedList
     *         If this is {@code true}, {@code messageIds} contains the thread IDs of the messages
     *         at the root of a thread. In that case return UIDs for all messages in these threads.
     *         If this is {@code false} only the UIDs for messages in {@code messageIds} are
     *         returned.
     *
     * @return The list of UIDs for the messages grouped by folder ID.
     *
     */
    public Map<Long, List<String>> getFolderIdsAndUids(final List<Long> messageIds,
            final boolean threadedList) throws MessagingException {

        final Map<Long, List<String>> folderMap = new HashMap<>();

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
            public void doDbWork(SQLiteDatabase db, String selectionSet, String[] selectionArgs) {

                if (threadedList) {
                    String sql = "SELECT m.uid, m.folder_id " +
                            "FROM threads t " +
                            "LEFT JOIN messages m ON (t.message_id = m.id) " +
                            "WHERE m.empty = 0 AND m.deleted = 0 " +
                            "AND t.root" + selectionSet;

                    getDataFromCursor(db.rawQuery(sql, selectionArgs));

                } else {
                    String sql =
                            "SELECT uid, folder_id " +
                            "FROM messages " +
                            "WHERE empty = 0 AND id" + selectionSet;

                    getDataFromCursor(db.rawQuery(sql, selectionArgs));
                }
            }

            private void getDataFromCursor(Cursor cursor) {
                try {
                    while (cursor.moveToNext()) {
                        String uid = cursor.getString(0);
                        Long folderId = cursor.getLong(1);

                        List<String> uidList = folderMap.get(folderId);
                        if (uidList == null) {
                            uidList = new ArrayList<>();
                            folderMap.put(folderId, uidList);
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

    public List<NotificationMessage> getNotificationMessages() throws MessagingException {
        return database.execute(false, db -> {
            try (Cursor cursor = db.rawQuery(
                    "SELECT " + GET_MESSAGES_COLS + ", notifications.notification_id, notifications.timestamp " +
                            "FROM notifications " +
                            "JOIN messages ON (messages.id = notifications.message_id) " +
                            "LEFT JOIN threads ON (threads.message_id = messages.id) " +
                            "LEFT JOIN message_parts ON (message_parts.id = messages.message_part_id) " +
                            "LEFT JOIN folders ON (folders.id = messages.folder_id) " +
                            "ORDER BY notifications.timestamp DESC", null)
            ) {
                List<NotificationMessage> messages = new ArrayList<>(cursor.getCount());
                while (cursor.moveToNext()) {
                    long folderId = cursor.getLong(MSG_INDEX_FOLDER_ID);
                    LocalFolder folder = getFolder(folderId);
                    LocalMessage message = new LocalMessage(LocalStore.this, null, folder);
                    message.populateFromGetMessageCursor(cursor);

                    Integer notificationId = CursorKt.getIntOrNull(cursor, MSG_INDEX_NOTIFICATION_ID);
                    long notificationTimeStamp = cursor.getLong(MSG_INDEX_NOTIFICATION_TIMESTAMP);

                    messages.add(new NotificationMessage(message, notificationId, notificationTimeStamp));
                }

                return messages;
            }
        });
    }

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

    class RealMigrationsHelper implements MigrationsHelper {
        @Override
        public Account getAccount() {
            return LocalStore.this.getAccount();
        }

        @Override
        public void saveAccount() {
            getPreferences().saveAccount(account);
        }
    }
}
