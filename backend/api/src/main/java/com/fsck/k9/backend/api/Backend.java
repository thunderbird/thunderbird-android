package com.fsck.k9.backend.api;


import com.fsck.k9.mail.Folder;


public interface Backend {
    // TODO: Add a way to cancel the sync process
    void sync(String folder, SyncConfig syncConfig, SyncListener listener, Folder providedRemoteFolder);
}
