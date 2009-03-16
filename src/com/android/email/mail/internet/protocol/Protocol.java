package com.android.email.mail.internet.protocol;

import com.android.email.mail.FetchProfile;
import com.android.email.mail.Flag;
import com.android.email.mail.Folder;
import com.android.email.mail.Message;
import com.android.email.mail.MessageRetrievalListener;
import com.android.email.mail.Store;
import com.android.email.mail.Folder.FolderType;

/**
 * This class represents the base object for protocols.
 * The model is that each "command" supported in any protocol has
 * a stub function body here.  Any protocols that support the command
 * implement an overridden function and will return true on a call to
 * isCommandSupported(command).  This allows for a Store to conditionally
 * implement behavior without needing specific knowledge of the underlying
 * protocol.
 *
 * @version .1
 * @author Matthew Brace
 */
public abstract class Protocol {
    abstract boolean isCommandSupported(String command);

    /**
     * Always return true if it's not implemented elsewhere since this verifies
     * that the connection can be successful.
     */
    public boolean checkSettings() {
        return true;
    }

    /**
     * Return an empty array of Folder objects since it cannot be populated here.
     */
    public Folder[] getFolderHierarchy(Store store) {
        Folder[] folders = new Folder[0];

        return folders;
    }

    public void openFolder(Folder folder) {

    }

    public void closeFolder(Folder folder, boolean expunge) {

    }

    public int getMessageCount(Folder folder, boolean isRead) {
        return 0;
    }

    public boolean createFolder(Folder folder, FolderType type) {
        return false;
    }

    /**
     * Return true here since a false may result in a call to create the folder and we should
     * assume that if it had something to hand to us, and the protocol doesn't support it, true is the
     * best answer
     */
    public boolean checkFolderExistence(Folder folder) {
        return true;
    }

    public Message[] getMessagesByRange(Folder folder, int start, int end, MessageRetrievalListener listener) {
        Message[] messages = new Message[0];

        return messages;
    }

    public Message[] getMessagesByList(Folder folder, String[] uids, MessageRetrievalListener listener) {
        Message[] messages = new Message[0];

        return messages;
    }

    public void appendMessages(Folder folder, Message[] messages) {
        return;
    }

    public void setMessageFlags(Folder folder, Message[] messages, Flag[] flags, boolean value) {
        return;
    }

    public String getUidFromMessageId(Folder folder, Message message) {
        return new String();
    }

    public Message[] expunge(Folder folder) {
        return new Message[0];
    }

    public void fetch(Folder folder,
                      Message[] messages,
                      FetchProfile fp,
                      MessageRetrievalListener listener) {
        return;
    }

    public void deleteFolder(Folder folder, boolean recurse) {
        return;
    }
}
