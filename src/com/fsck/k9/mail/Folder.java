package com.fsck.k9.mail;

import java.util.Date;
import java.util.List;
import java.util.Map;

import android.util.Log;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessageRetrievalListener;


public abstract class Folder {
    protected final Account mAccount;

    private String status = null;
    private long lastChecked = 0;
    private long lastPush = 0;

    public static final int OPEN_MODE_RW=0;
    public static final int OPEN_MODE_RO=1;

    // NONE is obsolete, it will be translated to NO_CLASS for display and to INHERITED for sync and push
    public enum FolderClass {
        NONE, NO_CLASS, INHERITED, FIRST_CLASS, SECOND_CLASS
    }

    public enum FolderType {
        HOLDS_FOLDERS, HOLDS_MESSAGES,
    }

    protected Folder(Account account) {
        mAccount = account;
    }

    /**
     * Forces an open of the MailProvider. If the provider is already open this
     * function returns without doing anything.
     *
     * @param mode READ_ONLY or READ_WRITE
     */
    public abstract void open(int mode) throws MessagingException;

    /**
     * Forces a close of the MailProvider. Any further access will attempt to
     * reopen the MailProvider.
     */
    public abstract void close();

    /**
     * @return True if further commands are not expected to have to open the
     *         connection.
     */
    public abstract boolean isOpen();

    /**
     * Get the mode the folder was opened with. This may be different than the mode the open
     * was requested with.
     * @return
     */
    public abstract int getMode();

    public abstract boolean create(FolderType type) throws MessagingException;

    /**
     * Create a new folder with a specified display limit.  Not abstract to allow
     * remote folders to not override or worry about this call if they don't care to.
     */
    public boolean create(FolderType type, int displayLimit) throws MessagingException {
        return create(type);
    }

    public abstract boolean exists() throws MessagingException;

    /**
     * @return A count of the messages in the selected folder.
     */
    public abstract int getMessageCount() throws MessagingException;

    public abstract int getUnreadMessageCount() throws MessagingException;
    public abstract int getFlaggedMessageCount() throws MessagingException;

    public abstract Message getMessage(String uid) throws MessagingException;

    /**
     * Fetch the shells of messages between a range of UIDs and after a given date.
     * @param start UID sequence start
     * @param end UID sequence end
     * @param earliestDate Date to start on
     * @param listener Listener to notify as we download messages.
     * @return List of messages
     * @throws MessagingException
     */
    public abstract Message[] getMessages(int start, int end, Date earliestDate, MessageRetrievalListener listener) throws MessagingException;

    /**
     * Fetches the given list of messages. The specified listener is notified as
     * each fetch completes. Messages are downloaded as (as) lightweight (as
     * possible) objects to be filled in with later requests. In most cases this
     * means that only the UID is downloaded.
     * @param listener Listener to notify as we download messages.
     * @return List of messages
     */
    public abstract Message[] getMessages(MessageRetrievalListener listener) throws MessagingException;

    public Message[] getMessages(MessageRetrievalListener listener, boolean includeDeleted) throws MessagingException {
        return getMessages(listener);
    }

    public abstract Message[] getMessages(String[] uids, MessageRetrievalListener listener)
    throws MessagingException;

    public abstract Map<String, String> appendMessages(Message[] messages) throws MessagingException;

    public Map<String, String> copyMessages(Message[] msgs, Folder folder) throws MessagingException {
        return null;
    }

    public Map<String, String> moveMessages(Message[] msgs, Folder folder) throws MessagingException {
        return null;
    }

    public void delete(Message[] msgs, String trashFolderName) throws MessagingException {
        for (Message message : msgs) {
            Message myMessage = getMessage(message.getUid());
            myMessage.delete(trashFolderName);
        }
    }

    public abstract void setFlags(Message[] messages, Flag[] flags, boolean value)
    throws MessagingException;

    public abstract void setFlags(Flag[] flags, boolean value) throws MessagingException;

    public abstract String getUidFromMessageId(Message message) throws MessagingException;

    public void expunge() throws MessagingException
        {}

    /**
     * Populate a list of messages based upon a FetchProfile.  See {@link FetchProfile} for the things that can
     * be fetched.
     * @param messages Messages to populate
     * @param fp Things to download
     * @param listener Listener to notify as we fetch messages.
     * @throws MessagingException
     */
    public abstract void fetch(Message[] messages, FetchProfile fp,
                               MessageRetrievalListener listener) throws MessagingException;

    public void fetchPart(Message message, Part part,
                          MessageRetrievalListener listener) throws MessagingException {
        // This is causing trouble. Disabled for now. See issue 1733
        //throw new RuntimeException("fetchPart() not implemented.");

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "fetchPart() not implemented.");
    }

    public abstract void delete(boolean recurse) throws MessagingException;

    public abstract String getName();


    /**
     * Indicated by the server "\*" ( * OK [PERMANENTFLAGS (\Answered .. \*)] Flags permitted). that
     * new keywords may be created
     */
    protected boolean mCanCreateKeywords = false;

    /**
     * 
     * @param oldPushState
     * @param message
     * @return empty string to clear the pushState, null to leave the state as-is
     */
    public String getNewPushState(String oldPushState, Message message) {
        return null;
    }

    public boolean isFlagSupported(Flag flag) {
        return true;
    }

    public boolean supportsFetchingFlags() {
        return true;
    }

    @Override
    public String toString() {
        return getName();
    }

    public long getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(long lastChecked) throws MessagingException {
        this.lastChecked = lastChecked;
    }

    public long getLastPush() {
        return lastPush;
    }

    public void setLastPush(long lastCheckedDisplay) throws MessagingException {
        this.lastPush = lastCheckedDisplay;
    }

    public long getLastUpdate() {
        return Math.max(getLastChecked(), getLastPush());
    }

    public FolderClass getDisplayClass() {
        return FolderClass.NO_CLASS;
    }

    public FolderClass getSyncClass() {
        return getDisplayClass();
    }
    public FolderClass getPushClass() {
        return getSyncClass();
    }

    public void refresh(Preferences preferences) throws MessagingException {

    }

    public boolean isInTopGroup() {
        return false;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) throws MessagingException {
        this.status = status;
    }

    public Account getAccount() {
        return mAccount;
    }

    public List<Message> search(String queryString, final Flag[] requiredFlags, final Flag[] forbiddenFlags)
        throws MessagingException {
        throw new MessagingException("K-9 does not support searches on this folder type");
    }
}
