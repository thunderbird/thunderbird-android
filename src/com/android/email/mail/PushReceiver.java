package com.android.email.mail;

import java.util.Collection;

public interface PushReceiver
{
    public void pushInProgress();
    public void pushComplete();
    public void messagesArrived(String folderName, Collection<Message> mess);
    public void messagesDeleted(String folderName, Collection<String> messageUids);
    public void pushError(String errorMessage, Exception e);
}
