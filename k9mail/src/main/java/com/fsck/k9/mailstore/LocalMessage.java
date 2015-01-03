package com.fsck.k9.mailstore;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.LockableDatabase.DbCallback;
import com.fsck.k9.mailstore.LockableDatabase.WrappedException;

public class LocalMessage extends MimeMessage {
    protected MessageReference mReference;
    private final LocalStore localStore;

    private long mId;
    private int mAttachmentCount;
    private String mSubject;

    private String mPreview = "";

    private boolean mHeadersLoaded = false;
    private boolean mMessageDirty = false;

    private long mThreadId;
    private long mRootId;

    private LocalMessage(LocalStore localStore) {
        this.localStore = localStore;
    }

    LocalMessage(LocalStore localStore, String uid, Folder folder) {
        this.localStore = localStore;
        this.mUid = uid;
        this.mFolder = folder;
    }

    void populateFromGetMessageCursor(Cursor cursor) throws MessagingException {
        final String subject = cursor.getString(0);
        this.setSubject(subject == null ? "" : subject);

        Address[] from = Address.unpack(cursor.getString(1));
        if (from.length > 0) {
            this.setFrom(from[0]);
        }
        this.setInternalSentDate(new Date(cursor.getLong(2)));
        this.setUid(cursor.getString(3));
        String flagList = cursor.getString(4);
        if (flagList != null && flagList.length() > 0) {
            String[] flags = flagList.split(",");

            for (String flag : flags) {
                try {
                    this.setFlagInternal(Flag.valueOf(flag), true);
                }

                catch (Exception e) {
                    if (!"X_BAD_FLAG".equals(flag)) {
                        Log.w(K9.LOG_TAG, "Unable to parse flag " + flag);
                    }
                }
            }
        }
        this.mId = cursor.getLong(5);
        this.setRecipients(RecipientType.TO, Address.unpack(cursor.getString(6)));
        this.setRecipients(RecipientType.CC, Address.unpack(cursor.getString(7)));
        this.setRecipients(RecipientType.BCC, Address.unpack(cursor.getString(8)));
        this.setReplyTo(Address.unpack(cursor.getString(9)));

        this.mAttachmentCount = cursor.getInt(10);
        this.setInternalDate(new Date(cursor.getLong(11)));
        this.setMessageId(cursor.getString(12));

        final String preview = cursor.getString(14);
        mPreview = (preview == null ? "" : preview);

        if (this.mFolder == null) {
            LocalFolder f = new LocalFolder(this.localStore, cursor.getInt(13));
            f.open(LocalFolder.OPEN_MODE_RW);
            this.mFolder = f;
        }

        mThreadId = (cursor.isNull(15)) ? -1 : cursor.getLong(15);
        mRootId = (cursor.isNull(16)) ? -1 : cursor.getLong(16);

        boolean deleted = (cursor.getInt(17) == 1);
        boolean read = (cursor.getInt(18) == 1);
        boolean flagged = (cursor.getInt(19) == 1);
        boolean answered = (cursor.getInt(20) == 1);
        boolean forwarded = (cursor.getInt(21) == 1);

        setFlagInternal(Flag.DELETED, deleted);
        setFlagInternal(Flag.SEEN, read);
        setFlagInternal(Flag.FLAGGED, flagged);
        setFlagInternal(Flag.ANSWERED, answered);
        setFlagInternal(Flag.FORWARDED, forwarded);
    }

    /**
     * Fetch the message text for display. This always returns an HTML-ified version of the
     * message, even if it was originally a text-only message.
     * @return HTML version of message for display purposes or null.
     * @throws MessagingException
     */
    public String getTextForDisplay() throws MessagingException {
        String text = null;    // First try and fetch an HTML part.
        Part part = MimeUtility.findFirstPartByMimeType(this, "text/html");
        if (part == null) {
            // If that fails, try and get a text part.
            part = MimeUtility.findFirstPartByMimeType(this, "text/plain");
            if (part != null && part.getBody() instanceof LocalTextBody) {
                text = ((LocalTextBody) part.getBody()).getBodyForDisplay();
            }
        } else {
            // We successfully found an HTML part; do the necessary character set decoding.
            text = MessageExtractor.getTextFromPart(part);
        }
        return text;
    }


    /* Custom version of writeTo that updates the MIME message based on localMessage
     * changes.
     */

    @Override
    public void writeTo(OutputStream out) throws IOException, MessagingException {
        if (mMessageDirty) buildMimeRepresentation();
        super.writeTo(out);
    }

    void buildMimeRepresentation() throws MessagingException {
        if (!mMessageDirty) {
            return;
        }

        super.setSubject(mSubject);
        if (this.mFrom != null && this.mFrom.length > 0) {
            super.setFrom(this.mFrom[0]);
        }

        super.setReplyTo(mReplyTo);
        super.setSentDate(this.getSentDate(), K9.hideTimeZone());
        super.setRecipients(RecipientType.TO, mTo);
        super.setRecipients(RecipientType.CC, mCc);
        super.setRecipients(RecipientType.BCC, mBcc);
        if (mMessageId != null) super.setMessageId(mMessageId);

        mMessageDirty = false;
    }

    @Override
    public String getPreview() {
        return mPreview;
    }

    @Override
    public String getSubject() {
        return mSubject;
    }


    @Override
    public void setSubject(String subject) throws MessagingException {
        mSubject = subject;
        mMessageDirty = true;
    }


    @Override
    public void setMessageId(String messageId) {
        mMessageId = messageId;
        mMessageDirty = true;
    }

    @Override
    public void setUid(String uid) {
        super.setUid(uid);
        this.mReference = null;
    }

    @Override
    public boolean hasAttachments() {
        return (mAttachmentCount > 0);
    }

    public int getAttachmentCount() {
        return mAttachmentCount;
    }

    @Override
    public void setFrom(Address from) throws MessagingException {
        this.mFrom = new Address[] { from };
        mMessageDirty = true;
    }


    @Override
    public void setReplyTo(Address[] replyTo) throws MessagingException {
        if (replyTo == null || replyTo.length == 0) {
            mReplyTo = null;
        } else {
            mReplyTo = replyTo;
        }
        mMessageDirty = true;
    }


    /*
     * For performance reasons, we add headers instead of setting them (see super implementation)
     * which removes (expensive) them before adding them
     */
    @Override
    public void setRecipients(RecipientType type, Address[] addresses) throws MessagingException {
        if (type == RecipientType.TO) {
            if (addresses == null || addresses.length == 0) {
                this.mTo = null;
            } else {
                this.mTo = addresses;
            }
        } else if (type == RecipientType.CC) {
            if (addresses == null || addresses.length == 0) {
                this.mCc = null;
            } else {
                this.mCc = addresses;
            }
        } else if (type == RecipientType.BCC) {
            if (addresses == null || addresses.length == 0) {
                this.mBcc = null;
            } else {
                this.mBcc = addresses;
            }
        } else {
            throw new MessagingException("Unrecognized recipient type.");
        }
        mMessageDirty = true;
    }

    public void setFlagInternal(Flag flag, boolean set) throws MessagingException {
        super.setFlag(flag, set);
    }

    @Override
    public long getId() {
        return mId;
    }

    @Override
    public void setFlag(final Flag flag, final boolean set) throws MessagingException {

        try {
            this.localStore.database.execute(true, new DbCallback<Void>() {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
                    try {
                        if (flag == Flag.DELETED && set) {
                            delete();
                        }

                        LocalMessage.super.setFlag(flag, set);
                    } catch (MessagingException e) {
                        throw new WrappedException(e);
                    }
                    /*
                     * Set the flags on the message.
                     */
                    ContentValues cv = new ContentValues();
                    cv.put("flags", LocalMessage.this.localStore.serializeFlags(getFlags()));
                    cv.put("read", isSet(Flag.SEEN) ? 1 : 0);
                    cv.put("flagged", isSet(Flag.FLAGGED) ? 1 : 0);
                    cv.put("answered", isSet(Flag.ANSWERED) ? 1 : 0);
                    cv.put("forwarded", isSet(Flag.FORWARDED) ? 1 : 0);

                    db.update("messages", cv, "id = ?", new String[] { Long.toString(mId) });

                    return null;
                }
            });
        } catch (WrappedException e) {
            throw(MessagingException) e.getCause();
        }

        this.localStore.notifyChange();
    }

    /*
     * If a message is being marked as deleted we want to clear out it's content
     * and attachments as well. Delete will not actually remove the row since we need
     * to retain the uid for synchronization purposes.
     */
    private void delete() throws MessagingException

    {
        /*
         * Delete all of the message's content to save space.
         */
        try {
            this.localStore.database.execute(true, new DbCallback<Void>() {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException,
                        UnavailableStorageException {
                    String[] idArg = new String[] { Long.toString(mId) };

                    ContentValues cv = new ContentValues();
                    cv.put("deleted", 1);
                    cv.put("empty", 1);
                    cv.putNull("subject");
                    cv.putNull("sender_list");
                    cv.putNull("date");
                    cv.putNull("to_list");
                    cv.putNull("cc_list");
                    cv.putNull("bcc_list");
                    cv.putNull("preview");
                    cv.putNull("html_content");
                    cv.putNull("text_content");
                    cv.putNull("reply_to_list");

                    db.update("messages", cv, "id = ?", idArg);

                    /*
                     * Delete all of the message's attachments to save space.
                     * We do this explicit deletion here because we're not deleting the record
                     * in messages, which means our ON DELETE trigger for messages won't cascade
                     */
                    try {
                        ((LocalFolder) mFolder).deleteAttachments(mId);
                    } catch (MessagingException e) {
                        throw new WrappedException(e);
                    }

                    db.delete("attachments", "message_id = ?", idArg);
                    return null;
                }
            });
        } catch (WrappedException e) {
            throw(MessagingException) e.getCause();
        }
        ((LocalFolder)mFolder).deleteHeaders(mId);

        this.localStore.notifyChange();
    }

    /*
     * Completely remove a message from the local database
     *
     * TODO: document how this updates the thread structure
     */
    @Override
    public void destroy() throws MessagingException {
        try {
            this.localStore.database.execute(true, new DbCallback<Void>() {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException,
                    UnavailableStorageException {
                    try {
                        LocalFolder localFolder = (LocalFolder) mFolder;

                        localFolder.deleteAttachments(mId);

                        if (hasThreadChildren(db, mId)) {
                            // This message has children in the thread structure so we need to
                            // make it an empty message.
                            ContentValues cv = new ContentValues();
                            cv.put("id", mId);
                            cv.put("folder_id", localFolder.getId());
                            cv.put("deleted", 0);
                            cv.put("message_id", getMessageId());
                            cv.put("empty", 1);

                            db.replace("messages", null, cv);

                            // Nothing else to do
                            return null;
                        }

                        // Get the message ID of the parent message if it's empty
                        long currentId = getEmptyThreadParent(db, mId);

                        // Delete the placeholder message
                        deleteMessageRow(db, mId);

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

                    } catch (MessagingException e) {
                        throw new WrappedException(e);
                    }
                    return null;
                }
            });
        } catch (WrappedException e) {
            throw(MessagingException) e.getCause();
        }

        this.localStore.notifyChange();
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
     * Delete a message from the 'messages' and 'threads' tables.
     *
     * @param db
     *         {@link SQLiteDatabase} instance to access the database.
     * @param messageId
     *         The database ID of the message to delete.
     */
    private void deleteMessageRow(SQLiteDatabase db, long messageId) {
        String[] idArg = { Long.toString(messageId) };

        // Delete the message
        db.delete("messages", "id = ?", idArg);

        // Delete row in 'threads' table
        // TODO: create trigger for 'messages' table to get rid of the row in 'threads' table
        db.delete("threads", "message_id = ?", idArg);
    }

    private void loadHeaders() throws MessagingException {
        List<LocalMessage> messages = new ArrayList<LocalMessage>();
        messages.add(this);
        mHeadersLoaded = true; // set true before calling populate headers to stop recursion
        getFolder().populateHeaders(messages);

    }

    @Override
    public void addHeader(String name, String value) throws MessagingException {
        if (!mHeadersLoaded)
            loadHeaders();
        super.addHeader(name, value);
    }

    @Override
    public void addRawHeader(String name, String raw) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void setHeader(String name, String value) throws MessagingException {
        if (!mHeadersLoaded)
            loadHeaders();
        super.setHeader(name, value);
    }

    @Override
    public String[] getHeader(String name) throws MessagingException {
        if (!mHeadersLoaded)
            loadHeaders();
        return super.getHeader(name);
    }

    @Override
    public void removeHeader(String name) throws MessagingException {
        if (!mHeadersLoaded)
            loadHeaders();
        super.removeHeader(name);
    }

    @Override
    public Set<String> getHeaderNames() throws MessagingException {
        if (!mHeadersLoaded)
            loadHeaders();
        return super.getHeaderNames();
    }

    @Override
    public LocalMessage clone() {
        LocalMessage message = new LocalMessage(this.localStore);
        super.copy(message);

        message.mId = mId;
        message.mAttachmentCount = mAttachmentCount;
        message.mSubject = mSubject;
        message.mPreview = mPreview;
        message.mHeadersLoaded = mHeadersLoaded;
        message.mMessageDirty = mMessageDirty;

        return message;
    }

    public long getThreadId() {
        return mThreadId;
    }

    public long getRootId() {
        return mRootId;
    }

    public Account getAccount() {
        return localStore.getAccount();
    }

    public MessageReference makeMessageReference() {
        if (mReference == null) {
            mReference = new MessageReference();
            mReference.folderName  = getFolder().getName();
            mReference.uid = mUid;
            mReference.accountUuid = getFolder().getAccountUuid();
        }
        return mReference;
    }

    @Override
    protected void copy(MimeMessage destination) {
        super.copy(destination);
        if (destination instanceof LocalMessage) {
            ((LocalMessage)destination).mReference = mReference;
        }
    }

    @Override
    public LocalFolder getFolder() {
        return (LocalFolder) super.getFolder();
    }

    public String getUri() {
        return "email://messages/" +  getAccount().getAccountNumber() + "/" + getFolder().getName() + "/" + getUid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final LocalMessage that = (LocalMessage) o;
        return !(getAccountUuid() != null ? !getAccountUuid().equals(that.getAccountUuid()) : that.getAccountUuid() != null);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getAccountUuid() != null ? getAccountUuid().hashCode() : 0);
        return result;
    }

    private String getAccountUuid() {
        return getAccount().getUuid();
    }
}
