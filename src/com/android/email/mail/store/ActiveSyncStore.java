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
import com.android.email.mail.internet.MimeMessage;
import com.android.email.mail.internet.protocol.ActiveSyncProtocol;

public class ActiveSyncStore extends Store {
    ActiveSyncProtocol protocol;
    
    public ActiveSyncStore(String _uri) throws MessagingException {
        try {
            protocol = new ActiveSyncProtocol(_uri);
        } catch (ProtocolException pe) {
            Log.e(Email.LOG_TAG, "ProtocolException instantiating ActiveSyncProtocol");
            throw new MessagingException("ProtocolException instantiating ActiveSyncProtocol: " + pe);
        }
    }

    public Folder getFolder(String name) throws MessagingException {
        Folder folder = new ActiveSyncFolder();

        return folder;
    }

    public Folder[] getPersonalNamespaces() throws MessagingException {
        Folder[] folders = new ActiveSyncFolder[0];

        return folders;
    }

    public void checkSettings() throws MessagingException {

    }

    /** ActiveSyncFolder */
    public class ActiveSyncFolder extends Folder {
        public void open(OpenMode mode) throws MessagingException {
            return;
        }
        
        public void close(boolean expunge) throws MessagingException {
            return;
        }

        public boolean isOpen() {
            return false;
        }
        
        public OpenMode getMode() throws MessagingException {
            return OpenMode.READ_WRITE;
        }
        
        public boolean create(FolderType type) throws MessagingException {
            return false;
        }
        
        public boolean exists() throws MessagingException {
            return false;
        }
        
        public int getMessageCount() throws MessagingException {
            return 0;
        }
        
        public int getUnreadMessageCount() throws MessagingException {
            return 0;
        }
        
        public Message getMessage(String uid) throws MessagingException {
            Message message = new MimeMessage();

            return message;
        }
        
        public Message[] getMessages(int start, int end, MessageRetrievalListener listener)
            throws MessagingException {
            Message[] messages = new Message[0];

            return messages;
        }
        
        public Message[] getMessages(MessageRetrievalListener listener)
            throws MessagingException {
            Message[] messages = new Message[0];

            return messages;
        }
        
        public Message[] getMessages(String[] uids, MessageRetrievalListener listener)
            throws MessagingException {
            Message[] messages = new Message[0];

            return messages;
        }
        
        public void appendMessages(Message[] messages) throws MessagingException {
            return;
        }
        
        public void setFlags(Message[] messages, Flag[] flags, boolean value)
            throws MessagingException {
            return;
        }
        
        public void setFlags(Flag[] flags, boolean value) throws MessagingException {
            return;
        }
        
        public String getUidFromMessageId(Message message) throws MessagingException {
            String uid = new String();

            return uid;
        }
        
        public Message[] expunge() throws MessagingException {
            Message[] messages = new Message[0];
            return messages;
        }
        
        public void fetch(Message[] messages,
                          FetchProfile fp,
                          MessageRetrievalListener listener) throws MessagingException {
            return;
        }
        
        public void delete(boolean recurse) throws MessagingException {
            return;
        }
        
        public String getName() {
            String name = new String();

            return name;
        }
        
        public Flag[] getPermanentFlags() throws MessagingException {
            Flag[] flags = new Flag[0];

            return flags;
        }
    }
}
