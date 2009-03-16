package com.android.email.mail.store;

import android.util.Log;

import com.android.email.Email;
import com.android.email.mail.FetchProfile;
import com.android.email.mail.Flag;
import com.android.email.mail.Folder;
import com.android.email.mail.Message;
import com.android.email.mail.MessageRetrievalListener;
import com.android.email.mail.MessagingException;
import com.android.email.mail.ProtocolException;
import com.android.email.mail.Store;
import com.android.email.mail.Folder.FolderType;
import com.android.email.mail.internet.MimeMessage;
import com.android.email.mail.internet.protocol.ActiveSyncProtocol;

public class ActiveSyncStore extends Store {
    ActiveSyncProtocol protocol;

    private static final Flag[] PERMANENT_FLAGS = { Flag.DELETED, Flag.SEEN, Flag.ANSWERED };
    
    public ActiveSyncStore(String _uri) throws MessagingException {
        try {
            protocol = new ActiveSyncProtocol(_uri);
        } catch (ProtocolException pe) {
            Log.e(Email.LOG_TAG, "ProtocolException instantiating ActiveSyncProtocol");
            throw new MessagingException("ProtocolException instantiating ActiveSyncProtocol: " + pe);
        }
    }

    public Folder getFolder(String name) throws MessagingException {
        Folder folder = new ActiveSyncFolder(name);

        return folder;
    }

    public Folder[] getPersonalNamespaces() throws MessagingException {
        Folder[] folders;

        /** getFolderHierarchy is required for normal functionality, so throw an Exception if it's not supported */
        if (!protocol.isCommandSupported("getFolderHierarchy")) {
            throw new MessagingException("getFolderHierarchy not supported by ActiveSyncProtocol");
        } 

        folders = protocol.getFolderHierarchy(this);

        return folders;
    }

    public void checkSettings() throws MessagingException {
        if (!protocol.isCommandSupported("checkSettings")) {
            Log.e(Email.LOG_TAG, "checkSettings() command not supported in ActiveSyncProtocol");
        } else {
            if (!protocol.checkSettings()) {
                throw new MessagingException("checkSettings() failed in ActiveSyncStore");
            }
        }
    }

    /**
     * Populates a folder with all of its information necessary to open the folder and
     * opens the folder if it is supported.
     */
    public void openFolder(ActiveSyncFolder folder) throws MessagingException {
        if (protocol.isCommandSupported("openFolder")) {
            protocol.openFolder(folder);
        }
    }

    /**
     * Closes a folder if the protocol supports it.
     */
    public void closeFolder(ActiveSyncFolder folder, boolean expunge) {
        if (protocol.isCommandSupported("closeFolder")) {
            protocol.closeFolder(folder, expunge);
        }
    }

    /**
     * Retrieves the number of messages in a folder based on the supplied read status
     * if supported by the protocol.
     */
    public int getMessageCount(ActiveSyncFolder folder, boolean isRead) {
        int messageCount = 0;
        
        if (protocol.isCommandSupported("getMessageCount")) {
            messageCount = protocol.getMessageCount(folder, isRead);
        }

        return messageCount;
    }

    /**
     * Creates a folder if supported by the protocol
     */
    public boolean createFolder(ActiveSyncFolder folder, FolderType type) {
        boolean result = false;

        if (protocol.isCommandSupported("createFolder")) {
            result = protocol.createFolder(folder, type);
        }

        return result;
    }

    /**
     * Checks whether the folder exists on the source the protocol supports.
     */
    public boolean checkFolderExistence(ActiveSyncFolder folder) {
        boolean result = false;

        if (protocol.isCommandSupported("checkFolderExistenc")) {
            result = protocol.checkFolderExistence(folder);
        }

        return result;
    }

    public Message[] getMessagesByRange(Folder folder, int start, int end, MessageRetrievalListener listener) {
        Message[] messages = new Message[0];

        if (protocol.isCommandSupported("getMessagesByRange")) {
            messages = protocol.getMessagesByRange(folder, start, end, listener);
        }

        return messages;
    }

    public Message[] getMessagesByList(Folder folder, String[] uids, MessageRetrievalListener listener) {
        Message[] messages = new Message[0];

        if (protocol.isCommandSupported("getMessagesByList")) {
            messages = protocol.getMessagesByList(folder, uids, listener);
        }

        return messages;
    }

    public void appendMessages(Folder folder, Message[] messages) {
        if (protocol.isCommandSupported("appendMessages")) {
            protocol.appendMessages(folder, messages);
        } else {
            Log.e(Email.LOG_TAG, "appendMessages not supported for ActiveSyncProtocol");
        }
    }

    public void setMessageFlags(Folder folder, Message[] messages, Flag[] flags, boolean value) {
        if (protocol.isCommandSupported("setMessageFlags")) {
            protocol.setMessageFlags(folder, messages, flags, value);
        } else {
            Log.e(Email.LOG_TAG, "setMessageFlags not supported for ActiveSyncProtocol");
        }
    }

    public String getUidFromMessageId(Folder folder, Message message) {
        String uid = new String();

        if (protocol.isCommandSupported("getUidFromMessageId")) {
            uid = protocol.getUidFromMessageId(folder, message);
        }

        return uid;
    }

    public Message[] expunge(Folder folder) {
        Message[] messages = new Message[0];

        if (protocol.isCommandSupported("expunge")) {
            messages = protocol.expunge(folder);
        }

        return messages;
    }

    public void fetch(Folder folder,
                      Message[] messages,
                      FetchProfile fp,
                      MessageRetrievalListener listener) throws MessagingException {
        if (!protocol.isCommandSupported("fetch")) {
            throw new MessagingException("fetch is not supported by ActiveSyncProtocol");
        }

        protocol.fetch(folder, messages, fp, listener);
    }

    public void deleteFolder(Folder folder, boolean recurse) {
        if (protocol.isCommandSupported("deleteFolder")) {
            protocol.deleteFolder(folder, recurse);
        }
    }
    
    /** ActiveSyncFolder */
    public class ActiveSyncFolder extends Folder {
        private String mName;
        private boolean mIsOpen = false;
        private String mFolderUid;
        private int mMessageCount = 0;
        private int mUnreadMessageCount = 0;

        /** Two pieces of data are needed to uniquely identify a folder for ActiveSync, folder name and folder uid.
         * It can be unique identified by just the UID, but only the protocol will know about a uid, so two
         * constructors are needed depending on what is instantiating the object.
         */
        public ActiveSyncFolder(String name) {
            mName = name;
        }

        public ActiveSyncFolder(String name, String uid) {
            mName = name;
            mFolderUid = uid;
        }

        /**
         * Sets the folder uid.  Accessor is provided for populating the data in case of a situation where it
         * wasn't known at creation time.
         */
        public void setUid(String uid) {
            if (uid != null) {
                mFolderUid = uid;
            }
        }

        @Override
        public void open(OpenMode mode) throws MessagingException {
            if (mFolderUid == null &&
                mIsOpen == false) {
                ActiveSyncStore.this.openFolder(this);
            } else {
                mIsOpen = true;
            }
            
            return;
        }

        @Override
        public void close(boolean expunge) throws MessagingException {
            mIsOpen = false;
            ActiveSyncStore.this.closeFolder(this, expunge);
            return;
        }

        @Override
        public boolean isOpen() {
            return mIsOpen;
        }

        @Override
        public OpenMode getMode() throws MessagingException {
            return OpenMode.READ_WRITE;
        }

        @Override
        public boolean create(FolderType type) throws MessagingException {
            boolean result = ActiveSyncStore.this.createFolder(this, type);
            return result;
        }

        @Override
        public boolean exists() throws MessagingException {
            boolean result = ActiveSyncStore.this.checkFolderExistence(this);
            return result;
        }

        @Override
        public int getMessageCount() throws MessagingException {
            int messageCount = ActiveSyncStore.this.getMessageCount(this, true);
            mMessageCount = messageCount;
            return mMessageCount;
        }

        @Override
        public int getUnreadMessageCount() throws MessagingException {
            int messageCount = ActiveSyncStore.this.getMessageCount(this, false);
            mMessageCount = messageCount;
            return mMessageCount;
        }

        @Override
        public Message getMessage(String uid) throws MessagingException {
            Message message = new ActiveSyncMessage(uid);

            return message;
        }

        @Override
        public Message[] getMessages(int start, int end, MessageRetrievalListener listener)
            throws MessagingException {
            Message[] messages = ActiveSyncStore.this.getMessagesByRange(this, start, end, listener);

            return messages;
        }

        @Override
        public Message[] getMessages(MessageRetrievalListener listener)
            throws MessagingException {
            return getMessages(null, listener);
        }

        @Override
        public Message[] getMessages(String[] uids, MessageRetrievalListener listener)
            throws MessagingException {
            Message[] messages = ActiveSyncStore.this.getMessagesByList(this, uids, listener);

            return messages;
        }

        @Override
        public void appendMessages(Message[] messages) throws MessagingException {
            ActiveSyncStore.this.appendMessages(this, messages);
            return;
        }

        @Override
        public void setFlags(Message[] messages, Flag[] flags, boolean value)
            throws MessagingException {
            ActiveSyncStore.this.setMessageFlags(this, messages, flags, value);
            return;
        }

        @Override
        public void setFlags(Flag[] flags, boolean value) throws MessagingException {
            ActiveSyncStore.this.setMessageFlags(this, null, flags, value);
            return;
        }

        @Override
        public String getUidFromMessageId(Message message) throws MessagingException {
            String uid = ActiveSyncStore.this.getUidFromMessageId(this, message);

            return uid;
        }

        @Override
        public Message[] expunge() throws MessagingException {
            Message[] messages = ActiveSyncStore.this.expunge(this);
            return messages;
        }

        @Override
        public void fetch(Message[] messages,
                          FetchProfile fp,
                          MessageRetrievalListener listener) throws MessagingException {
            ActiveSyncStore.this.fetch(this, messages, fp, listener);
            return;
        }

        @Override
        public void delete(boolean recurse) throws MessagingException {
            ActiveSyncStore.this.deleteFolder(this, recurse);
            return;
        }
        
        public String getName() {
            return mName;
        }

        @Override
        public Flag[] getPermanentFlags() throws MessagingException {
            return PERMANENT_FLAGS;
        }
    }

    public class ActiveSyncMessage extends MimeMessage {
        public ActiveSyncMessage(String uid) {
            this.mUid = uid;
        }
    }
}
