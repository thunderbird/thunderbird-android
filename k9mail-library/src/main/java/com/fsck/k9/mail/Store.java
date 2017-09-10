
package com.fsck.k9.mail;


import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


/**
 * Store is the access point for an email message store. It's location can be
 * local or remote and no specific protocol is defined. Store is intended to
 * loosely model in combination the JavaMail classes javax.mail.Store and
 * javax.mail.Folder along with some additional functionality to improve
 * performance on mobile devices. Implementations of this class should focus on
 * making as few network connections as possible.
 */
public abstract class Store {
    /**
     * Retrieve a Folder by id. This should not perform a network request.
     *
     * @param folderId The folder id
     * @return A {@link Folder} that may or may not exist.
     */
    @NonNull public abstract Folder<? extends Message> getFolder(String folderId);

    /**
     * Request a list of folders. This can perform a network request.
     */
    @NonNull public abstract List <? extends Folder > getPersonalNamespaces(boolean forceListAll) throws MessagingException;

    /**
     * Verify that the settings provided for the store can be used succesfuly.
     * @throws MessagingException If the settings are invalid
     */
    public abstract void checkSettings() throws MessagingException;

    /**
     * @return whether the store can copy messages.
     */
    public boolean isCopyCapable() {
        return false;
    }

    /**
     * @return whether the store can move messages.
     */
    public boolean isMoveCapable() {
        return false;
    }

    /**
     * @return whether the store supports push notifications
     */
    public boolean isPushCapable() {
        return false;
    }

    /**
     * @return whether the store can send messages.
     */
    public boolean isSendCapable() {
        return false;
    }

    /**
     * @return whether the store can expunge messages.
     */
    public boolean isExpungeCapable() {
        return false;
    }

    /**
     * @return whether the store supports marking messages as Seen.
     */
    public boolean isSeenFlagSupported() {
        return true;
    }

    /**
     * Send a series of messages (network request expected).
     */
    public void sendMessages(List<? extends Message> messages) throws MessagingException { }

    /**
     * Provide a {@link Pusher} to set-up receiving push notifications from the store
     * @param receiver Something to receive push notifications on
     * @return null if the store does not support push.
     */
    @Nullable public Pusher getPusher(PushReceiver receiver) {
        return null;
    }
}
