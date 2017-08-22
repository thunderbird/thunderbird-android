package com.fsck.k9.mailstore;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.VisibleForTesting;

import com.fsck.k9.Account;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.message.MessageHeaderParser;
import com.fsck.k9.mailstore.LockableDatabase.DbCallback;
import com.fsck.k9.mailstore.LockableDatabase.WrappedException;
import com.fsck.k9.message.extractors.PreviewResult.PreviewType;
import timber.log.Timber;


public class LocalMessage extends MimeMessage {
    protected MessageReference mReference;
    private final LocalStore localStore;

    private long mId;
    private int mAttachmentCount;
    private String mSubject;

    private String mPreview = "";

    private long mThreadId;
    private long mRootId;
    private long messagePartId;
    private String mimeType;
    private PreviewType previewType;
    private boolean headerNeedsUpdating = false;


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
                        Timber.w("Unable to parse flag %s", flag);
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

        String previewTypeString = cursor.getString(24);
        DatabasePreviewType databasePreviewType = DatabasePreviewType.fromDatabaseValue(previewTypeString);
        previewType = databasePreviewType.getPreviewType();
        if (previewType == PreviewType.TEXT) {
            mPreview = cursor.getString(14);
        } else {
            mPreview = "";
        }

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

        setMessagePartId(cursor.getLong(22));
        mimeType = cursor.getString(23);

        byte[] header = cursor.getBlob(25);
        if (header != null) {
            MessageHeaderParser.parse(this, new ByteArrayInputStream(header));
        } else {
            Timber.d("No headers available for this message!");
        }
        
        headerNeedsUpdating = false;
    }

    @VisibleForTesting
    void setMessagePartId(long messagePartId) {
        this.messagePartId = messagePartId;
    }

    public long getMessagePartId() {
        return messagePartId;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    /* Custom version of writeTo that updates the MIME message based on localMessage
     * changes.
     */

    public PreviewType getPreviewType() {
        return previewType;
    }

    public String getPreview() {
        return mPreview;
    }

    @Override
    public String getSubject() {
        return mSubject;
    }


    @Override
    public void setSubject(String subject) {
        mSubject = subject;
        headerNeedsUpdating = true;
    }


    @Override
    public void setMessageId(String messageId) {
        mMessageId = messageId;
        headerNeedsUpdating = true;
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
    public void setFrom(Address from) {
        this.mFrom = new Address[] { from };
        headerNeedsUpdating = true;
    }


    @Override
    public void setReplyTo(Address[] replyTo) {
        if (replyTo == null || replyTo.length == 0) {
            mReplyTo = null;
        } else {
            mReplyTo = replyTo;
        }

        headerNeedsUpdating = true;
    }


    /*
     * For performance reasons, we add headers instead of setting them (see super implementation)
     * which removes (expensive) them before adding them
     */
    @Override
    public void setRecipients(RecipientType type, Address[] addresses) {
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
            throw new IllegalArgumentException("Unrecognized recipient type.");
        }

        headerNeedsUpdating = true;
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
            this.localStore.getDatabase().execute(true, new DbCallback<Void>() {
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
     * If a message is being marked as deleted we want to clear out its content. Delete will not actually remove the
     * row since we need to retain the UID for synchronization purposes.
     */
    private void delete() throws MessagingException {
        try {
            localStore.getDatabase().execute(true, new DbCallback<Void>() {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException, UnavailableStorageException {
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
                    cv.putNull("reply_to_list");
                    cv.putNull("message_part_id");

                    db.update("messages", cv, "id = ?", new String[] { Long.toString(mId) });

                    try {
                        ((LocalFolder) mFolder).deleteMessagePartsAndDataFromDisk(messagePartId);
                    } catch (MessagingException e) {
                        throw new WrappedException(e);
                    }

                    getFolder().deleteFulltextIndexEntry(db, mId);

                    return null;
                }
            });
        } catch (WrappedException e) {
            throw (MessagingException) e.getCause();
        }

        localStore.notifyChange();
    }

    public void debugClearLocalData() throws MessagingException {
        if (!BuildConfig.DEBUG) {
            throw new AssertionError("method must only be used in debug build!");
        }

        try {
            localStore.getDatabase().execute(true, new DbCallback<Void>() {
                @Override
                public Void doDbWork(final SQLiteDatabase db) throws WrappedException, MessagingException {
                    ContentValues cv = new ContentValues();
                    cv.putNull("message_part_id");

                    db.update("messages", cv, "id = ?", new String[] { Long.toString(mId) });

                    try {
                        ((LocalFolder) mFolder).deleteMessagePartsAndDataFromDisk(messagePartId);
                    } catch (MessagingException e) {
                        throw new WrappedException(e);
                    }

                    setFlag(Flag.X_DOWNLOADED_FULL, false);
                    setFlag(Flag.X_DOWNLOADED_PARTIAL, false);

                    return null;
                }
            });
        } catch (WrappedException e) {
            throw (MessagingException) e.getCause();
        }

        localStore.notifyChange();
    }

    /*
     * Completely remove a message from the local database
     *
     * TODO: document how this updates the thread structure
     */
    @Override
    public void destroy() throws MessagingException {
        getFolder().destroyMessage(this);
    }

    @Override
    public LocalMessage clone() {
        LocalMessage message = new LocalMessage(localStore);
        super.copy(message);

        message.mReference = mReference;
        message.mId = mId;
        message.mAttachmentCount = mAttachmentCount;
        message.mSubject = mSubject;
        message.mPreview = mPreview;
        message.mThreadId = mThreadId;
        message.mRootId = mRootId;
        message.messagePartId = messagePartId;
        message.mimeType = mimeType;
        message.previewType = previewType;
        message.headerNeedsUpdating = headerNeedsUpdating;

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
            mReference = new MessageReference(getFolder().getAccountUuid(), getFolder().getName(), mUid, null);
        }
        return mReference;
    }

    @Override
    public LocalFolder getFolder() {
        return (LocalFolder) super.getFolder();
    }

    public String getUri() {
        return "email://messages/" +  getAccount().getAccountNumber() + "/" + getFolder().getName() + "/" + getUid();
    }

    @Override
    public void writeTo(OutputStream out) throws IOException, MessagingException {
        if (headerNeedsUpdating) {
            updateHeader();
        }
        
        super.writeTo(out);
    }

    private void updateHeader() {
        super.setSubject(mSubject);
        super.setReplyTo(mReplyTo);
        super.setRecipients(RecipientType.TO, mTo);
        super.setRecipients(RecipientType.CC, mCc);
        super.setRecipients(RecipientType.BCC, mBcc);

        if (mFrom != null && mFrom.length > 0) {
            super.setFrom(mFrom[0]);
        }

        if (mMessageId != null) {
            super.setMessageId(mMessageId);
        }
        
        headerNeedsUpdating = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        // distinguish by account uuid, in addition to what Message.equals() does above
        String thisAccountUuid = getAccountUuid();
        String thatAccountUuid = ((LocalMessage) o).getAccountUuid();
        return thisAccountUuid != null ? thisAccountUuid.equals(thatAccountUuid) : thatAccountUuid == null;
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

    public boolean isBodyMissing() {
        return getBody() == null;
    }
}
