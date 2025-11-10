package com.fsck.k9.mailstore;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Objects;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.VisibleForTesting;
import app.k9mail.legacy.message.controller.MessageReference;
import com.fsck.k9.core.BuildConfig;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import net.thunderbird.core.common.exception.MessagingException;
import com.fsck.k9.mail.MimeType;
import com.fsck.k9.mail.internet.AddressHeaderBuilder;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.message.MessageHeaderParser;
import com.fsck.k9.mailstore.LockableDatabase.DbCallback;
import app.k9mail.legacy.message.extractors.PreviewResult.PreviewType;
import net.thunderbird.core.android.account.LegacyAccountDto;
import net.thunderbird.core.logging.legacy.Log;
import net.thunderbird.core.preference.GeneralSettingsManager;


public class LocalMessage extends MimeMessage {
    private final LocalStore localStore;

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
    private LocalFolder mFolder;
    private GeneralSettingsManager generalSettingsManager;

    LocalMessage(LocalStore localStore, String uid, LocalFolder folder, GeneralSettingsManager generalSettingsManager) {
        this.localStore = localStore;
        this.mUid = uid;
        this.mFolder = folder;
        this.generalSettingsManager = generalSettingsManager;
    }

    LocalMessage(LocalStore localStore, long databaseId, LocalFolder folder, GeneralSettingsManager generalSettingsManager) {
        this.localStore = localStore;
        this.databaseId = databaseId;
        this.mFolder = folder;
        this.generalSettingsManager = generalSettingsManager;
    }


    void populateFromGetMessageCursor(Cursor cursor) throws MessagingException {
        final String subject = cursor.getString(LocalStore.MSG_INDEX_SUBJECT);
        this.setSubject(subject == null ? "" : subject);

        Address[] from = Address.unpack(cursor.getString(LocalStore.MSG_INDEX_SENDER_LIST));
        if (from.length > 0) {
            this.setFrom(from[0]);
        }
        this.setInternalSentDate(new Date(cursor.getLong(LocalStore.MSG_INDEX_DATE)));
        this.setUid(cursor.getString(LocalStore.MSG_INDEX_UID));
        String flagList = cursor.getString(LocalStore.MSG_INDEX_FLAGS);
        if (flagList != null && flagList.length() > 0) {
            String[] flags = flagList.split(",");

            for (String flag : flags) {
                try {
                    this.setFlagInternal(Flag.valueOf(flag), true);
                }

                catch (Exception e) {
                    if (!"X_BAD_FLAG".equals(flag)) {
                        Log.w("Unable to parse flag %s", flag);
                    }
                }
            }
        }
        this.databaseId = cursor.getLong(LocalStore.MSG_INDEX_ID);
        mTo = getAddressesOrNull(Address.unpack(cursor.getString(LocalStore.MSG_INDEX_TO)));
        mCc = getAddressesOrNull(Address.unpack(cursor.getString(LocalStore.MSG_INDEX_CC)));
        mBcc = getAddressesOrNull(Address.unpack(cursor.getString(LocalStore.MSG_INDEX_BCC)));
        headerNeedsUpdating = true;
        this.setReplyTo(Address.unpack(cursor.getString(LocalStore.MSG_INDEX_REPLY_TO)));

        this.attachmentCount = cursor.getInt(LocalStore.MSG_INDEX_ATTACHMENT_COUNT);
        this.setInternalDate(new Date(cursor.getLong(LocalStore.MSG_INDEX_INTERNAL_DATE)));
        this.setMessageId(cursor.getString(LocalStore.MSG_INDEX_MESSAGE_ID_HEADER));

        String previewTypeString = cursor.getString(LocalStore.MSG_INDEX_PREVIEW_TYPE);
        DatabasePreviewType databasePreviewType = DatabasePreviewType.fromDatabaseValue(previewTypeString);
        previewType = databasePreviewType.previewType;
        if (previewType == PreviewType.TEXT) {
            preview = cursor.getString(LocalStore.MSG_INDEX_PREVIEW);
        } else {
            preview = "";
        }

        if (this.mFolder == null) {
            LocalFolder f = new LocalFolder(this.localStore, cursor.getInt(LocalStore.MSG_INDEX_FOLDER_ID), generalSettingsManager);
            f.open();
            this.mFolder = f;
        }

        threadId = (cursor.isNull(LocalStore.MSG_INDEX_THREAD_ID)) ? -1 : cursor.getLong(LocalStore.MSG_INDEX_THREAD_ID);
        rootId = (cursor.isNull(LocalStore.MSG_INDEX_THREAD_ROOT_ID)) ? -1 : cursor.getLong(LocalStore.MSG_INDEX_THREAD_ROOT_ID);

        boolean deleted = (cursor.getInt(LocalStore.MSG_INDEX_FLAG_DELETED) == 1);
        boolean read = (cursor.getInt(LocalStore.MSG_INDEX_FLAG_READ) == 1);
        boolean flagged = (cursor.getInt(LocalStore.MSG_INDEX_FLAG_FLAGGED) == 1);
        boolean answered = (cursor.getInt(LocalStore.MSG_INDEX_FLAG_ANSWERED) == 1);
        boolean forwarded = (cursor.getInt(LocalStore.MSG_INDEX_FLAG_FORWARDED) == 1);

        setFlagInternal(Flag.DELETED, deleted);
        setFlagInternal(Flag.SEEN, read);
        setFlagInternal(Flag.FLAGGED, flagged);
        setFlagInternal(Flag.ANSWERED, answered);
        setFlagInternal(Flag.FORWARDED, forwarded);

        setMessagePartId(cursor.getLong(LocalStore.MSG_INDEX_MESSAGE_PART_ID));
        MimeType mimeType = MimeType.parseOrNull(cursor.getString(LocalStore.MSG_INDEX_MIME_TYPE));
        this.mimeType = mimeType != null ? mimeType.toString() : DEFAULT_MIME_TYPE;

        byte[] header = cursor.getBlob(LocalStore.MSG_INDEX_HEADER_DATA);
        if (header != null) {
            MessageHeaderParser.parse(new ByteArrayInputStream(header), this::addRawHeader);
        } else {
            Log.d("No headers available for this message!");
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
        return preview;
    }

    @Override
    public String getSubject() {
        return subject;
    }


    @Override
    public void setSubject(String subject) {
        this.subject = subject;
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
        this.messageReference = null;
    }

    @Override
    public boolean hasAttachments() {
        return (attachmentCount > 0);
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

    private Address[] getAddressesOrNull(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        } else {
            return addresses;
        }
    }

    public void setFlagInternal(Flag flag, boolean set) throws MessagingException {
        super.setFlag(flag, set);
    }

    public long getDatabaseId() {
        return databaseId;
    }

    public boolean hasCachedDecryptedSubject() {
        return isSet(Flag.X_SUBJECT_DECRYPTED);
    }

    public void setCachedDecryptedSubject(final String decryptedSubject) throws MessagingException {
        this.localStore.getDatabase().execute(true, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws MessagingException {
                LocalMessage.super.setFlag(Flag.X_SUBJECT_DECRYPTED, true);

                ContentValues cv = new ContentValues();
                cv.put("subject", decryptedSubject);
                cv.put("flags", LocalStore.serializeFlags(getFlags()));

                db.update("messages", cv, "id = ?", new String[] { Long.toString(databaseId) });

                return null;
            }
        });

        this.localStore.notifyChange();
    }

    @Override
    public void setFlag(final Flag flag, final boolean set) throws MessagingException {
        this.localStore.getDatabase().execute(true, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws MessagingException {
                if (flag == Flag.DELETED && set) {
                    delete();
                }

                LocalMessage.super.setFlag(flag, set);

                /*
                 * Set the flags on the message.
                 */
                ContentValues cv = new ContentValues();
                cv.put("flags", LocalStore.serializeFlags(getFlags()));
                cv.put("read", isSet(Flag.SEEN) ? 1 : 0);
                cv.put("flagged", isSet(Flag.FLAGGED) ? 1 : 0);
                cv.put("answered", isSet(Flag.ANSWERED) ? 1 : 0);
                cv.put("forwarded", isSet(Flag.FORWARDED) ? 1 : 0);

                db.update("messages", cv, "id = ?", new String[] { Long.toString(databaseId) });

                return null;
            }
        });

        this.localStore.notifyChange();
    }

    /*
     * If a message is being marked as deleted we want to clear out its content. Delete will not actually remove the
     * row since we need to retain the UID for synchronization purposes.
     */
    public void delete() throws MessagingException {
        localStore.getDatabase().execute(true, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws MessagingException {
                ContentValues cv = new ContentValues();
                cv.put("deleted", 1);
                cv.put("preview_type", DatabasePreviewType.fromPreviewType(PreviewType.NONE).databaseValue);
                cv.put("read", 0);
                cv.put("flagged", 0);
                cv.put("answered", 0);
                cv.put("forwarded", 0);
                cv.putNull("subject");
                cv.putNull("sender_list");
                cv.putNull("date");
                cv.putNull("to_list");
                cv.putNull("cc_list");
                cv.putNull("bcc_list");
                cv.putNull("preview");
                cv.putNull("reply_to_list");
                cv.putNull("message_part_id");
                cv.putNull("flags");
                cv.putNull("attachment_count");
                cv.putNull("internal_date");
                cv.putNull("mime_type");
                cv.putNull("encryption_type");

                db.update("messages", cv, "id = ?", new String[] { Long.toString(databaseId) });

                mFolder.deleteMessagePartsAndDataFromDisk(messagePartId);

                getFolder().deleteFulltextIndexEntry(db, databaseId);

                return null;
            }
        });

        localStore.notifyChange();
    }

    public void debugClearLocalData() throws MessagingException {
        if (!BuildConfig.DEBUG) {
            throw new AssertionError("method must only be used in developer mode!");
        }

        localStore.getDatabase().execute(true, new DbCallback<Void>() {
            @Override
            public Void doDbWork(final SQLiteDatabase db) throws MessagingException {
                ContentValues cv = new ContentValues();
                cv.putNull("message_part_id");

                db.update("messages", cv, "id = ?", new String[] { Long.toString(databaseId) });

                mFolder.deleteMessagePartsAndDataFromDisk(messagePartId);

                setFlag(Flag.X_DOWNLOADED_FULL, false);
                setFlag(Flag.X_DOWNLOADED_PARTIAL, false);

                return null;
            }
        });

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

    public long getThreadId() {
        return threadId;
    }

    public long getRootId() {
        return rootId;
    }

    public LegacyAccountDto getAccount() {
        return localStore.getAccount();
    }

    public MessageReference makeMessageReference() {
        if (messageReference == null) {
            String accountUuid = getFolder().getAccountUuid();
            long folderId = getFolder().getDatabaseId();
            messageReference = new MessageReference(accountUuid, folderId, mUid);
        }
        return messageReference;
    }

    public LocalFolder getFolder() {
        return mFolder;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException, MessagingException {
        if (headerNeedsUpdating) {
            updateHeader();
        }

        super.writeTo(out);
    }

    private void updateHeader() {
        super.setSubject(subject);
        super.setReplyTo(mReplyTo);

        setRecipients("To", mTo);
        setRecipients("CC", mCc);
        setRecipients("BCC", mBcc);

        if (mFrom != null && mFrom.length > 0) {
            super.setFrom(mFrom[0]);
        }

        if (mMessageId != null) {
            super.setMessageId(mMessageId);
        }

        headerNeedsUpdating = false;
    }

    private void setRecipients(String headerName, Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            removeHeader(headerName);
        } else {
            String headerValue = AddressHeaderBuilder.createHeaderValue(addresses);
            setHeader(headerName, headerValue);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LocalMessage other = (LocalMessage) o;
        return Objects.equals(mUid, other.mUid) &&
                Objects.equals(mFolder, other.mFolder) &&
                Objects.equals(getAccountUuid(), other.getAccountUuid());
    }

    @Override
    public int hashCode() {
        final int MULTIPLIER = 31;

        int result = 1;
        String accountUuid = getAccountUuid();
        result = MULTIPLIER * result + (accountUuid != null ? accountUuid.hashCode() : 0);
        result = MULTIPLIER * result + (mFolder != null ? mFolder.hashCode() : 0);
        result = MULTIPLIER * result + mUid.hashCode();
        return result;
    }

    private String getAccountUuid() {
        return getAccount().getUuid();
    }
}
