package com.fsck.k9.backend.api;


import com.fsck.k9.mail.Folder;


public interface RemoteMessageStore {
    // TODO: Add a way to cancel the sync process
    void sync(String folder, SyncListener listener, Folder providedRemoteFolder);
}
