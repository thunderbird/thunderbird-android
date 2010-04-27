package com.fsck.k9.mail;

import java.util.List;

public interface PushReceiver
{
    public void acquireWakeLock();
    public void releaseWakeLock();
    public void syncFolder(Folder folder);
    public void messagesArrived(Folder folder, List<Message> mess);
    public void messagesFlagsChanged(Folder folder, List<Message> mess);
    public String getPushState(String folderName);
    public void pushError(String errorMessage, Exception e);
    public void setPushActive(String folderName, boolean enabled);
    public void sleep(long millis);
}
