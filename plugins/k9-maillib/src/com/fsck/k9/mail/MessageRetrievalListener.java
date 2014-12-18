
package com.fsck.k9.mail;


public interface MessageRetrievalListener<T extends Message> {
    public void messageStarted(String uid, int number, int ofTotal);

    public void messageFinished(T message, int number, int ofTotal);

    /**
     * FIXME <strong>this method is almost never invoked by various Stores! Don't rely on it unless fixed!!</strong>
     *
     * @param total
     */
    public void messagesFinished(int total);
}
