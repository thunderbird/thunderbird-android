package com.fsck.k9.mail;

import java.util.List;

import com.fsck.k9.mail.power.TracingPowerManager.TracingWakeLock;

import android.content.Context;

public interface PushReceiver {
    Context getContext();
    void syncFolder(Folder folder);
    void messagesArrived(Folder folder, List<Message> mess);
    void messagesFlagsChanged(Folder folder, List<Message> mess);
    void messagesRemoved(Folder folder, List<Message> mess);
    String getPushState(String folderName);
    void pushError(String errorMessage, Exception e);
    void setPushActive(String folderName, boolean enabled);
    void sleep(TracingWakeLock wakeLock, long millis);
}
