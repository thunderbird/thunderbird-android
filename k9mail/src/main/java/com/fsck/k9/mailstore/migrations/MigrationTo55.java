package com.fsck.k9.mailstore.migrations;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.message.MessageHeaderParser;
import com.fsck.k9.mailstore.BinaryMemoryBody;
import com.fsck.k9.mailstore.DatabasePreviewType;
import com.fsck.k9.mailstore.FileBackedBody;
import com.fsck.k9.message.extractors.PreviewResult.PreviewType;
import timber.log.Timber;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.message.extractors.MessageFulltextCreator;


class MigrationTo55 {

    private static class DataLocation {
        private static final int MISSING = 0;
        private static final int IN_DATABASE = 1;
        private static final int ON_DISK = 2;
        private static final int CHILD_PART_CONTAINS_DATA = 3;
    }

    private static final int MSG_INDEX_SUBJECT = 0;
    private static final int MSG_INDEX_SENDER_LIST = 1;
    private static final int MSG_INDEX_DATE = 2;
    private static final int MSG_INDEX_UID = 3;
    private static final int MSG_INDEX_FLAGS = 4;
    private static final int MSG_INDEX_ID = 5;
    private static final int MSG_INDEX_TO = 6;
    private static final int MSG_INDEX_CC = 7;
    private static final int MSG_INDEX_BCC = 8;
    private static final int MSG_INDEX_REPLY_TO = 9;
    private static final int MSG_INDEX_ATTACHMENT_COUNT = 10;
    private static final int MSG_INDEX_INTERNAL_DATE = 11;
    private static final int MSG_INDEX_MESSAGE_ID_HEADER = 12;
    private static final int MSG_INDEX_FOLDER_ID = 13;
    private static final int MSG_INDEX_PREVIEW = 14;
    private static final int MSG_INDEX_THREAD_ID = 15;
    private static final int MSG_INDEX_THREAD_ROOT_ID = 16;
    private static final int MSG_INDEX_FLAG_DELETED = 17;
    private static final int MSG_INDEX_FLAG_READ = 18;
    private static final int MSG_INDEX_FLAG_FLAGGED = 19;
    private static final int MSG_INDEX_FLAG_ANSWERED = 20;
    private static final int MSG_INDEX_FLAG_FORWARDED = 21;
    private static final int MSG_INDEX_MESSAGE_PART_ID = 22;
    private static final int MSG_INDEX_MIME_TYPE = 23;
    private static final int MSG_INDEX_PREVIEW_TYPE = 24;
    private static final int MSG_INDEX_HEADER_DATA = 25;

    private static class LocalMessageData extends MimeMessage {

        private long databaseId;
        private long rootId;
        private long threadId;
        private long messagePartId;
        private MessageReference messageReference;
        private int attachmentCount;
        private String subject;
        private String preview = "";
        private String mimeType;
        private PreviewType previewType;
        private boolean headerNeedsUpdating = false;

        public LocalMessageData(String uid) {
            this.mUid = uid;
        }

        public long getDatabaseId() {
            return databaseId;
        }

        public void populateFromGetMessageCursor(Cursor cursor) throws MessagingException {
            final String subject = cursor.getString(MSG_INDEX_SUBJECT);
            this.setSubject(subject == null ? "" : subject);

            Address[] from = Address.unpack(cursor.getString(MSG_INDEX_SENDER_LIST));
            if (from.length > 0) {
                this.setFrom(from[0]);
            }
            this.setInternalSentDate(new Date(cursor.getLong(MSG_INDEX_DATE)));
            this.setUid(cursor.getString(MSG_INDEX_UID));
            String flagList = cursor.getString(MSG_INDEX_FLAGS);
            if (flagList != null && flagList.length() > 0) {
                String[] flags = flagList.split(",");

                for (String flag : flags) {
                    try {
                        this.setFlag(Flag.valueOf(flag), true);
                    }

                    catch (Exception e) {
                        if (!"X_BAD_FLAG".equals(flag)) {
                            Timber.w("Unable to parse flag %s", flag);
                        }
                    }
                }
            }
            this.databaseId = cursor.getLong(MSG_INDEX_ID);
            this.setRecipients(RecipientType.TO, Address.unpack(cursor.getString(MSG_INDEX_TO)));
            this.setRecipients(RecipientType.CC, Address.unpack(cursor.getString(MSG_INDEX_CC)));
            this.setRecipients(RecipientType.BCC, Address.unpack(cursor.getString(MSG_INDEX_BCC)));
            this.setReplyTo(Address.unpack(cursor.getString(MSG_INDEX_REPLY_TO)));

            this.attachmentCount = cursor.getInt(MSG_INDEX_ATTACHMENT_COUNT);
            this.setInternalDate(new Date(cursor.getLong(MSG_INDEX_INTERNAL_DATE)));
            this.setMessageId(cursor.getString(MSG_INDEX_MESSAGE_ID_HEADER));

            String previewTypeString = cursor.getString(MSG_INDEX_PREVIEW_TYPE);
            DatabasePreviewType databasePreviewType = DatabasePreviewType.fromDatabaseValue(previewTypeString);
            previewType = databasePreviewType.getPreviewType();
            if (previewType == PreviewType.TEXT) {
                preview = cursor.getString(MSG_INDEX_PREVIEW);
            } else {
                preview = "";
            }

            threadId = (cursor.isNull(MSG_INDEX_THREAD_ID)) ? -1 : cursor.getLong(MSG_INDEX_THREAD_ID);
            rootId = (cursor.isNull(MSG_INDEX_THREAD_ROOT_ID)) ? -1 : cursor.getLong(MSG_INDEX_THREAD_ROOT_ID);

            boolean deleted = (cursor.getInt(MSG_INDEX_FLAG_DELETED) == 1);
            boolean read = (cursor.getInt(MSG_INDEX_FLAG_READ) == 1);
            boolean flagged = (cursor.getInt(MSG_INDEX_FLAG_FLAGGED) == 1);
            boolean answered = (cursor.getInt(MSG_INDEX_FLAG_ANSWERED) == 1);
            boolean forwarded = (cursor.getInt(MSG_INDEX_FLAG_FORWARDED) == 1);

            setFlag(Flag.DELETED, deleted);
            setFlag(Flag.SEEN, read);
            setFlag(Flag.FLAGGED, flagged);
            setFlag(Flag.ANSWERED, answered);
            setFlag(Flag.FORWARDED, forwarded);

            setMessagePartId(cursor.getLong(MSG_INDEX_MESSAGE_PART_ID));
            mimeType = cursor.getString(MSG_INDEX_MIME_TYPE);

            byte[] header = cursor.getBlob(MSG_INDEX_HEADER_DATA);
            if (header != null) {
                MessageHeaderParser.parse(this, new ByteArrayInputStream(header));
            } else {
                Timber.d("No headers available for this message!");
            }

            headerNeedsUpdating = false;
        }

        private void setMessagePartId(long messagePartId) {
            this.messagePartId = messagePartId;
        }

        public long getMessagePartId() {
            return messagePartId;
        }
    }

    private static class LocalBodyPartData extends MimeBodyPart {
        public LocalBodyPartData(String accountUuid,
                LocalMessageData message, long id, long size) throws MessagingException {
            super();
        }
    }

    private static class LocalMimeMessageData extends MimeMessage {
        public LocalMimeMessageData(String accountUuid,
                LocalMessageData message, long id) {

        }
    }

    static void createFtsSearchTable(SQLiteDatabase db, MigrationsHelper migrationsHelper) {
        MessageFulltextCreator fulltextCreator = MessageFulltextCreator.newInstance();
        ContentValues cv = new ContentValues();
        db.execSQL("CREATE VIRTUAL TABLE messages_fulltext USING fts4 (fulltext)");

        try {
            List<Long> folders = fetchFolders(db);

            for (Long folderDatabaseId : folders) {
                List<String> messageUids = fetchAllMessageUids(db, folderDatabaseId);
                for (String messageUid : messageUids) {
                    LocalMessageData localMessageData = getMessage(db, messageUid, folderDatabaseId);
                    loadMessageParts(db, localMessageData, migrationsHelper.getAccount().getUuid(),
                            migrationsHelper.getLocalStore());

                    String fulltext = fulltextCreator.createFulltext(localMessageData);
                    if (!TextUtils.isEmpty(fulltext)) {
                        Timber.d("fulltext for msg id %d is %d chars long", localMessageData.getDatabaseId(),
                                fulltext.length());
                        cv.clear();
                        cv.put("docid", localMessageData.getDatabaseId());
                        cv.put("fulltext", fulltext);
                        db.insert("messages_fulltext", null, cv);
                    } else {
                        Timber.d("no fulltext for msg id %d :(", localMessageData.getDatabaseId());
                    }
                }
            }
        } catch (MessagingException e) {
            Timber.e(e, "error indexing fulltext - skipping rest, fts index is incomplete!");
        }
    }

    private static List<Long> fetchFolders(SQLiteDatabase db) {
        List<Long> folders = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT id FROM folders ORDER BY name ASC", null);
        while (cursor.moveToNext()) {
            folders.add(cursor.getLong(0));
        }
        cursor.close();
        return folders;
    }

    private static List<String> fetchAllMessageUids(SQLiteDatabase db, Long folderDatabaseId) {
        List<String> messageUids = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT uid " +
                        "FROM messages " +
                        "WHERE empty = 0 AND deleted = 0 AND " +
                        "folder_id = ? ORDER BY date DESC",
                new String[] { Long.toString(folderDatabaseId) });
        while (cursor.moveToNext()) {
            messageUids.add(cursor.getString(0));
        }
        cursor.close();
        return messageUids;
    }

    private static LocalMessageData getMessage(SQLiteDatabase db, String uid, Long databaseId) throws MessagingException {
        String GET_MESSAGES_COLS =
                "subject, sender_list, date, uid, flags, messages.id, to_list, cc_list, " +
                        "bcc_list, reply_to_list, attachment_count, internal_date, messages.message_id, " +
                        "folder_id, preview, threads.id, threads.root, deleted, read, flagged, answered, " +
                        "forwarded, message_part_id, messages.mime_type, preview_type, header ";
        LocalMessageData message = new LocalMessageData(uid);
        Cursor cursor = db.rawQuery(
                "SELECT " +
                        GET_MESSAGES_COLS +
                        "FROM messages " +
                        "LEFT JOIN message_parts ON (message_parts.id = messages.message_part_id) " +
                        "LEFT JOIN threads ON (threads.message_id = messages.id) " +
                        "WHERE uid = ? AND folder_id = ?",
                new String[] { message.getUid(), Long.toString(databaseId) });

        if (!cursor.moveToNext()) {
            return null;
        }

        message.populateFromGetMessageCursor(cursor);
        cursor.close();
        return message;
    }

    private static void loadMessageParts(SQLiteDatabase db, LocalMessageData message, String accountUuid,
            LocalStore realStore)
            throws MessagingException {
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
                loadMessagePart(message, partById, cursor, accountUuid, realStore);
            }
        } finally {
            cursor.close();
        }
    }

    private static void loadMessagePart(LocalMessageData message, Map<Long, Part> partById,
            Cursor cursor, String accountUuid, LocalStore realStore)
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
                BodyPart bodyPart = new LocalBodyPartData(accountUuid, message, id, size);
                ((Multipart) parentPart.getBody()).addBodyPart(bodyPart);
                part = bodyPart;
            } else if (MimeUtility.isMessage(parentMimeType)) {
                Message innerMessage = new LocalMimeMessageData(accountUuid, message, id);
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

            File file = realStore.getAttachmentFile(Long.toString(id));
            if (file.exists()) {
                Body body = new FileBackedBody(file, encoding);
                part.setBody(body);
            }
        }
    }

    private static void parseHeaderBytes(Part part, byte[] header) throws MessagingException {
        MessageHeaderParser.parse(part, new ByteArrayInputStream(header));
    }
}
