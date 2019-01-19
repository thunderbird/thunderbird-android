
package com.fsck.k9.mail;


public interface MessageRetrievalListener<T extends Message> {
    void messageStarted(String uid, int number, int ofTotal);

    void messageFinished(T message, int number, int ofTotal);

    /**
     * FIXME <strong>this method is almost never invoked by various Stores! Don't rely on it unless fixed!!</strong>
     */
    void messagesFinished(int total);
}
