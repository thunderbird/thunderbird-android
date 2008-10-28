
package com.android.email.mail.store;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Config;
import android.util.Log;

import com.android.email.Email;
import com.android.email.Utility;
import com.android.email.codec.binary.Base64OutputStream;
import com.android.email.mail.Address;
import com.android.email.mail.Body;
import com.android.email.mail.FetchProfile;
import com.android.email.mail.Flag;
import com.android.email.mail.Folder;
import com.android.email.mail.Message;
import com.android.email.mail.MessageRetrievalListener;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Part;
import com.android.email.mail.Store;
import com.android.email.mail.Message.RecipientType;
import com.android.email.mail.internet.MimeBodyPart;
import com.android.email.mail.internet.MimeHeader;
import com.android.email.mail.internet.MimeMessage;
import com.android.email.mail.internet.MimeMultipart;
import com.android.email.mail.internet.MimeUtility;
import com.android.email.mail.internet.TextBody;
import com.android.email.provider.AttachmentProvider;

/**
 * <pre>
 * Implements a SQLite database backed local store for Messages.
 * </pre>
 */
public class LocalStore extends Store {
    private static final int DB_VERSION = 18;
    private static final Flag[] PERMANENT_FLAGS = { Flag.DELETED, Flag.X_DESTROYED, Flag.SEEN };

    private String mPath;
    private SQLiteDatabase mDb;
    private File mAttachmentsDir;
    private Application mApplication;

    /**
     * @param uri local://localhost/path/to/database/uuid.db
     */
    public LocalStore(String _uri, Application application) throws MessagingException {
        mApplication = application;
        URI uri = null;
        try {
            uri = new URI(_uri);
        } catch (Exception e) {
            throw new MessagingException("Invalid uri for LocalStore");
        }
        if (!uri.getScheme().equals("local")) {
            throw new MessagingException("Invalid scheme");
        }
        mPath = uri.getPath();

        File parentDir = new File(mPath).getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        mDb = SQLiteDatabase.openOrCreateDatabase(mPath, null);
        if (mDb.getVersion() != DB_VERSION) {
            if (mDb.getVersion() < 18) {
                if (Config.LOGV) {
                    Log.v(Email.LOG_TAG, String.format("Upgrading database from %d to %d", mDb
                            .getVersion(), 18));
                }
                mDb.execSQL("DROP TABLE IF EXISTS folders");
                mDb.execSQL("CREATE TABLE folders (id INTEGER PRIMARY KEY, name TEXT, "
                        + "last_updated INTEGER, unread_count INTEGER, visible_limit INTEGER)");

                mDb.execSQL("DROP TABLE IF EXISTS messages");
                mDb.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY, folder_id INTEGER, uid TEXT, subject TEXT, "
                        + "date INTEGER, flags TEXT, sender_list TEXT, to_list TEXT, cc_list TEXT, bcc_list TEXT, reply_to_list TEXT, "
                        + "html_content TEXT, text_content TEXT, attachment_count INTEGER, internal_date INTEGER)");

                mDb.execSQL("DROP TABLE IF EXISTS attachments");
                mDb.execSQL("CREATE TABLE attachments (id INTEGER PRIMARY KEY, message_id INTEGER,"
                        + "store_data TEXT, content_uri TEXT, size INTEGER, name TEXT,"
                        + "mime_type TEXT)");

                mDb.execSQL("DROP TABLE IF EXISTS pending_commands");
                mDb.execSQL("CREATE TABLE pending_commands " +
                        "(id INTEGER PRIMARY KEY, command TEXT, arguments TEXT)");

                mDb.execSQL("DROP TRIGGER IF EXISTS delete_folder");
                mDb.execSQL("CREATE TRIGGER delete_folder BEFORE DELETE ON folders BEGIN DELETE FROM messages WHERE old.id = folder_id; END;");

                mDb.execSQL("DROP TRIGGER IF EXISTS delete_message");
                mDb.execSQL("CREATE TRIGGER delete_message BEFORE DELETE ON messages BEGIN DELETE FROM attachments WHERE old.id = message_id; END;");
                mDb.setVersion(18);
            }
            if (mDb.getVersion() != DB_VERSION) {
                throw new Error("Database upgrade failed!");
            }
        }
        mAttachmentsDir = new File(mPath + "_att");
        if (!mAttachmentsDir.exists()) {
            mAttachmentsDir.mkdirs();
        }
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        return new LocalFolder(name);
    }

    // TODO this takes about 260-300ms, seems slow.
    @Override
    public Folder[] getPersonalNamespaces() throws MessagingException {
        ArrayList<Folder> folders = new ArrayList<Folder>();
        Cursor cursor = null;
        try {
            cursor = mDb.rawQuery("SELECT name FROM folders", null);
            while (cursor.moveToNext()) {
                folders.add(new LocalFolder(cursor.getString(0)));
            }
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return folders.toArray(new Folder[] {});
    }

    @Override
    public void checkSettings() throws MessagingException {
    }

    /**
     * Delete the entire Store and it's backing database.
     */
    public void delete() {
        try {
            mDb.close();
        } catch (Exception e) {

        }
        try{
            File[] attachments = mAttachmentsDir.listFiles();
            for (File attachment : attachments) {
                if (attachment.exists()) {
                    attachment.delete();
                }
            }
            if (mAttachmentsDir.exists()) {
                mAttachmentsDir.delete();
            }
        }
        catch (Exception e) {
        }
        try {
            new File(mPath).delete();
        }
        catch (Exception e) {

        }
    }

    /**
     * Deletes all cached attachments for the entire store.
     */
    public void pruneCachedAttachments() throws MessagingException {
        File[] files = mAttachmentsDir.listFiles();
        for (File file : files) {
            if (file.exists()) {
                try {
                    Cursor cursor = null;
                    try {
                        cursor = mDb.query(
                            "attachments",
                            new String[] { "store_data" },
                            "id = ?",
                            new String[] { file.getName() },
                            null,
                            null,
                            null);
                        if (cursor.moveToNext()) {
                            if (cursor.getString(0) == null) {
                                /*
                                 * If the attachment has no store data it is not recoverable, so
                                 * we won't delete it.
                                 */
                                continue;
                            }
                        }
                    }
                    finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                    ContentValues cv = new ContentValues();
                    cv.putNull("content_uri");
                    mDb.update("attachments", cv, "id = ?", new String[] { file.getName() });
                }
                catch (Exception e) {
                    /*
                     * If the row has gone away before we got to mark it not-downloaded that's
                     * okay.
                     */
                }
                if (!file.delete()) {
                    file.deleteOnExit();
                }
            }
        }
    }

    public void resetVisibleLimits() {
        ContentValues cv = new ContentValues();
        cv.put("visible_limit", Integer.toString(Email.DEFAULT_VISIBLE_LIMIT));
        mDb.update("folders", cv, null, null);
    }

    public ArrayList<PendingCommand> getPendingCommands() {
        Cursor cursor = null;
        try {
            cursor = mDb.query("pending_commands",
                    new String[] { "id", "command", "arguments" },
                    null,
                    null,
                    null,
                    null,
                    "id ASC");
            ArrayList<PendingCommand> commands = new ArrayList<PendingCommand>();
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
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void addPendingCommand(PendingCommand command) {
        try {
            for (int i = 0; i < command.arguments.length; i++) {
                command.arguments[i] = URLEncoder.encode(command.arguments[i], "UTF-8");
            }
            ContentValues cv = new ContentValues();
            cv.put("command", command.command);
            cv.put("arguments", Utility.combine(command.arguments, ','));
            mDb.insert("pending_commands", "command", cv);
        }
        catch (UnsupportedEncodingException usee) {
            throw new Error("Aparently UTF-8 has been lost to the annals of history.");
        }
    }

    public void removePendingCommand(PendingCommand command) {
        mDb.delete("pending_commands", "id = ?", new String[] { Long.toString(command.mId) });
    }

    public static class PendingCommand {
        private long mId;
        public String command;
        public String[] arguments;

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(command);
            sb.append("\n");
            for (String argument : arguments) {
                sb.append("  ");
                sb.append(argument);
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    public class LocalFolder extends Folder {
        private String mName;
        private long mFolderId = -1;
        private int mUnreadMessageCount = -1;
        private int mVisibleLimit = -1;

        public LocalFolder(String name) {
            this.mName = name;
        }

        public long getId() {
            return mFolderId;
        }

        @Override
        public void open(OpenMode mode) throws MessagingException {
            if (isOpen()) {
                return;
            }
            if (!exists()) {
                create(FolderType.HOLDS_MESSAGES);
            }
            Cursor cursor = null;
            try {
                cursor = mDb.rawQuery("SELECT id, unread_count, visible_limit FROM folders "
                        + "where folders.name = ?",
                    new String[] {
                        mName
                    });
            cursor.moveToFirst();
            mFolderId = cursor.getInt(0);
            mUnreadMessageCount = cursor.getInt(1);
            mVisibleLimit = cursor.getInt(2);
            }
            finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        @Override
        public boolean isOpen() {
            return mFolderId != -1;
        }

        @Override
        public OpenMode getMode() throws MessagingException {
            return OpenMode.READ_WRITE;
        }

        @Override
        public String getName() {
            return mName;
        }

        @Override
        public boolean exists() throws MessagingException {
            return Utility.arrayContains(getPersonalNamespaces(), this);
        }

        @Override
        public boolean create(FolderType type) throws MessagingException {
            if (exists()) {
                throw new MessagingException("Folder " + mName + " already exists.");
            }
            mDb.execSQL("INSERT INTO folders (name, visible_limit) VALUES (?, ?)", new Object[] {
                mName,
                25
            });
            return true;
        }

        @Override
        public void close(boolean expunge) throws MessagingException {
            if (expunge) {
                expunge();
            }
            mFolderId = -1;
        }

        @Override
        public int getMessageCount() throws MessagingException {
            open(OpenMode.READ_WRITE);
            Cursor cursor = null;
            try {
                cursor = mDb.rawQuery("SELECT COUNT(*) FROM messages WHERE messages.folder_id = ?",
                        new String[] {
                            Long.toString(mFolderId)
                        });
                cursor.moveToFirst();
                int messageCount = cursor.getInt(0);
                return messageCount;
            }
            finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        @Override
        public int getUnreadMessageCount() throws MessagingException {
            open(OpenMode.READ_WRITE);
            return mUnreadMessageCount;
        }


        public void setUnreadMessageCount(int unreadMessageCount) throws MessagingException {
            open(OpenMode.READ_WRITE);
            mUnreadMessageCount = Math.max(0, unreadMessageCount);
            mDb.execSQL("UPDATE folders SET unread_count = ? WHERE id = ?",
                    new Object[] { mUnreadMessageCount, mFolderId });
        }

        public int getVisibleLimit() throws MessagingException {
            open(OpenMode.READ_WRITE);
            return mVisibleLimit;
        }


        public void setVisibleLimit(int visibleLimit) throws MessagingException {
            open(OpenMode.READ_WRITE);
            mVisibleLimit = visibleLimit;
            mDb.execSQL("UPDATE folders SET visible_limit = ? WHERE id = ?",
                    new Object[] { mVisibleLimit, mFolderId });
        }


        @Override
        public void fetch(Message[] messages, FetchProfile fp, MessageRetrievalListener listener)
                throws MessagingException {
            open(OpenMode.READ_WRITE);
            if (fp.contains(FetchProfile.Item.BODY)) {
                for (Message message : messages) {
                    LocalMessage localMessage = (LocalMessage)message;
                    Cursor cursor = null;
                    localMessage.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "multipart/mixed");
                    MimeMultipart mp = new MimeMultipart();
                    mp.setSubType("mixed");
                    localMessage.setBody(mp);
                    try {
                        cursor = mDb.rawQuery("SELECT html_content, text_content FROM messages "
                                + "WHERE id = ?",
                                new String[] { Long.toString(localMessage.mId) });
                        cursor.moveToNext();
                        String htmlContent = cursor.getString(0);
                        String textContent = cursor.getString(1);

                        if (htmlContent != null) {
                            TextBody body = new TextBody(htmlContent);
                            MimeBodyPart bp = new MimeBodyPart(body, "text/html");
                            mp.addBodyPart(bp);
                        }

                        if (textContent != null) {
                            TextBody body = new TextBody(textContent);
                            MimeBodyPart bp = new MimeBodyPart(body, "text/plain");
                            mp.addBodyPart(bp);
                        }
                    }
                    finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }

                    try {
                        cursor = mDb.query(
                                "attachments",
                                new String[] {
                                        "id",
                                        "size",
                                        "name",
                                        "mime_type",
                                        "store_data",
                                        "content_uri" },
                                "message_id = ?",
                                new String[] { Long.toString(localMessage.mId) },
                                null,
                                null,
                                null);

                        while (cursor.moveToNext()) {
                            long id = cursor.getLong(0);
                            int size = cursor.getInt(1);
                            String name = cursor.getString(2);
                            String type = cursor.getString(3);
                            String storeData = cursor.getString(4);
                            String contentUri = cursor.getString(5);
                            Body body = null;
                            if (contentUri != null) {
                                body = new LocalAttachmentBody(Uri.parse(contentUri), mApplication);
                            }
                            MimeBodyPart bp = new LocalAttachmentBodyPart(body, id);
                            bp.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                                    String.format("%s;\n name=\"%s\"",
                                    type,
                                    name));
                            bp.setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, "base64");
                            bp.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION,
                                    String.format("attachment;\n filename=\"%s\";\n size=%d",
                                    name,
                                    size));

                            /*
                             * HEADER_ANDROID_ATTACHMENT_STORE_DATA is a custom header we add to that
                             * we can later pull the attachment from the remote store if neccesary.
                             */
                            bp.setHeader(MimeHeader.HEADER_ANDROID_ATTACHMENT_STORE_DATA, storeData);

                            mp.addBodyPart(bp);
                        }
                    }
                    finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
            }
        }

        private void populateMessageFromGetMessageCursor(LocalMessage message, Cursor cursor)
                throws MessagingException{
            message.setSubject(cursor.getString(0) == null ? "" : cursor.getString(0));
            Address[] from = Address.unpack(cursor.getString(1));
            if (from.length > 0) {
                message.setFrom(from[0]);
            }
            message.setSentDate(new Date(cursor.getLong(2)));
            message.setUid(cursor.getString(3));
            String flagList = cursor.getString(4);
            if (flagList != null && flagList.length() > 0) {
                String[] flags = flagList.split(",");
                try {
                    for (String flag : flags) {
                        message.setFlagInternal(Flag.valueOf(flag.toUpperCase()), true);
                    }
                } catch (Exception e) {
                }
            }
            message.mId = cursor.getLong(5);
            message.setRecipients(RecipientType.TO, Address.unpack(cursor.getString(6)));
            message.setRecipients(RecipientType.CC, Address.unpack(cursor.getString(7)));
            message.setRecipients(RecipientType.BCC, Address.unpack(cursor.getString(8)));
            message.setReplyTo(Address.unpack(cursor.getString(9)));
            message.mAttachmentCount = cursor.getInt(10);
            message.setInternalDate(new Date(cursor.getLong(11)));
        }

        @Override
        public Message[] getMessages(int start, int end, MessageRetrievalListener listener)
                throws MessagingException {
            open(OpenMode.READ_WRITE);
            throw new MessagingException(
                    "LocalStore.getMessages(int, int, MessageRetrievalListener) not yet implemented");
        }

        @Override
        public Message getMessage(String uid) throws MessagingException {
            open(OpenMode.READ_WRITE);
            LocalMessage message = new LocalMessage(uid, this);
            Cursor cursor = null;
            try {
                cursor = mDb.rawQuery(
                        "SELECT subject, sender_list, date, uid, flags, id, to_list, cc_list, "
                        + "bcc_list, reply_to_list, attachment_count, internal_date "
                                + "FROM messages " + "WHERE uid = ? " + "AND folder_id = ?",
                        new String[] {
                                message.getUid(), Long.toString(mFolderId)
                        });
                if (!cursor.moveToNext()) {
                    return null;
                }
                populateMessageFromGetMessageCursor(message, cursor);
            }
            finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return message;
        }

        @Override
        public Message[] getMessages(MessageRetrievalListener listener) throws MessagingException {
            open(OpenMode.READ_WRITE);
            ArrayList<Message> messages = new ArrayList<Message>();
            Cursor cursor = null;
            try {
                cursor = mDb.rawQuery(
                        "SELECT subject, sender_list, date, uid, flags, id, to_list, cc_list, "
                        + "bcc_list, reply_to_list, attachment_count, internal_date "
                                + "FROM messages " + "WHERE folder_id = ?", new String[] {
                            Long.toString(mFolderId)
                        });

                while (cursor.moveToNext()) {
                    LocalMessage message = new LocalMessage(null, this);
                    populateMessageFromGetMessageCursor(message, cursor);
                    messages.add(message);
                }
            }
            finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            return messages.toArray(new Message[] {});
        }

        @Override
        public Message[] getMessages(String[] uids, MessageRetrievalListener listener)
                throws MessagingException {
            open(OpenMode.READ_WRITE);
            if (uids == null) {
                return getMessages(listener);
            }
            ArrayList<Message> messages = new ArrayList<Message>();
            for (String uid : uids) {
                messages.add(getMessage(uid));
            }
            return messages.toArray(new Message[] {});
        }

        @Override
        public void copyMessages(Message[] msgs, Folder folder) throws MessagingException {
            if (!(folder instanceof LocalFolder)) {
                throw new MessagingException("copyMessages called with incorrect Folder");
            }
            ((LocalFolder) folder).appendMessages(msgs, true);
        }

        /**
         * The method differs slightly from the contract; If an incoming message already has a uid
         * assigned and it matches the uid of an existing message then this message will replace the
         * old message. It is implemented as a delete/insert. This functionality is used in saving
         * of drafts and re-synchronization of updated server messages.
         */
        @Override
        public void appendMessages(Message[] messages) throws MessagingException {
            appendMessages(messages, false);
        }

        /**
         * The method differs slightly from the contract; If an incoming message already has a uid
         * assigned and it matches the uid of an existing message then this message will replace the
         * old message. It is implemented as a delete/insert. This functionality is used in saving
         * of drafts and re-synchronization of updated server messages.
         */
        public void appendMessages(Message[] messages, boolean copy) throws MessagingException {
            open(OpenMode.READ_WRITE);
            for (Message message : messages) {
                if (!(message instanceof MimeMessage)) {
                    throw new Error("LocalStore can only store Messages that extend MimeMessage");
                }

                String uid = message.getUid();
                if (uid == null) {
                    message.setUid("Local" + UUID.randomUUID().toString());
                }
                else {
                    /*
                     * The message may already exist in this Folder, so delete it first.
                     */
                    deleteAttachments(message.getUid());
                    mDb.execSQL("DELETE FROM messages WHERE folder_id = ? AND uid = ?",
                            new Object[] { mFolderId, message.getUid() });
                }

                ArrayList<Part> viewables = new ArrayList<Part>();
                ArrayList<Part> attachments = new ArrayList<Part>();
                MimeUtility.collectParts(message, viewables, attachments);

                StringBuffer sbHtml = new StringBuffer();
                StringBuffer sbText = new StringBuffer();
                for (Part viewable : viewables) {
                    try {
                        String text = MimeUtility.getTextFromPart(viewable);
                        /*
                         * Anything with MIME type text/html will be stored as such. Anything
                         * else will be stored as text/plain.
                         */
                        if (viewable.getMimeType().equalsIgnoreCase("text/html")) {
                            sbHtml.append(text);
                        }
                        else {
                            sbText.append(text);
                        }
                    } catch (Exception e) {
                        throw new MessagingException("Unable to get text for message part", e);
                    }
                }

                try {
                    ContentValues cv = new ContentValues();
                    cv.put("uid", message.getUid());
                    cv.put("subject", message.getSubject());
                    cv.put("sender_list", Address.pack(message.getFrom()));
                    cv.put("date", message.getSentDate() == null
                            ? System.currentTimeMillis() : message.getSentDate().getTime());
                    cv.put("flags", Utility.combine(message.getFlags(), ',').toUpperCase());
                    cv.put("folder_id", mFolderId);
                    cv.put("to_list", Address.pack(message.getRecipients(RecipientType.TO)));
                    cv.put("cc_list", Address.pack(message.getRecipients(RecipientType.CC)));
                    cv.put("bcc_list", Address.pack(message.getRecipients(RecipientType.BCC)));
                    cv.put("html_content", sbHtml.length() > 0 ? sbHtml.toString() : null);
                    cv.put("text_content", sbText.length() > 0 ? sbText.toString() : null);
                    cv.put("reply_to_list", Address.pack(message.getReplyTo()));
                    cv.put("attachment_count", attachments.size());
                    cv.put("internal_date",  message.getInternalDate() == null
                            ? System.currentTimeMillis() : message.getInternalDate().getTime());
                    long messageId = mDb.insert("messages", "uid", cv);
                    for (Part attachment : attachments) {
                        saveAttachment(messageId, attachment, copy);
                    }
                } catch (Exception e) {
                    throw new MessagingException("Error appending message", e);
                }
            }
        }

        /**
         * Update the given message in the LocalStore without first deleting the existing
         * message (contrast with appendMessages). This method is used to store changes
         * to the given message while updating attachments and not removing existing
         * attachment data.
         * TODO In the future this method should be combined with appendMessages since the Message
         * contains enough data to decide what to do.
         * @param message
         * @throws MessagingException
         */
        public void updateMessage(LocalMessage message) throws MessagingException {
            open(OpenMode.READ_WRITE);
            ArrayList<Part> viewables = new ArrayList<Part>();
            ArrayList<Part> attachments = new ArrayList<Part>();
            MimeUtility.collectParts(message, viewables, attachments);

            StringBuffer sbHtml = new StringBuffer();
            StringBuffer sbText = new StringBuffer();
            for (int i = 0, count = viewables.size(); i < count; i++)  {
                Part viewable = viewables.get(i);
                try {
                    String text = MimeUtility.getTextFromPart(viewable);
                    /*
                     * Anything with MIME type text/html will be stored as such. Anything
                     * else will be stored as text/plain.
                     */
                    if (viewable.getMimeType().equalsIgnoreCase("text/html")) {
                        sbHtml.append(text);
                    }
                    else {
                        sbText.append(text);
                    }
                } catch (Exception e) {
                    throw new MessagingException("Unable to get text for message part", e);
                }
            }

            try {
                mDb.execSQL("UPDATE messages SET "
                        + "uid = ?, subject = ?, sender_list = ?, date = ?, flags = ?, "
                        + "folder_id = ?, to_list = ?, cc_list = ?, bcc_list = ?, "
                        + "html_content = ?, text_content = ?, reply_to_list = ?, "
                        + "attachment_count = ? WHERE id = ?",
                        new Object[] {
                                message.getUid(),
                                message.getSubject(),
                                Address.pack(message.getFrom()),
                                message.getSentDate() == null ? System
                                        .currentTimeMillis() : message.getSentDate()
                                        .getTime(),
                                Utility.combine(message.getFlags(), ',').toUpperCase(),
                                mFolderId,
                                Address.pack(message
                                        .getRecipients(RecipientType.TO)),
                                Address.pack(message
                                        .getRecipients(RecipientType.CC)),
                                Address.pack(message
                                        .getRecipients(RecipientType.BCC)),
                                sbHtml.length() > 0 ? sbHtml.toString() : null,
                                sbText.length() > 0 ? sbText.toString() : null,
                                Address.pack(message.getReplyTo()),
                                attachments.size(),
                                message.mId
                                });

                for (int i = 0, count = attachments.size(); i < count; i++) {
                    Part attachment = attachments.get(i);
                    saveAttachment(message.mId, attachment, false);
                }
            } catch (Exception e) {
                throw new MessagingException("Error appending message", e);
            }
        }

        /**
         * @param messageId
         * @param attachment
         * @param attachmentId -1 to create a new attachment or >= 0 to update an existing
         * @throws IOException
         * @throws MessagingException
         */
        private void saveAttachment(long messageId, Part attachment, boolean saveAsNew)
                throws IOException, MessagingException {
            long attachmentId = -1;
            Uri contentUri = null;
            int size = -1;
            File tempAttachmentFile = null;

            if ((!saveAsNew) && (attachment instanceof LocalAttachmentBodyPart)) {
                attachmentId = ((LocalAttachmentBodyPart) attachment).getAttachmentId();
            }

            if (attachment.getBody() != null) {
                Body body = attachment.getBody();
                if (body instanceof LocalAttachmentBody) {
                    contentUri = ((LocalAttachmentBody) body).getContentUri();
                }
                else {
                    /*
                     * If the attachment has a body we're expected to save it into the local store
                     * so we copy the data into a cached attachment file.
                     */
                    InputStream in = attachment.getBody().getInputStream();
                    tempAttachmentFile = File.createTempFile("att", null, mAttachmentsDir);
                    FileOutputStream out = new FileOutputStream(tempAttachmentFile);
                    size = IOUtils.copy(in, out);
                    in.close();
                    out.close();
                }
            }

            if (size == -1) {
                /*
                 * If the attachment is not yet downloaded see if we can pull a size
                 * off the Content-Disposition.
                 */
                String disposition = attachment.getDisposition();
                if (disposition != null) {
                    String s = MimeUtility.getHeaderParameter(disposition, "size");
                    if (s != null) {
                        size = Integer.parseInt(s);
                    }
                }
            }
            if (size == -1) {
                size = 0;
            }

            String storeData =
                Utility.combine(attachment.getHeader(
                        MimeHeader.HEADER_ANDROID_ATTACHMENT_STORE_DATA), ',');

            String name = MimeUtility.getHeaderParameter(attachment.getContentType(), "name");

            if (attachmentId == -1) {
                ContentValues cv = new ContentValues();
                cv.put("message_id", messageId);
                cv.put("content_uri", contentUri != null ? contentUri.toString() : null);
                cv.put("store_data", storeData);
                cv.put("size", size);
                cv.put("name", name);
                cv.put("mime_type", attachment.getMimeType());

                attachmentId = mDb.insert("attachments", "message_id", cv);
            }
            else {
                ContentValues cv = new ContentValues();
                cv.put("content_uri", contentUri != null ? contentUri.toString() : null);
                cv.put("size", size);
                mDb.update(
                        "attachments",
                        cv,
                        "id = ?",
                        new String[] { Long.toString(attachmentId) });
            }

            if (tempAttachmentFile != null) {
                File attachmentFile = new File(mAttachmentsDir, Long.toString(attachmentId));
                tempAttachmentFile.renameTo(attachmentFile);
                contentUri = AttachmentProvider.getAttachmentUri(
                        new File(mPath).getName(),
                        attachmentId);
                attachment.setBody(new LocalAttachmentBody(contentUri, mApplication));
                ContentValues cv = new ContentValues();
                cv.put("content_uri", contentUri != null ? contentUri.toString() : null);
                mDb.update(
                        "attachments",
                        cv,
                        "id = ?",
                        new String[] { Long.toString(attachmentId) });
            }

            if (attachment instanceof LocalAttachmentBodyPart) {
                ((LocalAttachmentBodyPart) attachment).setAttachmentId(attachmentId);
            }
        }

        /**
         * Changes the stored uid of the given message (using it's internal id as a key) to
         * the uid in the message.
         * @param message
         */
        public void changeUid(LocalMessage message) throws MessagingException {
            open(OpenMode.READ_WRITE);
            ContentValues cv = new ContentValues();
            cv.put("uid", message.getUid());
            mDb.update("messages", cv, "id = ?", new String[] { Long.toString(message.mId) });
        }

        @Override
        public void setFlags(Message[] messages, Flag[] flags, boolean value)
                throws MessagingException {
            open(OpenMode.READ_WRITE);
            for (Message message : messages) {
                message.setFlags(flags, value);
            }
        }

        @Override
        public Message[] expunge() throws MessagingException {
            open(OpenMode.READ_WRITE);
            ArrayList<Message> expungedMessages = new ArrayList<Message>();
            /*
             * epunge() doesn't do anything because deleted messages are saved for their uids
             * and really, really deleted messages are "Destroyed" and removed immediately.
             */
            return expungedMessages.toArray(new Message[] {});
        }

        @Override
        public void delete(boolean recurse) throws MessagingException {
            // We need to open the folder first to make sure we've got it's id
            open(OpenMode.READ_ONLY);
            Message[] messages = getMessages(null);
            for (Message message : messages) {
                deleteAttachments(message.getUid());
            }
            mDb.execSQL("DELETE FROM folders WHERE id = ?", new Object[] {
                Long.toString(mFolderId),
            });
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof LocalFolder) {
                return ((LocalFolder)o).mName.equals(mName);
            }
            return super.equals(o);
        }

        @Override
        public Flag[] getPermanentFlags() throws MessagingException {
            return PERMANENT_FLAGS;
        }

        private void deleteAttachments(String uid) throws MessagingException {
            open(OpenMode.READ_WRITE);
            Cursor messagesCursor = null;
            try {
                messagesCursor = mDb.query(
                        "messages",
                        new String[] { "id" },
                        "folder_id = ? AND uid = ?",
                        new String[] { Long.toString(mFolderId), uid },
                        null,
                        null,
                        null);
                while (messagesCursor.moveToNext()) {
                    long messageId = messagesCursor.getLong(0);
                    Cursor attachmentsCursor = null;
                    try {
                        attachmentsCursor = mDb.query(
                                "attachments",
                                new String[] { "id" },
                                "message_id = ?",
                                new String[] { Long.toString(messageId) },
                                null,
                                null,
                                null);
                        while (attachmentsCursor.moveToNext()) {
                            long attachmentId = attachmentsCursor.getLong(0);
                            try{
                                File file = new File(mAttachmentsDir, Long.toString(attachmentId));
                                if (file.exists()) {
                                    file.delete();
                                }
                            }
                            catch (Exception e) {

                            }
                        }
                    }
                    finally {
                        if (attachmentsCursor != null) {
                            attachmentsCursor.close();
                        }
                    }
                }
            }
            finally {
                if (messagesCursor != null) {
                    messagesCursor.close();
                }
            }
        }
    }

    public class LocalMessage extends MimeMessage {
        private long mId;
        private int mAttachmentCount;

        LocalMessage(String uid, Folder folder) throws MessagingException {
            this.mUid = uid;
            this.mFolder = folder;
        }

        public int getAttachmentCount() {
            return mAttachmentCount;
        }

        public void parse(InputStream in) throws IOException, MessagingException {
            super.parse(in);
        }

        public void setFlagInternal(Flag flag, boolean set) throws MessagingException {
            super.setFlag(flag, set);
        }

        public long getId() {
            return mId;
        }

        public void setFlag(Flag flag, boolean set) throws MessagingException {
            if (flag == Flag.DELETED && set) {
                /*
                 * If a message is being marked as deleted we want to clear out it's content
                 * and attachments as well. Delete will not actually remove the row since we need
                 * to retain the uid for synchronization purposes.
                 */

                /*
                 * Delete all of the messages' content to save space.
                 */
                mDb.execSQL(
                        "UPDATE messages SET " +
                        "subject = NULL, " +
                        "sender_list = NULL, " +
                        "date = NULL, " +
                        "to_list = NULL, " +
                        "cc_list = NULL, " +
                        "bcc_list = NULL, " +
                        "html_content = NULL, " +
                        "text_content = NULL, " +
                        "reply_to_list = NULL " +
                        "WHERE id = ?",
                        new Object[] {
                                mId
                        });

                ((LocalFolder) mFolder).deleteAttachments(getUid());

                /*
                 * Delete all of the messages' attachments to save space.
                 */
                mDb.execSQL("DELETE FROM attachments WHERE id = ?",
                        new Object[] {
                                mId
                        });
            }
            else if (flag == Flag.X_DESTROYED && set) {
                ((LocalFolder) mFolder).deleteAttachments(getUid());
                mDb.execSQL("DELETE FROM messages WHERE id = ?",
                        new Object[] { mId });
            }

            /*
             * Update the unread count on the folder.
             */
            try {
                if (flag == Flag.DELETED || flag == Flag.X_DESTROYED || flag == Flag.SEEN) {
                    LocalFolder folder = (LocalFolder)mFolder;
                    if (set && !isSet(Flag.SEEN)) {
                        folder.setUnreadMessageCount(folder.getUnreadMessageCount() - 1);
                    }
                    else if (!set && isSet(Flag.SEEN)) {
                        folder.setUnreadMessageCount(folder.getUnreadMessageCount() + 1);
                    }
                }
            }
            catch (MessagingException me) {
                Log.e(Email.LOG_TAG, "Unable to update LocalStore unread message count",
                        me);
                throw new RuntimeException(me);
            }

            super.setFlag(flag, set);
            /*
             * Set the flags on the message.
             */
            mDb.execSQL("UPDATE messages " + "SET flags = ? " + "WHERE id = ?", new Object[] {
                    Utility.combine(getFlags(), ',').toUpperCase(), mId
            });
        }
    }

    public class LocalAttachmentBodyPart extends MimeBodyPart {
        private long mAttachmentId = -1;

        public LocalAttachmentBodyPart(Body body, long attachmentId) throws MessagingException {
            super(body);
            mAttachmentId = attachmentId;
        }

        /**
         * Returns the local attachment id of this body, or -1 if it is not stored.
         * @return
         */
        public long getAttachmentId() {
            return mAttachmentId;
        }

        public void setAttachmentId(long attachmentId) {
            mAttachmentId = attachmentId;
        }

        public String toString() {
            return "" + mAttachmentId;
        }
    }

    public static class LocalAttachmentBody implements Body {
        private Application mApplication;
        private Uri mUri;

        public LocalAttachmentBody(Uri uri, Application application) {
            mApplication = application;
            mUri = uri;
        }

        public InputStream getInputStream() throws MessagingException {
            try {
                return mApplication.getContentResolver().openInputStream(mUri);
            }
            catch (FileNotFoundException fnfe) {
                /*
                 * Since it's completely normal for us to try to serve up attachments that
                 * have been blown away, we just return an empty stream.
                 */
                return new ByteArrayInputStream(new byte[0]);
            }
            catch (IOException ioe) {
                throw new MessagingException("Invalid attachment.", ioe);
            }
        }

        public void writeTo(OutputStream out) throws IOException, MessagingException {
            InputStream in = getInputStream();
            Base64OutputStream base64Out = new Base64OutputStream(out);
            IOUtils.copy(in, base64Out);
            base64Out.close();
        }

        public Uri getContentUri() {
            return mUri;
        }
    }
}
