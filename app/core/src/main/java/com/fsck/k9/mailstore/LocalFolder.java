package com.fsck.k9.mailstore;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.controller.MessageReference;
import com.fsck.k9.helper.FileHelper;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.BoundaryGenerator;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.FolderClass;
import com.fsck.k9.mail.FolderType;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.filter.CountingOutputStream;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.SizeAware;
import com.fsck.k9.mail.message.MessageHeaderParser;
import com.fsck.k9.mailstore.LockableDatabase.DbCallback;
import com.fsck.k9.message.extractors.AttachmentInfoExtractor;

import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.util.MimeUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;


public class LocalFolder {
    private static final int MAX_BODY_SIZE_FOR_DATABASE = 16 * 1024;
    private static final long INVALID_MESSAGE_PART_ID = -1;


    private final LocalStore localStore;
    private final AttachmentInfoExtractor attachmentInfoExtractor;


    private String status = null;
    private long lastChecked = 0;
    private FolderType type = FolderType.REGULAR;
    private String serverId = null;
    private String name;
    private long databaseId = -1L;
    private int visibleLimit = -1;

    private FolderClass displayClass = FolderClass.NO_CLASS;
    private FolderClass syncClass = FolderClass.INHERITED;
    private FolderClass pushClass = FolderClass.SECOND_CLASS;
    private FolderClass notifyClass = FolderClass.INHERITED;

    private boolean isInTopGroup = false;
    private boolean isIntegrate = false;

    private MoreMessages moreMessages = MoreMessages.UNKNOWN;
    private boolean localOnly = false;


    public LocalFolder(LocalStore localStore, String serverId) {
        this(localStore, serverId, null);
    }

    public LocalFolder(LocalStore localStore, String serverId, String name) {
        this(localStore, serverId, name, FolderType.REGULAR);
    }

    public LocalFolder(LocalStore localStore, String serverId, String name, FolderType type) {
        this.localStore = localStore;
        this.serverId = serverId;
        this.name = name;
        this.type = type;
        attachmentInfoExtractor = localStore.getAttachmentInfoExtractor();
    }

    public LocalFolder(LocalStore localStore, long databaseId) {
        super();
        this.localStore = localStore;
        this.databaseId = databaseId;
        attachmentInfoExtractor = localStore.getAttachmentInfoExtractor();
    }

    public FolderType getType() {
        return type;
    }

    public long getLastChecked() {
        return lastChecked;
    }

    public String getStatus() {
        return status;
    }

    public long getDatabaseId() {
        return databaseId;
    }

    public String getAccountUuid()
    {
        return getAccount().getUuid();
    }

    public boolean getSignatureUse() {
        return getAccount().getSignatureUse();
    }

    public void open() throws MessagingException {
        if (isOpen()) {
            return;
        }

        this.localStore.getDatabase().execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws MessagingException {
                Cursor cursor = null;
                try {
                    String baseQuery = "SELECT " + LocalStore.GET_FOLDER_COLS + " FROM folders ";

                    if (serverId != null) {
                        cursor = db.rawQuery(baseQuery + "where folders.server_id = ?", new String[] { serverId });
                    } else {
                        cursor = db.rawQuery(baseQuery + "where folders.id = ?", new String[] { Long.toString(
                                databaseId) });
                    }

                    if (cursor.moveToFirst() && !cursor.isNull(LocalStore.FOLDER_ID_INDEX)) {
                        open(cursor);
                    } else {
                        throw new MessagingException("LocalFolder.open(): Folder not found: " +
                                serverId + " (" + databaseId + ")", true);
                    }
                } finally {
                    Utility.closeQuietly(cursor);
                }
                return null;
            }
        });
    }

    void open(Cursor cursor) throws MessagingException {
        databaseId = cursor.getLong(LocalStore.FOLDER_ID_INDEX);
        serverId = cursor.getString(LocalStore.FOLDER_SERVER_ID_INDEX);
        visibleLimit = cursor.getInt(LocalStore.FOLDER_VISIBLE_LIMIT_INDEX);
        status = cursor.getString(LocalStore.FOLDER_STATUS_INDEX);
        // Only want to set the local variable stored in the super class.  This class
        // does a DB update on setLastChecked
        lastChecked = cursor.getLong(LocalStore.FOLDER_LAST_CHECKED_INDEX);
        isInTopGroup = cursor.getInt(LocalStore.FOLDER_TOP_GROUP_INDEX) == 1;
        isIntegrate = cursor.getInt(LocalStore.FOLDER_INTEGRATE_INDEX) == 1;
        String noClass = FolderClass.NO_CLASS.toString();
        String displayClass = cursor.getString(LocalStore.FOLDER_DISPLAY_CLASS_INDEX);
        this.displayClass = FolderClass.valueOf((displayClass == null) ? noClass : displayClass);
        String notifyClass = cursor.getString(LocalStore.FOLDER_NOTIFY_CLASS_INDEX);
        this.notifyClass = FolderClass.valueOf((notifyClass == null) ? noClass : notifyClass);
        String pushClass = cursor.getString(LocalStore.FOLDER_PUSH_CLASS_INDEX);
        this.pushClass = FolderClass.valueOf((pushClass == null) ? noClass : pushClass);
        String syncClass = cursor.getString(LocalStore.FOLDER_SYNC_CLASS_INDEX);
        this.syncClass = FolderClass.valueOf((syncClass == null) ? noClass : syncClass);
        String moreMessagesValue = cursor.getString(LocalStore.MORE_MESSAGES_INDEX);
        moreMessages = MoreMessages.fromDatabaseName(moreMessagesValue);
        name = cursor.getString(LocalStore.FOLDER_NAME_INDEX);
        localOnly = cursor.getInt(LocalStore.LOCAL_ONLY_INDEX) == 1;
        String typeString = cursor.getString(LocalStore.TYPE_INDEX);
        type = FolderTypeConverter.fromDatabaseFolderType(typeString);
    }

    public boolean isOpen() {
        return (databaseId != -1L && name != null);
    }

    public String getServerId() {
        return serverId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws MessagingException {
        open();

        if (name.equals(this.name)) {
            return;
        }

        this.name = name;
        updateFolderColumn("name", name);
    }

    public void setType(FolderType type) {
        this.type = type;
        try {
            updateFolderColumn("type", FolderTypeConverter.toDatabaseFolderType(type));
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean exists() throws MessagingException {
        return this.localStore.getDatabase().execute(false, new DbCallback<Boolean>() {
            @Override
            public Boolean doDbWork(final SQLiteDatabase db) {
                Cursor cursor = null;
                try {
                    cursor = db.rawQuery("SELECT id FROM folders where id = ?",
                            new String[] { Long.toString(getDatabaseId()) });
                    if (cursor.moveToFirst()) {
                        int folderId = cursor.getInt(0);
                        return (folderId > 0);
                    }

                    return false;
                } finally {
                    Utility.closeQuietly(cursor);
                }
            }
        });
    }

    public int getUnreadMessageCount() throws MessagingException {
        if (databaseId == -1L) {
            open();
        }

        return this.localStore.getDatabase().execute(false, new DbCallback<Integer>() {
            @Override
            public Integer doDbWork(final SQLiteDatabase db) {
                int unreadMessageCount = 0;
                Cursor cursor = db.query("messages", new String[] { "COUNT(id)" },
                        "folder_id = ? AND empty = 0 AND deleted = 0 AND read=0",
                        new String[] { Long.toString(databaseId) }, null, null, null);

                try {
                    if (cursor.moveToFirst()) {
                        unreadMessageCount = cursor.getInt(0);
                    }
                } finally {
                    cursor.close();
                }

                return unreadMessageCount;
            }
        });
    }

    public int getVisibleLimit() throws MessagingException {
        open();
        return visibleLimit;
    }

    public void setVisibleLimit(final int visibleLimit) throws MessagingException {
        updateMoreMessagesOnVisibleLimitChange(visibleLimit, this.visibleLimit);

        this.visibleLimit = visibleLimit;
        updateFolderColumn("visible_limit", this.visibleLimit);
    }

    private void updateMoreMessagesOnVisibleLimitChange(int newVisibleLimit, int oldVisibleLimit)
            throws MessagingException {

        boolean growVisibleLimit = newVisibleLimit > oldVisibleLimit;
        boolean shrinkVisibleLimit = newVisibleLimit < oldVisibleLimit;
        boolean moreMessagesWereAvailable = getMoreMessages() == MoreMessages.TRUE;

        if (growVisibleLimit || (shrinkVisibleLimit && !moreMessagesWereAvailable)) {
            setMoreMessages(MoreMessages.UNKNOWN);
        }
    }

    public void setStatus(final String status) throws MessagingException {
        this.status = status;
        updateFolderColumn("status", status);
    }

    private void updateFolderColumn(final String column, final Object value) throws MessagingException {
        this.localStore.getDatabase().execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws MessagingException {
                open();
                db.execSQL("UPDATE folders SET " + column + " = ? WHERE id = ?", new Object[] { value, databaseId });
                return null;
            }
        });
    }

    public FolderClass getDisplayClass() {
        return displayClass;
    }

    public FolderClass getSyncClass() {
        return (FolderClass.INHERITED == syncClass) ? getDisplayClass() : syncClass;
    }

    public FolderClass getNotifyClass() {
        return (FolderClass.INHERITED == notifyClass) ? getPushClass() : notifyClass;
    }

    public FolderClass getPushClass() {
        return (FolderClass.INHERITED == pushClass) ? getSyncClass() : pushClass;
    }

    public void setDisplayClass(FolderClass displayClass) throws MessagingException {
        this.displayClass = displayClass;
        updateFolderColumn("display_class", this.displayClass.name());
    }

    public void setSyncClass(FolderClass syncClass) throws MessagingException {
        this.syncClass = syncClass;
        updateFolderColumn("poll_class", this.syncClass.name());
    }

    public void setPushClass(FolderClass pushClass) throws MessagingException {
        this.pushClass = pushClass;
        updateFolderColumn("push_class", this.pushClass.name());
    }

    public void setNotifyClass(FolderClass notifyClass) throws MessagingException {
        this.notifyClass = notifyClass;
        updateFolderColumn("notify_class", this.notifyClass.name());
    }

    public boolean isIntegrate() {
        return isIntegrate;
    }

    public void setIntegrate(boolean integrate) throws MessagingException {
        isIntegrate = integrate;
        updateFolderColumn("integrate", isIntegrate ? 1 : 0);
    }

    public boolean hasMoreMessages() {
        return moreMessages != MoreMessages.FALSE;
    }

    public MoreMessages getMoreMessages() {
        return moreMessages;
    }

    public void setMoreMessages(MoreMessages moreMessages) throws MessagingException {
        this.moreMessages = moreMessages;
        updateFolderColumn("more_messages", moreMessages.getDatabaseName());
    }

    public boolean isLocalOnly() {
        return localOnly;
    }

    public void fetch(final List<LocalMessage> messages, final FetchProfile fp, final MessageRetrievalListener<LocalMessage> listener)
    throws MessagingException {
        this.localStore.getDatabase().execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws MessagingException {
                open();
                if (fp.contains(FetchProfile.Item.BODY)) {
                    for (LocalMessage message : messages) {
                        loadMessageParts(db, message);
                    }
                }
                return null;
            }
        });
    }

    private void loadMessageParts(SQLiteDatabase db, LocalMessage message) throws MessagingException {
        Map<Long, Part> partById = new HashMap<>();

        String[] columns = {
                "id",                   // 0
                "type",                 // 1
                "parent",               // 2
                "mime_type",            // 3
                "decoded_body_size",    // 4
                "display_name",         // 5
                "header",               // 6
                "encoding",             // 7
                "charset",              // 8
                "data_location",        // 9
                "data",                 // 10
                "preamble",             // 11
                "epilogue",             // 12
                "boundary",             // 13
                "content_id",           // 14
                "server_extra",         // 15
        };
        Cursor cursor = db.query("message_parts", columns, "root = ?",
                new String[] { String.valueOf(message.getMessagePartId()) }, null, null, "seq");
        try {
            while (cursor.moveToNext()) {
                loadMessagePart(message, partById, cursor);
            }
        } finally {
            cursor.close();
        }
    }

    private void loadMessagePart(LocalMessage message, Map<Long, Part> partById, Cursor cursor)
            throws MessagingException {

        long id = cursor.getLong(0);
        long parentId = cursor.getLong(2);
        String mimeType = cursor.getString(3);
        long size = cursor.getLong(4);
        byte[] header = cursor.getBlob(6);
        int dataLocation = cursor.getInt(9);
        String serverExtra = cursor.getString(15);
        // TODO we don't currently cache much of the part data which is computed with AttachmentInfoExtractor,
        // TODO might want to do that at a later point?
        // String displayName = cursor.getString(5);
        // int type = cursor.getInt(1);
        // boolean inlineAttachment = (type == MessagePartType.HIDDEN_ATTACHMENT);

        final Part part;
        if (id == message.getMessagePartId()) {
            part = message;
        } else {
            Part parentPart = partById.get(parentId);
            if (parentPart == null) {
                throw new IllegalStateException("Parent part not found");
            }

            String parentMimeType = parentPart.getMimeType();
            if (MimeUtility.isMultipart(parentMimeType)) {
                BodyPart bodyPart = new LocalBodyPart(getAccountUuid(), message, id, size);
                ((Multipart) parentPart.getBody()).addBodyPart(bodyPart);
                part = bodyPart;
            } else if (MimeUtility.isMessage(parentMimeType)) {
                Message innerMessage = new LocalMimeMessage(getAccountUuid(), message, id);
                parentPart.setBody(innerMessage);
                part = innerMessage;
            } else {
                throw new IllegalStateException("Parent is neither a multipart nor a message");
            }

            parseHeaderBytes(part, header);
        }
        partById.put(id, part);
        part.setServerExtra(serverExtra);

        if (MimeUtility.isMultipart(mimeType)) {
            byte[] preamble = cursor.getBlob(11);
            byte[] epilogue = cursor.getBlob(12);
            String boundary = cursor.getString(13);

            MimeMultipart multipart = new MimeMultipart(mimeType, boundary);
            part.setBody(multipart);
            multipart.setPreamble(preamble);
            multipart.setEpilogue(epilogue);
        } else if (dataLocation == DataLocation.IN_DATABASE) {
            String encoding = cursor.getString(7);
            byte[] data = cursor.getBlob(10);

            Body body = new BinaryMemoryBody(data, encoding);
            part.setBody(body);
        } else if (dataLocation == DataLocation.ON_DISK) {
            String encoding = cursor.getString(7);

            File file = localStore.getAttachmentFile(Long.toString(id));
            if (file.exists()) {
                Body body = new FileBackedBody(file, encoding);
                part.setBody(body);
            }
        }
    }

    private void parseHeaderBytes(Part part, byte[] header) throws MessagingException {
        MessageHeaderParser.parse(new ByteArrayInputStream(header), part::addRawHeader);
    }

    public String getMessageUidById(final long id) throws MessagingException {
        return this.localStore.getDatabase().execute(false, new DbCallback<String>() {
            @Override
            public String doDbWork(final SQLiteDatabase db) throws MessagingException {
                open();
                Cursor cursor = null;

                try {
                    cursor = db.rawQuery(
                            "SELECT uid FROM messages WHERE id = ? AND folder_id = ?",
                            new String[] { Long.toString(id), Long.toString(LocalFolder.this.databaseId) });
                    if (!cursor.moveToNext()) {
                        return null;
                    }
                    return cursor.getString(0);
                } finally {
                    Utility.closeQuietly(cursor);
                }
            }
        });
    }

    public LocalMessage getMessage(final String uid) throws MessagingException {
        return this.localStore.getDatabase().execute(false, new DbCallback<LocalMessage>() {
            @Override
            public LocalMessage doDbWork(final SQLiteDatabase db) throws MessagingException {
                open();
                LocalMessage message = new LocalMessage(LocalFolder.this.localStore, uid, LocalFolder.this);
                Cursor cursor = null;

                try {
                    cursor = db.rawQuery(
                            "SELECT " +
                            LocalStore.GET_MESSAGES_COLS +
                            "FROM messages " +
                            "LEFT JOIN message_parts ON (message_parts.id = messages.message_part_id) " +
                            "LEFT JOIN threads ON (threads.message_id = messages.id) " +
                            "WHERE uid = ? AND folder_id = ?",
                            new String[] { message.getUid(), Long.toString(databaseId) });

                    if (!cursor.moveToNext()) {
                        return null;
                    }
                    message.populateFromGetMessageCursor(cursor);
                } finally {
                    Utility.closeQuietly(cursor);
                }
                return message;
            }
        });
    }

    @Nullable
    public LocalMessage getMessage(long messageId) throws MessagingException {
        return localStore.getDatabase().execute(false, db -> {
            open();
            LocalMessage message = new LocalMessage(localStore, messageId, LocalFolder.this);

            Cursor cursor = db.rawQuery(
                    "SELECT " +
                            LocalStore.GET_MESSAGES_COLS +
                            "FROM messages " +
                            "LEFT JOIN message_parts ON (message_parts.id = messages.message_part_id) " +
                            "LEFT JOIN threads ON (threads.message_id = messages.id) " +
                            "WHERE messages.id = ? AND folder_id = ?",
                    new String[] { Long.toString(messageId), Long.toString(databaseId) });
            try {
                if (cursor.moveToNext()) {
                    message.populateFromGetMessageCursor(cursor);
                } else {
                    return null;
                }
            } finally {
                Utility.closeQuietly(cursor);
            }

            return message;
        });
    }

    public List<LocalMessage> getMessages(MessageRetrievalListener<LocalMessage> listener) throws MessagingException {
        return getMessages(listener, true);
    }

    public List<LocalMessage> getMessages(final MessageRetrievalListener<LocalMessage> listener,
            final boolean includeDeleted) throws MessagingException {
        return localStore.getDatabase().execute(false, new DbCallback<List<LocalMessage>>() {
            @Override
            public List<LocalMessage> doDbWork(final SQLiteDatabase db) throws MessagingException {
                open();
                return LocalFolder.this.localStore.getMessages(listener, LocalFolder.this,
                        "SELECT " + LocalStore.GET_MESSAGES_COLS +
                        "FROM messages " +
                        "LEFT JOIN message_parts ON (message_parts.id = messages.message_part_id) " +
                        "LEFT JOIN threads ON (threads.message_id = messages.id) " +
                        "WHERE empty = 0 AND " +
                        (includeDeleted ? "" : "deleted = 0 AND ") +
                        "folder_id = ? ORDER BY date DESC",
                        new String[] { Long.toString(databaseId) });
            }
        });
    }

    public List<LocalMessage> getMessagesByUids(@NonNull List<String> uids) throws MessagingException {
        open();
        List<LocalMessage> messages = new ArrayList<>();
        for (String uid : uids) {
            LocalMessage message = getMessage(uid);
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    public List<LocalMessage> getMessagesByReference(@NonNull List<MessageReference> messageReferences)
            throws MessagingException {
        open();

        String accountUuid = getAccountUuid();
        long folderId = getDatabaseId();

        List<LocalMessage> messages = new ArrayList<>();
        for (MessageReference messageReference : messageReferences) {
            if (!accountUuid.equals(messageReference.getAccountUuid())) {
                throw new IllegalArgumentException("all message references must belong to this Account!");
            }
            if (folderId != messageReference.getFolderId()) {
                throw new IllegalArgumentException("all message references must belong to this LocalFolder!");
            }

            LocalMessage message = getMessage(messageReference.getUid());
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    public void destroyMessages(final List<LocalMessage> messages) throws MessagingException {
        this.localStore.getDatabase().execute(true, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws MessagingException {
                for (LocalMessage message : messages) {
                    message.destroy();
                }
                return null;
            }
        });
    }

    private void moveTemporaryFile(File tempFile, String messagePartId) throws IOException {
        File destinationFile = localStore.getAttachmentFile(messagePartId);
        FileHelper.renameOrMoveByCopying(tempFile, destinationFile);
    }

    private long updateOrInsertMessagePart(SQLiteDatabase db, ContentValues cv, Part part, long existingMessagePartId)
            throws IOException, MessagingException {
        byte[] headerBytes = getHeaderBytes(part);

        cv.put("mime_type", part.getMimeType());
        cv.put("header", headerBytes);
        cv.put("type", MessagePartType.UNKNOWN);

        File file = null;
        Body body = part.getBody();
        if (body instanceof Multipart) {
            multipartToContentValues(cv, (Multipart) body);
        } else if (body == null) {
            missingPartToContentValues(cv, part);
        } else if (body instanceof Message) {
            messageMarkerToContentValues(cv);
        } else {
            file = leafPartToContentValues(cv, part, body);
        }

        long messagePartId;
        if (existingMessagePartId != INVALID_MESSAGE_PART_ID) {
            messagePartId = existingMessagePartId;
            db.update("message_parts", cv, "id = ?", new String[] { Long.toString(messagePartId) });
        } else {
            messagePartId = db.insertOrThrow("message_parts", null, cv);
        }

        if (file != null) {
            moveTemporaryFile(file, Long.toString(messagePartId));
        }

        return messagePartId;
    }

    private void multipartToContentValues(ContentValues cv, Multipart multipart) {
        cv.put("data_location", DataLocation.CHILD_PART_CONTAINS_DATA);
        cv.put("preamble", multipart.getPreamble());
        cv.put("epilogue", multipart.getEpilogue());
        cv.put("boundary", multipart.getBoundary());
    }

    private void missingPartToContentValues(ContentValues cv, Part part) throws MessagingException {
        AttachmentViewInfo attachment = attachmentInfoExtractor.extractAttachmentInfoForDatabase(part);
        cv.put("display_name", attachment.displayName);
        cv.put("data_location", DataLocation.MISSING);
        cv.put("decoded_body_size", attachment.size);

        if (MimeUtility.isMultipart(part.getMimeType())) {
            cv.put("boundary", BoundaryGenerator.getInstance().generateBoundary());
        }
    }

    private void messageMarkerToContentValues(ContentValues cv) {
        cv.put("data_location", DataLocation.CHILD_PART_CONTAINS_DATA);
    }

    private File leafPartToContentValues(ContentValues cv, Part part, Body body)
            throws MessagingException, IOException {
        AttachmentViewInfo attachment = attachmentInfoExtractor.extractAttachmentInfoForDatabase(part);
        cv.put("display_name", attachment.displayName);

        String encoding = getTransferEncoding(part);

        if (!(body instanceof SizeAware)) {
            throw new IllegalStateException("Body needs to implement SizeAware");
        }

        SizeAware sizeAwareBody = (SizeAware) body;
        long fileSize = sizeAwareBody.getSize();

        File file = null;
        int dataLocation;
        if (fileSize > MAX_BODY_SIZE_FOR_DATABASE) {
            dataLocation = DataLocation.ON_DISK;

            file = writeBodyToDiskIfNecessary(part);

            long size = decodeAndCountBytes(file, encoding, fileSize);
            cv.put("decoded_body_size", size);
        } else {
            dataLocation = DataLocation.IN_DATABASE;

            byte[] bodyData = getBodyBytes(body);
            cv.put("data", bodyData);

            long size = decodeAndCountBytes(bodyData, encoding, bodyData.length);
            cv.put("decoded_body_size", size);
        }
        cv.put("data_location", dataLocation);
        cv.put("encoding", encoding);
        cv.put("content_id", part.getContentId());

        return file;
    }

    private File writeBodyToDiskIfNecessary(Part part) throws MessagingException, IOException {
        Body body = part.getBody();
        if (body instanceof BinaryTempFileBody) {
            return ((BinaryTempFileBody) body).getFile();
        } else {
            return writeBodyToDisk(body);
        }
    }

    private File writeBodyToDisk(Body body) throws IOException, MessagingException {
        File file = File.createTempFile("body", null, BinaryTempFileBody.getTempDirectory());
        OutputStream out = new FileOutputStream(file);
        try {
            body.writeTo(out);
        } finally {
            out.close();
        }

        return file;
    }

    private long decodeAndCountBytes(byte[] bodyData, String encoding, long fallbackValue) {
        ByteArrayInputStream rawInputStream = new ByteArrayInputStream(bodyData);
        return decodeAndCountBytes(rawInputStream, encoding, fallbackValue);
    }

    private long decodeAndCountBytes(File file, String encoding, long fallbackValue)
            throws IOException {
        InputStream inputStream = new FileInputStream(file);
        try {
            return decodeAndCountBytes(inputStream, encoding, fallbackValue);
        } finally {
            inputStream.close();
        }
    }

    private long decodeAndCountBytes(InputStream rawInputStream, String encoding, long fallbackValue) {
        InputStream decodingInputStream = localStore.getDecodingInputStream(rawInputStream, encoding);
        try {
            CountingOutputStream countingOutputStream = new CountingOutputStream();
            try {
                IOUtils.copy(decodingInputStream, countingOutputStream);

                return countingOutputStream.getCount();
            } catch (IOException e) {
                return fallbackValue;
            }
        } finally {
            try {
                decodingInputStream.close();
            } catch (IOException e) { /* ignore */ }
        }
    }

    private byte[] getHeaderBytes(Part part) throws IOException, MessagingException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        part.writeHeaderTo(output);
        return output.toByteArray();
    }

    private byte[] getBodyBytes(Body body) throws IOException, MessagingException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        body.writeTo(output);
        return output.toByteArray();
    }

    private String getTransferEncoding(Part part) {
        String[] contentTransferEncoding = part.getHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING);
        if (contentTransferEncoding.length > 0) {
            return contentTransferEncoding[0].toLowerCase(Locale.US);
        }

        return MimeUtil.ENC_7BIT;
    }

    public void addPartToMessage(final LocalMessage message, final Part part) throws MessagingException {
        open();

        localStore.getDatabase().execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) {
                long messagePartId;

                Cursor cursor = db.query("message_parts", new String[] { "id" }, "root = ? AND server_extra = ?",
                        new String[] { Long.toString(message.getMessagePartId()), part.getServerExtra() },
                        null, null, null);
                try {
                    if (!cursor.moveToFirst()) {
                        throw new IllegalStateException("Message part not found");
                    }

                    messagePartId = cursor.getLong(0);
                } finally {
                    cursor.close();
                }

                try {
                    updateOrInsertMessagePart(db, new ContentValues(), part, messagePartId);
                } catch (Exception e) {
                    Timber.e(e, "Error writing message part");
                }

                return null;
            }
        });

        localStore.notifyChange();
    }

    /**
     * Changes the stored uid of the given message (using it's internal id as a key) to
     * the uid in the message.
     */
    public void changeUid(final LocalMessage message) throws MessagingException {
        open();
        final ContentValues cv = new ContentValues();
        cv.put("uid", message.getUid());
        this.localStore.getDatabase().execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) {
                db.update("messages", cv, "id = ?", new String[]
                        { Long.toString(message.getDatabaseId()) });
                return null;
            }
        });

        //TODO: remove this once the UI code exclusively uses the database id
        this.localStore.notifyChange();
    }

    public void setFlags(final List<LocalMessage> messages, final Set<Flag> flags, final boolean value)
    throws MessagingException {
        open();

        // Use one transaction to set all flags
        this.localStore.getDatabase().execute(true, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) {

                for (LocalMessage message : messages) {
                    try {
                        message.setFlags(flags, value);
                    } catch (MessagingException e) {
                        Timber.e(e, "Something went wrong while setting flag");
                    }
                }

                return null;
            }
        });
    }

    public void setFlags(final Set<Flag> flags, boolean value)
    throws MessagingException {
        open();
        for (LocalMessage message : getMessages(null)) {
            message.setFlags(flags, value);
        }
    }

    public void clearAllMessages() throws MessagingException {
        final String[] folderIdArg = new String[] { Long.toString(databaseId) };

        open();

        this.localStore.getDatabase().execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws MessagingException {
                Cursor cursor = db.query("messages", new String[] { "message_part_id" },
                        "folder_id = ? AND empty = 0",
                        folderIdArg, null, null, null);
                try {
                    while (cursor.moveToNext()) {
                        long messagePartId = cursor.getLong(0);
                        deleteMessageDataFromDisk(messagePartId);
                    }
                } finally {
                    cursor.close();
                }

                db.execSQL("DELETE FROM threads WHERE message_id IN " +
                        "(SELECT id FROM messages WHERE folder_id = ?)", folderIdArg);
                db.execSQL("DELETE FROM messages WHERE folder_id = ?", folderIdArg);

                setMoreMessages(MoreMessages.UNKNOWN);
                resetLastChecked(db);

                return null;
            }
        });

        this.localStore.notifyChange();

        setVisibleLimit(getAccount().getDisplayCount());
    }

    private void resetLastChecked(SQLiteDatabase db) {
        lastChecked = 0;

        ContentValues values = new ContentValues();
        values.putNull("last_updated");
        db.update("folders", values, "id = ?", new String[] { Long.toString(databaseId) });
    }

    public void destroyLocalOnlyMessages() throws MessagingException {
        destroyMessages("uid LIKE '" + K9.LOCAL_UID_PREFIX + "%'");
    }

    public void destroyDeletedMessages() throws MessagingException {
        destroyMessages("empty = 0 AND deleted = 1");
    }

    private void destroyMessages(String messageSelection) throws MessagingException {
        localStore.getDatabase().execute(false, (DbCallback<Void>) db -> {
            try (Cursor cursor = db.query(
                    "messages",
                    new String[] { "id", "message_part_id", "message_id" },
                    "folder_id = ? AND " + messageSelection,
                    new String[] { Long.toString(databaseId) },
                    null,
                    null,
                    null)
            ) {
                while (cursor.moveToNext()) {
                    long messageId = cursor.getLong(0);
                    long messagePartId = cursor.getLong(1);
                    String messageIdHeader = cursor.getString(2);
                    destroyMessage(messageId, messagePartId, messageIdHeader);
                }
            }

            compactFulltextEntries(db);

            return null;
        });
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LocalFolder) {
            return ((LocalFolder)o).databaseId == databaseId;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        long value = databaseId;
        return (int) (value ^ (value >>> 32));
    }

    void destroyMessage(LocalMessage localMessage) throws MessagingException {
        destroyMessage(localMessage.getDatabaseId(), localMessage.getMessagePartId(), localMessage.getMessageId());
    }

    private void destroyMessage(final long messageId, final long messagePartId, final String messageIdHeader)
            throws MessagingException {
        localStore.getDatabase().execute(true, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws MessagingException {
                deleteMessagePartsAndDataFromDisk(messagePartId);

                deleteFulltextIndexEntry(db, messageId);

                if (hasThreadChildren(db, messageId)) {
                    // This message has children in the thread structure so we need to
                    // make it an empty message.
                    ContentValues cv = new ContentValues();
                    cv.put("id", messageId);
                    cv.put("folder_id", getDatabaseId());
                    cv.put("deleted", 0);
                    cv.put("message_id", messageIdHeader);
                    cv.put("empty", 1);

                    db.replace("messages", null, cv);

                    // Nothing else to do
                    return null;
                }

                // Get the message ID of the parent message if it's empty
                long currentId = getEmptyThreadParent(db, messageId);

                // Delete the placeholder message
                deleteMessageRow(db, messageId);

                /*
                 * Walk the thread tree to delete all empty parents without children
                 */

                while (currentId != -1) {
                    if (hasThreadChildren(db, currentId)) {
                        // We made sure there are no empty leaf nodes and can stop now.
                        break;
                    }

                    // Get ID of the (empty) parent for the next iteration
                    long newId = getEmptyThreadParent(db, currentId);

                    // Delete the empty message
                    deleteMessageRow(db, currentId);

                    currentId = newId;
                }

                return null;
            }
        });

        localStore.notifyChange();
    }

    /**
     * Check whether or not a message has child messages in the thread structure.
     *
     * @param db
     *         {@link SQLiteDatabase} instance to access the database.
     * @param messageId
     *         The database ID of the message to get the children for.
     *
     * @return {@code true} if the message has children. {@code false} otherwise.
     */
    private boolean hasThreadChildren(SQLiteDatabase db, long messageId) {
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(t2.id) " +
                        "FROM threads t1 " +
                        "JOIN threads t2 ON (t2.parent = t1.id) " +
                        "WHERE t1.message_id = ?",
                new String[] { Long.toString(messageId) });

        try {
            return (cursor.moveToFirst() && !cursor.isNull(0) && cursor.getLong(0) > 0L);
        } finally {
            cursor.close();
        }
    }

    /**
     * Get ID of the the given message's parent if the parent is an empty message.
     *
     * @param db
     *         {@link SQLiteDatabase} instance to access the database.
     * @param messageId
     *         The database ID of the message to get the parent for.
     *
     * @return Message ID of the parent message if there exists a parent and it is empty.
     *         Otherwise {@code -1}.
     */
    private long getEmptyThreadParent(SQLiteDatabase db, long messageId) {
        Cursor cursor = db.rawQuery(
                "SELECT m.id " +
                        "FROM threads t1 " +
                        "JOIN threads t2 ON (t1.parent = t2.id) " +
                        "LEFT JOIN messages m ON (t2.message_id = m.id) " +
                        "WHERE t1.message_id = ? AND m.empty = 1",
                new String[] { Long.toString(messageId) });

        try {
            return (cursor.moveToFirst() && !cursor.isNull(0)) ? cursor.getLong(0) : -1;
        } finally {
            cursor.close();
        }
    }

    private void deleteMessageRow(SQLiteDatabase db, long messageId) {
        db.delete("messages", "id = ?", new String[] { Long.toString(messageId) });
    }

    void deleteFulltextIndexEntry(SQLiteDatabase db, long messageId) {
        String[] idArg = { Long.toString(messageId) };
        db.delete("messages_fulltext", "docid = ?", idArg);
    }

    void compactFulltextEntries(SQLiteDatabase db) {
        db.execSQL("INSERT INTO messages_fulltext(messages_fulltext) VALUES('optimize')");
    }

    void deleteMessagePartsAndDataFromDisk(final long rootMessagePartId) throws MessagingException {
        deleteMessageDataFromDisk(rootMessagePartId);
        deleteMessageParts(rootMessagePartId);
    }

    private void deleteMessageParts(final long rootMessagePartId) throws MessagingException {
        localStore.getDatabase().execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) {
                db.delete("message_parts", "root = ?", new String[] { Long.toString(rootMessagePartId) });
                return null;
            }
        });
    }

    private void deleteMessageDataFromDisk(final long rootMessagePartId) throws MessagingException {
        localStore.getDatabase().execute(false, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) {
                deleteMessagePartsFromDisk(db, rootMessagePartId);
                return null;
            }
        });
    }

    private void deleteMessagePartsFromDisk(SQLiteDatabase db, long rootMessagePartId) {
        Cursor cursor = db.query("message_parts", new String[] { "id" },
                "root = ? AND data_location = " + DataLocation.ON_DISK,
                new String[] { Long.toString(rootMessagePartId) }, null, null, null);
        try {
            while (cursor.moveToNext()) {
                String messagePartId = cursor.getString(0);
                File file = localStore.getAttachmentFile(messagePartId);
                if (file.exists()) {
                    if (!file.delete() && K9.isDebugLoggingEnabled()) {
                        Timber.d("Couldn't delete message part file: %s", file.getAbsolutePath());
                    }
                }
            }
        } finally {
            cursor.close();
        }
    }

    public boolean isInTopGroup() {
        return isInTopGroup;
    }

    public void setInTopGroup(boolean inTopGroup) throws MessagingException {
        isInTopGroup = inTopGroup;
        updateFolderColumn("top_group", isInTopGroup ? 1 : 0);
    }

    public List<String> extractNewMessages(final List<String> messageServerIds)
            throws MessagingException {

        return this.localStore.getDatabase().execute(false, new DbCallback<List<String>>() {
            @Override
            public List<String> doDbWork(final SQLiteDatabase db) throws MessagingException {
                open();

                List<String> result = new ArrayList<>();

                List<String> selectionArgs = new ArrayList<>();
                Set<String> existingMessages = new HashSet<>();
                int start = 0;

                while (start < messageServerIds.size()) {
                    StringBuilder selection = new StringBuilder();

                    selection.append("folder_id = ? AND UID IN (");
                    selectionArgs.add(Long.toString(databaseId));

                    int count = Math.min(messageServerIds.size() - start, LocalStore.UID_CHECK_BATCH_SIZE);

                    for (int i = start, end = start + count; i < end; i++) {
                        if (i > start) {
                            selection.append(",?");
                        } else {
                            selection.append("?");
                        }

                        selectionArgs.add(messageServerIds.get(i));
                    }

                    selection.append(")");

                    Cursor cursor = db.query("messages", LocalStore.UID_CHECK_PROJECTION,
                            selection.toString(), selectionArgs.toArray(LocalStore.EMPTY_STRING_ARRAY),
                            null, null, null);

                    try {
                        while (cursor.moveToNext()) {
                            String uid = cursor.getString(0);
                            existingMessages.add(uid);
                        }
                    } finally {
                        Utility.closeQuietly(cursor);
                    }

                    for (int i = start, end = start + count; i < end; i++) {
                        String messageServerId = messageServerIds.get(i);
                        if (!existingMessages.contains(messageServerId)) {
                            result.add(messageServerId);
                        }
                    }

                    existingMessages.clear();
                    selectionArgs.clear();
                    start += count;
                }

                return result;
            }
        });
    }

    private Account getAccount() {
        return localStore.getAccount();
    }

    // Note: The contents of the 'message_parts' table depend on these values.
    // TODO currently unused, might be for caching at a later point
    private static class MessagePartType {
        static final int UNKNOWN = 0;
        static final int ALTERNATIVE_PLAIN = 1;
        static final int ALTERNATIVE_HTML = 2;
        static final int TEXT = 3;
        static final int RELATED = 4;
        static final int ATTACHMENT = 5;
        static final int HIDDEN_ATTACHMENT = 6;
    }

    // Note: The contents of the 'message_parts' table depend on these values.
    static class DataLocation {
        static final int MISSING = 0;
        static final int IN_DATABASE = 1;
        static final int ON_DISK = 2;
        static final int CHILD_PART_CONTAINS_DATA = 3;
    }

    public static boolean isModeMismatch(Account.FolderMode aMode, FolderClass fMode) {
        return aMode == Account.FolderMode.NONE
                || (aMode == Account.FolderMode.FIRST_CLASS &&
                fMode != FolderClass.FIRST_CLASS)
                || (aMode == Account.FolderMode.FIRST_AND_SECOND_CLASS &&
                fMode != FolderClass.FIRST_CLASS &&
                fMode != FolderClass.SECOND_CLASS)
                || (aMode == Account.FolderMode.NOT_SECOND_CLASS &&
                fMode == FolderClass.SECOND_CLASS);
    }
}
