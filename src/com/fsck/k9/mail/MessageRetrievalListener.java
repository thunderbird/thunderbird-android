
package com.fsck.k9.mail;

public interface MessageRetrievalListener
{
    public void messageStarted(String uid, int number, int ofTotal);

    public void messageFinished(Message message, int number, int ofTotal);

    public void messagesFinished(int total);
}
