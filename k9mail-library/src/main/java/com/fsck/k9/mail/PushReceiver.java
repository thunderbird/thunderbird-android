package com.fsck.k9.mail;

import android.content.Context;

import com.fsck.k9.mail.power.TracingPowerManager.TracingWakeLock;

public interface PushReceiver {
    Context getContext();
    void syncFolder(String folderName);
    String getPushState(String folderName);
    void pushError(String errorMessage, Exception e);
    void authenticationFailed();
    void setPushActive(String folderName, boolean enabled);
    void sleep(TracingWakeLock wakeLock, long millis);
}
