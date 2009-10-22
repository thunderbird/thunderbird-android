package com.android.email.mail;

import java.util.List;

public interface PushReceiver
{
    public void pushInProgress();
    public void pushComplete();
    public void messagesArrived(String folderName, List<Message> mess);
    public void messagesFlagsChanged(String folderName, List<Message> mess);
    public String getPushState(String folderName);
    public void pushError(String errorMessage, Exception e);
    public void setPushActive(String folderName, boolean enabled);
}
