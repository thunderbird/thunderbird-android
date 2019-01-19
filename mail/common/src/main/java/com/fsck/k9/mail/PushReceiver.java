package com.fsck.k9.mail;


import java.util.List;

import com.fsck.k9.mail.power.WakeLock;


public interface PushReceiver {
    void syncFolder(Folder folder);
    void messagesArrived(Folder folder, List<Message> mess);
    void messagesFlagsChanged(Folder folder, List<Message> mess);
    void messagesRemoved(Folder folder, List<Message> mess);
    String getPushState(String folderServerId);
    void pushError(String errorMessage, Exception e);
    void authenticationFailed();
    void setPushActive(String folderServerId, boolean enabled);
    void sleep(WakeLock wakeLock, long millis);
}
