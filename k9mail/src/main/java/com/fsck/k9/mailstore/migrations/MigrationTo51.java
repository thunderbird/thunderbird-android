package com.fsck.k9.mailstore.migrations;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.StorageManager;
import org.apache.james.mime4j.codec.QuotedPrintableOutputStream;
import org.apache.james.mime4j.util.MimeUtil;


class MigrationTo51 {
    private static final int MESSAGE_PART_TYPE__UNKNOWN = 0;
    private static final int MESSAGE_PART_TYPE__HIDDEN_ATTACHMENT = 6;
    private static final int DATA_LOCATION__MISSING = 0;
    private static final int DATA_LOCATION__IN_DATABASE = 1;
    private static final int DATA_LOCATION__ON_DISK = 2;

    /**
     * This method converts from the old message table structure to the new one.
     *
     * This is a complex migration, and ultimately we do not have enough
     * information to recreate the mime structure of the original mails.
     * What we have:
     *  - general mail info
     *  - html_content and text_content data, which is the squashed readable content of the mail
     *  - a table with message headers
     *  - attachments
     *
     * What we need to do:
     *  - migrate general mail info as-is
     *  - flag mails as migrated for re-download
     *  - for each message, recreate a mime structure from its message content and attachments:
     *    + insert one or both of textContent and htmlContent, depending on mimeType
     *    + if mimeType is text/plain, text/html or multipart/alternative and no
     *      attachments are present, just insert that.
     *    + otherwise, use multipart/mixed, adding attachments after textual content
     *    + revert content:// URIs in htmlContent to original cid: URIs.
     */
    public static void db51MigrateMessageFormat(SQLiteDatabase db, MigrationsHelper migrationsHelper) {
        renameOldMessagesTableAndCreateNew(db);

        copyMessageMetadataToNewTable(db);

        File attachmentDirNew, attachmentDirOld;
        Account account = migrationsHelper.getAccount();
        attachmentDirNew = StorageManager.getInstance(K9.app).getAttachmentDirectory(
                account.getUuid(), account.getLocalStorageProviderId());
        attachmentDirOld = renameOldAttachmentDirAndCreateNew(account, attachmentDirNew);

        Cursor msgCursor = db.query("messages_old",
                new String[] { "id", "flags", "html_content", "text_content", "mime_type", "attachment_count" },
                null, null, null, null, null);
        try {
            Log.d(K9.LOG_TAG, "migrating " + msgCursor.getCount() + " messages");
            ContentValues cv = new ContentValues();
            while (msgCursor.moveToNext()) {
                long messageId = msgCursor.getLong(0);
                String messageFlags = msgCursor.getString(1);
                String htmlContent = msgCursor.getString(2);
                String textContent = msgCursor.getString(3);
                String mimeType = msgCursor.getString(4);
                int attachmentCount = msgCursor.getInt(5);

                try {
                    updateFlagsForMessage(db, messageId, messageFlags, migrationsHelper);
                    MimeHeader mimeHeader = loadHeaderFromHeadersTable(db, messageId);

                    MimeStructureState structureState = MimeStructureState.getNewRootState();

                    boolean messageHadSpecialFormat = false;

                    // we do not rely on the protocol parameter here but guess by the multipart structure
                    boolean isMaybePgpMimeEncrypted = attachmentCount == 2
                            && MimeUtil.isSameMimeType(mimeType, "multipart/encrypted");
                    if (isMaybePgpMimeEncrypted) {
                        MimeStructureState maybeStructureState =
                                migratePgpMimeEncryptedContent(db, messageId, attachmentDirOld, attachmentDirNew,
                                        mimeHeader, structureState);
                        if (maybeStructureState != null) {
                            structureState = maybeStructureState;
                            messageHadSpecialFormat = true;
                        }
                    }

                    if (!messageHadSpecialFormat) {
                        boolean isSimpleStructured = attachmentCount == 0 &&
                                Utility.isAnyMimeType(mimeType, "text/plain", "text/html", "multipart/alternative");
                        if (isSimpleStructured) {
                            structureState = migrateSimpleMailContent(db, htmlContent, textContent,
                                    mimeType, mimeHeader, structureState);
                        } else {
                            mimeType = "multipart/mixed";
                            structureState =
                                    migrateComplexMailContent(db, attachmentDirOld, attachmentDirNew, messageId,
                                            htmlContent, textContent, mimeHeader, structureState);
                        }
                    }

                    cv.clear();
                    cv.put("mime_type", mimeType);
                    cv.put("message_part_id", structureState.rootPartId);
                    cv.put("attachment_count", attachmentCount);
                    db.update("messages", cv, "id = ?", new String[] { Long.toString(messageId) });
                } catch (IOException e) {
                    Log.e(K9.LOG_TAG, "error inserting into database", e);
                }
            }

        } finally {
            msgCursor.close();
        }

        cleanUpOldAttachmentDirectory(attachmentDirOld);

        dropOldMessagesTable(db);
    }

    @NonNull
    private static File renameOldAttachmentDirAndCreateNew(Account account, File attachmentDirNew) {
        File attachmentDirOld = new File(attachmentDirNew.getParent(),
                account.getUuid() + ".old_attach-" + System.currentTimeMillis());
        boolean moveOk = attachmentDirNew.renameTo(attachmentDirOld);
        if (!moveOk) {
            // TODO escalate?
            Log.e(K9.LOG_TAG, "Error moving attachment dir! All attachments might be lost!");
        }
        boolean mkdirOk = attachmentDirNew.mkdir();
        if (!mkdirOk) {
            // TODO escalate?
            Log.e(K9.LOG_TAG, "Error creating new attachment dir!");
        }
        return attachmentDirOld;
    }

    private static void dropOldMessagesTable(SQLiteDatabase db) {
        Log.d(K9.LOG_TAG, "Migration succeeded, dropping old tables.");
        db.execSQL("DROP TABLE messages_old");
        db.execSQL("DROP TABLE attachments");
        db.execSQL("DROP TABLE headers");
    }

    private static void cleanUpOldAttachmentDirectory(File attachmentDirOld) {
        for (File file : attachmentDirOld.listFiles()) {
            Log.d(K9.LOG_TAG, "deleting stale attachment file: " + file.getName());
            if (file.exists() && !file.delete()) {
                Log.d(K9.LOG_TAG, "Failed to delete stale attachement file: " + file.getAbsolutePath());
            }
        }

        Log.d(K9.LOG_TAG, "deleting old attachment directory");
        if (attachmentDirOld.exists() && !attachmentDirOld.delete()) {
            Log.d(K9.LOG_TAG, "Failed to delete old attachement directory: " + attachmentDirOld.getAbsolutePath());
        }
    }

    private static void copyMessageMetadataToNewTable(SQLiteDatabase db) {
        db.execSQL("INSERT INTO messages (" +
                "id, deleted, folder_id, uid, subject, date, sender_list, " +
                "to_list, cc_list, bcc_list, reply_to_list, attachment_count, " +
                "internal_date, message_id, preview, mime_type, " +
                "normalized_subject_hash, empty, read, flagged, answered" +
                ") SELECT " +
                "id, deleted, folder_id, uid, subject, date, sender_list, " +
                "to_list, cc_list, bcc_list, reply_to_list, attachment_count, " +
                "internal_date, message_id, preview, mime_type, " +
                "normalized_subject_hash, empty, read, flagged, answered " +
                "FROM messages_old");
    }

    private static void renameOldMessagesTableAndCreateNew(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE messages RENAME TO messages_old");

        db.execSQL("CREATE TABLE messages (" +
                "id INTEGER PRIMARY KEY, " +
                "deleted INTEGER default 0, " +
                "folder_id INTEGER, " +
                "uid TEXT, " +
                "subject TEXT, " +
                "date INTEGER, " +
                "flags TEXT, " +
                "sender_list TEXT, " +
                "to_list TEXT, " +
                "cc_list TEXT, " +
                "bcc_list TEXT, " +
                "reply_to_list TEXT, " +
                "attachment_count INTEGER, " +
                "internal_date INTEGER, " +
                "message_id TEXT, " +
                "preview TEXT, " +
                "mime_type TEXT, "+
                "normalized_subject_hash INTEGER, " +
                "empty INTEGER default 0, " +
                "read INTEGER default 0, " +
                "flagged INTEGER default 0, " +
                "answered INTEGER default 0, " +
                "forwarded INTEGER default 0, " +
                "message_part_id INTEGER" +
                ")");

        db.execSQL("CREATE TABLE message_parts (" +
                "id INTEGER PRIMARY KEY, " +
                "type INTEGER NOT NULL, " +
                "root INTEGER, " +
                "parent INTEGER NOT NULL, " +
                "seq INTEGER NOT NULL, " +
                "mime_type TEXT, " +
                "decoded_body_size INTEGER, " +
                "display_name TEXT, " +
                "header TEXT, " +
                "encoding TEXT, " +
                "charset TEXT, " +
                "data_location INTEGER NOT NULL, " +
                "data BLOB, " +
                "preamble TEXT, " +
                "epilogue TEXT, " +
                "boundary TEXT, " +
                "content_id TEXT, " +
                "server_extra TEXT" +
                ")");

        db.execSQL("CREATE TRIGGER set_message_part_root " +
                "AFTER INSERT ON message_parts " +
                "BEGIN " +
                "UPDATE message_parts SET root=id WHERE root IS NULL AND ROWID = NEW.ROWID; " +
                "END");
    }

    @Nullable
    private static MimeStructureState migratePgpMimeEncryptedContent(SQLiteDatabase db, long messageId,
            File attachmentDirOld, File attachmentDirNew, MimeHeader mimeHeader, MimeStructureState structureState) {

        Log.d(K9.LOG_TAG, "Attempting to migrate multipart/encrypted as pgp/mime");

        // we only handle attachment count == 2 here, so simply sorting application/pgp-encrypted
        // to the front (and application/octet-stream second) should suffice.
        String orderBy = "(mime_type LIKE 'application/pgp-encrypted') DESC";
        Cursor cursor = db.query("attachments",
                new String[] {
                        "id", "size", "name", "mime_type", "store_data",
                        "content_uri", "content_id", "content_disposition"
                },
                "message_id = ?", new String[] { Long.toString(messageId) }, null, null, orderBy);

        try {
            if (cursor.getCount() != 2) {
                Log.e(K9.LOG_TAG, "Found multipart/encrypted but bad number of attachments, handling as regular mail");
                return null;
            }

            cursor.moveToFirst();

            long firstPartId = cursor.getLong(0);
            int firstPartSize = cursor.getInt(1);
            String firstPartName = cursor.getString(2);
            String firstPartMimeType = cursor.getString(3);
            String firstPartStoreData = cursor.getString(4);
            String firstPartContentUriString = cursor.getString(5);

            if (!MimeUtil.isSameMimeType(firstPartMimeType, "application/pgp-encrypted")) {
                Log.e(K9.LOG_TAG,
                        "First part in multipart/encrypted wasn't application/pgp-encrypted, not handling as pgp/mime");
                return null;
            }

            cursor.moveToNext();

            long secondPartId = cursor.getLong(0);
            int secondPartSize = cursor.getInt(1);
            String secondPartName = cursor.getString(2);
            String secondPartMimeType = cursor.getString(3);
            String secondPartStoreData = cursor.getString(4);
            String secondPartContentUriString = cursor.getString(5);

            if (!MimeUtil.isSameMimeType(secondPartMimeType, "application/octet-stream")) {
                Log.e(K9.LOG_TAG,
                        "First part in multipart/encrypted wasn't application/octet-stream, not handling as pgp/mime");
                return null;
            }

            String boundary = MimeUtility.getHeaderParameter(
                    mimeHeader.getFirstHeader(MimeHeader.HEADER_CONTENT_TYPE), "boundary");
            if (TextUtils.isEmpty(boundary)) {
                boundary = MimeUtil.createUniqueBoundary();
            }
            mimeHeader.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                    String.format("multipart/encrypted; boundary=\"%s\"; protocol=\"application/pgp-encrypted\"", boundary));

            ContentValues cv = new ContentValues();
            cv.put("type", MESSAGE_PART_TYPE__UNKNOWN);
            cv.put("data_location", DATA_LOCATION__IN_DATABASE);
            cv.put("mime_type", "multipart/encrypted");
            cv.put("header", mimeHeader.toString());
            cv.put("boundary", boundary);
            structureState.applyValues(cv);

            long rootMessagePartId = db.insertOrThrow("message_parts", null, cv);
            structureState = structureState.nextMultipartChild(rootMessagePartId);

            structureState =
                    insertMimeAttachmentPart(db, attachmentDirOld, attachmentDirNew, structureState, firstPartId,
                            firstPartSize, firstPartName, "application/pgp-encrypted", firstPartStoreData,
                            firstPartContentUriString, null, null);

            structureState =
                    insertMimeAttachmentPart(db, attachmentDirOld, attachmentDirNew, structureState, secondPartId,
                            secondPartSize, secondPartName, "application/octet-stream", secondPartStoreData,
                            secondPartContentUriString, null, null);
        } finally {
            cursor.close();
        }

        return structureState;

    }

    private static MimeStructureState migrateComplexMailContent(SQLiteDatabase db,
            File attachmentDirOld, File attachmentDirNew, long messageId, String htmlContent, String textContent,
            MimeHeader mimeHeader, MimeStructureState structureState) throws IOException {
        Log.d(K9.LOG_TAG, "Processing mail with complex data structure as multipart/mixed");

        String boundary = MimeUtility.getHeaderParameter(
                mimeHeader.getFirstHeader(MimeHeader.HEADER_CONTENT_TYPE), "boundary");
        if (TextUtils.isEmpty(boundary)) {
            boundary = MimeUtil.createUniqueBoundary();
        }
        mimeHeader.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                String.format("multipart/mixed; boundary=\"%s\";", boundary));

        ContentValues cv = new ContentValues();
        cv.put("type", MESSAGE_PART_TYPE__UNKNOWN);
        cv.put("data_location", DATA_LOCATION__IN_DATABASE);
        cv.put("mime_type", "multipart/mixed");
        cv.put("header", mimeHeader.toString());
        cv.put("boundary", boundary);
        structureState.applyValues(cv);

        long rootMessagePartId = db.insertOrThrow("message_parts", null, cv);
        structureState = structureState.nextMultipartChild(rootMessagePartId);

        if (htmlContent != null) {
            htmlContent = replaceContentUriWithContentIdInHtmlPart(db, messageId, htmlContent);
        }

        if (textContent != null && htmlContent != null) {
            structureState = insertBodyAsMultipartAlternative(db, structureState, null, textContent, htmlContent);
            structureState = structureState.popParent();
        } else if (textContent != null) {
            structureState = insertTextualPartIntoDatabase(db, structureState, null, textContent, false);
        } else if (htmlContent != null) {
            structureState = insertTextualPartIntoDatabase(db, structureState, null, htmlContent, true);
        }

        structureState = insertAttachments(db, attachmentDirOld, attachmentDirNew, messageId, structureState);

        return structureState;
    }

    private static String replaceContentUriWithContentIdInHtmlPart(
            SQLiteDatabase db, long messageId, String htmlContent) {
        Cursor cursor = db.query("attachments", new String[] { "content_uri", "content_id" },
                "content_id IS NOT NULL AND message_id = ?", new String[] { Long.toString(messageId) }, null, null, null);

        try {
            while (cursor.moveToNext()) {
                String contentUriString = cursor.getString(0);
                String contentId = cursor.getString(1);
                // this is not super efficient, but occurs only once or twice
                htmlContent = htmlContent.replaceAll(Pattern.quote(contentUriString), "cid:" + contentId);
            }
        } finally {
            cursor.close();
        }

        return htmlContent;
    }

    private static MimeStructureState migrateSimpleMailContent(SQLiteDatabase db, String htmlContent,
            String textContent, String mimeType, MimeHeader mimeHeader, MimeStructureState structureState)
            throws IOException {
        Log.d(K9.LOG_TAG, "Processing mail with simple structure");

        if (MimeUtil.isSameMimeType(mimeType, "text/plain")) {
            return insertTextualPartIntoDatabase(db, structureState, mimeHeader, textContent, false);
        } else if (MimeUtil.isSameMimeType(mimeType, "text/html")) {
            return insertTextualPartIntoDatabase(db, structureState, mimeHeader, htmlContent, true);
        } else if (MimeUtil.isSameMimeType(mimeType, "multipart/alternative")) {
            return insertBodyAsMultipartAlternative(db, structureState, mimeHeader, textContent, htmlContent);
        } else {
            throw new IllegalStateException("migrateSimpleMailContent cannot handle mimeType " + mimeType);
        }
    }

    private static MimeStructureState insertAttachments(SQLiteDatabase db, File attachmentDirOld, File attachmentDirNew,
            long messageId, MimeStructureState structureState) {
        Cursor cursor = db.query("attachments",
                new String[] {
                        "id", "size", "name", "mime_type", "store_data",
                        "content_uri", "content_id", "content_disposition"
                },
                "message_id = ?", new String[] { Long.toString(messageId) }, null, null, null);

        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                int size = cursor.getInt(1);
                String name = cursor.getString(2);
                String mimeType = cursor.getString(3);
                String storeData = cursor.getString(4);
                String contentUriString = cursor.getString(5);
                String contentId = cursor.getString(6);
                String contentDisposition = cursor.getString(7);

                structureState =
                        insertMimeAttachmentPart(db, attachmentDirOld, attachmentDirNew, structureState, id, size, name,
                                mimeType, storeData, contentUriString, contentId, contentDisposition);

            }
        } finally {
            cursor.close();
        }

        return structureState;
    }

    private static MimeStructureState insertMimeAttachmentPart(SQLiteDatabase db, File attachmentDirOld,
            File attachmentDirNew, MimeStructureState structureState, long id, int size, String name, String mimeType,
            String storeData, String contentUriString, String contentId, String contentDisposition) {
        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "processing attachment " + id + ", " + name + ", "
                    + mimeType + ", " + storeData + ", " + contentUriString);
        }

        if (contentDisposition == null) {
            contentDisposition = "attachment";
        }

        MimeHeader mimeHeader = new MimeHeader();
        mimeHeader.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                String.format("%s;\r\n name=\"%s\"", mimeType, name));
        mimeHeader.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION,
                String.format(Locale.US, "%s;\r\n filename=\"%s\";\r\n size=%d",
                        contentDisposition, name, size)); // TODO: Should use encoded word defined in RFC 2231.
        if (contentId != null) {
            mimeHeader.setHeader(MimeHeader.HEADER_CONTENT_ID, contentId);
        }

        boolean hasData = contentUriString != null;
        File attachmentFileToMove;
        if (hasData) {
            try {
                Uri contentUri = Uri.parse(contentUriString);
                List<String> pathSegments = contentUri.getPathSegments();
                String attachmentId = pathSegments.get(1);
                boolean isMatchingAttachmentId = Long.parseLong(attachmentId) == id;

                File attachmentFile = new File(attachmentDirOld, attachmentId);
                boolean isExistingAttachmentFile = attachmentFile.exists();

                if (!isMatchingAttachmentId) {
                    Log.e(K9.LOG_TAG, "mismatched attachment id. mark as missing");
                    attachmentFileToMove = null;
                } else if (!isExistingAttachmentFile) {
                    Log.e(K9.LOG_TAG, "attached file doesn't exist. mark as missing");
                    attachmentFileToMove = null;
                } else {
                    attachmentFileToMove = attachmentFile;
                }
            } catch (Exception e) {
                // anything here fails, conservatively assume the data doesn't exist
                attachmentFileToMove = null;
            }
        } else {
            attachmentFileToMove = null;
        }
        if (K9.DEBUG && attachmentFileToMove == null) {
            Log.d(K9.LOG_TAG, "matching attachment is in local cache");
        }

        boolean hasContentTypeAndIsInline = !TextUtils.isEmpty(contentId) && "inline".equalsIgnoreCase(contentDisposition);
        int messageType = hasContentTypeAndIsInline ?
                MESSAGE_PART_TYPE__HIDDEN_ATTACHMENT : MESSAGE_PART_TYPE__UNKNOWN;

        ContentValues cv = new ContentValues();
        cv.put("type", messageType);
        cv.put("mime_type", mimeType);
        cv.put("decoded_body_size", size);
        cv.put("display_name", name);
        cv.put("header", mimeHeader.toString());
        cv.put("encoding", MimeUtil.ENC_BINARY);
        cv.put("data_location", attachmentFileToMove != null ? DATA_LOCATION__ON_DISK : DATA_LOCATION__MISSING);
        cv.put("content_id", contentId);
        cv.put("server_extra", storeData);
        structureState.applyValues(cv);

        long partId = db.insertOrThrow("message_parts", null, cv);
        structureState = structureState.nextChild(partId);

        if (attachmentFileToMove != null) {
            boolean moveOk = attachmentFileToMove.renameTo(new File(attachmentDirNew, Long.toString(partId)));
            if (!moveOk) {
                Log.e(K9.LOG_TAG, "Moving attachment to new dir failed!");
            }
        }
        return structureState;
    }

    private static void updateFlagsForMessage(SQLiteDatabase db, long messageId, String messageFlags,
            MigrationsHelper migrationsHelper) {
        List<Flag> extraFlags = new ArrayList<>();
        if (messageFlags != null && messageFlags.length() > 0) {
            String[] flags = messageFlags.split(",");

            for (String flagStr : flags) {
                try {
                    Flag flag = Flag.valueOf(flagStr);
                    extraFlags.add(flag);
                } catch (Exception e) {
                    // Ignore bad flags
                }
            }
        }
        extraFlags.add(Flag.X_MIGRATED_FROM_V50);

        String flagsString = migrationsHelper.serializeFlags(extraFlags);
        db.execSQL("UPDATE messages SET flags = ? WHERE id = ?", new Object[] { flagsString, messageId } );
    }

    private static MimeStructureState insertBodyAsMultipartAlternative(SQLiteDatabase db,
            MimeStructureState structureState, MimeHeader mimeHeader,
            String textContent, String htmlContent) throws IOException {
        if (mimeHeader == null) {
            mimeHeader = new MimeHeader();
        }
        String boundary = MimeUtility.getHeaderParameter(
                mimeHeader.getFirstHeader(MimeHeader.HEADER_CONTENT_TYPE), "boundary");
        if (TextUtils.isEmpty(boundary)) {
            boundary = MimeUtil.createUniqueBoundary();
        }
        mimeHeader.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                String.format("multipart/alternative; boundary=\"%s\";", boundary));

        int dataLocation = textContent != null || htmlContent != null
                ? DATA_LOCATION__IN_DATABASE : DATA_LOCATION__MISSING;

        ContentValues cv = new ContentValues();
        cv.put("type", MESSAGE_PART_TYPE__UNKNOWN);
        cv.put("data_location", dataLocation);
        cv.put("mime_type", "multipart/alternative");
        cv.put("header", mimeHeader.toString());
        cv.put("boundary", boundary);
        structureState.applyValues(cv);

        long multipartAlternativePartId = db.insertOrThrow("message_parts", null, cv);
        structureState = structureState.nextMultipartChild(multipartAlternativePartId);

        if (textContent != null) {
            structureState = insertTextualPartIntoDatabase(db, structureState, null, textContent, false);
        }

        if (htmlContent != null) {
            structureState = insertTextualPartIntoDatabase(db, structureState, null, htmlContent, true);
        }

        return structureState;
    }

    private static MimeStructureState insertTextualPartIntoDatabase(SQLiteDatabase db, MimeStructureState structureState,
            MimeHeader mimeHeader, String content, boolean isHtml) throws IOException {
        if (mimeHeader == null) {
            mimeHeader = new MimeHeader();
        }
        mimeHeader.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                isHtml ? "text/html; charset=\"utf-8\"" : "text/plain; charset=\"utf-8\"");
        mimeHeader.setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, MimeUtil.ENC_QUOTED_PRINTABLE);

        byte[] contentBytes;
        int decodedBodySize;
        int dataLocation;
        if (content != null) {
            ByteArrayOutputStream contentOutputStream = new ByteArrayOutputStream();
            QuotedPrintableOutputStream quotedPrintableOutputStream =
                    new QuotedPrintableOutputStream(contentOutputStream, false);
            quotedPrintableOutputStream.write(content.getBytes());
            quotedPrintableOutputStream.flush();

            dataLocation = DATA_LOCATION__IN_DATABASE;
            contentBytes = contentOutputStream.toByteArray();
            decodedBodySize = content.length();
        } else {
            dataLocation = DATA_LOCATION__MISSING;
            contentBytes = null;
            decodedBodySize = 0;
        }

        ContentValues cv = new ContentValues();
        cv.put("type", MESSAGE_PART_TYPE__UNKNOWN);
        cv.put("data_location", dataLocation);
        cv.put("mime_type", isHtml ? "text/html" : "text/plain");
        cv.put("header", mimeHeader.toString());
        cv.put("data", contentBytes);
        cv.put("decoded_body_size", decodedBodySize);
        cv.put("encoding", MimeUtil.ENC_QUOTED_PRINTABLE);
        cv.put("charset", "utf-8");
        structureState.applyValues(cv);

        long partId = db.insertOrThrow("message_parts", null, cv);
        return structureState.nextChild(partId);
    }

    private static MimeHeader loadHeaderFromHeadersTable(SQLiteDatabase db, long messageId) {
        Cursor headersCursor = db.query("headers",
                new String[] { "name", "value" },
                "message_id = ?", new String[] { Long.toString(messageId) }, null, null, null);
        try {
            MimeHeader mimeHeader = new MimeHeader();
            while (headersCursor.moveToNext()) {
                String name = headersCursor.getString(0);
                String value = headersCursor.getString(1);
                mimeHeader.addHeader(name, value);
            }
            return mimeHeader;
        } finally {
            headersCursor.close();
        }
    }


    /**
     * Objects of this class hold immutable information on a database position for
     * one part of the mime structure of a message.
     *
     * An object of this class must be passed to and returned by every operation
     * which inserts mime parts into the database. Each mime part which is inserted
     * must call the {#applyValues()} method on its ContentValues, then obtain the
     * next state object by calling the appropriate next*() method.
     *
     * While the data carried by this object is immutable, it contains some state
     * to ensure that the operations are called correctly and in order.
     *
     * Because the insertion operations required for the database migration are
     * strictly linear, we do not require a more complex stack-based data structure
     * here.
     */
    @VisibleForTesting
    static class MimeStructureState {
        private final Long rootPartId;
        private final Long prevParentId;
        private final long parentId;
        private final int nextOrder;

        // just some diagnostic state to make sure all operations are called in order
        private boolean isValuesApplied;
        private boolean isStateAdvanced;


        private MimeStructureState(Long rootPartId, Long prevParentId, long parentId, int nextOrder) {
            this.rootPartId = rootPartId;
            this.prevParentId = prevParentId;
            this.parentId = parentId;
            this.nextOrder = nextOrder;
        }

        public static MimeStructureState getNewRootState() {
            return new MimeStructureState(null, null, -1, 0);
        }

        public MimeStructureState nextChild(long newPartId) {
            if (!isValuesApplied || isStateAdvanced) {
                throw new IllegalStateException("next* methods must only be called once");
            }
            isStateAdvanced = true;

            if (rootPartId == null) {
                return new MimeStructureState(newPartId, null, -1, nextOrder+1);
            }
            return new MimeStructureState(rootPartId, prevParentId, parentId, nextOrder+1);
        }

        public MimeStructureState nextMultipartChild(long newPartId) {
            if (!isValuesApplied || isStateAdvanced) {
                throw new IllegalStateException("next* methods must only be called once");
            }
            isStateAdvanced = true;

            if (rootPartId == null) {
                return new MimeStructureState(newPartId, parentId, newPartId, nextOrder+1);
            }
            return new MimeStructureState(rootPartId, parentId, newPartId, nextOrder+1);
        }

        public void applyValues(ContentValues cv) {
            if (isValuesApplied || isStateAdvanced) {
                throw new IllegalStateException("applyValues must be called exactly once, after a call to next*");
            }
            if (rootPartId != null && parentId == -1L) {
                throw new IllegalStateException("applyValues must not be called after a root nextChild call");
            }
            isValuesApplied = true;

            if (rootPartId != null) {
                cv.put("root", rootPartId);
            }
            cv.put("parent", parentId);
            cv.put("seq", nextOrder);
        }

        public MimeStructureState popParent() {
            if (prevParentId == null) {
                throw new IllegalStateException("popParent must only be called if parent depth is >= 2");
            }
            return new MimeStructureState(rootPartId, null, prevParentId, nextOrder);
        }
    }
}
